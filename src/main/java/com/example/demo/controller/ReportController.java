package com.example.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.User;
import com.example.demo.service.ReportService;

@Controller
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/items/{itemId}/report")
    public String reportItem(
            @PathVariable Integer itemId,
            @RequestParam String reason,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        User loginUser = getLoginUser(session);
        if (loginUser == null) {
            return "redirect:/login";
        }

        try {
            reportService.reportItem(itemId, loginUser.getUserId(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "通報を送信しました。管理者が確認します。");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return redirectBack(request, "/detail/" + itemId);
    }

    @PostMapping("/messages/{messageId}/report")
    public String reportMessage(
            @PathVariable Integer messageId,
            @RequestParam String reason,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        User loginUser = getLoginUser(session);
        if (loginUser == null) {
            return "redirect:/login";
        }

        try {
            reportService.reportMessage(messageId, loginUser.getUserId(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "メッセージを通報しました。管理者が確認します。");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return redirectBack(request, "/messages");
    }

    private String redirectBack(HttpServletRequest request, String fallbackPath) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "redirect:" + fallbackPath;
        }
        return "redirect:" + referer;
    }

    private User getLoginUser(HttpSession session) {
        Object value = session.getAttribute("loginUser");
        if (value instanceof User user) {
            return user;
        }
        return null;
    }
}
