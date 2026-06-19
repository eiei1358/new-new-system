package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.entity.Item;
import com.example.demo.entity.Message;
import com.example.demo.entity.Report;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.ReportRepository;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final ItemRepository itemRepository;
    private final MessageRepository messageRepository;

    public ReportService(
            ReportRepository reportRepository,
            ItemRepository itemRepository,
            MessageRepository messageRepository) {
        this.reportRepository = reportRepository;
        this.itemRepository = itemRepository;
        this.messageRepository = messageRepository;
    }

    public List<Report> findAll() {
        return reportRepository.findAllByOrderByCreatedAtDesc();
    }

    public Report createReport(Integer targetType, Integer targetId, Integer userId, String reason) {
        Report report = new Report();
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setUserId(userId);
        report.setReason(normalizeReason(reason));
        report.setStatus(0);
        report.setCreatedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }

    public Report reportItem(Integer itemId, Integer reporterUserId, String reason) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("商品が見つかりません。"));

        if (item.getUserId() != null && item.getUserId().equals(reporterUserId)) {
            throw new IllegalArgumentException("自分の出品物は通報できません。");
        }

        Report report = createReport(0, itemId, reporterUserId, reason);

        item.setStatus(3);
        itemRepository.save(item);

        return report;
    }

    public Report reportMessage(Integer messageId, Integer reporterUserId, String reason) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("メッセージが見つかりません。"));

        boolean participant = reporterUserId != null
                && (reporterUserId.equals(message.getSenderId())
                        || reporterUserId.equals(message.getReceiverId()));
        if (!participant) {
            throw new IllegalArgumentException("このメッセージは通報できません。");
        }

        return createReport(1, messageId, reporterUserId, reason);
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "理由未入力";
        }
        return reason.trim();
    }
}
