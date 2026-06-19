package com.example.demo.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendTemporaryPassword(String to, String name, String temporaryPassword) {
        MailUtil.sendTemporaryPasswordMail(mailSender, to, name, temporaryPassword);
    }

    public void sendMessageNotification(String to, String receiverName, String senderName, String itemName) {
        MailUtil.sendMessageNotificationMail(mailSender, to, receiverName, senderName, itemName);
    }
}
