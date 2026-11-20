package com.yt.backend.dto.admin;

/** Thong tin may chu (trang He thong). `node` giu ten field cu, gia tri la phien ban Java */
public record SystemResponse(String node, String platform, long pid, long uptime, Memory memory, Config config) {

    public record Memory(long rss, long heapUsed, long heapTotal) {}

    public record Config(int port, String dbHost, String dbName, String accessExpires, String refreshExpires,
                         boolean cookieSecure) {}
}
