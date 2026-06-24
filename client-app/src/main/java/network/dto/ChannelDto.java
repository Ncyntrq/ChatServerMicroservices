package network.dto;

public class ChannelDto {
    private Long id;
    private String name;
    private Long serverId;
    private String type; // TEXT, VOICE
    // CH3 fields
    private String topic;
    private Integer slowmode;
    // CH5 field
    private String category;
    // CH8 field
    private Long pinnedAt;

    public ChannelDto() {
    }

    public ChannelDto(Long id, String name, Long serverId, String type, String topic, Integer slowmode, String category, Long pinnedAt) {
        this.id = id;
        this.name = name;
        this.serverId = serverId;
        this.type = type;
        this.topic = topic;
        this.slowmode = slowmode;
        this.category = category;
        this.pinnedAt = pinnedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public Integer getSlowmode() { return slowmode; }
    public void setSlowmode(Integer slowmode) { this.slowmode = slowmode; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getPinnedAt() { return pinnedAt; }
    public void setPinnedAt(Long pinnedAt) { this.pinnedAt = pinnedAt; }
}
