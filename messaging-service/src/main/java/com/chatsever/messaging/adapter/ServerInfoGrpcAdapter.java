package com.chatsever.messaging.adapter;

import com.chatsever.grpc.server.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class ServerInfoGrpcAdapter {

    @GrpcClient("server-service")
    private ServerInfoServiceGrpc.ServerInfoServiceBlockingStub serverInfoServiceClient;

    @CircuitBreaker(name = "default")
    @Retry(name = "default")
    public GetServerDetailsResponse getServerDetails(GetServerDetailsRequest request) {
        return serverInfoServiceClient.getServerDetails(request);
    }

    @CircuitBreaker(name = "default")
    @Retry(name = "default")
    public EnsureMemberResponse ensureMember(EnsureMemberRequest request) {
        return serverInfoServiceClient.ensureMember(request);
    }
}
