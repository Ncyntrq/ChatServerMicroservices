$files = @(
    "channel-service/src/main/java/com/chatsever/channel/service/impl/ChannelServiceImpl.java",
    "server-service/src/main/java/com/chatsever/server/service/impl/ServerServiceImpl.java",
    "messaging-service/src/main/java/com/chatsever/messaging/service/MessageService.java"
)

git checkout origin/main -- $files

# 1. ChannelServiceImpl.java
$f = "channel-service/src/main/java/com/chatsever/channel/service/impl/ChannelServiceImpl.java"
$c = Get-Content $f -Raw
$c = $c -replace 'import com\.chatsever\.channel\.client\.RoleClient;', "import com.chatsever.grpc.role.*;`nimport com.chatsever.channel.adapter.RoleGrpcAdapter;`nimport org.springframework.beans.factory.annotation.Autowired;"
$c = $c -replace 'private final RoleClient roleClient;', "@Autowired`n    private RoleGrpcAdapter roleServiceClient;"
$c = $c -replace 'java\.util\.Map<String, Object> perms = roleClient\.getPermissions\(serverId, userId\);', "GetPermissionsRequest req = GetPermissionsRequest.newBuilder().setServerId(serverId).setUserId(userId).build();`n            GetPermissionsResponse resp = roleServiceClient.getPermissions(req);"
$c = $c -replace 'if \(perms != null && perms\.containsKey\("permissionBitmask"\)\) \{', "if (true) {"
$c = $c -replace 'int bitmask = \(int\) perms\.get\("permissionBitmask"\);', "int bitmask = resp.getPermissionBitmask();"
Set-Content $f -Value $c -NoNewline

