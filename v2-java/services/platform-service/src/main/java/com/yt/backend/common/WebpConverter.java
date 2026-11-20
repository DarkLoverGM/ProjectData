package com.yt.backend.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Doi thumbnail sang WebP bang ffmpeg (FileStorage.store(file, "thumb")):
 * - GIF -> WebP dong (libwebp_anim): giu chuyen dong nhung nho hon nhieu lan, thu ve toi da 720px, 15 fps,
 *   15 giay dau. GIF thuan nang va ton RAM khi trang chu hien hang chuc thumbnail cung luc.
 * - JPG / PNG -> WebP tinh (libwebp), toi da 1280x720, giu nen trong suot cua PNG.
 * - WebP giu nguyen (ffmpeg 4.x khong doc duoc WebP dong).
 * Server khong co ffmpeg (hoac ffmpeg thieu libwebp) thi giu file goc, chi ghi canh bao luc khoi dong.
 */
@Component
public class WebpConverter {

    private static final Logger log = LoggerFactory.getLogger(WebpConverter.class);

    public static final String WEBP = "image/webp";

    // Anh lon hon thi tu choi truoc khi giai nen (chong anh "bom" vai chuc nghin pixel moi chieu)
    private static final int MAX_SIDE = 10_000;
    private static final long MAX_PIXELS = 40_000_000L;
    private static final long TIMEOUT_SECONDS = 60;
    // ffmpeg ton CPU: toi da 2 file cung luc, cac request khac cho
    private static final Semaphore SLOTS = new Semaphore(2);

    /** MIME -> demuxer ffmpeg. Chi dinh ro de ffmpeg khong tu doan dinh dang (vd. playlist tro toi file khac) */
    private static final Map<String, String> DEMUXERS = Map.of(
            "image/gif", "gif",
            "image/jpeg", "jpeg_pipe",
            "image/png", "png_pipe");

    private final String ffmpeg;
    private final boolean available;

    public WebpConverter(@Value("${app.ffmpeg-path}") String ffmpeg) {
        this.ffmpeg = ffmpeg;
        this.available = detect(ffmpeg);
    }

    public boolean isAvailable() {
        return available;
    }

    private static boolean detect(String ffmpeg) {
        try {
            Process p = new ProcessBuilder(ffmpeg, "-hide_banner", "-encoders").redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!p.waitFor(10, TimeUnit.SECONDS)) p.destroyForcibly();
            if (out.contains("libwebp_anim") && out.contains(" libwebp ")) {
                log.info("[thumb] Đổi thumbnail sang WebP bằng {}", ffmpeg);
                return true;
            }
            log.warn("[thumb] {} không có encoder libwebp / libwebp_anim -> giữ nguyên định dạng thumbnail", ffmpeg);
        } catch (IOException e) {
            log.warn("[thumb] Không chạy được {} ({}) -> giữ nguyên định dạng thumbnail. Cài ffmpeg hoặc đặt FFMPEG_PATH",
                    ffmpeg, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    /**
     * Doi anh `input` (dinh dang theo `mime` da kiem tra) sang WebP, file moi nam canh file goc.
     * @return file .webp moi, hoac chinh `input` neu khong can / khong doi duoc (khong co ffmpeg, da la WebP)
     * @throws ApiException 400 anh hong / qua lon, 503 server dang ban
     */
    public Path toWebp(Path input, String mime) throws IOException {
        String demuxer = DEMUXERS.get(mime);
        if (!available || demuxer == null) return input;
        checkDimensions(input);

        String name = input.getFileName().toString();
        Path output = input.resolveSibling(name.replaceFirst("\\.[^.]+$", "") + ".webp");
        List<String> cmd = new ArrayList<>(List.of(ffmpeg, "-nostdin", "-hide_banner", "-loglevel", "error",
                "-protocol_whitelist", "file", "-f", demuxer, "-i", input.toString()));
        if ("image/gif".equals(mime)) {
            cmd.addAll(List.of("-t", "15", "-frames:v", "300", "-fpsmax", "15",
                    "-vf", scale(720, 720), "-an", "-c:v", "libwebp_anim", "-quality", "75", "-loop", "0"));
        } else {
            cmd.addAll(List.of("-frames:v", "1", "-vf", scale(1280, 720), "-c:v", "libwebp", "-quality", "80"));
        }
        cmd.addAll(List.of("-y", output.toString()));

        try {
            if (!SLOTS.tryAcquire(30, TimeUnit.SECONDS)) {
                throw new ApiException(503, "Máy chủ đang bận xử lý ảnh, vui lòng thử lại");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(503, "Máy chủ đang bận xử lý ảnh, vui lòng thử lại");
        }
        try {
            run(cmd, output);
            return output;
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(output);
            throw e;
        } finally {
            SLOTS.release();
        }
    }

    /** Thu nho vao khung w x h (khong phong to), giu ty le */
    private static String scale(int w, int h) {
        return "scale='min(" + w + ",iw)':'min(" + h + ",ih)':force_original_aspect_ratio=decrease:flags=lanczos";
    }

    private void run(List<String> cmd, Path output) throws IOException {
        // Ghi log ffmpeg ra file: doc qua pipe ma ffmpeg in nhieu loi (anh hong) thi co the ket khi day pipe
        Path logFile = Files.createTempFile("ffmpeg-", ".log");
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true)
                    .redirectOutput(logFile.toFile()).start();
            boolean done;
            try {
                done = p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                p.destroyForcibly();
                Thread.currentThread().interrupt();
                throw new ApiException(503, "Máy chủ đang bận xử lý ảnh, vui lòng thử lại");
            }
            if (!done) {
                p.destroyForcibly();
                throw ApiException.badRequest("Thumbnail quá nặng, không xử lý kịp. Hãy dùng ảnh nhỏ hơn");
            }
            if (p.exitValue() != 0 || !Files.exists(output) || Files.size(output) == 0) {
                String err = Files.readString(logFile, StandardCharsets.UTF_8);
                log.warn("[thumb] ffmpeg lỗi ({}): {}", p.exitValue(), err.length() > 500 ? err.substring(err.length() - 500) : err);
                throw ApiException.badRequest("Không đọc được ảnh thumbnail, file có thể bị hỏng");
            }
        } finally {
            Files.deleteIfExists(logFile);
        }
    }

    /** Doc kich thuoc tu header (khong giai nen ca anh) */
    private static void checkDimensions(Path file) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(file.toFile())) {
            Iterator<ImageReader> readers = in == null ? null : ImageIO.getImageReaders(in);
            if (readers == null || !readers.hasNext()) throw ApiException.badRequest("Không đọc được ảnh thumbnail, file có thể bị hỏng");
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                long w = reader.getWidth(0);
                long h = reader.getHeight(0);
                if (w > MAX_SIDE || h > MAX_SIDE || w * h > MAX_PIXELS) {
                    throw ApiException.badRequest("Thumbnail quá lớn (tối đa " + MAX_SIDE + "px mỗi chiều)");
                }
            } catch (IOException e) {
                throw ApiException.badRequest("Không đọc được ảnh thumbnail, file có thể bị hỏng");
            } finally {
                reader.dispose();
            }
        }
    }
}
