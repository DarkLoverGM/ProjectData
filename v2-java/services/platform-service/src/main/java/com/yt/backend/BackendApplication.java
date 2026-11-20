package com.yt.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// Dang nhap bang JWT tu viet (security/), khong dung user/mat khau mac dinh cua Spring Security
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
