package com.chatsever.presence.service;

import com.chatsever.grpc.presence.PresenceServiceGrpc;
import com.chatsever.grpc.presence.NotifyPresenceRequest;
import com.chatsever.grpc.presence.EmptyPresenceResponse;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class PresenceGrpcServiceImpl extends PresenceServiceGrpc.PresenceServiceImplBase {

    private final PresenceService presenceService;

    @Override
    public void notifyPresence(NotifyPresenceRequest request, StreamObserver<EmptyPresenceResponse> responseObserver) {
        try {
            if ("connect".equalsIgnoreCase(request.getAction())) {
                presenceService.connect(request.getUsername());
            } else if ("disconnect".equalsIgnoreCase(request.getAction())) {
                presenceService.disconnect(request.getUsername());
            } else {
                throw new IllegalArgumentException("Unknown action: " + request.getAction());
            }
            responseObserver.onNext(EmptyPresenceResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}
