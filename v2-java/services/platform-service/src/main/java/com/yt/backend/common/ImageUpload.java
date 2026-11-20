package com.yt.backend.common;

import com.yt.backend.config.AppPaths;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.*;

/**
 * Anh avatar / banner trong form multipart (utils/imageUpload.js).
 * save() kiem tra roi ghi tam vao server/uploads/<field>/, Saved.store(field) moi quyet dinh luu local
 * (/avatar/123.png, WebConfig phuc vu) hay day len Firebase Storage (xem FileStorage).
 */
@Component
public class ImageUpload {

    public static final String DEFAULT_AVATAR = "/default-avatar.png";
    private static final long MB = 1024L * 1024;

    private record Field(long maxSize, String label) {}

    // Moi loai anh 1 thu muc + gioi han dung luong rieng
    private static final Map<String, Field> IMAGE_FIELDS = Map.of(
            "avatar", new Field(2 * MB, "Avatar"),
            "banner", new Field(5 * MB, "Ảnh bìa"));

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif");

    private final AppPaths paths;
    private final FileStorage storage;
    private final SecureRandom random = new SecureRandom();

    public ImageUpload(AppPaths paths, FileStorage storage) {
        this.paths = paths;
        this.storage = storage;
    }

    /** Cac anh vua nhan trong 1 request: file tam tren dia theo field + anh da luu (de xoa khi bi tu choi) */
    public static final class Saved {
        private final FileStorage storage;
        private final Map<String, Path> files = new HashMap<>();
        private final Map<String, String> types = new HashMap<>();
        private final List<String> stored = new ArrayList<>();

        private Saved(FileStorage storage) {
            this.storage = storage;
        }

        /**
         * Luu anh o field `field` (local hoac Firebase). Chi goi sau khi request da hop le,
         * khong thi anh bi day len roi moi bi tu choi.
         * @return duong dan/URL luu vao DB, khong co anh thi null
         */
        public String store(String field) {
            Path file = files.remove(field);
            if (file == null) return null;
            try {
                String url = storage.store(file, field, types.get(field));
                stored.add(url);
                return url;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /** Xoa moi file vua nhan + anh da luu trong request nay (khi request bi tu choi / loi) */
        public void removeAll() {
            files.values().forEach(f -> {
                try {
                    Files.deleteIfExists(f);
                } catch (IOException ignored) {
                    // file tam, xoa khong duoc thi thoi
                }
            });
            files.clear();
            stored.forEach(storage::removeStored);
            stored.clear();
        }
    }

    private static MultipartHttpServletRequest multipart(HttpServletRequest req) {
        return WebUtils.getNativeRequest(req, MultipartHttpServletRequest.class);
    }

    /**
     * Nhan cac field anh (khong bat buoc) trong form multipart, vd. save(req, "avatar", "banner").
     * Request JSON thi bo qua. Loi thi xoa het file vua luu va nem ApiException (400 / 413).
     */
    public Saved save(HttpServletRequest req, String... allowed) {
        Saved saved = new Saved(storage);
        MultipartHttpServletRequest mp = multipart(req);
        if (mp == null) return saved;

        Set<String> allowedSet = Set.of(allowed);
        try {
            for (Map.Entry<String, List<MultipartFile>> entry : mp.getMultiFileMap().entrySet()) {
                String fieldName = entry.getKey();
                List<MultipartFile> files = entry.getValue().stream().filter(f -> !f.isEmpty()).toList();
                if (files.isEmpty()) continue;
                if (!allowedSet.contains(fieldName) || files.size() > 1) {
                    throw ApiException.badRequest("Không nhận file ở field \"" + fieldName + "\"");
                }

                MultipartFile file = files.get(0);
                Field field = IMAGE_FIELDS.get(fieldName);
                String ext = file.getContentType() == null ? null : ALLOWED_TYPES.get(file.getContentType());
                if (ext == null) {
                    throw ApiException.badRequest(field.label() + " chỉ nhận JPG, PNG, WEBP hoặc GIF");
                }
                if (file.getSize() > field.maxSize()) {
                    throw new ApiException(413, field.label() + " tối đa " + field.maxSize() / MB + "MB");
                }

                // Khong dung ten/duoi file nguoi dung gui len, chi lay duoi theo mimetype da kiem tra
                String filename = System.currentTimeMillis() + "-" + String.format("%08x", random.nextInt()) + ext;
                Path dir = paths.uploads().resolve(fieldName);
                Files.createDirectories(dir);
                Path dest = dir.resolve(filename);
                file.transferTo(dest);
                saved.files.put(fieldName, dest);
                saved.types.put(fieldName, file.getContentType());
            }
            return saved;
        } catch (ApiException e) {
            saved.removeAll();
            throw e;
        } catch (IOException e) {
            saved.removeAll();
            throw ApiException.badRequest("Upload ảnh lỗi");
        }
    }

    /** Xoa anh cu da upload; anh mac dinh, anh Google... nam ngoai uploads/bucket nen duoc bo qua */
    public void removeStoredImage(String url) {
        storage.removeStored(url);
    }
}
