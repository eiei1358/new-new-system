package com.example.demo.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.service.MailUtil;
import com.example.demo.service.PasswordUtil;
import com.example.demo.entity.User;

@Controller
public class AdminUserController {

    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JavaMailSender mailSender;

    public AdminUserController(
            NamedParameterJdbcTemplate jdbcTemplate,
            ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    // 社員管理
    @GetMapping("/admin/users")
    public String listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            HttpSession session,
            Model model) {

        if (!isAdmin(session)) {
            return redirectByLoginState(session);
        }

        model.addAttribute("loginUser", getLoginUser(session));
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("users", findUsers(keyword, status));
        return "admin_users";
    }

    // 社員追加画面
    @GetMapping("/admin/users/new")
    public String newUser(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return redirectByLoginState(session);
        }

        model.addAttribute("loginUser", getLoginUser(session));
        return "admin_user_new";
    }

    // 社員追加
    @PostMapping("/admin/users/create")
    public String createUser(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam(required = false) String password,
            @RequestParam Integer role,
            @RequestParam(required = false) String department,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) {
            return redirectByLoginState(session);
        }

        String error = validateUserInput(name, email, role);
        if (error != null) {
            redirectAttributes.addFlashAttribute("error", error);
            return "redirect:/admin/users/new";
        }

        String fixedEmail = email.trim();
        if (existsEmail(fixedEmail, null)) {
            redirectAttributes.addFlashAttribute("error", "このメールアドレスは既に使用されています。");
            return "redirect:/admin/users/new";
        }

        boolean temporary = isBlank(password);
        String plainPassword = temporary ? generateTemporaryPassword() : password.trim();
        Integer userId = nextUserId();

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("name", name.trim())
                .addValue("email", fixedEmail)
                .addValue("password", PasswordUtil.encode(plainPassword))
                .addValue("role", role)
                .addValue("department", emptyToNull(department))
                .addValue("status", 0)
                .addValue("now", LocalDateTime.now())
                .addValue("temporaryPassword", temporary)
                .addValue("loginFailCount", 0);

        jdbcTemplate.update("""
                INSERT INTO users
                  (user_id, name, email, password, role, department, status,
                   created_at, icon_url, login_at, temporary_password, last_password_change, login_fail_count)
                VALUES
                  (:userId, :name, :email, :password, :role, :department, :status,
                   :now, NULL, :now, :temporaryPassword, :now, :loginFailCount)
                """, params);

        MailUtil.sendTemporaryPasswordMail(mailSender, fixedEmail, name.trim(), plainPassword);

        redirectAttributes.addFlashAttribute("message", "社員を追加しました。メール設定が有効な場合は仮パスワードを送信しています。");
        redirectAttributes.addFlashAttribute("tempPassword", plainPassword);
        return "redirect:/admin/users";
    }

    // CSV一括登録画面
    @GetMapping("/admin/users/csv")
    public String csvForm(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return redirectByLoginState(session);
        }

        model.addAttribute("loginUser", getLoginUser(session));
        return "admin_user_csv";
    }

    // CSV一括登録
    @PostMapping("/admin/users/csv")
    @Transactional
    public String importCsv(
            @RequestParam("file") MultipartFile file,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) {
            return redirectByLoginState(session);
        }

        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "CSVファイルを選択してください。");
            return "redirect:/admin/users/csv";
        }

        try {
            ImportResult result = importUsersFromCsv(file);
            redirectAttributes.addFlashAttribute("message", result.successCount + "件の社員をCSV登録しました。");
            if (!result.temporaryPasswords.isEmpty()) {
                redirectAttributes.addFlashAttribute("tempPasswords", result.temporaryPasswords);
            }
            if (!result.errors.isEmpty()) {
                redirectAttributes.addFlashAttribute("errors", result.errors);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "CSV登録に失敗しました。形式を確認してください。" + e.getMessage());
            return "redirect:/admin/users/csv";
        }

        return "redirect:/admin/users";
    }

    // テキスト一括登録画面
    @GetMapping("/admin/users/text")
    public String textForm(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return redirectByLoginState(session);
        }

        model.addAttribute("loginUser", getLoginUser(session));
        return "admin_user_text";
    }

    // テキスト一括登録
    @PostMapping("/admin/users/text")
    @Transactional
    public String importText(
            @RequestParam String text,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) {
            return redirectByLoginState(session);
        }

        if (isBlank(text)) {
            redirectAttributes.addFlashAttribute("error", "社員情報を入力してください。");
            return "redirect:/admin/users/text";
        }

        ImportResult result = importUsersFromLines(List.of(text.split("\\R")));
        redirectAttributes.addFlashAttribute("message", result.successCount + "件の社員をテキスト登録しました。");
        if (!result.temporaryPasswords.isEmpty()) {
            redirectAttributes.addFlashAttribute("tempPasswords", result.temporaryPasswords);
        }
        if (!result.errors.isEmpty()) {
            redirectAttributes.addFlashAttribute("errors", result.errors);
        }

        return "redirect:/admin/users";
    }

    // 社員編集画面
    @GetMapping("/admin/users/{id}/edit")
    public String editUser(@PathVariable Integer id, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return redirectByLoginState(session);
        }

        Map<String, Object> user = findUserById(id);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "対象の社員が見つかりません。");
            return "redirect:/admin/users";
        }

        model.addAttribute("loginUser", getLoginUser(session));
        model.addAttribute("user", user);
        return "admin_user_edit";
    }

    // 社員編集
    @PostMapping("/admin/users/{id}/update")
    public String updateUser(
            @PathVariable Integer id,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam Integer role,
            @RequestParam(required = false) String department,
            @RequestParam Integer status,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) {
            return redirectByLoginState(session);
        }

        String error = validateUserInput(name, email, role);
        if (error != null) {
            redirectAttributes.addFlashAttribute("error", error);
            return "redirect:/admin/users/" + id + "/edit";
        }

        if (existsEmail(email.trim(), id)) {
            redirectAttributes.addFlashAttribute("error", "このメールアドレスは既に使用されています。");
            return "redirect:/admin/users/" + id + "/edit";
        }

        jdbcTemplate.update("""
                UPDATE users
                SET name = :name,
                    email = :email,
                    role = :role,
                    department = :department,
                    status = :status
                WHERE user_id = :userId
                """, new MapSqlParameterSource()
                .addValue("userId", id)
                .addValue("name", name.trim())
                .addValue("email", email.trim())
                .addValue("role", role)
                .addValue("department", emptyToNull(department))
                .addValue("status", status));

        redirectAttributes.addFlashAttribute("message", "社員情報を更新しました。");
        return "redirect:/admin/users";
    }

    // 社員削除
    @PostMapping("/admin/users/{id}/delete")
    public String deleteUser(@PathVariable Integer id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return redirectByLoginState(session);
        }

        User loginUser = getLoginUser(session);
        if (loginUser != null && id.equals(loginUser.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "ログイン中の管理者自身は削除できません。");
            return "redirect:/admin/users";
        }

        if (hasRelatedData(id)) {
            redirectAttributes.addFlashAttribute("error", "商品・応募・取引・メッセージなどの関連データがあるため削除できません。利用制限を使ってください。");
            return "redirect:/admin/users";
        }

        int count = jdbcTemplate.update("""
                DELETE FROM users
                WHERE user_id = :userId
                """, new MapSqlParameterSource("userId", id));

        redirectAttributes.addFlashAttribute("message", count > 0 ? "社員を削除しました。" : "対象の社員が見つかりません。");
        return "redirect:/admin/users";
    }

    // パスワードリセット
    @PostMapping("/admin/users/{id}/reset-password")
    public String resetPassword(@PathVariable Integer id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return redirectByLoginState(session);
        }

        Map<String, Object> targetUser = findUserById(id);
        if (targetUser == null) {
            redirectAttributes.addFlashAttribute("error", "対象の社員が見つかりません。");
            return "redirect:/admin/users";
        }

        String temporaryPassword = generateTemporaryPassword();
        int count = jdbcTemplate.update("""
                UPDATE users
                SET password = :password,
                    temporary_password = true,
                    last_password_change = :now,
                    status = 0,
                    login_fail_count = 0
                WHERE user_id = :userId
                """, new MapSqlParameterSource()
                .addValue("userId", id)
                .addValue("password", PasswordUtil.encode(temporaryPassword))
                .addValue("now", LocalDateTime.now()));

        if (count > 0) {
            MailUtil.sendTemporaryPasswordMail(
                    mailSender,
                    String.valueOf(targetUser.get("email")),
                    String.valueOf(targetUser.get("name")),
                    temporaryPassword);
            redirectAttributes.addFlashAttribute("message", "パスワードをリセットしました。メール設定が有効な場合は仮パスワードを送信しています。");
            redirectAttributes.addFlashAttribute("tempPassword", temporaryPassword);
        } else {
            redirectAttributes.addFlashAttribute("error", "対象の社員が見つかりません。");
        }

        return "redirect:/admin/users";
    }

    // 利用制限
    @PostMapping("/admin/users/{id}/restrict")
    public String restrictUser(@PathVariable Integer id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return redirectByLoginState(session);
        }

        jdbcTemplate.update("""
                UPDATE users
                SET status = 1
                WHERE user_id = :userId
                """, new MapSqlParameterSource("userId", id));

        redirectAttributes.addFlashAttribute("message", "利用制限しました。");
        return "redirect:/admin/users";
    }

    // 利用制限解除
    @PostMapping("/admin/users/{id}/unrestrict")
    public String unrestrictUser(@PathVariable Integer id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return redirectByLoginState(session);
        }

        jdbcTemplate.update("""
                UPDATE users
                SET status = 0,
                    login_fail_count = 0
                WHERE user_id = :userId
                """, new MapSqlParameterSource("userId", id));

        redirectAttributes.addFlashAttribute("message", "利用制限を解除しました。");
        return "redirect:/admin/users";
    }

    private List<Map<String, Object>> findUsers(String keyword, Integer status) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("""
                SELECT user_id AS "userId",
                       name,
                       email,
                       role,
                       department,
                       status,
                       created_at AS "createdAt",
                       login_at AS "loginAt",
                       temporary_password AS "temporaryPassword",
                       last_password_change AS "lastPasswordChange",
                       login_fail_count AS "loginFailCount"
                FROM users
                WHERE 1 = 1
                """);

        if (!isBlank(keyword)) {
            sql.append("""
                      AND (name ILIKE :keyword
                           OR email ILIKE :keyword
                           OR department ILIKE :keyword)
                    """);
            params.addValue("keyword", "%" + keyword.trim() + "%");
        }

        if (status != null) {
            sql.append("""
                      AND status = :status
                    """);
            params.addValue("status", status);
        }

        sql.append("""
                ORDER BY user_id ASC
                """);

        return jdbcTemplate.queryForList(sql.toString(), params);
    }

    private Map<String, Object> findUserById(Integer userId) {
        List<Map<String, Object>> users = jdbcTemplate.queryForList("""
                SELECT user_id AS "userId",
                       name,
                       email,
                       role,
                       department,
                       status,
                       created_at AS "createdAt",
                       login_at AS "loginAt",
                       temporary_password AS "temporaryPassword",
                       last_password_change AS "lastPasswordChange",
                       login_fail_count AS "loginFailCount"
                FROM users
                WHERE user_id = :userId
                """, new MapSqlParameterSource("userId", userId));

        return users.isEmpty() ? null : users.get(0);
    }

    private ImportResult importUsersFromCsv(MultipartFile file) throws Exception {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return importUsersFromLines(lines);
    }

    private ImportResult importUsersFromLines(List<String> lines) {
        ImportResult result = new ImportResult();
        Map<String, Integer> header = null;
        boolean checkedFirstDataLine = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = removeBom(lines.get(i));
            if (isBlank(line)) {
                continue;
            }

            List<String> columns = parseCsvLine(line);
            if (!checkedFirstDataLine) {
                checkedFirstDataLine = true;
                if (looksLikeHeader(columns)) {
                    header = createHeaderMap(columns);
                    continue;
                }
            }

            try {
                ImportedUser importedUser = toImportedUser(columns, header);
                String error = validateUserInput(importedUser.name, importedUser.email, importedUser.role);
                if (error != null) {
                    result.errors.add((i + 1) + "行目: " + error);
                    continue;
                }

                if (existsEmail(importedUser.email, null)) {
                    result.errors.add((i + 1) + "行目: 既に登録済みのメールアドレスです。" + importedUser.email);
                    continue;
                }
                if (importedUser.userId != null && existsUserId(importedUser.userId)) {
                    result.errors.add((i + 1) + "行目: 既に登録済みの社員番号です。" + importedUser.userId);
                    continue;
                }

                String password = isBlank(importedUser.password) ? generateTemporaryPassword() : importedUser.password;
                insertImportedUser(importedUser, password);
                result.successCount++;
                result.temporaryPasswords.add(importedUser.email + " / " + password);
            } catch (Exception e) {
                result.errors.add((i + 1) + "行目: 登録できませんでした。" + e.getMessage());
            }
        }

        return result;
    }

    private ImportedUser toImportedUser(List<String> columns, Map<String, Integer> header) {
        ImportedUser user = new ImportedUser();
        if (header != null) {
            user.userId = parseUserId(getColumn(columns, header, "user_id", "userid", "社員番号", "社員ID"));
            user.name = getColumn(columns, header, "name", "氏名", "名前");
            user.email = getColumn(columns, header, "email", "メール", "メールアドレス");
            user.role = parseRole(getColumn(columns, header, "role", "権限"));
            user.department = getColumn(columns, header, "department", "部署");
            user.password = getColumn(columns, header, "password", "パスワード");
            return user;
        }

        // ヘッダーなしの場合1: user_id,name,email,password,role,department,status
        if (columns.size() >= 5 && !isBlank(get(columns, 2)) && get(columns, 2).contains("@")) {
            user.userId = parseUserId(get(columns, 0));
            user.name = get(columns, 1);
            user.email = get(columns, 2);
            user.password = get(columns, 3);
            user.role = parseRole(get(columns, 4));
            user.department = get(columns, 5);
            return user;
        }

        // ヘッダーなしの場合2: name,email,role,department,password
        user.name = get(columns, 0);
        user.email = get(columns, 1);
        user.role = parseRole(get(columns, 2));
        user.department = get(columns, 3);
        user.password = get(columns, 4);
        return user;
    }

    private void insertImportedUser(ImportedUser importedUser, String password) {
        jdbcTemplate.update("""
                INSERT INTO users
                  (user_id, name, email, password, role, department, status,
                   created_at, icon_url, login_at, temporary_password, last_password_change, login_fail_count)
                VALUES
                  (:userId, :name, :email, :password, :role, :department, 0,
                   :now, NULL, :now, true, :now, 0)
                """, new MapSqlParameterSource()
                .addValue("userId", importedUser.userId == null ? nextUserId() : importedUser.userId)
                .addValue("name", importedUser.name.trim())
                .addValue("email", importedUser.email.trim())
                .addValue("password", PasswordUtil.encode(password.trim()))
                .addValue("role", importedUser.role)
                .addValue("department", emptyToNull(importedUser.department))
                .addValue("now", LocalDateTime.now()));

        MailUtil.sendTemporaryPasswordMail(mailSender, importedUser.email, importedUser.name, password);
    }

    private String validateUserInput(String name, String email, Integer role) {
        if (isBlank(name)) {
            return "氏名を入力してください。";
        }
        if (isBlank(email)) {
            return "メールアドレスを入力してください。";
        }
        if (!email.contains("@")) {
            return "メールアドレスの形式が正しくありません。";
        }
        if (role == null || (role != 0 && role != 1)) {
            return "権限は 0（一般）または 1（管理者）を指定してください。";
        }
        return null;
    }

    private boolean existsEmail(String email, Integer ignoreUserId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("email", email);
        String sql = """
                SELECT COUNT(*)
                FROM users
                WHERE email = :email
                """;

        if (ignoreUserId != null) {
            sql += """
                  AND user_id <> :ignoreUserId
                """;
            params.addValue("ignoreUserId", ignoreUserId);
        }

        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null && count > 0;
    }

    private boolean existsUserId(Integer userId) {
        if (userId == null) {
            return false;
        }
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM users
                WHERE user_id = :userId
                """, new MapSqlParameterSource("userId", userId), Long.class);
        return count != null && count > 0;
    }

    private Integer nextUserId() {
        Integer next = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(user_id), 26000) + 1
                FROM users
                """, new MapSqlParameterSource(), Integer.class);
        return next == null ? 26001 : next;
    }

    private boolean hasRelatedData(Integer userId) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        Long count = jdbcTemplate.queryForObject("""
                SELECT
                  (SELECT COUNT(*) FROM items WHERE user_id = :userId)
                + (SELECT COUNT(*) FROM applications WHERE user_id = :userId)
                + (SELECT COUNT(*) FROM transactions WHERE seller_id = :userId OR buyer_id = :userId)
                + (SELECT COUNT(*) FROM messages WHERE sender_id = :userId OR receiver_id = :userId)
                + (SELECT COUNT(*) FROM reports WHERE user_id = :userId)
                """, params, Long.class);
        return count != null && count > 0;
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            password.append(TEMP_PASSWORD_CHARS.charAt(RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return password.toString();
    }

    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());
        return result;
    }

    private boolean looksLikeHeader(List<String> columns) {
        for (String column : columns) {
            String value = normalizeHeader(column);
            if (value.equals("name") || value.equals("email") || value.equals("氏名") || value.equals("メール") || value.equals("メールアドレス")) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Integer> createHeaderMap(List<String> columns) {
        Map<String, Integer> header = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            header.put(normalizeHeader(columns.get(i)), i);
        }
        return header;
    }

    private String getColumn(List<String> columns, Map<String, Integer> header, String... names) {
        for (String name : names) {
            Integer index = header.get(normalizeHeader(name));
            if (index != null) {
                return get(columns, index);
            }
        }
        return null;
    }

    private String normalizeHeader(String value) {
        return removeBom(value == null ? "" : value).trim().toLowerCase();
    }

    private String removeBom(String value) {
        if (value != null && value.startsWith("\uFEFF")) {
            return value.substring(1);
        }
        return value;
    }

    private String get(List<String> values, int index) {
        return index >= 0 && index < values.size() ? values.get(index) : null;
    }

    private Integer parseUserId(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseRole(String value) {
        if (isBlank(value)) {
            return 0;
        }
        String trimmed = value.trim();
        if ("管理者".equals(trimmed) || "admin".equalsIgnoreCase(trimmed)) {
            return 1;
        }
        if ("一般".equals(trimmed) || "社員".equals(trimmed) || "user".equalsIgnoreCase(trimmed)) {
            return 0;
        }
        return Integer.valueOf(trimmed);
    }

    private String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isAdmin(HttpSession session) {
        User loginUser = getLoginUser(session);
        return loginUser != null && Integer.valueOf(1).equals(loginUser.getRole());
    }

    private String redirectByLoginState(HttpSession session) {
        return getLoginUser(session) == null ? "redirect:/login" : "redirect:/item-101";
    }

    private User getLoginUser(HttpSession session) {
        Object value = session.getAttribute("loginUser");
        if (value instanceof User user) {
            return user;
        }
        return null;
    }

    private static class ImportedUser {
        private Integer userId;
        private String name;
        private String email;
        private String password;
        private Integer role;
        private String department;
    }

    private static class ImportResult {
        private int successCount;
        private final List<String> temporaryPasswords = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();
    }
}
