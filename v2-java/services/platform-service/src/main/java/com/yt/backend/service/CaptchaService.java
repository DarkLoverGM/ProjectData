package com.yt.backend.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Captcha luu trong bo nho, het han sau 2 phut (UserServices.createCaptcha / verifyCaptcha ben Node). */
@Service
public class CaptchaService {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final long TTL_MS = 2 * 60 * 1000;
    // Spam /create-captcha trong vong 2 phut -> bo ma cu nhat thay vi de day RAM
    private static final int CAPTCHA_MAX = 10000;

    private record Entry(String text, long expiresAt) {}

    // LinkedHashMap giu thu tu them vao (giong Map ben Node) de bo duoc ma cu nhat
    private final Map<String, Entry> store = Collections.synchronizedMap(new LinkedHashMap<>());

    /** @return token cua captcha moi */
    public String createCaptcha() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 6; i++) text.append(CHARS.charAt(rnd.nextInt(CHARS.length())));

        // Don cac captcha het han de store khong phinh ra mai
        long now = System.currentTimeMillis();
        String token = UUID.randomUUID().toString();
        synchronized (store) {
            store.values().removeIf(e -> now > e.expiresAt());
            if (store.size() >= CAPTCHA_MAX) {
                Iterator<String> oldest = store.keySet().iterator();
                oldest.next();
                oldest.remove();
            }
            store.put(token, new Entry(text.toString(), now + TTL_MS));
        }
        return token;
    }

    public String createCaptchaSVG(String token) {
        int width = 200;
        int height = 80;
        Entry data = store.get(token);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        StringBuilder letters = new StringBuilder();
        String text = data.text();
        for (int i = 0; i < text.length(); i++) {
            double x = 20 + i * 30 + rnd.nextDouble() * 5;
            double y = 50 + rnd.nextDouble() * 10;
            double rotate = (rnd.nextDouble() - 0.5) * 30;
            String color = "hsl(" + rnd.nextDouble() * 360 + ", 70%, 40%)";
            letters.append("<text x=\"").append(x).append("\" y=\"").append(y)
                    .append("\" fill=\"").append(color).append("\" font-size=\"36\" transform=\"rotate(")
                    .append(rotate).append(',').append(x).append(',').append(y).append(")\">")
                    .append(text.charAt(i)).append("</text>");
        }

        StringBuilder noise = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            noise.append("<line x1=\"").append(rnd.nextDouble() * width)
                    .append("\" y1=\"").append(rnd.nextDouble() * height)
                    .append("\" x2=\"").append(rnd.nextDouble() * width)
                    .append("\" y2=\"").append(rnd.nextDouble() * height)
                    .append("\" stroke=\"hsl(").append(rnd.nextDouble() * 360)
                    .append(", 30%, 70%)\" stroke-width=\"1\"/>");
        }

        return """
                <svg xmlns="http://www.w3.org/2000/svg"
                    viewBox="0 0 %d %d"
                    width="100%%" height="100%%"
                    preserveAspectRatio="xMidYMid meet">
                <rect width="100%%" height="100%%" fill="#f0f0f0"/>
                %s
                %s
                </svg>
                """.formatted(width, height, noise, letters);
    }

    /** Moi ma chi duoc thu 1 lan (dung hay sai deu xoa), khong thi doan mai duoc */
    public boolean verifyCaptcha(String token, String userInput) {
        if (token == null) return false;
        Entry data = store.remove(token);
        if (data == null) return false;
        if (System.currentTimeMillis() > data.expiresAt()) return false;
        return data.text().equalsIgnoreCase(String.valueOf(userInput));
    }
}
