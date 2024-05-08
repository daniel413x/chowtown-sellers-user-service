package com.user.handlers;

import com.user.dto.UserDto;
import com.user.dto.UserPATCHReq;
import com.user.dto.UserPOSTReq;
import com.user.model.User;
import com.user.repository.UserRepository;
import com.user.utils.ValidationHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Component
public class UserRoutesHandler {

    private UserRepository userRepository;
    private ReactiveJwtDecoder jwtDecoder;
    private ValidationHandler validationHandler;

    @Autowired
    public UserRoutesHandler(UserRepository userRepository, ReactiveJwtDecoder jwtDecoder, ValidationHandler validationHandler) {
        this.userRepository = userRepository;
        this.jwtDecoder = jwtDecoder;
        this.validationHandler = validationHandler;
    }

    public UserRoutesHandler() {}

    public Mono<ServerResponse> getById(ServerRequest req) {
        String id = req.pathVariable("id");
        String authorizationHeader = req.headers().firstHeader("Authorization");
        return userRepository.findByAuth0Id(id)
                .flatMap(user -> this.getAuth0IdFromToken(authorizationHeader)
                        .flatMap(decodedAuth0Id -> {
                            if (!decodedAuth0Id.equals(user.getAuth0Id())) {
                                return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Credentials mismatch"));
                            }
                            return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(convertToDto(user));
                        }))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")));
    }

    public Mono<ServerResponse> create(ServerRequest req) {
        return req.bodyToMono(UserPOSTReq.class)
                .flatMap(userPostReq -> {
                    this.validationHandler.validate(userPostReq, "userPostReq");
                    return userRepository.findByAuth0Id(userPostReq.getAuth0Id())
                            .flatMap(existingUser -> ServerResponse.ok().bodyValue(convertToDto(existingUser)))
                            .switchIfEmpty(Mono.defer(() -> {
                                User newUser = new User();
                                newUser.setAuth0Id(userPostReq.getAuth0Id());
                                newUser.setEmail(userPostReq.getEmail());
                                return userRepository.save(newUser)
                                        .flatMap(savedUser -> ServerResponse.status(HttpStatus.CREATED).bodyValue(convertToDto(savedUser)));
                            }));
                });
    }

    public Mono<ServerResponse> patch(ServerRequest req) {
        String id = req.pathVariable("id");
        String authorizationHeader = req.headers().firstHeader("Authorization");
        return userRepository.findByAuth0Id(id)
                .flatMap(user -> this.getAuth0IdFromToken(authorizationHeader)
                        .flatMap(decodedAuth0Id -> {
                            if (!decodedAuth0Id.equals(user.getAuth0Id())) {
                                return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Credentials mismatch"));
                            }
                            return req.bodyToMono(UserPATCHReq.class)
                                    .flatMap(userPutReq -> {
                                        this.validationHandler.validate(userPutReq, "userPutReq");
                                        user.setName(userPutReq.getName());
                                        return userRepository.save(user)
                                                .flatMap(updatedUser -> ServerResponse.ok()
                                                        .bodyValue(convertToDto(updatedUser)));
                                    });
                        }))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")));
    }

    private UserDto convertToDto(User user) {
        return new UserDto(user);
    }

    private Mono<String> getAuth0IdFromToken(String authorizationHeader) {
        String tokenValue = authorizationHeader.replace("Bearer ", "");
        return jwtDecoder.decode(tokenValue)
                .map(j -> j.getClaimAsString("sub"));
    }
}
