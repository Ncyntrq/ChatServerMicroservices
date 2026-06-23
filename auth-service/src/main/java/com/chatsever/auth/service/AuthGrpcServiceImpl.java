package com.chatsever.auth.service;

import com.chatsever.auth.dto.AuthResponse;
import com.chatsever.grpc.auth.*;
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
            responseObserver.onNext(ValidateTokenResponse.newBuilder()
                    .setValid(true)
                    .setUsername(response.getUsername() != null ? response.getUsername() : "")
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.warn("gRPC validateToken failed: {}", e.getMessage());
            responseObserver.onNext(ValidateTokenResponse.newBuilder()
                    .setValid(false)
                    .setError(e.getMessage() != null ? e.getMessage() : "Unknown error")
                    .build());
            responseObserver.onCompleted();
        }
    }
}
