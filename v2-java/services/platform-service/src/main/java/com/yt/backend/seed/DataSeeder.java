package com.yt.backend.seed;

import com.yt.backend.config.AppPaths;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Nap du lieu mau khi khoi dong tren may moi, de chay duoc ngay ma khong phai tao lai DB:
 * - DB chua co user nao -> insert toan bo server-java/seed/*.jsonl (xuat bang SeedExport).
 * - Co seed/seed-media.zip -> giai nen file con thieu vao server/uploads (file da co thi giu nguyen).
 * Chay truoc MongoIndexConfig (@Order 0) va BanList (@Order 10). Tat bang APP_SEED_ENABLED=false.
 */
@Component
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final int BATCH = 1000;

    private final MongoTemplate mongo;
    private final AppPaths paths;
    private final boolean enabled;

    public DataSeeder(MongoTemplate mongo, AppPaths paths, @Value("${app.seed.enabled:true}") boolean enabled) {
        this.mongo = mongo;
        this.paths = paths;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(-10)
    public void seedOnStartup() {
        if (!enabled) return;
        Path dir = SeedFiles.dir(paths.root());
        if (!Files.isDirectory(dir)) return;
        try {
            importCollections(dir);
            extractMedia(dir.resolve(SeedFiles.MEDIA_ZIP));
        } catch (Exception e) {
            log.error("Nap du lieu mau loi", e);
        }
    }

    private void importCollections(Path dir) throws IOException {
        if (mongo.getCollection("users").estimatedDocumentCount() > 0) {
            log.info("DB da co du lieu, bo qua nap du lieu mau");
            return;
        }
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(f -> f.getFileName().toString().endsWith(SeedFiles.EXT)).sorted().toList();
        }
        int total = 0;
        for (Path file : files) {
            String name = file.getFileName().toString();
            String collection = name.substring(0, name.length() - SeedFiles.EXT.length());
            List<Document> batch = new ArrayList<>();
            int count = 0;
            for (String line : Files.readAllLines(file)) {
                if (line.isBlank()) continue;
                batch.add(Document.parse(line));
                if (batch.size() == BATCH) {
                    mongo.getCollection(collection).insertMany(batch);
                    count += batch.size();
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                mongo.getCollection(collection).insertMany(batch);
                count += batch.size();
            }
            total += count;
            log.info("Nap du lieu mau: {} ({} ban ghi)", collection, count);
        }
        log.info("Nap xong {} ban ghi tu {}. Tai khoan mau: admin@test.local, mat khau {}", total, dir, SeedFiles.PASSWORD);
    }

    private void extractMedia(Path zip) throws IOException {
        if (!Files.isRegularFile(zip)) return;
        Path uploads = paths.uploads();
        int extracted = 0;
        try (ZipFile z = new ZipFile(zip.toFile())) {
            Enumeration<? extends ZipEntry> entries = z.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (e.isDirectory()) continue;
                Path target = uploads.resolve(e.getName()).normalize();
                // Chan zip slip: entry "../../x" khong duoc ghi ra ngoai server/uploads
                if (!target.startsWith(uploads) || Files.exists(target)) continue;
                Files.createDirectories(target.getParent());
                try (InputStream in = z.getInputStream(e)) {
                    Files.copy(in, target);
                }
                extracted++;
            }
        }
        if (extracted > 0) log.info("Giai nen {} file media vao {}", extracted, uploads);
    }
}
