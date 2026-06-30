package com.example.groupshop.config;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Jackson configuration for global serialization rules.
 * <p>
 * Long / long fields whose name is {@code "id"} or ends with {@code "Id"} are
 * serialized as JSON strings rather than numbers. This prevents JavaScript
 * precision loss when MyBatis-Plus ASSIGN_ID (snowflake) values exceed
 * {@code Number.MAX_SAFE_INTEGER} (9,007,199,254,740,991).
 * <p>
 * Amount fields ({@code totalAmount}, {@code payAmount}, …) remain numeric
 * because their values are always within the safe integer range.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longIdToStringCustomizer() {
        return builder -> builder.postConfigurer(objectMapper ->
                objectMapper.setSerializerFactory(
                        objectMapper.getSerializerFactory()
                                .withSerializerModifier(new BeanSerializerModifier() {
                                    @Override
                                    public List<BeanPropertyWriter> changeProperties(
                                            SerializationConfig config,
                                            BeanDescription beanDesc,
                                            List<BeanPropertyWriter> beanProperties) {
                                        for (BeanPropertyWriter writer : beanProperties) {
                                            if (isLongType(writer) && isIdField(writer)) {
                                                writer.assignSerializer(ToStringSerializer.instance);
                                            }
                                        }
                                        return beanProperties;
                                    }

                                    private boolean isLongType(BeanPropertyWriter writer) {
                                        Class<?> raw = writer.getType().getRawClass();
                                        return raw == Long.class || raw == long.class;
                                    }

                                    private boolean isIdField(BeanPropertyWriter writer) {
                                        String name = writer.getName();
                                        return "id".equals(name) || name.endsWith("Id");
                                    }
                                })
                ));
    }
}
