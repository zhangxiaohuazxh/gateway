package cn.hubbo.filter;

import cn.hubbo.utils.CipherUtils;
import cn.hubbo.utils.ResponseUtils;
import io.github.resilience4j.core.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static cn.hubbo.utils.CipherUtils.decrypt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpMethod.*;

@Slf4j
@Component
public class EncryptFilter implements GlobalFilter, Ordered {

	private static final String SIGN_HEADER = "X-Sign";

	private static final String SIGN_TYPE = "X-SignType";

	private static final String TIMESTAMP = "X-Timestamp";

	private static final String NONCE = "X-Nonce";

	private static final String NOT_TRUSTED_REQUEST_RESPONSE = "{\"code\":500,\"msg\":\"不被信任的外部来源请求，服务端拒绝\"}";

	private static final String BAD_REQUEST_RESPONSE = "{\"code\":500,\"msg\":\"非法请求，服务端拒绝处理\"}";

	private static final String RSA_PRIVATE_KEY = "";

	private static final String AES_KEY = "";


	@Override
	public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		if (request.getMethod() != POST &&
				request.getMethod() != PUT &&
				request.getMethod() != PATCH) {
			return chain.filter(exchange);
		}
		HttpHeaders headers = request.getHeaders();
		String sign = headers.getFirst(SIGN_HEADER);
		String signType = headers.getFirst(SIGN_TYPE);
		String timestamp = headers.getFirst(TIMESTAMP);
		String nonce = headers.getFirst(NONCE);
		if (!StringUtils.isNotEmpty(signType) || !StringUtils.isNotEmpty(sign) || !StringUtils.isNotEmpty(timestamp) || !StringUtils.isNotEmpty(nonce)) {
			log.warn("记录到一次不被信任的请求");
			return ResponseUtils.writeError(exchange, HttpStatus.UNAUTHORIZED, NOT_TRUSTED_REQUEST_RESPONSE);
		}
		// todo 防重放攻击
		// 2. 获取原始 Body 并缓存 (DataBufferUtils.join 将流合并为一个 DataBuffer)
		return DataBufferUtils.join(request.getBody())
				.flatMap(dataBuffer -> {
					try {
						// 读取原始报文
						byte[] originalBytes = new byte[dataBuffer.readableByteCount()];
						dataBuffer.read(originalBytes);
						DataBufferUtils.release(dataBuffer);
						String encryptedContent = String.format("%s%s%s", new String(originalBytes, UTF_8), timestamp, nonce);
						String serverSign = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, AES_KEY).hmacHex(encryptedContent);
						if (!sign.equalsIgnoreCase(serverSign)) {
							log.warn("MD5 校验失败, sign={}", sign);
							return ResponseUtils.writeError(exchange, HttpStatus.UNAUTHORIZED, "{\"code\":500,\"msg\":\"签名校验失败\"}");
						}
						byte[] decryptedBytes = decrypt(CipherUtils.SignType.valueOf(signType), AES_KEY, nonce, originalBytes);
						if (decryptedBytes == null) {
							return ResponseUtils.writeError(exchange, HttpStatus.BAD_REQUEST, "{\"code\":500,\"msg\":\"解密失败\"}");
						}
						// 5. 重新包装 Request (重要！)
						ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
							@Override
							public @NonNull HttpHeaders getHeaders() {
								HttpHeaders headers = new HttpHeaders();
								headers.putAll(super.getHeaders());
								// 更新 Content-Length，因为解密后长度会变
								headers.setContentLength(decryptedBytes.length);
								// 视情况修改 Content-Type
								headers.setContentType(MediaType.APPLICATION_JSON);
								headers.remove(SIGN_HEADER);
								headers.remove(SIGN_TYPE);
								return headers;
							}

							@Override
							public @NonNull Flux<DataBuffer> getBody() {
								return Flux.just(exchange.getResponse().bufferFactory().wrap(decryptedBytes));
							}
						};
						return chain.filter(exchange.mutate().request(decorator).build());
					} catch (Exception e) {
						log.error("解密过滤器处理异常", e);
						return ResponseUtils.writeError(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "{\"code\":500,\"msg\":\"网关内部错误\"}");
					}
				})
				// 🌟 关键：处理 Body 为空时的 Mono.empty() 情况
				.switchIfEmpty(chain.filter(exchange));
	}

	@Override
	public int getOrder() {
		return 1;
	}

}
