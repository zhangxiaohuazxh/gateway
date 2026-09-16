package cn.hubbo.filter;

import io.github.resilience4j.core.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class EncryptFilter implements GlobalFilter, Ordered {

    private static final String SIGN_HEADER = "Sign";

    private static final String SIGN_TYPE = "Sign-type";

    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();
        if (!headers.containsHeader(SIGN_TYPE)) {

        }
        String sign = headers.getFirst(SIGN_HEADER);
        if (!StringUtils.isNotEmpty(sign)) {
            // 拒绝服务
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 1;
    }

}
