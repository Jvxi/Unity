package com.jvxi.unity.novel.service.story;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.model.commit.StoryEvent;
import com.jvxi.unity.novel.persistence.entity.StoryEventEntity;
import com.jvxi.unity.novel.persistence.repository.StoryEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EventAuditService {

    private static final Logger log = LoggerFactory.getLogger(EventAuditService.class);

    private final StoryEventRepository storyEventRepository;
    private final ObjectMapper objectMapper;

    public EventAuditService(StoryEventRepository storyEventRepository, ObjectMapper objectMapper) {
        this.storyEventRepository = storyEventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 记录事件
     */
    @Transactional
    public StoryEvent recordEvent(String bookId, StoryEvent event) {
        StoryEventEntity entity = new StoryEventEntity();
        entity.setBookId(bookId);
        entity.setChapterNumber(event.chapterNumber());
        entity.setEventType(StoryEventEntity.EventType.valueOf(event.eventType()));
        entity.setSubject(event.subject());
        entity.setField(event.field());
        entity.setOldValue(event.oldValue());
        entity.setNewValue(event.newValue());
        entity.setReason(event.reason());
        entity.setPayloadJson(serialize(event.payload()));

        storyEventRepository.save(entity);
        log.info("记录故事事件: bookId={}, type={}, subject={}, chapter={}", bookId, event.eventType(), event.subject(), event.chapterNumber());

        return toModel(entity);
    }

    /**
     * 获取所有事件
     */
    public List<StoryEvent> getAllEvents(String bookId) {
        return storyEventRepository.findByBookId(bookId)
                .stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }

    /**
     * 获取章节事件
     */
    public List<StoryEvent> getChapterEvents(String bookId, int chapterNumber) {
        return storyEventRepository.findByBookIdAndChapterNumber(bookId, chapterNumber)
                .stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }

    /**
     * 获取实体相关事件
     */
    public List<StoryEvent> getEntityEvents(String bookId, String entityId) {
        return storyEventRepository.findByBookIdAndSubject(bookId, entityId)
                .stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }

    /**
     * 按类型获取事件
     */
    public List<StoryEvent> getEventsByType(String bookId, String eventType) {
        StoryEventEntity.EventType type = StoryEventEntity.EventType.valueOf(eventType);
        return storyEventRepository.findByBookIdAndEventType(bookId, type)
                .stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }

    // ============ 私有辅助方法 ============

    private StoryEvent toModel(StoryEventEntity entity) {
        Map<String, Object> payload = Map.of();
        try {
            if (entity.getPayloadJson() != null) {
                payload = objectMapper.readValue(entity.getPayloadJson(), new TypeReference<>() {});
            }
        } catch (IOException e) {
            log.warn("解析payload失败", e);
        }

        return new StoryEvent(
                entity.getId(),
                entity.getChapterNumber(),
                entity.getEventType().name(),
                entity.getSubject(),
                entity.getField(),
                entity.getOldValue(),
                entity.getNewValue(),
                entity.getReason(),
                payload
        );
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSON序列化失败", e);
            return "{}";
        }
    }
}

