package com.yt.backend.security;

import com.yt.backend.auth.BanList;
import com.yt.backend.auth.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security cho REST API dang nhap bang JWT (cookie httpOnly):
 * - Stateless: khong tao HttpSession, moi request tu mang token (JwtAuthenticationFilter doc)
 * - /api/admin/** chi role ADMIN; cac endpoint can dang nhap khai bao bang @PreAuthorize("isAuthenticated()")
 * - CSRF tat: cookie dang nhap la SameSite=Lax nen trinh duyet khong gui kem request POST tu trang web khac,
 *   CORS chi cho cac origin trong WebConfig.
 * - CORS xu ly o WebConfig.corsFilter (chay truoc ca rate limit) nen o day tat.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // cost 10 giong bcrypt.hash(..., 10) ben Node; doc duoc ca hash $2b$ cu
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwt, BanList banList, AccessTokenReader tokens,
                                           SecurityErrorWriter errors, RestAuthenticationEntryPoint entryPoint,
                                           RestAccessDeniedHandler accessDenied) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h
                        // Trang admin nhung player /review/:id cua chinh site trong iframe
                        .frameOptions(f -> f.sameOrigin())
                        // Giu Cache-Control rieng cua file tinh (WebConfig), khong ep no-cache cho moi response
                        .cacheControl(c -> c.disable()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDenied))
                .addFilterBefore(new JwtAuthenticationFilter(jwt, banList, tokens, errors),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
