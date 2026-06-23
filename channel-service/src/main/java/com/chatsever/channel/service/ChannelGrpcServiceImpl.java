package com.chatsever.channel.service;

import com.chatsever.common.dto.ChannelDto;
import com.chatsever.grpc.channel.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class ChannelGrpcServiceImpl extends ChannelServiceGrpc.ChannelServiceImplBase {

    private final ChannelService channelService;

    @Override
    public void getChannelsByServerId(GetChannelsRequest request, StreamObserver<GetChannelsResponse> responseObserver) {
        try {
            List<ChannelDto> channels = channelService.getChannelsByServerId(request.getServerId());
            GetChannelsResponse.Builder builder = GetChannelsResponse.newBuilder();

            for (ChannelDto c : channels) {
                builder.addChannels(ChannelResponse.newBuilder()
                        .setId(c.getId() != null ? c.getId() : 0)
                        .setServerId(c.getServerId() != null ? c.getServerId() : 0)
                        .setName(c.getName() != null ? c.getName() : "")
                        .setType(c.getType() != null ? c.getType() : "")
                        .build());
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void createChannel(CreateChannelRequest request, StreamObserver<ChannelResponse> responseObserver) {
        try {
            com.chatsever.channel.dto.ChannelRequest req = new com.chatsever.channel.dto.ChannelRequest();
            req.setServerId(request.getServerId());
            req.setName(request.getName());
            req.setType(request.getType());

            ChannelDto created = channelService.createChannel(req, request.getUserId());

            responseObserver.onNext(ChannelResponse.newBuilder()
                    .setId(created.getId() != null ? created.getId() : 0)
                    .setServerId(created.getServerId() != null ? created.getServerId() : 0)
                    .setName(created.getName() != null ? created.getName() : "")
                    .setType(created.getType() != null ? created.getType() : "")
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void deleteChannelsByServerId(DeleteChannelsRequest request, StreamObserver<EmptyChannelResponse> responseObserver) {
        try {
            channelService.deleteChannelsByServerId(request.getServerId());
            responseObserver.onNext(EmptyChannelResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void removePinnedMessage(RemovePinnedMessageRequest request, StreamObserver<EmptyChannelResponse> responseObserver) {
        try {
            channelService.unpinMessage(request.getChannelId(), request.getMessageId(), request.getUserId());
            responseObserver.onNext(EmptyChannelResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}

