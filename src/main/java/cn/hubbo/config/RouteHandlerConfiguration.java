package cn.hubbo.config;

import cn.hubbo.web.SecurityHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouteHandlerConfiguration {


    @Bean
    public RouterFunction<ServerResponse> customRoutes(SecurityHandler handler) {
        return route()
                .POST("/api/security/security/computed", handler::computedSharedSecret)
                .POST("/api/security/security/encrypt/test", handler::encryptTest)
                .build();
    }

}
