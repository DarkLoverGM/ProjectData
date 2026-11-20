package com.yt.backend.seed;

import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;

import java.nio.file.Path;
import java.util.Set;

/**
 * Vi tri va dinh dang du lieu mau (server-java/seed/), dung chung cho SeedExport (xuat) va DataSeeder (nap).
 * Moi collection 1 file <ten>.jsonl, moi dong 1 document dang MongoDB Extended JSON
 * (giu nguyen kieu ObjectId, Date) -> nap lai duoc dung nhu cu ma khong can mongorestore / mongosh.
 */
public final class SeedFiles {

    private SeedFiles() {}

    public static final String EXT = ".jsonl";

    /** File nen video / thumbnail / avatar / anh bia, giai nen vao server/uploads (nang nen khong commit) */
    public static final String MEDIA_ZIP = "seed-media.zip";

    /** Thu muc media duoc DB tham chieu dang "/video/123.mp4" */
    public static final Set<String> MEDIA_DIRS = Set.of("video", "thumb", "avatar", "banner");

    /** Khong xuat: token / ma xac minh chi co gia tri tren may dang chay, chua du lieu nhay cam */
    public static final Set<String> SKIP = Set.of("push_tokens", "email_verification", "user_resetpass");

    /** Mat khau chung cua moi tai khoan trong du lieu mau */
    public static final String PASSWORD = "123456";

    public static final JsonWriterSettings JSON = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();

    /** root = thu muc goc repo hello-react (app.root-dir) */
    public static Path dir(Path root) {
        return root.resolve("server-java/seed");
    }
}
