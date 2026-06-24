package com.chatsever.role.service;

import com.chatsever.grpc.role.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.Map;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RoleGrpcServiceImpl extends RoleServiceGrpc.RoleServiceImplBase {

    private final RoleService roleService;

    @Override
    public void initDefaultRoles(InitDefaultRolesRequest request, StreamObserver<EmptyResponse> responseObserver) {
        try {
            roleService.createDefaultRoles(String.valueOf(request.getServerId()));
            responseObserver.onNext(EmptyResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void checkBanned(CheckBannedRequest request, StreamObserver<CheckBannedResponse> responseObserver) {
        try {
            boolean isBanned = roleService.isBanned(String.valueOf(request.getServerId()), request.getUserId());
            responseObserver.onNext(CheckBannedResponse.newBuilder()
                    .setIsBanned(isBanned)
                    .setReason("Checked by gRPC")
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
    @SuppressWarnings("unchecked")
    public void getPermissions(GetPermissionsRequest request, StreamObserver<GetPermissionsResponse> responseObserver) {
        try {
            Map<String, Object> perms = roleService.getEffectivePermissions(String.valueOf(request.getServerId()), request.getUserId());
            List<String> permissions = (List<String>) perms.get("permissions");
            String roleName = (String) perms.get("role");
            Integer bitmask = (Integer) perms.get("permissionBitmask");

            GetPermissionsResponse.Builder builder = GetPermissionsResponse.newBuilder();
            if (permissions != null) {
                builder.addAllPermissions(permissions);
            }
            if (roleName != null) {
                builder.setRoleName(roleName);
            }
            if (bitmask != null) {
                builder.setPermissionBitmask(bitmask);
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
}
