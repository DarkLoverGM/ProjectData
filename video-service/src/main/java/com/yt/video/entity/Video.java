package com.yt.video.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "videos")
public class Video {

    @Id
    private String id;
    private String title;
    private String description;
    private String thumbnail;
    private String videoUrl;
    private String channelId;
    private String category;
    private long views;
    private boolean membersOnly;
    private String status;
    private Instant uploadAt;

    public Video() {
    }

    public Video(String id, String title, String description, String thumbnail, String videoUrl,
                 String channelId, String category, long views, boolean membersOnly, String status, Instant uploadAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.thumbnail = thumbnail;
        this.videoUrl = videoUrl;
        this.channelId = channelId;
        this.category = category;
        this.views = views;
        this.membersOnly = membersOnly;
        this.status = status;
        this.uploadAt = uploadAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public long getViews() { return views; }
    public void setViews(long views) { this.views = views; }

    public boolean isMembersOnly() { return membersOnly; }
    public void setMembersOnly(boolean membersOnly) { this.membersOnly = membersOnly; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getUploadAt() { return uploadAt; }
    public void setUploadAt(Instant uploadAt) { this.uploadAt = uploadAt; }
}
