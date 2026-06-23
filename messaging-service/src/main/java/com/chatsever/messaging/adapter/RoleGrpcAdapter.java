package com.chatsever.messaging.adapter;

import com.chatsever.grpc.role.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class RoleGrpcAdapter {

    @GrpcClient("role-service")
    private RoleServiceGrpc.RoleServiceBlockingStub roleServiceClient;

    @CircuitBreaker(name = "default")
    @Retry(name = "default")
    public CheckBannedResponse checkBanned(CheckBannedRequest request) {
        return roleServiceClient.checkBanned(request);
    }

    @CircuitBreaker(name = "default")
    @Retry(name = "default")
    public GetPermissionsResponse getPermissions(GetPermissionsRequest request) {
        return roleServiceClient.getPermissions(request);
    }
}
