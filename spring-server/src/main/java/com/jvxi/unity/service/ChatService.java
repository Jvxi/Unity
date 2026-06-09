package com.jvxi.unity.service;

import com.jvxi.unity.model.*;
import com.jvxi.unity.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChatService {

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private ChatSessionRepository sessionRepository;

    @Autowired
    private ChatGroupMemberRepository groupMemberRepository;

    @Autowired
    private ChatGroupRepository groupRepository;

    @Autowired
    private UserService userService;

    @Transactional
    public ChatMessage sendPrivateMessage(Long senderId, Long receiverId, String content, ChatMessageType type) {
        ChatMessage msg = new ChatMessage();
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(content);
        msg.setMessageType(type);
        msg = messageRepository.save(msg);

        // 更新发送者的会话
        upsertPrivateSession(senderId, receiverId, msg.getId());

        // 更新接收者的会话并增加未读数
        upsertPrivateSession(receiverId, senderId, msg.getId());
        sessionRepository.incrementUnread(receiverId, senderId, null, msg.getId());

        return msg;
    }

    @Transactional
    public ChatMessage sendGroupMessage(Long senderId, Long groupId, String content, ChatMessageType type) {
        ChatMessage msg = new ChatMessage();
        msg.setSenderId(senderId);
        msg.setGroupId(groupId);
        msg.setContent(content);
        msg.setMessageType(type);
        msg = messageRepository.save(msg);

        // 更新发送者会话
        upsertGroupSession(senderId, groupId, msg.getId());

        // 更新群内其他成员会话
        List<ChatGroupMember> members = groupMemberRepository.findByGroupId(groupId);
        for (ChatGroupMember member : members) {
            if (!member.getUserId().equals(senderId)) {
                upsertGroupSession(member.getUserId(), groupId, msg.getId());
                sessionRepository.incrementUnread(member.getUserId(), null, groupId, msg.getId());
            }
        }

        return msg;
    }

    @Transactional
    public ChatMessage sendPrivateFileMessage(Long senderId, Long receiverId, String fileUrl, String fileName, Long fileSize, ChatMessageType type) {
        ChatMessage msg = new ChatMessage();
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(type == ChatMessageType.IMAGE ? "[图片]" : "[文件]");
        msg.setMessageType(type);
        msg.setFileUrl(fileUrl);
        msg.setFileName(fileName);
        msg.setFileSize(fileSize);
        msg = messageRepository.save(msg);

        upsertPrivateSession(senderId, receiverId, msg.getId());
        upsertPrivateSession(receiverId, senderId, msg.getId());
        sessionRepository.incrementUnread(receiverId, senderId, null, msg.getId());
        return msg;
    }

    @Transactional
    public ChatMessage sendGroupFileMessage(Long senderId, Long groupId, String fileUrl, String fileName, Long fileSize, ChatMessageType type) {
        ChatMessage msg = new ChatMessage();
        msg.setSenderId(senderId);
        msg.setGroupId(groupId);
        msg.setContent(type == ChatMessageType.IMAGE ? "[图片]" : "[文件]");
        msg.setMessageType(type);
        msg.setFileUrl(fileUrl);
        msg.setFileName(fileName);
        msg.setFileSize(fileSize);
        msg = messageRepository.save(msg);

        upsertGroupSession(senderId, groupId, msg.getId());
        List<ChatGroupMember> members = groupMemberRepository.findByGroupId(groupId);
        for (ChatGroupMember member : members) {
            if (!member.getUserId().equals(senderId)) {
                upsertGroupSession(member.getUserId(), groupId, msg.getId());
                sessionRepository.incrementUnread(member.getUserId(), null, groupId, msg.getId());
            }
        }
        return msg;
    }

    public List<Map<String, Object>> getPrivateHistory(Long user1, Long user2, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<ChatMessage> messages = messageRepository.findPrivateMessages(user1, user2, pageable);
        return messages.stream().map(this::messageToMap).toList();
    }

    public List<Map<String, Object>> getPrivateHistoryDesc(Long user1, Long user2, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<ChatMessage> messages = messageRepository.findPrivateMessagesDesc(user1, user2, pageable).getContent();
        Collections.reverse(messages);
        return messages.stream().map(this::messageToMap).toList();
    }

    public List<Map<String, Object>> getGroupHistory(Long groupId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<ChatMessage> messages = messageRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageable);
        Collections.reverse(messages);
        return messages.stream().map(this::messageToMap).toList();
    }

    public List<Map<String, Object>> searchMessages(Long userId, Long targetUserId, Long groupId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (groupId != null) {
            return messageRepository.searchGroupMessages(groupId, keyword, pageable)
                    .stream().map(this::messageToMap).toList();
        } else if (targetUserId != null) {
            return messageRepository.searchPrivateMessages(userId, targetUserId, keyword, pageable)
                    .stream().map(this::messageToMap).toList();
        }
        return Collections.emptyList();
    }

    @Transactional
    public boolean recallMessage(Long messageId, Long userId) {
        ChatMessage msg = messageRepository.findById(messageId).orElse(null);
        if (msg == null || !msg.getSenderId().equals(userId)) return false;
        if (msg.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(2))) return false;
        msg.setRecalled(true);
        msg.setContent("[消息已撤回]");
        msg.setFileUrl(null);
        msg.setFileName(null);
        msg.setFileSize(null);
        messageRepository.save(msg);
        return true;
    }

    public List<Map<String, Object>> getSessions(Long userId) {
        List<ChatSession> sessions = sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatSession s : sessions) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", s.getId());
            map.put("updatedAt", s.getUpdatedAt().toString());
            map.put("unreadCount", s.getUnreadCount());
            map.put("lastMessageId", s.getLastMessageId());

            if (s.getTargetUserId() != null) {
                map.put("type", "PRIVATE");
                map.put("targetUserId", s.getTargetUserId());
                Map<String, Object> userInfo = userService.getUserBasicInfo(s.getTargetUserId());
                map.put("targetNickname", userInfo.get("nickname"));
                map.put("targetAvatarUrl", userInfo.get("avatarUrl"));
                map.put("targetOnlineStatus", userInfo.get("onlineStatus"));
            } else if (s.getTargetGroupId() != null) {
                map.put("type", "GROUP");
                map.put("targetGroupId", s.getTargetGroupId());
                groupRepository.findById(s.getTargetGroupId()).ifPresent(group -> {
                    map.put("targetNickname", group.getName());
                    map.put("targetAvatarUrl", group.getAvatarUrl() != null ? group.getAvatarUrl() : "");
                });
            }

            // 获取最后一条消息预览
            if (s.getLastMessageId() != null) {
                messageRepository.findById(s.getLastMessageId()).ifPresent(lastMsg -> {
                    map.put("lastMessageContent", lastMsg.isRecalled() ? "[消息已撤回]" : lastMsg.getContent());
                    map.put("lastMessageSenderId", lastMsg.getSenderId());
                    map.put("lastMessageType", lastMsg.getMessageType().name());
                });
            }

            result.add(map);
        }
        return result;
    }

    public Map<String, Object> getUserBasicInfo(Long userId) {
        return userService.getUserBasicInfo(userId);
    }

    @Transactional
    public void markAsRead(Long userId, Long targetUserId, Long groupId) {
        if (targetUserId != null) {
            sessionRepository.clearPrivateUnread(userId, targetUserId);
        } else if (groupId != null) {
            sessionRepository.clearGroupUnread(userId, groupId);
        }
    }

    @Transactional
    public void ensureGroupSession(Long userId, Long groupId) {
        if (sessionRepository.findByUserIdAndTargetGroupId(userId, groupId).isPresent()) {
            return;
        }
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTargetGroupId(groupId);
        session.setUnreadCount(0);
        sessionRepository.save(session);
    }

    private void upsertPrivateSession(Long userId, Long targetUserId, Long messageId) {
        ChatSession session = sessionRepository.findByUserIdAndTargetUserId(userId, targetUserId).orElse(null);
        if (session == null) {
            session = new ChatSession();
            session.setUserId(userId);
            session.setTargetUserId(targetUserId);
        }
        session.setLastMessageId(messageId);
        sessionRepository.save(session);
    }

    private void upsertGroupSession(Long userId, Long groupId, Long messageId) {
        ChatSession session = sessionRepository.findByUserIdAndTargetGroupId(userId, groupId).orElse(null);
        if (session == null) {
            session = new ChatSession();
            session.setUserId(userId);
            session.setTargetGroupId(groupId);
        }
        session.setLastMessageId(messageId);
        sessionRepository.save(session);
    }

    private Map<String, Object> messageToMap(ChatMessage msg) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", msg.getId());
        map.put("senderId", msg.getSenderId());
        map.put("receiverId", msg.getReceiverId());
        map.put("groupId", msg.getGroupId());
        map.put("content", msg.getContent());
        map.put("messageType", msg.getMessageType().name());
        map.put("fileUrl", msg.getFileUrl());
        map.put("fileName", msg.getFileName());
        map.put("fileSize", msg.getFileSize());
        map.put("createdAt", msg.getCreatedAt().toString());
        map.put("recalled", msg.isRecalled());

        // 附加发送者信息
        Map<String, Object> senderInfo = userService.getUserBasicInfo(msg.getSenderId());
        map.put("senderNickname", senderInfo.get("nickname"));
        map.put("senderAvatarUrl", senderInfo.get("avatarUrl"));
        return map;
    }
}
