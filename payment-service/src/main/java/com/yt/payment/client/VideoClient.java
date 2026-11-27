package com.yt.payment.client;

import com.yt.payment.dto.VideoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class VideoClient {

    private final RestClient restClient;

    public VideoClient(
            RestClient.Builder builder,
            @Value("${services.video.url}") String url
    ) {
        this.restClient = builder
                .baseUrl(url)
                .build();
    }

    public VideoResponse getById(String id) {
        return restClient.get()
                .uri("/api/videos/{id}", id)
                .retrieve()
                .body(VideoResponse.class);
    }
}
