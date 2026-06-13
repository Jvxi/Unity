package com.jvxi.unity.controller;

import com.jvxi.unity.model.ChatMessage;
import com.jvxi.unity.model.ChatMessageType;
import com.jvxi.unity.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ChatWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatService chatService;

    @MessageMapping("/chat.private")
    public void handlePrivateMessage(@Payload Map<String, Object> payload, Principal principal) {
        Long senderId = extractUserId(principal);
        if (senderId == null) return;

        try {
            Long receiverId = Long.valueOf(payload.get("receiverId").toString());
            String content = valueAsString(payload.get("content"));
            String typeStr = payload.getOrDefault("messageType", "TEXT").toString();
            ChatMessageType type = ChatMessageType.valueOf(typeStr);

            ChatMessage msg;
            if (type == ChatMessageType.IMAGE || type == ChatMessageType.FILE) {
                String fileUrl = valueAsString(payload.getOrDefault("fileUrl", content));
                String fileName = valueAsString(payload.get("fileName"));
                Long fileSize = valueAsLong(payload.get("fileSize"));
                msg = chatService.sendPrivateFileMessage(senderId, receiverId, fileUrl, fileName, fileSize, type);
            } else {
                msg = chatService.sendPrivateMessage(senderId, receiverId, content, type);
            }

            Map<String, Object> response = buildMessageResponse(msg);
            messagingTemplate.convertAndSendToUser(receiverId.toString(), "/queue/private", response);
            messagingTemplate.convertAndSendToUser(senderId.toString(), "/queue/private", response);
        } catch (Exception e) {
            sendError(senderId, e.getMessage());
        }
    }

    @MessageMapping("/chat.group")
    public void handleGroupMessage(@Payload Map<String, Object> payload, Principal principal) {
        Long senderId = extractUserId(principal);
        if (senderId == null) return;

        try {
            Long groupId = Long.valueOf(payload.get("groupId").toString());
            String content = valueAsString(payload.get("content"));
            String typeStr = payload.getOrDefault("messageType", "TEXT").toString();
            ChatMessageType type = ChatMessageType.valueOf(typeStr);

            ChatMessage msg;
            if (type == ChatMessageType.IMAGE || type == ChatMessageType.FILE) {
                String fileUrl = valueAsString(payload.getOrDefault("fileUrl", content));
                String fileName = valueAsString(payload.get("fileName"));
                Long fileSize = valueAsLong(payload.get("fileSize"));
                msg = chatService.sendGroupFileMessage(senderId, groupId, fileUrl, fileName, fileSize, type);
            } else {
                msg = chatService.sendGroupMessage(senderId, groupId, content, type);
            }

            Map<String, Object> response = buildMessageResponse(msg);
            messagingTemplate.convertAndSend("/topic/group/" + groupId, response);
        } catch (Exception e) {
            sendError(senderId, e.getMessage());
        }
    }

    @MessageMapping("/chat.recall")
    public void handleRecallMessage(@Payload Map<String, Object> payload, Principal principal) {
        Long senderId = extractUserId(principal);
        if (senderId == null) return;

        try {
            Long messageId = Long.valueOf(payload.get("messageId").toString());
            boolean success = chatService.recallMessage(messageId, senderId);

            if (success) {
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "RECALL");
                notification.put("messageId", messageId);

                if (payload.containsKey("groupId")) {
                    Long groupId = Long.valueOf(payload.get("groupId").toString());
                    messagingTemplate.convertAndSend("/topic/group/" + groupId, notification);
                } else if (payload.containsKey("receiverId")) {
                    Long receiverId = Long.valueOf(payload.get("receiverId").toString());
                    messagingTemplate.convertAndSendToUser(receiverId.toString(), "/queue/private", notification);
                    messagingTemplate.convertAndSendToUser(senderId.toString(), "/queue/private", notification);
                }
            }
        } catch (Exception e) {
            sendError(senderId, e.getMessage());
        }
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload Map<String, Object> payload, Principal principal) {
        Long senderId = extractUserId(principal);
        if (senderId == null) return;

        try {
            Map<String, Object> typing = new HashMap<>();
            typing.put("type", "TYPING");
            typing.put("senderId", senderId);

            if (payload.containsKey("groupId")) {
                Long groupId = Long.valueOf(payload.get("groupId").toString());
                messagingTemplate.convertAndSend("/topic/group/" + groupId, typing);
            } else if (payload.containsKey("receiverId")) {
                Long receiverId = Long.valueOf(payload.get("receiverId").toString());
                typing.put("receiverId", receiverId);
                messagingTemplate.convertAndSendToUser(receiverId.toString(), "/queue/private", typing);
            }
        } catch (Exception e) {
            sendError(senderId, e.getMessage());
        }
    }

    private Long extractUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token) {
            return (Long) token.getPrincipal();
        }
        return null;
    }

    private Map<String, Object> buildMessageResponse(ChatMessage msg) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", "MESSAGE");
        response.put("id", msg.getId());
        response.put("senderId", msg.getSenderId());
        response.put("receiverId", msg.getReceiverId());
        response.put("groupId", msg.getGroupId());
        response.put("content", msg.getContent());
        response.put("messageType", msg.getMessageType().name());
        response.put("fileUrl", msg.getFileUrl());
        response.put("fileName", msg.getFileName());
        response.put("fileSize", msg.getFileSize());
        response.put("createdAt", msg.getCreatedAt().toString());
        response.put("recalled", msg.isRecalled());
        Map<String, Object> sender = chatService.getUserBasicInfo(msg.getSenderId());
        response.put("senderNickname", sender.get("nickname"));
        response.put("senderAvatarUrl", sender.get("avatarUrl"));
        return response;
    }

    private String valueAsString(Object value) {
        return value == null ? "" : value.toString();
    }

    private Long valueAsLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void sendError(Long userId, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "CHAT_ERROR");
        payload.put("message", message != null ? message : "消息发送失败");
        messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/private", payload);
    }
}
