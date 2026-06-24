package com.chatsever.friend.adapter;

import com.chatsever.grpc.user.UserServiceGrpc;
import com.chatsever.grpc.user.CheckUserExistsRequest;
import com.chatsever.grpc.user.CheckUserExistsResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class UserGrpcAdapter {

    @GrpcClient("user-profile-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceClient;

    @CircuitBreaker(name = "default")
    @Retry(name = "default")
    public boolean checkUserExists(String username) {
        CheckUserExistsRequest request = CheckUserExistsRequest.newBuilder()
                .setUsername(username)
                .build();
        CheckUserExistsResponse response = userServiceClient.checkUserExists(request);
        return response.getExists();
    }
}
