package com.fintrack.service.impl;

import com.fintrack.model.AuditLog;
import com.fintrack.model.User;
import com.fintrack.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AuditService — Owner: Saanvi Kakkar
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async
    public void log(User user, String action, String entityType, Long entityId,
                    String oldValue, String newValue, String ipAddress, String userAgent) {
        AuditLog entry = new AuditLog();
        entry.setUser(user);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setOldValue(oldValue);
        entry.setNewValue(newValue);
        entry.setIpAddress(ipAddress);
        entry.setUserAgent(userAgent);
        auditLogRepository.save(entry);
    }

    public Page<AuditLog> getRecentLogs(int page, int size) {
        return auditLogRepository.findAllByOrderByTimestampDesc(PageRequest.of(page, size));
    }

    public long countSince(LocalDateTime since) {
        return auditLogRepository.countByTimestampAfter(since);
    }

    public List<AuditLog> getLogsForEntity(String type, Long id) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(type, id);
    }
}
