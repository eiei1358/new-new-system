package com.example.demo.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public final class MailUtil {

    private MailUtil() {
    }

    public static void sendTemporaryPasswordMail(JavaMailSender mailSender, String to, String name, String tempPassword) {
        if (mailSender == null || to == null || to.isBlank()) {
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("【社内フリマ】仮パスワードのお知らせ");
            message.setText((name == null ? "社員" : name) + " 様\n\n"
                    + "社内フリマシステムの仮パスワードを発行しました。\n\n"
                    + "仮パスワード：" + tempPassword + "\n\n"
                    + "ログイン後、パスワード変更画面で新しいパスワードに変更してください。");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("仮パスワード通知メールの送信に失敗しました: " + e.getMessage());
        }
    }

    public static void sendMessageNotificationMail(JavaMailSender mailSender, String to, String receiverName,
            String senderName, String content) {
        if (mailSender == null || to == null || to.isBlank()) {
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("【社内フリマ】新着メッセージのお知らせ");
            message.setText((receiverName == null ? "社員" : receiverName) + " 様\n\n"
                    + (senderName == null ? "相手" : senderName) + " さんからメッセージが届きました。\n\n"
                    + "――――――――――――――――――\n"
                    + content + "\n"
                    + "――――――――――――――――――\n\n"
                    + "社内フリマシステムにログインして確認してください。");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("メッセージ通知メールの送信に失敗しました: " + e.getMessage());
        }
    }
}
