package com.chatsever.server.adapter;

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
    public ChannelResponse createChannel(CreateChannelRequest request) {
        return channelServiceClient.createChannel(request);
    }

    @CircuitBreaker(name = "default")
    @Retry(name = "default")
    public GetChannelsResponse getChannelsByServerId(GetChannelsRequest request) {
        return channelServiceClient.getChannelsByServerId(request);
    }

    @CircuitBreaker(name = "default")
    @Retry(name = "default")
    public EmptyChannelResponse deleteChannelsByServerId(DeleteChannelsRequest request) {
        return channelServiceClient.deleteChannelsByServerId(request);
    }
}
