package com.yt.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/** Duong dan toi cac thu muc dung chung voi frontend (build/, public/, server/data, server/uploads). */
@Component
public class AppPaths {

    private final Path root;

    public AppPaths(@Value("${app.root-dir}") String rootDir) {
        this.root = Path.of(rootDir).toAbsolutePath().normalize();
    }

    public Path root() {
        return root;
    }

    public Path build() {
        return root.resolve("build");
    }

    public Path publicDir() {
        return root.resolve("public");
    }

    /** server/uploads: video, thumbnail, avatar, anh bia nguoi dung dang (giong ban Node) */
    public Path uploads() {
        return root.resolve("server/uploads");
    }

    public Path logFile() {
        return root.resolve("server/data/log.json");
    }

    /** "file:/abs/path/" cho ResourceHandlerRegistry */
    public static String location(Path dir) {
        return dir.toUri().toString().replaceAll("/?$", "/");
    }
}
