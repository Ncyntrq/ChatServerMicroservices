package gui.chat;

import gui.components.channels.ChannelSidebar;
import gui.components.friends.FriendSidebar;
import gui.components.navigation.ServerSidebar;
import network.NotificationApiClient;
import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;

import javax.swing.SwingWorker;
import java.util.HashMap;
import java.util.Map;

/** Đồng bộ số tin chưa đọc (channel/DM/server) lên các sidebar + gửi ack khi đọc. */
public class UnreadCountSync {

    private final NotificationApiClient notificationApi;
    private final ServerSidebar serverSidebar;
    private final ChannelSidebar channelSidebar;
    private final FriendSidebar friendSidebar;
    private final String sessionUsername;

    public UnreadCountSync(NotificationApiClient notificationApi, ServerSidebar serverSidebar,
                           ChannelSidebar channelSidebar, FriendSidebar friendSidebar, String sessionUsername) {
        this.notificationApi = notificationApi;
        this.serverSidebar = serverSidebar;
        this.channelSidebar = channelSidebar;
        this.friendSidebar = friendSidebar;
        this.sessionUsername = sessionUsername;
    }

    /** Tải lại toàn bộ số chưa đọc và cập nhật badge trên sidebar. */
    public void refresh() {
        new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() {
                try {
                    return notificationApi.getUnreadCounts(sessionUsername);
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Map<String, Object> resp = get();
                    if (resp == null) return;

                    if (resp.get("unreadCounts") != null) {
                        Map<String, Number> unreadMap = (Map<String, Number>) resp.get("unreadCounts");
                        Map<Long, Integer> channelCounts = new HashMap<>();
                        for (Map.Entry<String, Number> entry : unreadMap.entrySet()) {
                            channelCounts.put(Long.parseLong(entry.getKey()), entry.getValue().intValue());
                        }
                        channelSidebar.updateUnreadCounts(channelCounts);
                    }

                    if (resp.get("privateCounts") != null) {
                        Map<String, Number> privateMap = (Map<String, Number>) resp.get("privateCounts");
                        Map<String, Integer> friendCounts = new HashMap<>();
                        for (Map.Entry<String, Number> entry : privateMap.entrySet()) {
                            friendCounts.put(entry.getKey(), entry.getValue().intValue());
                        }
                        friendSidebar.updateUnreadCounts(friendCounts);
                    }

                    if (resp.get("serverCounts") != null) {
                        Map<String, Number> serverMap = (Map<String, Number>) resp.get("serverCounts");
                        Map<Long, Integer> serverCounts = new HashMap<>();
                        for (Map.Entry<String, Number> entry : serverMap.entrySet()) {
                            serverCounts.put(Long.parseLong(entry.getKey()), entry.getValue().intValue());
                        }
                        serverSidebar.updateUnreadCounts(serverCounts);
                    }
                } catch (Exception ignore) {
                    if (ignore instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }.execute();
    }

    /** Đánh dấu đã đọc 1 tin (DM hoặc channel). */
    public void ack(MessageDTO msg) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                if (msg.getType() == MessageType.PRIVATE) {
                    notificationApi.ackDm(msg.getSender(), sessionUsername);
                } else if (msg.getChannelId() != null) {
                    notificationApi.ackChannel(msg.getChannelId(), sessionUsername);
                }
                return null;
            }
        }.execute();
    }
}
