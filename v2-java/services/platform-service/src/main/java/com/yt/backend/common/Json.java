package com.yt.backend.common;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper nho cho cho phai tu dung JSON bang tay: body loi cua filter (chay ngoai Spring MVC)
 * va body gui cho cong thanh toan ben ngoai. Du lieu cua app di qua DTO (package dto).
 */
public final class Json {

    private static final Pattern LEADING_INT = Pattern.compile("^\\s*([+-]?\\d+)");

    private Json() {}

    /** Map giu thu tu key: of("a", 1, "b", 2) -> {"a":1,"b":2} */
    public static Map<String, Object> of(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    /** Giong parseInt cua JS: doc so nguyen o dau chuoi, khong duoc (hoac = 0) thi tra fallback */
    public static int parseIntLoose(String s, int fallback) {
        if (s == null) return fallback;
        Matcher matcher = LEADING_INT.matcher(s);
        if (!matcher.find()) return fallback;
        try {
            int v = Integer.parseInt(matcher.group(1));
            return v == 0 ? fallback : v; // parseInt(...) || fallback
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
