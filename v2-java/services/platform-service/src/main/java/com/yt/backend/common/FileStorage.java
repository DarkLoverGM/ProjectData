package com.yt.backend.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yt.backend.config.AppPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Noi luu file nguoi dung dang (video, thumbnail, avatar, anh bia), giong utils/fileStorage.js:
 * - Co cau hinh Firebase (FIREBASE_STORAGE_BUCKET + service account) -> day len Firebase Storage,
 *   DB luu URL day du https://firebasestorage.googleapis.com/... nen deploy khong can mang theo uploads/.
 * - Khong co -> file nam o server/uploads, DB luu "/video/123.mp4", WebConfig phuc vu.
 * File luon duoc ghi tam vao uploads/ truoc (saveTemp), store() xong moi xoa ban tam.
 * Goi Firebase qua REST (Google Cloud Storage JSON API) bang service account (FirebaseCredentials), khong can them thu vien.
 */
@Component
public class FileStorage {

    private static final Logger log = LoggerFactory.getLogger(FileStorage.class);

    public static final List<String> FOLDERS = List.of("video", "thumb", "avatar", "banner");
    private static final Pattern LOCAL = Pattern.compile("^/(" + String.join("|", FOLDERS) + ")/([\\w.-]+)$");

    private final AppPaths paths;
    private final ObjectMapper mapper;
    private final FirebaseCredentials firebase;
    private final String bucket;
    private final WebpConverter webp;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public FileStorage(AppPaths paths, ObjectMapper mapper, FirebaseCredentials firebase, WebpConverter webp,
                       @Value("${app.firebase.bucket}") String bucket) {
        this.paths = paths;
        this.webp = webp;
        this.mapper = mapper;
        this.firebase = firebase;
        boolean remote = !bucket.isBlank() && firebase.isConfigured();
        if (remote) {
            log.info("[storage] Lưu file lên Firebase Storage ({})", bucket.trim());
        } else if (!bucket.isBlank()) {
            log.warn("[storage] Có FIREBASE_STORAGE_BUCKET nhưng thiếu service account -> lưu local ở server/uploads");
        }
        this.bucket = remote ? bucket.trim() : null;
    }

    public boolean isRemote() {
        return bucket != null;
    }

    // ================= Luu file =================

    /**
     * Ghi tam file vua nhan vao server/uploads/<folder>/<filename> (giong multer diskStorage).
     * Khong luu vao public/: dev server cua CRA theo doi public/, co file moi la reload trang ngay giua luc upload.
     */
    public Path saveTemp(MultipartFile file, String folder, String filename) throws IOException {
        Path dir = paths.uploads().resolve(folder);
        Files.createDirectories(dir);
        Path dest = dir.resolve(filename);
        file.transferTo(dest);
        return dest;
    }

    /**
     * Dinh dang duoc nhan theo thu muc -> duoi file luu tren dia.
     * Khong dung duoi file nguoi dung gui: neu de nguyen, file "x.html" se duoc phuc vu dang text/html
     * ngay tren domain cua web -> chay duoc script voi cookie cua nguoi mo link (stored XSS).
     */
    private static final Map<String, Map<String, String>> ALLOWED_TYPES = Map.of(
            "video", Map.of(
                    "video/mp4", ".mp4",
                    "video/webm", ".webm",
                    "video/ogg", ".ogv",
                    "video/quicktime", ".mov",
                    "video/x-matroska", ".mkv"),
            "thumb", Map.of(
                    "image/jpeg", ".jpg",
                    "image/png", ".png",
                    "image/webp", ".webp",
                    "image/gif", ".gif"));

    /** Kiem tra dinh dang truoc (khi request co nhieu file, de khong luu file dau roi moi bao loi file sau) */
    public static void checkType(MultipartFile file, String folder) {
        if (file != null && !file.isEmpty()) safeName(file, folder);
    }

