package com.chatsever.messaging.adapter;

import com.chatsever.grpc.channel.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class ChannelGrpcAdapter {

    @GrpcClient("channel-service")
    private ChannelServiceGrpc.ChannelServiceBlockingStub channelServiceClient;

    @CircuitBreaker(name = "default")
    @Retry(name = "default")
    public EmptyChannelResponse removePinnedMessage(RemovePinnedMessageRequest request) {
        return channelServiceClient.removePinnedMessage(request);
    }
}
