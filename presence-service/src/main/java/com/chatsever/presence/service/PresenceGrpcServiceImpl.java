package com.chatsever.presence.service;

import com.chatsever.grpc.presence.*;
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
            String username = request.getUsername();
            String action = request.getAction();
            
            if ("connect".equalsIgnoreCase(action)) {
                presenceService.connect(username);
            } else if ("disconnect".equalsIgnoreCase(action)) {
                presenceService.disconnect(username);
            } else {
                log.warn("Unknown presence action: {}", action);
            }
            
            responseObserver.onNext(EmptyPresenceResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in notifyPresence: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}
