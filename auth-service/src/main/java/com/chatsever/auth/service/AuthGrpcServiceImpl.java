package com.chatsever.auth.service;

import com.chatsever.grpc.auth.AuthServiceGrpc;
import com.chatsever.grpc.auth.ValidateTokenRequest;
import com.chatsever.grpc.auth.ValidateTokenResponse;
import com.chatsever.common.dto.AuthResponse;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AuthGrpcServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {

    private final AuthService authService;

    @Override
    public void validateToken(ValidateTokenRequest request, StreamObserver<ValidateTokenResponse> responseObserver) {
        try {
            AuthResponse response = authService.validateToken(request.getToken());
            if (response != null && response.getUsername() != null) {
                responseObserver.onNext(ValidateTokenResponse.newBuilder()
                        .setValid(true)
                        .setUsername(response.getUsername())
                        .build());
            } else {
                responseObserver.onNext(ValidateTokenResponse.newBuilder()
                        .setValid(false)
                        .setError("Invalid token")
                        .build());
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Token validation error: ", e);
            responseObserver.onNext(ValidateTokenResponse.newBuilder()
                    .setValid(false)
                    .setError(e.getMessage() != null ? e.getMessage() : "Unknown error")
                    .build());
            responseObserver.onCompleted();
        }
    }
}

