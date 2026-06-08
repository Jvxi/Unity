package com.jvxi.unity.service;

import com.jvxi.unity.model.AnalysisRecord;
import com.jvxi.unity.repository.AnalysisRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalysisRecordService {

    @Autowired
    private AnalysisRecordRepository repository;

    public AnalysisRecord save(AnalysisRecord record) {
        return repository.save(record);
    }

    public List<AnalysisRecord> getUserRecords(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Page<AnalysisRecord> getUserRecords(Long userId, int page, int size) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    public AnalysisRecord getRecord(Long id, Long userId) {
        AnalysisRecord record = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("记录不存在"));
        if (!record.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问");
        }
        return record;
    }

    public void deleteRecord(Long id, Long userId) {
        AnalysisRecord record = getRecord(id, userId);
        repository.delete(record);
    }

    public long getUserRecordCount(Long userId) {
        return repository.countByUserId(userId);
    }
}