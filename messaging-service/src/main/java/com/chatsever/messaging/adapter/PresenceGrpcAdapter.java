package com.chatsever.messaging.adapter;

import com.chatsever.grpc.presence.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class PresenceGrpcAdapter {

    @GrpcClient("presence-service")
    private PresenceServiceGrpc.PresenceServiceBlockingStub presenceServiceClient;

    @CircuitBreaker(name = "default")
    @Retry(name = "default")
    public EmptyPresenceResponse notifyPresence(NotifyPresenceRequest request) {
        return presenceServiceClient.notifyPresence(request);
    }
}
