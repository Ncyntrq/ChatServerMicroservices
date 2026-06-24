package com.chatsever.notification.adapter;

import com.chatsever.grpc.server.GetServerDetailsRequest;
import com.chatsever.grpc.server.GetServerDetailsResponse;
import com.chatsever.grpc.server.ServerInfoServiceGrpc;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ServerInfoGrpcAdapter {

    @GrpcClient("server-service")
    private ServerInfoServiceGrpc.ServerInfoServiceBlockingStub serverServiceClient;

    @CircuitBreaker(name = "default")
    @Retry(name = "default")
    public List<String> getServerMembers(Long serverId) {
        GetServerDetailsRequest request = GetServerDetailsRequest.newBuilder()
                .setServerId(serverId)
                .build();
        GetServerDetailsResponse response = serverServiceClient.getServerDetails(request);
        return response.getMembersList().stream()
                .map(com.chatsever.grpc.server.MemberDetails::getUserId)
                .collect(Collectors.toList());
    }
}
