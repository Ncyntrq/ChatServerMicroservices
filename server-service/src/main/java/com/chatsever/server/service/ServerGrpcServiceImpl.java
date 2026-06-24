package com.chatsever.server.service;

import com.chatsever.grpc.server.*;
import com.chatsever.server.model.Member;
import com.chatsever.server.model.Server;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class ServerGrpcServiceImpl extends ServerInfoServiceGrpc.ServerInfoServiceImplBase {

    private final ServerService serverService;

    @Override
    @SuppressWarnings("unchecked")
    public void getServerDetails(GetServerDetailsRequest request, StreamObserver<GetServerDetailsResponse> responseObserver) {
        try {
            Map<String, Object> details = serverService.getServerDetails(request.getServerId());
            Server s = (Server) details.get("server");
            List<Member> members = (List<Member>) details.get("members");

            ServerDetails serverDetails = ServerDetails.newBuilder()
                    .setId(s.getId())
                    .setName(s.getName() != null ? s.getName() : "")
                    .setOwnerId(s.getOwnerId() != null ? s.getOwnerId() : "")
                    .setDescription(s.getDescription() != null ? s.getDescription() : "")
                    .setIcon(s.getIcon() != null ? s.getIcon() : "")
                    .setInviteCode(s.getInviteCode() != null ? s.getInviteCode() : "")
                    .build();

            GetServerDetailsResponse.Builder responseBuilder = GetServerDetailsResponse.newBuilder()
                    .setServer(serverDetails);

            if (members != null) {
                for (Member m : members) {
                    MemberDetails.Builder memberBuilder = MemberDetails.newBuilder()
                            .setUserId(m.getUserId() != null ? m.getUserId() : "");
                    if (m.getRoleIds() != null) {
                        memberBuilder.addAllRoleIds(m.getRoleIds());
                    }
                    if (m.getJoinedAt() != null) {
                        memberBuilder.setJoinedAt(m.getJoinedAt().toInstant(ZoneOffset.UTC).toEpochMilli());
                    }
                    responseBuilder.addMembers(memberBuilder.build());
                }
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void ensureMember(EnsureMemberRequest request, StreamObserver<EnsureMemberResponse> responseObserver) {
        try {
            serverService.ensureMember(request.getServerId(), request.getUserId());
            responseObserver.onNext(EnsureMemberResponse.newBuilder()
                    .setStatus("success")
                    .setMessage("Ensure member ok")
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
    public void updateMemberRoles(UpdateMemberRolesRequest request, StreamObserver<EmptyServerResponse> responseObserver) {
        try {
            serverService.updateMemberRoles(request.getServerId(), request.getUserId(), request.getRoleIdsList());
            responseObserver.onNext(EmptyServerResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void leaveServer(LeaveServerRequest request, StreamObserver<EmptyServerResponse> responseObserver) {
        try {
            serverService.leaveServer(request.getServerId(), request.getUserId());
            responseObserver.onNext(EmptyServerResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}
