package com.chatsever.messaging.adapter;

import com.chatsever.grpc.auth.AuthServiceGrpc;
import com.chatsever.grpc.auth.ValidateTokenRequest;
import com.chatsever.grpc.auth.ValidateTokenResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class AuthGrpcAdapter {

    @GrpcClient("auth-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authServiceClient;

    @CircuitBreaker(name = "default")
    @Retry(name = "default")
    public ValidateTokenResponse validateToken(String token) {
        ValidateTokenRequest request = ValidateTokenRequest.newBuilder()
                .setToken(token)
                .build();
        return authServiceClient.validateToken(request);
    }
}
