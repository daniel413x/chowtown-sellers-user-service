package com.user.router;

import com.user.handlers.UserRoutesHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration(proxyBeanMethods = false)
public class UserRouter {

    @Bean
    public RouterFunction<ServerResponse> userRoutes(UserRoutesHandler userRoutesHandler) {
        return RouterFunctions
                .route()
                .nest(RequestPredicates.path("/api"), builder -> {
                    builder.POST("", userRoutesHandler::create);
                    builder.GET("/{id}", userRoutesHandler::getById);
                    builder.PATCH("/{id}", userRoutesHandler::patch);
                })
                .build();
    }
}
