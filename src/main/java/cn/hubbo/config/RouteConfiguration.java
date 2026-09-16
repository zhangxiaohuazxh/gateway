package cn.hubbo.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import static org.springframework.cloud.gateway.support.RouteMetadataUtils.CONNECT_TIMEOUT_ATTR;
import static org.springframework.cloud.gateway.support.RouteMetadataUtils.RESPONSE_TIMEOUT_ATTR;

@Component
public class RouteConfiguration {


	@Bean
	public RouteLocator routeLocator(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("message-service", route -> route.path("/api/message/**")
						.filters(f -> f.stripPrefix(1)
								.metadata(RESPONSE_TIMEOUT_ATTR, 10_000)
								.metadata(CONNECT_TIMEOUT_ATTR, 30_000))
						// todo 后期可以配置到nacos或者数据库
						.uri("http://localhost:9999")
				)
				.build();
	}


}
