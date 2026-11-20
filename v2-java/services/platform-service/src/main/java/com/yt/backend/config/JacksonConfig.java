package com.yt.backend.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * MongoDB tra ngay gio ve dang java.util.Date (BSON Date, UTC). Ben Node JSON.stringify ra
 * ISO UTC ("2026-06-15T17:00:00.000Z"), frontend dang dung dinh dang do.
 * Cac kieu LocalDateTime / LocalDate van giu cho nhung cho tu tao ngay (gio local cua server).
 *
 * (Ghi chu cu) DATETIME / DATE cua MySQL la gio local cua server. mysql2 ben Node doc thanh Date
 * roi JSON.stringify ra ISO UTC ("2026-06-15T17:00:00.000Z"), frontend dang dung dinh dang do.
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter JS_ISO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private static <T> JsonSerializer<T> serializer(java.util.function.Function<T, Instant> toInstant) {
        return new JsonSerializer<>() {
            @Override
            public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(JS_ISO.format(toInstant.apply(value)));
            }
        };
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsDates() {
        SimpleModule module = new SimpleModule("js-dates");
        module.addSerializer(LocalDateTime.class,
                serializer((LocalDateTime v) -> v.atZone(ZoneId.systemDefault()).toInstant()));
        module.addSerializer(LocalDate.class,
                serializer((LocalDate v) -> v.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        module.addSerializer(java.util.Date.class, serializer(java.util.Date::toInstant));
        module.addSerializer(java.sql.Timestamp.class, serializer(java.sql.Timestamp::toInstant));
        module.addSerializer(java.sql.Date.class,
                serializer((java.sql.Date v) -> v.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        return builder -> builder.modulesToInstall(module);
    }
}
