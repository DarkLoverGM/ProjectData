package com.yt.backend.common;

import java.util.List;

/**
 * Tieu chuan cong dong dung khi duyet video (utils/videoRules.js): trang admin hien thanh checklist,
 * Studio cua nguoi dang hien ten tieu chuan bi vi pham.
 */
public final class VideoRules {

    private VideoRules() {}

    public record Rule(String id, String label, String hint) {}

    public static final List<Rule> VIDEO_RULES = List.of(
            new Rule("violence", "Bạo lực, máu me", "Cảnh đánh nhau, gây thương tích, ngược đãi người/động vật"),
            new Rule("sexual", "Khiêu dâm, khoả thân", "Nội dung tình dục, khoả thân, gợi dục với trẻ vị thành niên"),
            new Rule("hate", "Thù ghét, phân biệt đối xử", "Công kích theo sắc tộc, tôn giáo, giới tính, khuyết tật..."),
            new Rule("harassment", "Quấy rối, xâm phạm riêng tư", "Bắt nạt, lộ thông tin cá nhân, quay lén"),
            new Rule("dangerous", "Hành vi nguy hiểm", "Hướng dẫn tự hại, chất cấm, thử thách nguy hiểm"),
            new Rule("spam", "Spam, lừa đảo", "Câu view, link lừa đảo, tiêu đề/thumbnail sai sự thật"),
            new Rule("copyright", "Vi phạm bản quyền", "Đăng lại phim, nhạc, video của người khác khi không có quyền"),
            new Rule("misinformation", "Thông tin sai lệch", "Tin giả gây hại về y tế, thiên tai, bầu cử..."),
            new Rule("other", "Khác", "Ghi rõ trong phần ghi chú"));

    public static boolean isRule(String id) {
        return VIDEO_RULES.stream().anyMatch(r -> r.id().equals(id));
    }

    public static String ruleLabel(String id) {
        return VIDEO_RULES.stream().filter(r -> r.id().equals(id)).map(Rule::label).findFirst().orElse(String.valueOf(id));
    }
}
