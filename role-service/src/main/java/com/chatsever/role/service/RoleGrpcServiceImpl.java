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
            // Permission.toNames() trả về String "A,B,C" (không phải List) -> tách thành List cho proto
            // (repeated string permissions). Trước đây ép thẳng (List<String>) gây ClassCastException
            // -> GetPermissions luôn INTERNAL -> channel/server check quyền fail.
            Object permsObj = perms.get("permissions");
            List<String> permissions;
            if (permsObj instanceof List<?> list) {
                permissions = (List<String>) list;
            } else if (permsObj instanceof String s && !s.isBlank()) {
                permissions = java.util.Arrays.asList(s.split(","));
            } else {
                permissions = java.util.List.of();
            }
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
