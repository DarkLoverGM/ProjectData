package com.yt.backend.common;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoWriteException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.UUID;
import java.util.function.Supplier;

/** Helper nho dung chung khi lam viec voi MongoDB */
public final class MongoUtils {

    private MongoUtils() {}

    /**
     * MySQL cu so sanh chuoi KHONG phan biet hoa/thuong, Mongo thi co.
     * Dung collation nay cho email / username de "User1@Test.local" van dang nhap duoc nhu cu.
     */
    public static final Collation CASE_INSENSITIVE = Collation.of("en").strength(Collation.ComparisonLevel.secondary());

    /** Khoa `id` cua moi ban ghi la UUID dang chuoi (giong ban Node) */
    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    /** Escape chuoi nguoi dung nhap truoc khi dua vao $regex (chong ReDoS / tim sai) */
    public static String escapeRegex(String s) {
        return String.valueOf(s).replaceAll("[.*+?^${}()|\\[\\]\\\\]", "\\\\$0");
    }

    /** `field` chua `text`, khong phan biet hoa/thuong (thay cho LIKE '%text%') */
    public static Criteria contains(String field, String text) {
        return Criteria.where(field).regex(escapeRegex(text), "i");
    }

    public static boolean isDuplicateKey(Throwable err) {
        if (err instanceof DuplicateKeyException) return true;
        if (err instanceof MongoWriteException e) return e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY;
        if (err instanceof MongoBulkWriteException e) {
            return e.getWriteErrors().stream()
                    .allMatch(w -> ErrorCategory.fromErrorCode(w.getCode()) == ErrorCategory.DUPLICATE_KEY);
        }
        if (err instanceof MongoCommandException e) return e.getErrorCode() == 11000;
        return err.getCause() != null && err.getCause() != err && isDuplicateKey(err.getCause());
    }

    /** Tuong duong INSERT IGNORE: trung unique index thi tra null, loi khac van nem ra */
    public static <T> T ignoreDuplicate(Supplier<T> op) {
        try {
            return op.get();
        } catch (RuntimeException err) {
            if (isDuplicateKey(err)) return null;
            throw err;
        }
    }

    public static void ignoreDuplicate(Runnable op) {
        ignoreDuplicate(() -> {
            op.run();
            return null;
        });
    }
}
