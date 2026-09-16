package cn.hubbo.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledDirectByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
public class ResponseUtils {

    private static final String DEFAULT_UNKNOWN_ERROR_RESPONSE = "{\"code\":500,\"msg\":\"系统内部错误，请稍候重试\"}";

    public static Mono<Void> writeError(@NonNull ServerWebExchange exchange, @NonNull HttpStatus status, @NonNull String body) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        DataBuffer dataBuffer = null;
        try {
            dataBuffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(dataBuffer));
        } catch (Exception e) {
            log.error("响应失败", e);
            DataBuffer buffer = response.bufferFactory().wrap(DEFAULT_UNKNOWN_ERROR_RESPONSE.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer)).doFinally(ignore -> {
                DataBufferUtils.release(buffer);
            });
        }
    }


}
