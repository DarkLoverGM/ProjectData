package com.yt.backend.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Chay ffmpeg that; may khong co ffmpeg (hoac thieu libwebp) thi bo qua */
class WebpConverterTest {

    @TempDir
    Path dir;

    private final WebpConverter converter = new WebpConverter("ffmpeg");

    @BeforeEach
    void requireFfmpeg() {
        assumeTrue(converter.isAvailable(), "ffmpeg co libwebp khong co tren may nay");
    }

    /** Thong tin doc tu cac chunk RIFF cua file WebP */
    private record WebpInfo(boolean animated, int frames, boolean alpha, int width, int height) {}

    private static WebpInfo inspect(Path file) throws IOException {
        ByteBuffer b = ByteBuffer.wrap(Files.readAllBytes(file)).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals("RIFF", new String(b.array(), 0, 4));
        assertEquals("WEBP", new String(b.array(), 8, 4));
        boolean animated = false, alpha = false;
        int frames = 0, w = 0, h = 0;
        int i = 12;
        while (i + 8 <= b.limit()) {
            String id = new String(b.array(), i, 4);
            int size = b.getInt(i + 4);
            int d = i + 8;
            if (id.equals("VP8X")) {
                int flags = b.get(d) & 0xff;
                alpha = (flags & 0x10) != 0;
                animated = (flags & 0x02) != 0;
                w = 1 + ((b.get(d + 4) & 0xff) | (b.get(d + 5) & 0xff) << 8 | (b.get(d + 6) & 0xff) << 16);
                h = 1 + ((b.get(d + 7) & 0xff) | (b.get(d + 8) & 0xff) << 8 | (b.get(d + 9) & 0xff) << 16);
            }
            if (id.equals("ANMF")) frames++;
            i = d + size + (size & 1);
        }
        return new WebpInfo(animated, frames, alpha, w, h);
    }

    private Path ffmpegGif(String name, String size, int seconds) throws Exception {
        Path out = dir.resolve(name);
        Process p = new ProcessBuilder("ffmpeg", "-nostdin", "-loglevel", "error", "-y", "-f", "lavfi",
                "-i", "testsrc=size=" + size + ":rate=25:duration=" + seconds, out.toString())
                .redirectErrorStream(true).start();
        p.getInputStream().readAllBytes();
        assertEquals(0, p.waitFor());
        return out;
    }

    @Test
    void animatedGifBecomesSmallerAnimatedWebp() throws Exception {
        Path gif = ffmpegGif("t.gif", "1280x720", 2);

        Path out = converter.toWebp(gif, "image/gif");

        assertTrue(out.toString().endsWith(".webp"));
        WebpInfo info = inspect(out);
        assertTrue(info.animated(), "phai giu chuyen dong");
        assertTrue(info.frames() > 1 && info.frames() <= 31, "frames=" + info.frames()); // 2 giay, toi da 15 fps
        assertEquals(720, info.width());
        assertEquals(405, info.height());
        assertTrue(Files.size(out) < Files.size(gif));
    }

    @Test
    void pngKeepsTransparencyAndIsDownscaled() throws Exception {
        BufferedImage img = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_ARGB);
        Path png = dir.resolve("t.png");
        ImageIO.write(img, "png", png.toFile()); // anh trong suot hoan toan

        WebpInfo info = inspect(converter.toWebp(png, "image/png"));

        assertFalse(info.animated());
        assertTrue(info.alpha());
        assertEquals(1280, info.width());
        assertEquals(720, info.height());
    }

    @Test
    void jpegBecomesWebpAndSmallImageIsNotUpscaled() throws Exception {
        Path jpg = dir.resolve("t.jpg");
        ImageIO.write(new BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB), "jpg", jpg.toFile());

        Path out = converter.toWebp(jpg, "image/jpeg");

        byte[] head = Files.readAllBytes(out);
        assertEquals("WEBP", new String(head, 8, 4));
        assertEquals("VP8 ", new String(head, 12, 4)); // WebP tinh don gian, 320x180 giu nguyen
    }

    @Test
    void webpIsKeptAsIs() throws Exception {
        Path webp = dir.resolve("t.webp");
        Files.write(webp, new byte[]{1, 2, 3});
        assertSame(webp, converter.toWebp(webp, "image/webp"));
    }

    @Test
    void rejectsFileWhoseContentDoesNotMatchType() throws Exception {
        Path fake = dir.resolve("fake.gif");
        ImageIO.write(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "png", fake.toFile());

        ApiException e = assertThrows(ApiException.class, () -> converter.toWebp(fake, "image/gif"));
        assertEquals(400, e.getStatus());
        assertFalse(Files.exists(dir.resolve("fake.webp")));
    }

    @Test
    void rejectsHugeDimensionsBeforeDecoding() throws Exception {
        // GIF chi co header khai bao 20000x20000 (anh "bom"): phai bi tu choi ma khong giai nen
        ByteBuffer b = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN);
        b.put("GIF89a".getBytes()).putShort((short) 20000).putShort((short) 20000).put((byte) 0).put((byte) 0).put((byte) 0);
        b.put((byte) 0x2C).putShort((short) 0).putShort((short) 0).putShort((short) 20000).putShort((short) 20000).put((byte) 0);
        b.put((byte) 2).put((byte) 2).put((byte) 0x4C).put((byte) 0x01).put((byte) 0).put((byte) 0x3B);
        Path bomb = dir.resolve("bomb.gif");
        Files.write(bomb, java.util.Arrays.copyOf(b.array(), b.position()));

        ApiException e = assertThrows(ApiException.class, () -> converter.toWebp(bomb, "image/gif"));
        assertEquals(400, e.getStatus());
        assertTrue(e.getMessage().contains("quá lớn"), e.getMessage());
    }
}
