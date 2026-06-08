package com.jvxi.unity.controller;

import com.jvxi.unity.model.AnalysisRecord;
import com.jvxi.unity.service.AnalysisRecordService;
import com.jvxi.unity.service.JwtService;
import com.jvxi.unity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/records")
public class AnalysisRecordController {

    @Autowired
    private AnalysisRecordService recordService;

    @Autowired
    private JwtService jwtService;

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getRecords(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            Long userId = jwtService.extractUserId(cleanToken);
            Page<AnalysisRecord> records = recordService.getUserRecords(userId, page, size);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", records.getContent(),
                "total", records.getTotalElements(),
                "page", page,
                "size", size
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getRecord(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        try {
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            Long userId = jwtService.extractUserId(cleanToken);
            AnalysisRecord record = recordService.getRecord(id, userId);
            return ResponseEntity.ok(Map.of("success", true, "data", record));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteRecord(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        try {
            String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            Long userId = jwtService.extractUserId(cleanToken);
            recordService.deleteRecord(id, userId);
            return ResponseEntity.ok(Map.of("success", true, "message", "删除成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}