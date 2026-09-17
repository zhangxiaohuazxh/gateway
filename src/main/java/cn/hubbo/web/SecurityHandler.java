package cn.hubbo.web;

import cn.hubbo.utils.CipherUtils;
import cn.hubbo.vo.SecurityVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
public class SecurityHandler {

    private String aes_key = null;

    public Mono<ServerResponse> computedSharedSecret(ServerRequest request) {
        return request.bodyToMono(SecurityVO.class)
                .flatMap(vo -> {
                    log.info("client pub key {}", vo.getClientPubKey());
                    SecurityVO computeSharedSecret = CipherUtils.computeSharedSecret(vo.getClientPubKey());
                    aes_key = computeSharedSecret.getSecret();
                    return ServerResponse.ok()
                            .bodyValue(Map.of("code", 200, "msg", "success", "serverPubKey", computeSharedSecret.getServerPubKey()))
                            .switchIfEmpty(ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(Map.of("code", 200, "msg", "请求体不能为空")));
                });
    }

    public Mono<ServerResponse> encryptTest(ServerRequest request) {
        return request.bodyToMono(Map.class)
                .flatMap(map -> {
                    String body = map.get("data").toString();
                    String iv = map.get("iv").toString();
                    String source = CipherUtils.decrypt(body, iv, aes_key);
                    log.info("前端传递的原始消息 {}", source);
                    Map<String, Object> encrypt = CipherUtils.encrypt(aes_key, String.format("前端传递的原始消息 %s", source));
                    return ServerResponse.ok()
                            .bodyValue(Map.of("code", 200, "msg", "success", "data", encrypt));
                }).switchIfEmpty(ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of("code", 200, "msg", "请求体不能为空")));
    }

}
