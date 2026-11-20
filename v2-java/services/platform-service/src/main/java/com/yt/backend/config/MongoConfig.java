package com.yt.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

/**
 * Spring Data MongoDB. Ket noi doc tu spring.data.mongodb.uri (application.properties, bien MONGO_URI).
 * Mac dinh Spring Data ghi them field `_class` (ten class Java) vao moi document.
 * Tat di de document giong het ban Node ghi (2 server dung chung DB).
 */
@Configuration
public class MongoConfig {

    @Bean
    public MappingMongoConverter mappingMongoConverter(MongoDatabaseFactory factory, MongoMappingContext context,
                                                       MongoCustomConversions conversions) {
        MappingMongoConverter converter = new MappingMongoConverter(new DefaultDbRefResolver(factory), context);
        converter.setCustomConversions(conversions);
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        return converter;
    }
}
