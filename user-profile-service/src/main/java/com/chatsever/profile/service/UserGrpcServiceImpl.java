package com.chatsever.profile.service;

import com.chatsever.grpc.user.*;
import com.chatsever.profile.repository.UserProfileRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserGrpcServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private final UserProfileRepository userProfileRepository;

    @Override
    public void checkUserExists(CheckUserExistsRequest request, StreamObserver<CheckUserExistsResponse> responseObserver) {
        try {
            boolean exists = userProfileRepository.findByUsername(request.getUsername()).isPresent();
            responseObserver.onNext(CheckUserExistsResponse.newBuilder()
                    .setExists(exists)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error checking user existence: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}