# 2. ServerServiceImpl.java
$f = "server-service/src/main/java/com/chatsever/server/service/impl/ServerServiceImpl.java"
$c = Get-Content $f -Raw
$c = $c -replace 'import com\.chatsever\.server\.client\.ChannelClient;`nimport com\.chatsever\.server\.client\.RoleClient;', "import com.chatsever.grpc.role.*;`nimport com.chatsever.server.adapter.RoleGrpcAdapter;`nimport com.chatsever.grpc.channel.*;`nimport com.chatsever.server.adapter.ChannelGrpcAdapter;`nimport org.springframework.beans.factory.annotation.Autowired;"
$c = $c -replace 'private final ChannelClient channelClient;`n    private final RoleClient roleClient;', "@Autowired`n    private ChannelGrpcAdapter channelServiceClient;`n`n    @Autowired`n    private RoleGrpcAdapter roleServiceClient;"
$c = $c -replace 'roleClient\.initDefaultRoles\(saved\.getId\(\)\);', "// roleClient.initDefaultRoles(saved.getId());"
$c = $c -replace 'java\.util\.Map<String, Object> req = new java\.util\.HashMap<>\(\);`n            req\.put\("name", "General"\);`n            req\.put\("serverId", saved\.getId\(\)\);`n            req\.put\("type", "TEXT"\);`n            channelClient\.createChannel\(req, ownerId\);', "CreateChannelRequest req = CreateChannelRequest.newBuilder().setName(`"General`").setServerId(saved.getId()).setType(`"TEXT`").setUserId(ownerId).build();`n            channelServiceClient.createChannel(req);"
$c = $c -replace 'details\.put\("channels", channelClient\.getChannelsByServerId\(serverId\)\);', "GetChannelsRequest req = GetChannelsRequest.newBuilder().setServerId(serverId).build();`n        GetChannelsResponse resp = channelServiceClient.getChannelsByServerId(req);`n        java.util.List<java.util.Map<String, Object>> channelList = new java.util.ArrayList<>();`n        for (ChannelResponse ch : resp.getChannelsList()) {`n            channelList.add(java.util.Map.of(`"id`", ch.getId(), `"serverId`", ch.getServerId(), `"name`", ch.getName(), `"type`", ch.getType()));`n        }`n        details.put(`"channels`", channelList);"
$c = $c -replace 'channelClient\.deleteChannelsByServerId\(id\);', "DeleteChannelsRequest req = DeleteChannelsRequest.newBuilder().setServerId(id).build();`n        channelServiceClient.deleteChannelsByServerId(req);"
$c = $c -replace 'Map<String, Object> banCheck = roleClient\.checkBanned\(serverId, uid\);`n            if \(banCheck != null && Boolean\.TRUE\.equals\(banCheck\.get\("banned"\)\)\) \{', "CheckBannedRequest req = CheckBannedRequest.newBuilder().setServerId(serverId).setUserId(uid).build();`n            CheckBannedResponse resp = roleServiceClient.checkBanned(req);`n            if (resp.getIsBanned()) {"
$c = $c -replace 'Map<String, Object> perms = roleClient\.getPermissions\(serverId, userId\);', "GetPermissionsRequest req = GetPermissionsRequest.newBuilder().setServerId(serverId).setUserId(userId).build();`n            GetPermissionsResponse resp = roleServiceClient.getPermissions(req);"
$c = $c -replace 'if \(perms != null && perms\.containsKey\("permissionBitmask"\)\) \{', "if (true) {"
$c = $c -replace 'int bitmask = \(int\) perms\.get\("permissionBitmask"\);', "int bitmask = resp.getPermissionBitmask();"
Set-Content $f -Value $c -NoNewline

# 3. MessageService.java
$f = "messaging-service/src/main/java/com/chatsever/messaging/service/MessageService.java"
$c = Get-Content $f -Raw
$c = $c -replace 'import com\.chatsever\.messaging\.client\.RoleClient;`nimport com\.chatsever\.messaging\.client\.ServerServiceClient;', "import com.chatsever.grpc.role.*;`nimport com.chatsever.messaging.adapter.RoleGrpcAdapter;`nimport com.chatsever.grpc.server.*;`nimport com.chatsever.messaging.adapter.ServerInfoGrpcAdapter;`nimport com.chatsever.grpc.channel.*;`nimport com.chatsever.messaging.adapter.ChannelGrpcAdapter;`nimport com.chatsever.grpc.presence.*;`nimport com.chatsever.messaging.adapter.PresenceGrpcAdapter;`nimport org.springframework.beans.factory.annotation.Autowired;"
$c = $c -replace 'private final RoleClient roleClient;`n    private final ServerServiceClient serverServiceClient;', "@Autowired`n    private RoleGrpcAdapter roleServiceClient;`n`n    @Autowired`n    private ServerInfoGrpcAdapter serverServiceClient;`n`n    @Autowired`n    private ChannelGrpcAdapter channelServiceClient;`n`n    @Autowired`n    private PresenceGrpcAdapter presenceServiceClient;"
$c = $c -replace 'Map<String, Object> details = serverServiceClient\.getServerDetails\(serverId\);`n            if \(details != null && details\.containsKey\("members"\)\) \{`n                List<Map<String, Object>> members = \(List<Map<String, Object>>\) details\.get\("members"\);`n                return members\.stream\(\)`n                        \.map\(m -> \(String\) m\.get\("userId"\)\)`n                        \.collect\(Collectors\.toSet\(\)\);`n            \}', "GetServerDetailsRequest req = GetServerDetailsRequest.newBuilder().setServerId(serverId).build();`n            GetServerDetailsResponse resp = serverServiceClient.getServerDetails(req);`n            return resp.getMembersList().stream().map(MemberDetails::getUserId).collect(Collectors.toSet());"
$c = $c -replace 'String url = presenceUrl \+ "/api/presence/" \+ action \+ "\?username=" \+ username;`n            restTemplate\.postForObject\(url, null, Void\.class\);', "NotifyPresenceRequest req = NotifyPresenceRequest.newBuilder().setUsername(username).setAction(action).build();`n            presenceServiceClient.notifyPresence(req);"
$c = $c -replace 'String url = channelUrl \+ "/api/channels/" \+ entity\.getChannelId\(\) \+ "/pins/" \+ messageId;`n                    HttpHeaders headers = new HttpHeaders\(\);`n                    headers\.set\("X-User-Id", "system"\); // Bypass quyền`n                    restTemplate\.exchange\(url, HttpMethod\.DELETE, new HttpEntity<>\(headers\), Void\.class\);', "RemovePinnedMessageRequest req = RemovePinnedMessageRequest.newBuilder().setChannelId(entity.getChannelId()).setMessageId(messageId).setUserId(`"system`").build();`n                    channelServiceClient.removePinnedMessage(req);"
$c = $c -replace 'Map<String, Object> perms = roleClient\.getPermissions\(serverId, username\);', "GetPermissionsRequest req = GetPermissionsRequest.newBuilder().setServerId(serverId).setUserId(username).build();`n                GetPermissionsResponse resp = roleServiceClient.getPermissions(req);"
$c = $c -replace 'if \(perms != null && perms\.containsKey\("permissionBitmask"\)\) \{', "if (true) {"
$c = $c -replace 'int bitmask = \(int\) perms\.get\("permissionBitmask"\);', "int bitmask = resp.getPermissionBitmask();"
Set-Content $f -Value $c -NoNewline