    /** Ten file: Date.now() + so ngau nhien + duoi theo MIME da kiem tra */
    private static String safeName(MultipartFile file, String folder) {
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(java.util.Locale.ROOT);
        String ext = ALLOWED_TYPES.getOrDefault(folder, Map.of()).get(type);
        if (ext == null) {
            throw ApiException.badRequest("video".equals(folder)
                    ? "Video chỉ nhận MP4, WEBM, OGG, MOV hoặc MKV"
                    : "Thumbnail chỉ nhận JPG, PNG, WEBP hoặc GIF");
        }
        return System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8) + ext;
    }

    /**
     * Luu file da ghi tam vao `folder`. @return duong dan/URL de luu vao DB
     * (local: "/video/123.mp4", Firebase: URL tai kem token)
     */
    public String store(Path file, String folder, String contentType) throws IOException {
        if (bucket == null) return "/" + folder + "/" + file.getFileName();

        String objectPath = folder + "/" + file.getFileName();
        String token = UUID.randomUUID().toString();
        try {
            upload(file, objectPath, contentType == null ? "application/octet-stream" : contentType, token);
        } finally {
            Files.deleteIfExists(file);
        }
        return downloadUrl(objectPath, token);
    }

    /**
     * Kiem tra dinh dang, ghi tam roi luu luon (khi request da hop le).
     * Thumbnail duoc doi sang WebP truoc khi luu (WebpConverter): GIF -> WebP dong, JPG / PNG -> WebP tinh.
     * @throws ApiException 400 neu file khong phai video / anh duoc ho tro, hoac anh hong
     */
    public String store(MultipartFile file, String folder) throws IOException {
        Path temp = saveTemp(file, folder, safeName(file, folder));
        Path stored = temp;
        try {
            String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(java.util.Locale.ROOT);
            if ("thumb".equals(folder)) {
                stored = webp.toWebp(temp, type);
                if (stored != temp) {
                    Files.deleteIfExists(temp);
                    type = WebpConverter.WEBP;
                }
            }
            return store(stored, folder, type);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(temp);
            Files.deleteIfExists(stored);
            throw e;
        }
    }

    // URL tai kem token: doc duoc ma khong can mo public bucket hay sua Security Rules
    private String downloadUrl(String objectPath, String token) {
        return "https://firebasestorage.googleapis.com/v0/b/" + bucket + "/o/" + encodeURIComponent(objectPath)
                + "?alt=media&token=" + token;
    }

    /** Duong dan object trong bucket cua URL do store() tra ve, khong phai thi null */
    private String objectPathOf(String url) {
        String prefix = "https://firebasestorage.googleapis.com/v0/b/" + bucket + "/o/";
        if (bucket == null || !url.startsWith(prefix)) return null;
        return URLDecoder.decode(url.substring(prefix.length()).split("\\?")[0], StandardCharsets.UTF_8);
    }

    /**
     * Xoa file da luu (khi bi thay bang file moi). Chi dung toi file trong uploads/<folder> hoac bucket cua minh,
     * bo qua anh mac dinh, du lieu mau o public/ va link ngoai. Xoa tren Firebase chay nen, loi thi chi ghi log.
     */
    public void removeStored(String url) {
        if (url == null || url.isEmpty()) return;
        Matcher local = LOCAL.matcher(url);
        if (local.matches()) {
            try {
                Files.deleteIfExists(paths.uploads().resolve(local.group(1)).resolve(local.group(2)));
            } catch (IOException ignored) {
                // file cu khong xoa duoc thi thoi
            }
            return;
        }
        String objectPath = objectPathOf(url);
        if (objectPath == null || !FOLDERS.contains(objectPath.split("/")[0])) return;
        CompletableFuture.runAsync(() -> {
            try {
                delete(objectPath);
            } catch (Exception e) {
                log.error("[storage] Không xoá được {}: {}", objectPath, e.getMessage());
            }
        });
    }

    // ================= Google Cloud Storage REST =================

    /** Resumable upload: video 2GB van duoc, khong phai doc het file vao RAM */
    private void upload(Path file, String objectPath, String contentType, String downloadToken) throws IOException {
        String metadata = mapper.writeValueAsString(Map.of(
                "name", objectPath,
                "contentType", contentType,
                "cacheControl", "public, max-age=86400",
                "metadata", Map.of("firebaseStorageDownloadTokens", downloadToken)));
        HttpResponse<String> init = send(HttpRequest.newBuilder(URI.create(
                        "https://storage.googleapis.com/upload/storage/v1/b/" + encodeURIComponent(bucket)
                                + "/o?uploadType=resumable&name=" + encodeURIComponent(objectPath)))
                .header("Authorization", "Bearer " + firebase.accessToken())
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("X-Upload-Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(metadata)).build());
        String session = init.headers().firstValue("Location").orElse(null);
        if (init.statusCode() / 100 != 2 || session == null) {
            throw new IOException("Firebase upload init error " + init.statusCode() + ": " + init.body());
        }

        HttpResponse<String> put = send(HttpRequest.newBuilder(URI.create(session))
                .timeout(Duration.ofHours(2))
                .header("Content-Type", contentType)
                .PUT(HttpRequest.BodyPublishers.ofFile(file)).build());
        if (put.statusCode() / 100 != 2) {
            throw new IOException("Firebase upload error " + put.statusCode() + ": " + put.body());
        }
    }

    private void delete(String objectPath) throws IOException {
        HttpResponse<String> res = send(HttpRequest.newBuilder(URI.create(
                        "https://storage.googleapis.com/storage/v1/b/" + encodeURIComponent(bucket)
                                + "/o/" + encodeURIComponent(objectPath)))
                .header("Authorization", "Bearer " + firebase.accessToken())
                .DELETE().build());
        // ignoreNotFound
        if (res.statusCode() / 100 != 2 && res.statusCode() != 404) {
            throw new IOException("HTTP " + res.statusCode() + ": " + res.body());
        }
    }

    private HttpResponse<String> send(HttpRequest req) throws IOException {
        try {
            return http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    /** Giong encodeURIComponent cua JS */
    public static String encodeURIComponent(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%21", "!")
                .replace("%27", "'")
                .replace("%28", "(")
                .replace("%29", ")")
                .replace("%7E", "~");
    }
}
