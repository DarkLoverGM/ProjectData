package com.yt.backend.seed;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Xuat DB hien tai thanh du lieu mau server-java/seed/ (DataSeeder nap lai tren may khac).
 * Chay doc lap, khong khoi dong Spring (server dang chay van giu nguyen):
 *   java -Dloader.main=com.yt.backend.seed.SeedExport -cp target/server-java-1.0.0.jar \
 *        org.springframework.boot.loader.launch.PropertiesLauncher [mongoUri] [rootDir]
 *
 * An danh hoa truoc khi ghi ra file:
 * - Email that -> admin@test.local (admin) / userN@test.local, moi mat khau = "123456".
 * - Bo google_id va avatar Google; bo push token, ma xac minh, OTP (SeedFiles.SKIP).
 */
public final class SeedExport {

    private static final Pattern TEST_EMAIL = Pattern.compile(".*@(seed\\.local|example\\.com|test\\.local)$");
    private static final String DEFAULT_AVATAR = "/default-avatar.png";

    public static void main(String[] args) throws IOException {
        String uri = args.length > 0 ? args[0] : System.getenv().getOrDefault("MONGO_URI", "mongodb://127.0.0.1:27017/YT");
        Path root = Path.of(args.length > 1 ? args[1] : "..").toAbsolutePath().normalize();
        Path dir = SeedFiles.dir(root);
        Files.createDirectories(dir);

        try (MongoClient client = MongoClients.create(uri)) {
            MongoDatabase db = client.getDatabase(new ConnectionString(uri).getDatabase());
            Map<String, String> emails = anonymousEmails(db);
            // Thay email dai truoc: "editor@gmail.com" nam trong "darklovereditor@gmail.com"
            List<Map.Entry<String, String>> replacements = new ArrayList<>(emails.entrySet());
            replacements.sort((a, b) -> b.getKey().length() - a.getKey().length());
            String password = new BCryptPasswordEncoder().encode(SeedFiles.PASSWORD);

            for (String collection : new TreeSet<>(db.listCollectionNames().into(new ArrayList<>()))) {
                if (SeedFiles.SKIP.contains(collection)) continue;
                List<String> lines = new ArrayList<>();
                for (Document doc : db.getCollection(collection).find().sort(new Document("_id", 1))) {
                    if (collection.equals("users")) anonymizeUser(doc, password);
                    String json = doc.toJson(SeedFiles.JSON);
                    // Email co the nam trong noi dung khac (nhat ky admin, thong bao) -> thay het
                    for (Map.Entry<String, String> e : replacements) json = json.replace(e.getKey(), e.getValue());
                    lines.add(json);
                }
                Files.write(dir.resolve(collection + SeedFiles.EXT), lines);
                System.out.printf("%-22s %d%n", collection, lines.size());
            }
            emails.forEach((from, to) -> System.out.println("  " + from + " -> " + to));
            writeMedia(db, root, dir.resolve(SeedFiles.MEDIA_ZIP));
        }
    }

    /** Email that -> email test; admin dau tien la admin@test.local, con lai user1, user2... */
    private static Map<String, String> anonymousEmails(MongoDatabase db) {
        Map<String, String> map = new LinkedHashMap<>();
        int n = 0;
        Document sort = new Document("role", 1).append("_id", 1); // "admin" < "user"
        for (Document u : db.getCollection("users").find().sort(sort)) {
            String email = u.getString("email");
            if (email == null || TEST_EMAIL.matcher(email).matches()) continue;
            boolean admin = "admin".equals(u.getString("role")) && !map.containsValue("admin@test.local");
            map.put(email, admin ? "admin@test.local" : "user" + (++n) + "@test.local");
        }
        return map;
    }

    private static void anonymizeUser(Document u, String password) {
        u.put("password", password);
        u.remove("google_id");
        for (String field : List.of("avatar", "banner")) {
            Object v = u.get(field);
            if (v instanceof String s && s.startsWith("http")) u.put(field, field.equals("avatar") ? DEFAULT_AVATAR : null);
        }
    }

    /** Nen file media DB dang tham chieu (lay o server/uploads, khong co thi public/) */
    private static void writeMedia(MongoDatabase db, Path root, Path zip) throws IOException {
        TreeSet<String> refs = new TreeSet<>();
        for (Document v : db.getCollection("videos").find()) addRef(refs, v.get("src"), v.get("thumbnail"));
        for (Document u : db.getCollection("users").find()) addRef(refs, u.get("avatar"), u.get("banner"));

        int count = 0;
        try (OutputStream out = Files.newOutputStream(zip); ZipOutputStream z = new ZipOutputStream(out)) {
            z.setLevel(Deflater.BEST_SPEED); // video / anh da nen san, nen manh chi ton thoi gian
            for (String ref : refs) {
                Path file = root.resolve("server/uploads").resolve(ref);
                if (!Files.isRegularFile(file)) file = root.resolve("public").resolve(ref);
                if (!Files.isRegularFile(file)) {
                    System.out.println("  thieu file media: /" + ref);
                    continue;
                }
                z.putNextEntry(new ZipEntry(ref));
                Files.copy(file, z);
                z.closeEntry();
                count++;
            }
        }
        System.out.printf("%s: %d file, %.1f MB%n", zip.getFileName(), count, Files.size(zip) / 1e6);
    }

    private static void addRef(TreeSet<String> refs, Object... values) {
        for (Object v : values) {
            if (!(v instanceof String s) || !s.startsWith("/")) continue;
            String rel = s.substring(1);
            int slash = rel.indexOf('/');
            if (slash > 0 && SeedFiles.MEDIA_DIRS.contains(rel.substring(0, slash))) refs.add(rel);
        }
    }
}
