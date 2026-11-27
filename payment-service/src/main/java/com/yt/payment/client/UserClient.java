package com.yt.payment.client;

import com.yt.payment.dto.UserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UserClient {

    private final RestClient restClient;

    public UserClient(
            RestClient.Builder builder,
            @Value("${services.user.url}") String url
    ) {
        this.restClient = builder
                .baseUrl(url)
                .build();
    }

    public UserResponse getById(String id) {
        return restClient.get()
                .uri("/api/users/{id}", id)
                .retrieve()
                .body(UserResponse.class);
    }
}
