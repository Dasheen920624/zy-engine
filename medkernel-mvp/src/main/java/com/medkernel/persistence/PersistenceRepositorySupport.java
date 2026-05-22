package com.medkernel.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medkernel.common.IdAllocatorRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public abstract class PersistenceRepositorySupport {
    protected final EnginePersistenceProperties properties;
    protected final ObjectMapper objectMapper;
    protected final DataSource dataSource;
    protected final IdAllocatorRepository idAllocatorRepository;

    protected PersistenceRepositorySupport(EnginePersistenceProperties properties,
                                           ObjectMapper objectMapper,
                                           DataSource dataSource,
                                           IdAllocatorRepository idAllocatorRepository) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.dataSource = dataSource;
        this.idAllocatorRepository = idAllocatorRepository;
    }

    public boolean enabled() {
        return properties.isEnabled() && properties.hasRequiredCredentials();
    }

    protected Connection connection() throws SQLException {
        // PR-FINAL-15: 全部走 HikariCP 连接池（EngineDataSourceConfig 暴露的 DataSource）。
        // HikariCP 内部自带连接获取重试、失效检测、leak detection（threshold=2s）等机制。
        return dataSource.getConnection();
    }

    protected long nextId() {
        return idAllocatorRepository.nextId();
    }

    protected long nextId(String tenantId) {
        return idAllocatorRepository.nextId(tenantId);
    }

    protected String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("JSON serialization failed", ex);
        }
    }

    /**
     * 提供给其他 Service 复用的安全 JSON 序列化：null 返回 null，序列化异常返回 null 不抛出，
     * 避免持久化层因输入异常打断业务主链路。
     */
    public String toJsonOrNull(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    protected String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text;
    }

    /**
     * 解析 Object 为 double，null 或不可解析时返回 defaultValue。
     */
    protected double doubleValue(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    protected String filterValue(Map<String, String> filters, String key) {
        if (filters == null) {
            return null;
        }
        String value = filters.get(key);
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    protected int filterInt(Map<String, String> filters, String key, int defaultValue) {
        String value = filterValue(filters, key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    protected String nowText() {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now());
    }

    protected String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    protected java.sql.Date parseSqlDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return java.sql.Date.valueOf(LocalDate.parse(value.trim()));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    protected Timestamp parseTimestamp(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String text = value.trim();
        try {
            return Timestamp.from(OffsetDateTime.parse(text).toInstant());
        } catch (RuntimeException ignored) {
            try {
                return Timestamp.valueOf(text.replace('T', ' '));
            } catch (RuntimeException ex) {
                return null;
            }
        }
    }

    protected String formatDate(java.sql.Date value) {
        return value == null ? null : value.toLocalDate().toString();
    }

    protected String formatTimestamp(Timestamp value) {
        return value == null
                ? null
                : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value.toInstant().atOffset(ZoneOffset.UTC));
    }

    protected long extractNumericId(String text) {
        if (text == null) {
            return nextId();
        }
        String digits = text.replaceAll("\\D", "");
        if (digits.length() > 15) {
            digits = digits.substring(digits.length() - 15);
        }
        if (digits.isEmpty()) {
            return nextId();
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return nextId();
        }
    }

    protected String orgValue(Map<String, Object> detail, String snakeKey, String camelKey) {
        if (detail == null) {
            return null;
        }
        String value = string(detail.get(snakeKey), null);
        if (value == null) {
            value = string(detail.get(camelKey), null);
        }
        return value;
    }
}
