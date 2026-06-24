package com.chatsever.channel.adapter;

import com.chatsever.grpc.role.GetPermissionsRequest;
import com.chatsever.grpc.role.GetPermissionsResponse;
import com.chatsever.grpc.role.RoleServiceGrpc;
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
    public GetPermissionsResponse getPermissions(GetPermissionsRequest request) {
        return roleServiceClient.getPermissions(request);
    }
}
