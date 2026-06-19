package com.example.demo.controller;

import java.time.LocalDateTime;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.service.MailUtil;
import com.example.demo.service.PasswordUtil;
import com.example.demo.repository.UserRepository;
import com.example.demo.entity.User;

@Controller
public class LoginController {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    public LoginController(
            UserRepository userRepository,
            ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.userRepository = userRepository;
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        if (email == null || email.isBlank()
                || password == null || password.isBlank()) {
            model.addAttribute("error", "メールアドレスとパスワードを入力してください。");
            return "login";
        }

        User user = userRepository.findByEmail(email.trim()).orElse(null);

        if (user == null) {
            model.addAttribute("email", email);
            model.addAttribute("error", "メールアドレスまたはパスワードが違います。");
            return "login";
        }

        if (!Integer.valueOf(0).equals(user.getStatus())) {
            model.addAttribute("email", email);
            model.addAttribute("error", "このアカウントは無効です。パスワードリセットを行うか、管理者に連絡してください。");
            return "login";
        }

        if (!PasswordUtil.matches(password, user.getPassword())) {
            int failCount = user.getLoginFailCount() == null ? 1 : user.getLoginFailCount() + 1;
            user.setLoginFailCount(failCount);

            if (failCount >= 3) {
                user.setStatus(1);
                userRepository.save(user);
                model.addAttribute("error", "ログインに3回失敗したため、アカウントを無効化しました。パスワードリセットを行ってください。");
            } else {
                userRepository.save(user);
                model.addAttribute("error", "メールアドレスまたはパスワードが違います。あと" + (3 - failCount) + "回失敗するとアカウントが無効化されます。");
            }

            model.addAttribute("email", email);
            return "login";
        }

        LocalDateTime previousLoginAt = user.getLoginAt();
        user.setLoginFailCount(0);
        user.setLoginAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        session.setAttribute("loginUser", updatedUser);
        session.setAttribute("previousLoginAt", previousLoginAt);

        if (needsPasswordChange(updatedUser)) {
            return "redirect:/change-password";
        }

        if (Integer.valueOf(1).equals(updatedUser.getRole())) {
            return "redirect:/admin/users";
        }

        return "redirect:/item-101";
    }

    @GetMapping("/change-password")
    public String changePasswordForm(HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) {
            return "redirect:/login";
        }
        model.addAttribute("loginUser", loginUser);
        return "change_password";
    }

    @PostMapping("/change-password")
    public String changePassword(
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session,
            Model model) {

        User loginUser = getLoginUser(session);
        if (loginUser == null) {
            return "redirect:/login";
        }

        if (newPassword == null || newPassword.isBlank()) {
            model.addAttribute("error", "新しいパスワードを入力してください。");
            return "change_password";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "確認用パスワードが一致しません。");
            return "change_password";
        }

        User user = userRepository.findById(loginUser.getUserId()).orElse(null);
        if (user == null) {
            session.invalidate();
            return "redirect:/login";
        }

        user.setPassword(PasswordUtil.encode(newPassword));
        user.setTemporaryPassword(false);
        user.setLastPasswordChange(LocalDateTime.now());
        user.setLoginFailCount(0);
        user.setStatus(0);

        User updatedUser = userRepository.save(user);
        session.setAttribute("loginUser", updatedUser);

        if (Integer.valueOf(1).equals(updatedUser.getRole())) {
            return "redirect:/admin/users";
        }

        return "redirect:/item-101";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "forgot_password";
    }

    @PostMapping("/forgot-password/reset")
    public String forgotPasswordReset(
            @RequestParam String email,
            Model model) {

        if (email == null || email.isBlank()) {
            model.addAttribute("error", "メールアドレスを入力してください。");
            return "forgot_password";
        }

        User user = userRepository.findByEmail(email.trim()).orElse(null);
        if (user == null) {
            model.addAttribute("error", "入力されたメールアドレスの社員は存在しません。");
            return "forgot_password";
        }

        String temporaryPassword = PasswordUtil.generateTemporaryPassword();
        user.setPassword(PasswordUtil.encode(temporaryPassword));
        user.setTemporaryPassword(true);
        user.setLastPasswordChange(LocalDateTime.now());
        user.setStatus(0);
        user.setLoginFailCount(0);
        userRepository.save(user);

        MailUtil.sendTemporaryPasswordMail(mailSender, user.getEmail(), user.getName(), temporaryPassword);

        model.addAttribute("message", "仮パスワードを発行しました。メール設定が有効な場合は社員へ送信されます。");
        model.addAttribute("tempPassword", temporaryPassword);
        return "forgot_password";
    }

    @GetMapping("/users")
    public String users(HttpSession session, Model model) {

        Object loginUserObject = session.getAttribute("loginUser");

        if (!(loginUserObject instanceof User)) {
            return "redirect:/login";
        }

        User loginUser = (User) loginUserObject;

        if (!Integer.valueOf(1).equals(loginUser.getRole())) {
            return "redirect:/item-101";
        }

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("users", userRepository.findAll());

        return "users";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    private boolean needsPasswordChange(User user) {
        if (Boolean.TRUE.equals(user.getTemporaryPassword())) {
            return true;
        }

        if (user.getLastPasswordChange() == null) {
            return true;
        }

        return user.getLastPasswordChange().isBefore(LocalDateTime.now().minusMonths(3));
    }

    private User getLoginUser(HttpSession session) {
        Object value = session.getAttribute("loginUser");
        if (value instanceof User user) {
            return user;
        }
        return null;
    }
}
