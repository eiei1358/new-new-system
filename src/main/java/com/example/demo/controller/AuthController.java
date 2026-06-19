package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.entity.Category;
import com.example.demo.entity.User;

@Controller
public class AuthController {

	private final UserRepository userRepository;
	private final CategoryRepository categoryRepository;
	private final NamedParameterJdbcTemplate jdbcTemplate;

	public AuthController(
			UserRepository userRepository,
			CategoryRepository categoryRepository,
			NamedParameterJdbcTemplate jdbcTemplate) {

		this.userRepository = userRepository;
		this.categoryRepository = categoryRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

	// 管理者トップ
	@GetMapping("/auth")
	public String menu(HttpSession session) {
		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}
		return "redirect:/admin/users";
	}

	// 旧URLから新しい社員管理URLへ転送
	@GetMapping("/auth/users")
	public String users(HttpSession session) {
		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}
		return "redirect:/admin/users";
	}

	// カテゴリ一覧
	@GetMapping("/auth/categories")
	public String categories(HttpSession session, Model model) {
		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		model.addAttribute("loginUser", getLoginUser(session));
		model.addAttribute("categories", categoryRepository.findAllByOrderByCategoryIdAsc());
		return "category";
	}

	// カテゴリ追加画面
	@GetMapping("/auth/categories/new")
	public String newCategory(HttpSession session, Model model) {
		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		model.addAttribute("loginUser", getLoginUser(session));
		return "category_new";
	}

	// カテゴリ追加処理
	@PostMapping("/auth/categories/add")
	public String addCategory(
			@RequestParam String name,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		if (name == null || name.isBlank()) {
			redirectAttributes.addFlashAttribute("error", "カテゴリ名を入力してください。");
			return "redirect:/auth/categories";
		}

		Category category = new Category();
		category.setName(name.trim());
		categoryRepository.save(category);

		redirectAttributes.addFlashAttribute("message", "カテゴリを追加しました。");
		return "redirect:/auth/categories";
	}

	@PostMapping("/auth/category/delete/{id}")
	public String deleteCategory(
			@PathVariable Integer id,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		Long usedCount = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM items
				WHERE category_id = :categoryId
				""", new MapSqlParameterSource("categoryId", id), Long.class);

		if (usedCount != null && usedCount > 0) {
			redirectAttributes.addFlashAttribute(
					"error",
					"このカテゴリを使用している商品があるため削除できません。");
			return "redirect:/auth/categories";
		}

		if (categoryRepository.existsById(id)) {
			categoryRepository.deleteById(id);
			redirectAttributes.addFlashAttribute("message", "カテゴリを削除しました。");
		} else {
			redirectAttributes.addFlashAttribute("error", "カテゴリが見つかりません。");
		}

		return "redirect:/auth/categories";
	}

	// 編集画面表示
	@GetMapping("/auth/categories/edit/{id}")
	public String editCategory(
			@PathVariable Integer id,
			HttpSession session,
			Model model,
			RedirectAttributes redirectAttributes) {

		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		Category category = categoryRepository.findById(id).orElse(null);

		if (category == null) {
			redirectAttributes.addFlashAttribute("error", "カテゴリが見つかりません。");
			return "redirect:/auth/categories";
		}

		model.addAttribute("loginUser", getLoginUser(session));
		model.addAttribute("category", category);
		return "category_edit";
	}

	// 編集更新処理
	@PostMapping("/auth/categories/update")
	public String updateCategory(
			@RequestParam Integer categoryId,
			@RequestParam String name,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		if (name == null || name.isBlank()) {
			redirectAttributes.addFlashAttribute("error", "カテゴリ名を入力してください。");
			return "redirect:/auth/categories/edit/" + categoryId;
		}

		Category category = categoryRepository.findById(categoryId).orElse(null);

		if (category != null) {
			category.setName(name.trim());
			categoryRepository.save(category);
			redirectAttributes.addFlashAttribute("message", "カテゴリを更新しました。");
		} else {
			redirectAttributes.addFlashAttribute("error", "カテゴリが見つかりません。");
		}

		return "redirect:/auth/categories";
	}

	// 検索キー管理
	@GetMapping("/auth/search-keys")
	public String searchKeys(HttpSession session, Model model) {
		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		setSearchKeyModel(model, null, session);
		return "search";
	}

	@GetMapping("/auth/search-keys/edit/{id}")
	public String editSearchKey(
			@PathVariable Integer id,
			HttpSession session,
			Model model,
			RedirectAttributes redirectAttributes) {

		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		List<Map<String, Object>> editKeywordList = jdbcTemplate.queryForList("""
				SELECT
					keyword_id AS "keywordId",
					keyword AS "keyword",
					status AS "status",
					TO_CHAR(created_at, 'YYYY-MM-DD HH24:MI') AS "createdAt"
				FROM search_keywords
				WHERE keyword_id = :keywordId
				""", new MapSqlParameterSource("keywordId", id));

		if (editKeywordList.isEmpty()) {
			redirectAttributes.addFlashAttribute("error", "検索キーが見つかりません。");
			return "redirect:/auth/search-keys";
		}

		setSearchKeyModel(model, editKeywordList.get(0), session);
		return "search";
	}

	@PostMapping("/auth/search-keys/add")
	public String addSearchKey(
			@RequestParam String keyword,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		if (keyword == null || keyword.isBlank()) {
			redirectAttributes.addFlashAttribute("error", "検索キーを入力してください。");
			return "redirect:/auth/search-keys";
		}

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("keyword", keyword.trim());
		params.addValue("createdAt", LocalDateTime.now());

		try {
			jdbcTemplate.update("""
					INSERT INTO search_keywords (keyword, status, created_at)
					VALUES (:keyword, 0, :createdAt)
					""", params);
			redirectAttributes.addFlashAttribute("message", "検索キーを追加しました。");
		} catch (DataIntegrityViolationException e) {
			redirectAttributes.addFlashAttribute("error", "同じ検索キーがすでに登録されています。");
		}

		return "redirect:/auth/search-keys";
	}

	@PostMapping("/auth/search-keys/update/{id}")
	public String updateSearchKey(
			@PathVariable Integer id,
			@RequestParam String keyword,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		if (keyword == null || keyword.isBlank()) {
			redirectAttributes.addFlashAttribute("error", "検索キーを入力してください。");
			return "redirect:/auth/search-keys/edit/" + id;
		}

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("keywordId", id);
		params.addValue("keyword", keyword.trim());

		int updatedCount = jdbcTemplate.update("""
				UPDATE search_keywords
				SET keyword = :keyword
				WHERE keyword_id = :keywordId
				""", params);

		redirectAttributes.addFlashAttribute(
				updatedCount > 0 ? "message" : "error",
				updatedCount > 0 ? "検索キーを更新しました。" : "検索キーが見つかりません。");

		return "redirect:/auth/search-keys";
	}

	@PostMapping("/auth/search-keys/toggle/{id}")
	public String toggleSearchKey(
			@PathVariable Integer id,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		int updatedCount = jdbcTemplate.update("""
				UPDATE search_keywords
				SET status = CASE WHEN status = 0 THEN 1 ELSE 0 END
				WHERE keyword_id = :keywordId
				""", new MapSqlParameterSource("keywordId", id));

		redirectAttributes.addFlashAttribute(
				updatedCount > 0 ? "message" : "error",
				updatedCount > 0 ? "検索キーの状態を変更しました。" : "検索キーが見つかりません。");

		return "redirect:/auth/search-keys";
	}

	@PostMapping("/auth/search-keys/delete/{id}")
	public String deleteSearchKey(
			@PathVariable Integer id,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		int deletedCount = jdbcTemplate.update("""
				DELETE FROM search_keywords
				WHERE keyword_id = :keywordId
				""", new MapSqlParameterSource("keywordId", id));

		redirectAttributes.addFlashAttribute(
				deletedCount > 0 ? "message" : "error",
				deletedCount > 0 ? "検索キーを削除しました。" : "検索キーが見つかりません。");

		return "redirect:/auth/search-keys";
	}

	// NGキーワード管理
	@GetMapping("/auth/ng-keywords")
	public String ngKeywords(HttpSession session, Model model) {
		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		setNgKeywordModel(model, null, session);
		return "ngword";
	}

	@GetMapping("/auth/ng-keywords/edit/{id}")
	public String editNgKeyword(
			@PathVariable Integer id,
			HttpSession session,
			Model model,
			RedirectAttributes redirectAttributes) {

		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		List<Map<String, Object>> editKeywordList = jdbcTemplate.queryForList("""
				SELECT
					keyword_id AS "keywordId",
					keyword AS "keyword",
					type AS "type",
					status AS "status",
					TO_CHAR(created_at, 'YYYY-MM-DD HH24:MI') AS "createdAt"
				FROM ng_keywords
				WHERE keyword_id = :keywordId
				""", new MapSqlParameterSource("keywordId", id));

		if (editKeywordList.isEmpty()) {
			redirectAttributes.addFlashAttribute("error", "NGキーワードが見つかりません。");
			return "redirect:/auth/ng-keywords";
		}

		setNgKeywordModel(model, editKeywordList.get(0), session);
		return "ngword";
	}

	@PostMapping("/auth/ng-keywords/add")
	public String addNgKeyword(
			@RequestParam String keyword,
			@RequestParam Integer type,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		String error = validateKeywordAndType(keyword, type);
		if (error != null) {
			redirectAttributes.addFlashAttribute("error", error);
			return "redirect:/auth/ng-keywords";
		}

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("keyword", keyword.trim());
		params.addValue("type", type);
		params.addValue("createdAt", LocalDateTime.now());

		jdbcTemplate.update("""
				INSERT INTO ng_keywords (keyword, type, status, created_at)
				VALUES (:keyword, :type, 0, :createdAt)
				""", params);

		redirectAttributes.addFlashAttribute("message", "NGキーワードを追加しました。");
		return "redirect:/auth/ng-keywords";
	}

	@PostMapping("/auth/ng-keywords/update/{id}")
	public String updateNgKeyword(
			@PathVariable Integer id,
			@RequestParam String keyword,
			@RequestParam Integer type,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		String error = validateKeywordAndType(keyword, type);
		if (error != null) {
			redirectAttributes.addFlashAttribute("error", error);
			return "redirect:/auth/ng-keywords/edit/" + id;
		}

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("keywordId", id);
		params.addValue("keyword", keyword.trim());
		params.addValue("type", type);

		int updatedCount = jdbcTemplate.update("""
				UPDATE ng_keywords
				SET keyword = :keyword,
				    type = :type
				WHERE keyword_id = :keywordId
				""", params);

		redirectAttributes.addFlashAttribute(
				updatedCount > 0 ? "message" : "error",
				updatedCount > 0 ? "NGキーワードを更新しました。" : "NGキーワードが見つかりません。");

		return "redirect:/auth/ng-keywords";
	}

	@PostMapping("/auth/ng-keywords/toggle/{id}")
	public String toggleNgKeyword(
			@PathVariable Integer id,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		int updatedCount = jdbcTemplate.update("""
				UPDATE ng_keywords
				SET status = CASE WHEN status = 0 THEN 1 ELSE 0 END
				WHERE keyword_id = :keywordId
				""", new MapSqlParameterSource("keywordId", id));

		redirectAttributes.addFlashAttribute(
				updatedCount > 0 ? "message" : "error",
				updatedCount > 0 ? "NGキーワードの状態を変更しました。" : "NGキーワードが見つかりません。");

		return "redirect:/auth/ng-keywords";
	}

	@PostMapping("/auth/ng-keywords/delete/{id}")
	public String deleteNgKeyword(
			@PathVariable Integer id,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		int deletedCount = jdbcTemplate.update("""
				DELETE FROM ng_keywords
				WHERE keyword_id = :keywordId
				""", new MapSqlParameterSource("keywordId", id));

		redirectAttributes.addFlashAttribute(
				deletedCount > 0 ? "message" : "error",
				deletedCount > 0 ? "NGキーワードを削除しました。" : "NGキーワードが見つかりません。");

		return "redirect:/auth/ng-keywords";
	}

	// 不適切コンテンツ監視
	@GetMapping("/auth/watch")
	public String watch(
			@RequestParam(required = false) String keyword,
			HttpSession session,
			Model model) {
		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		model.addAttribute("loginUser", getLoginUser(session));
		model.addAttribute("keyword", keyword);
		model.addAttribute("items", findFlaggedItems(keyword));
		model.addAttribute("messages", findFlaggedMessages(keyword));
		return "watch";
	}

	@GetMapping("/auth/watch/items/{id}")
	public String watchItemDetail(
			@PathVariable Integer id,
			HttpSession session,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		Map<String, Object> item = findItemForAdmin(id);
		if (item == null) {
			redirectAttributes.addFlashAttribute("error", "物品が見つかりません。");
			return "redirect:/auth/watch";
		}

		model.addAttribute("loginUser", getLoginUser(session));
		model.addAttribute("item", item);
		return "watch_item_detail";
	}

	@GetMapping("/auth/watch/messages/{id}")
	public String watchMessageDetail(
			@PathVariable Integer id,
			HttpSession session,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		Map<String, Object> message = findMessageForAdmin(id);
		if (message == null) {
			redirectAttributes.addFlashAttribute("error", "メッセージが見つかりません。");
			return "redirect:/auth/watch";
		}

		model.addAttribute("loginUser", getLoginUser(session));
		model.addAttribute("message", message);
		return "watch_message_detail";
	}

	@PostMapping("/auth/watch/items/delete/{id}")
	@Transactional
	public String deleteFlaggedItem(
			@PathVariable Integer id,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		int deletedCount = deleteItemWithRelations(id);
		redirectAttributes.addFlashAttribute(
				deletedCount > 0 ? "message" : "error",
				deletedCount > 0 ? "物品を削除しました。" : "物品が見つかりません。");
		return "redirect:/auth/watch";
	}

	@PostMapping("/auth/watch/messages/delete/{id}")
	public String deleteFlaggedMessage(
			@PathVariable Integer id,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		int deletedCount = deleteMessageById(id);
		redirectAttributes.addFlashAttribute(
				deletedCount > 0 ? "message" : "error",
				deletedCount > 0 ? "メッセージを削除しました。" : "メッセージが見つかりません。");
		return "redirect:/auth/watch";
	}

	// 物品削除
	@GetMapping("/auth/items/delete")
	public String deleteItems(HttpSession session, Model model) {
		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		model.addAttribute("loginUser", getLoginUser(session));
		model.addAttribute("items", findAllItemsForAdmin());
		return "deitem";
	}

	@PostMapping("/auth/items/delete/{id}")
	@Transactional
	public String deleteItem(
			@PathVariable Integer id,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		int deletedCount = deleteItemWithRelations(id);
		redirectAttributes.addFlashAttribute(
				deletedCount > 0 ? "message" : "error",
				deletedCount > 0 ? "物品を削除しました。" : "物品が見つかりません。");
		return "redirect:/auth/items/delete";
	}

	// メッセージ削除
	@GetMapping("/auth/messages/delete")
	public String deleteMessages(HttpSession session, Model model) {
		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		model.addAttribute("loginUser", getLoginUser(session));
		model.addAttribute("messages", findAllMessagesForAdmin());
		return "demessage";
	}

	@PostMapping("/auth/messages/delete/{id}")
	public String deleteMessage(
			@PathVariable Integer id,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		int deletedCount = deleteMessageById(id);
		redirectAttributes.addFlashAttribute(
				deletedCount > 0 ? "message" : "error",
				deletedCount > 0 ? "メッセージを削除しました。" : "メッセージが見つかりません。");
		return "redirect:/auth/messages/delete";
	}


	// 通報確認
	@GetMapping({ "/auth/reports", "/admin/reports" })
	public String reports(
			@RequestParam(required = false) Integer status,
			HttpSession session,
			Model model) {
		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		model.addAttribute("loginUser", getLoginUser(session));
		model.addAttribute("status", status);
		model.addAttribute("reports", findReports(status));
		return "reports";
	}

	@GetMapping({ "/auth/reports/{id}", "/admin/reports/{id}" })
	public String reportDetail(
			@PathVariable Integer id,
			HttpSession session,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		Map<String, Object> report = findReportById(id);
		if (report == null) {
			redirectAttributes.addFlashAttribute("error", "通報が見つかりません。");
			return "redirect:/auth/reports";
		}

		model.addAttribute("loginUser", getLoginUser(session));
		model.addAttribute("report", report);
		return "report_detail";
	}

	@PostMapping({ "/auth/reports/{id}/status", "/admin/reports/{id}/status" })
	public String updateReportStatus(
			@PathVariable Integer id,
			@RequestParam Integer status,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		if (status == null || status < 0 || status > 2) {
			redirectAttributes.addFlashAttribute("error", "対応状況が不正です。");
			return "redirect:/auth/reports/" + id;
		}

		int updatedCount = jdbcTemplate.update("""
				UPDATE reports
				SET status = :status
				WHERE report_id = :reportId
				""", new MapSqlParameterSource()
					.addValue("status", status)
					.addValue("reportId", id));

		redirectAttributes.addFlashAttribute(
				updatedCount > 0 ? "message" : "error",
				updatedCount > 0 ? "通報の対応状況を更新しました。" : "通報が見つかりません。");
		return "redirect:/auth/reports/" + id;
	}

	@PostMapping("/auth/users/restrict/{id}")
	public String restrictUser(
			@PathVariable Integer id,
			@RequestParam(required = false, defaultValue = "/admin/users") String back,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		int updatedCount = jdbcTemplate.update("""
				UPDATE users
				SET status = 1
				WHERE user_id = :userId
				""", new MapSqlParameterSource("userId", id));

		redirectAttributes.addFlashAttribute(
				updatedCount > 0 ? "message" : "error",
				updatedCount > 0 ? "対象社員を利用制限しました。" : "社員が見つかりません。");
		return "redirect:" + safeBackUrl(back);
	}

	@PostMapping("/auth/users/unrestrict/{id}")
	public String unrestrictUser(
			@PathVariable Integer id,
			@RequestParam(required = false, defaultValue = "/admin/users") String back,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		int updatedCount = jdbcTemplate.update("""
				UPDATE users
				SET status = 0,
				    login_fail_count = 0
				WHERE user_id = :userId
				""", new MapSqlParameterSource("userId", id));

		redirectAttributes.addFlashAttribute(
				updatedCount > 0 ? "message" : "error",
				updatedCount > 0 ? "利用制限を解除しました。" : "社員が見つかりません。");
		return "redirect:" + safeBackUrl(back);
	}

	// 統計情報（正式URL: /admin/statistics、/auth/stats は互換用に残す）
	@GetMapping({ "/admin/statistics", "/auth/stats" })
	public String stats(
			@RequestParam(required = false, defaultValue = "daily") String period,
			HttpSession session,
			Model model) {
		if (!isAdmin(session)) {
			return redirectByLoginState(session);
		}

		// period が null・空・想定外の場合は日別をデフォルトにする
		if (period == null || period.isBlank() || !"monthly".equals(period)) {
			period = "daily";
		}

		long itemCount = countTable("items");
		long dealCount = countDeals();
		double dealRate = calculateRate(dealCount, itemCount);

		List<Map<String, Object>> categoryStats = categoryStats();
		List<Map<String, Object>> timeStats = timeStats(period);

		long maxListingCount = maxValue(categoryStats, timeStats, "listingCount");
		long maxDealCount = maxValue(categoryStats, timeStats, "dealCount");

		model.addAttribute("loginUser", getLoginUser(session));
		model.addAttribute("period", period);

		// サマリーカード
		model.addAttribute("userCount", countTable("users"));
		model.addAttribute("categoryCount", countTable("categories"));
		model.addAttribute("itemCount", itemCount);
		model.addAttribute("dealCount", dealCount);
		model.addAttribute("dealRate", dealRate);
		model.addAttribute("applicationCount", countTable("applications"));
		model.addAttribute("transactionCount", countTable("transactions"));
		model.addAttribute("messageCount", countTable("messages"));
		model.addAttribute("reportCount", countTable("reports"));

		// 既存互換の集計値（テンプレートで使わなくても害はない）
		model.addAttribute("completedItemCount", countCompletedDeals());
		model.addAttribute("ngKeywordCount", countTable("ng_keywords"));
		model.addAttribute("searchKeywordCount", countTable("search_keywords"));

		// カテゴリ別・時系列
		model.addAttribute("categoryStats", categoryStats);
		model.addAttribute("timeStats", timeStats);

		// グラフ用の最大値（0除算・空データ対策で最低1を渡す）
		model.addAttribute("maxListingCount", maxListingCount == 0 ? 1 : maxListingCount);
		model.addAttribute("maxDealCount", maxDealCount == 0 ? 1 : maxDealCount);
		return "infomation";
	}


	// カテゴリ別統計（成約数は transactions.status = 1 で集計）
	private List<Map<String, Object>> categoryStats() {
		return jdbcTemplate.queryForList("""
				SELECT
					c.name AS "categoryName",
					COUNT(DISTINCT i.item_id) AS "listingCount",
					COUNT(DISTINCT t.transaction_id) AS "dealCount",
					CASE
						WHEN COUNT(DISTINCT i.item_id) = 0 THEN 0
						ELSE ROUND(
							COUNT(DISTINCT t.transaction_id) * 100.0
							/ COUNT(DISTINCT i.item_id), 1)
					END AS "rate"
				FROM categories c
				LEFT JOIN items i
					ON i.category_id = c.category_id
				LEFT JOIN transactions t
					ON t.item_id = i.item_id
					AND t.status = 1
				GROUP BY c.category_id, c.name
				ORDER BY c.category_id ASC
				""", new MapSqlParameterSource());
	}

	// 時系列統計（period = monthly なら月別、それ以外は日別）
	// 出品数と同じ期間軸で比較するため、成約数も items.created_at 基準で集計する
	private List<Map<String, Object>> timeStats(String period) {
		if ("monthly".equals(period)) {
			return jdbcTemplate.queryForList("""
					SELECT
						TO_CHAR(DATE_TRUNC('month', i.created_at), 'YYYY-MM') AS "label",
						COUNT(DISTINCT i.item_id) AS "listingCount",
						COUNT(DISTINCT t.transaction_id) AS "dealCount",
						CASE
							WHEN COUNT(DISTINCT i.item_id) = 0 THEN 0
							ELSE ROUND(
								COUNT(DISTINCT t.transaction_id) * 100.0
								/ COUNT(DISTINCT i.item_id), 1)
						END AS "rate"
					FROM items i
					LEFT JOIN transactions t
						ON t.item_id = i.item_id
						AND t.status = 1
					GROUP BY DATE_TRUNC('month', i.created_at)
					ORDER BY DATE_TRUNC('month', i.created_at) ASC
					""", new MapSqlParameterSource());
		}

		return jdbcTemplate.queryForList("""
				SELECT
					TO_CHAR(CAST(i.created_at AS DATE), 'YYYY-MM-DD') AS "label",
					COUNT(DISTINCT i.item_id) AS "listingCount",
					COUNT(DISTINCT t.transaction_id) AS "dealCount",
					CASE
						WHEN COUNT(DISTINCT i.item_id) = 0 THEN 0
						ELSE ROUND(
							COUNT(DISTINCT t.transaction_id) * 100.0
							/ COUNT(DISTINCT i.item_id), 1)
					END AS "rate"
				FROM items i
				LEFT JOIN transactions t
					ON t.item_id = i.item_id
					AND t.status = 1
				GROUP BY CAST(i.created_at AS DATE)
				ORDER BY CAST(i.created_at AS DATE) ASC
				""", new MapSqlParameterSource());
	}

	private void setSearchKeyModel(
			Model model,
			Map<String, Object> editKeyword,
			HttpSession session) {

		List<Map<String, Object>> keywords = jdbcTemplate.queryForList("""
				SELECT
					keyword_id AS "keywordId",
					keyword AS "keyword",
					status AS "status",
					CASE status WHEN 0 THEN '有効' ELSE '無効' END AS "statusName",
					TO_CHAR(created_at, 'YYYY-MM-DD HH24:MI') AS "createdAt"
				FROM search_keywords
				ORDER BY keyword_id ASC
				""", new MapSqlParameterSource());

		model.addAttribute("loginUser", getLoginUser(session));
		model.addAttribute("keywords", keywords);
		model.addAttribute("editKeyword", editKeyword);
	}

	private void setNgKeywordModel(
			Model model,
			Map<String, Object> editKeyword,
			HttpSession session) {

		List<Map<String, Object>> keywords = jdbcTemplate.queryForList("""
				SELECT
					keyword_id AS "keywordId",
					keyword AS "keyword",
					type AS "type",
					CASE type
						WHEN 0 THEN '物品'
						WHEN 1 THEN 'メッセージ'
						WHEN 2 THEN '共通'
						ELSE '不明'
					END AS "typeName",
					status AS "status",
					CASE status WHEN 0 THEN '有効' ELSE '無効' END AS "statusName",
					TO_CHAR(created_at, 'YYYY-MM-DD HH24:MI') AS "createdAt"
				FROM ng_keywords
				ORDER BY keyword_id ASC
				""", new MapSqlParameterSource());

		model.addAttribute("loginUser", getLoginUser(session));
		model.addAttribute("keywords", keywords);
		model.addAttribute("editKeyword", editKeyword);
	}

	private String validateKeywordAndType(String keyword, Integer type) {
		if (keyword == null || keyword.isBlank()) {
			return "キーワードを入力してください。";
		}

		if (type == null || type < 0 || type > 2) {
			return "種別を選択してください。";
		}

		return null;
	}

	private List<Map<String, Object>> findFlaggedItems(String keyword) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		String keywordCondition;

		if (keyword != null && !keyword.isBlank()) {
			params.addValue("keyword", "%" + keyword.trim() + "%");
			keywordCondition = """
					AND (
						i.name ILIKE :keyword
						OR COALESCE(i.description, '') ILIKE :keyword
					)
					""";
		} else {
			keywordCondition = """
					AND EXISTS (
						SELECT 1
						FROM ng_keywords n
						WHERE n.status = 0
						  AND n.type IN (0, 2)
						  AND POSITION(LOWER(n.keyword) IN LOWER(i.name || ' ' || COALESCE(i.description, ''))) > 0
					)
					""";
		}

		return jdbcTemplate.queryForList("""
				SELECT DISTINCT
					i.item_id AS "itemId",
					i.name AS "name",
					COALESCE(i.description, '') AS "description",
					u.name AS "sellerName",
					c.name AS "categoryName",
					CASE i.status
						WHEN 0 THEN '出品中'
						WHEN 1 THEN '成約中'
						WHEN 2 THEN '譲渡完了'
						WHEN 3 THEN '通報済み'
						ELSE '不明'
					END AS "statusName",
					TO_CHAR(i.created_at, 'YYYY-MM-DD HH24:MI') AS "createdAt"
				FROM items i
				INNER JOIN users u
					ON i.user_id = u.user_id
				LEFT JOIN categories c
					ON i.category_id = c.category_id
				WHERE 1 = 1
				""" + keywordCondition + """
				ORDER BY i.item_id DESC
				""", params);
	}

	private List<Map<String, Object>> findFlaggedMessages(String keyword) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		String keywordCondition;

		if (keyword != null && !keyword.isBlank()) {
			params.addValue("keyword", "%" + keyword.trim() + "%");
			keywordCondition = """
					AND m.content ILIKE :keyword
					""";
		} else {
			keywordCondition = """
					AND EXISTS (
						SELECT 1
						FROM ng_keywords n
						WHERE n.status = 0
						  AND n.type IN (1, 2)
						  AND POSITION(LOWER(n.keyword) IN LOWER(m.content)) > 0
					)
					""";
		}

		return jdbcTemplate.queryForList("""
				SELECT DISTINCT
					m.message_id AS "messageId",
					m.item_id AS "itemId",
					COALESCE(i.name, '商品なし') AS "itemName",
					sender_user.name AS "senderName",
					receiver_user.name AS "receiverName",
					m.content AS "content",
					TO_CHAR(m.created_at, 'YYYY-MM-DD HH24:MI') AS "createdAt"
				FROM messages m
				INNER JOIN users sender_user
					ON m.sender_id = sender_user.user_id
				INNER JOIN users receiver_user
					ON m.receiver_id = receiver_user.user_id
				LEFT JOIN items i
					ON m.item_id = i.item_id
				WHERE 1 = 1
				""" + keywordCondition + """
				ORDER BY m.message_id DESC
				""", params);
	}


	private Map<String, Object> findItemForAdmin(Integer itemId) {
		List<Map<String, Object>> items = jdbcTemplate.queryForList("""
				SELECT
					i.item_id AS "itemId",
					i.name AS "name",
					COALESCE(i.description, '') AS "description",
					i.user_id AS "sellerId",
					u.name AS "sellerName",
					c.name AS "categoryName",
					i.price AS "price",
					CASE i.condition
						WHEN 0 THEN '新品'
						WHEN 1 THEN '中古'
						WHEN 2 THEN '傷あり'
						ELSE '不明'
					END AS "conditionName",
					CASE i.type
						WHEN 0 THEN '無償'
						WHEN 1 THEN '有償'
						WHEN 2 THEN 'オークション'
						ELSE '不明'
					END AS "typeName",
					CASE i.status
						WHEN 0 THEN '出品中'
						WHEN 1 THEN '成約中'
						WHEN 2 THEN '譲渡完了'
						WHEN 3 THEN '通報済み'
						ELSE '不明'
					END AS "statusName",
					TO_CHAR(i.created_at, 'YYYY-MM-DD HH24:MI') AS "createdAt"
				FROM items i
				INNER JOIN users u
					ON i.user_id = u.user_id
				LEFT JOIN categories c
					ON i.category_id = c.category_id
				WHERE i.item_id = :itemId
				""", new MapSqlParameterSource("itemId", itemId));

		return items.isEmpty() ? null : items.get(0);
	}

	private Map<String, Object> findMessageForAdmin(Integer messageId) {
		List<Map<String, Object>> messages = jdbcTemplate.queryForList("""
				SELECT
					m.message_id AS "messageId",
					m.item_id AS "itemId",
					COALESCE(i.name, '商品なし') AS "itemName",
					m.sender_id AS "senderId",
					sender_user.name AS "senderName",
					m.receiver_id AS "receiverId",
					receiver_user.name AS "receiverName",
					m.content AS "content",
					TO_CHAR(m.created_at, 'YYYY-MM-DD HH24:MI') AS "createdAt"
				FROM messages m
				INNER JOIN users sender_user
					ON m.sender_id = sender_user.user_id
				INNER JOIN users receiver_user
					ON m.receiver_id = receiver_user.user_id
				LEFT JOIN items i
					ON m.item_id = i.item_id
				WHERE m.message_id = :messageId
				""", new MapSqlParameterSource("messageId", messageId));

		return messages.isEmpty() ? null : messages.get(0);
	}

	private List<Map<String, Object>> findReports(Integer status) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		StringBuilder sql = new StringBuilder("""
				SELECT
					r.report_id AS "reportId",
					r.target_type AS "targetType",
					CASE r.target_type
						WHEN 0 THEN '物品'
						WHEN 1 THEN 'メッセージ'
						WHEN 2 THEN '取引トラブル'
						ELSE 'その他'
					END AS "targetTypeName",
					r.target_id AS "targetId",
					r.user_id AS "userId",
					u.name AS "reporterName",
					r.reason AS "reason",
					r.status AS "status",
					CASE r.status
						WHEN 0 THEN '未対応'
						WHEN 1 THEN '対応中'
						WHEN 2 THEN '対応済み'
						ELSE '不明'
					END AS "statusName",
					TO_CHAR(r.created_at, 'YYYY-MM-DD HH24:MI') AS "createdAt"
				FROM reports r
				INNER JOIN users u
					ON r.user_id = u.user_id
				WHERE 1 = 1
				""");

		if (status != null) {
			sql.append(" AND r.status = :status ");
			params.addValue("status", status);
		}

		sql.append(" ORDER BY r.report_id DESC ");
		return jdbcTemplate.queryForList(sql.toString(), params);
	}

	private Map<String, Object> findReportById(Integer reportId) {
		List<Map<String, Object>> reports = jdbcTemplate.queryForList("""
				SELECT
					r.report_id AS "reportId",
					r.target_type AS "targetType",
					CASE r.target_type
						WHEN 0 THEN '物品'
						WHEN 1 THEN 'メッセージ'
						WHEN 2 THEN '取引トラブル'
						ELSE 'その他'
					END AS "targetTypeName",
					r.target_id AS "targetId",
					r.user_id AS "userId",
					u.name AS "reporterName",
					r.reason AS "reason",
					r.status AS "status",
					CASE r.status
						WHEN 0 THEN '未対応'
						WHEN 1 THEN '対応中'
						WHEN 2 THEN '対応済み'
						ELSE '不明'
					END AS "statusName",
					TO_CHAR(r.created_at, 'YYYY-MM-DD HH24:MI') AS "createdAt"
				FROM reports r
				INNER JOIN users u
					ON r.user_id = u.user_id
				WHERE r.report_id = :reportId
				""", new MapSqlParameterSource("reportId", reportId));

		return reports.isEmpty() ? null : reports.get(0);
	}

	private List<Map<String, Object>> findAllItemsForAdmin() {
		return jdbcTemplate.queryForList("""
				SELECT
					i.item_id AS "itemId",
					i.name AS "name",
					u.name AS "sellerName",
					c.name AS "categoryName",
					i.price AS "price",
					CASE i.type
						WHEN 0 THEN '無償'
						WHEN 1 THEN '有償'
						WHEN 2 THEN 'オークション'
						ELSE '不明'
					END AS "typeName",
					CASE i.status
						WHEN 0 THEN '出品中'
						WHEN 1 THEN '成約中'
						WHEN 2 THEN '譲渡完了'
						WHEN 3 THEN '通報済み'
						ELSE '不明'
					END AS "statusName",
					TO_CHAR(i.created_at, 'YYYY-MM-DD HH24:MI') AS "createdAt"
				FROM items i
				INNER JOIN users u
					ON i.user_id = u.user_id
				LEFT JOIN categories c
					ON i.category_id = c.category_id
				ORDER BY i.item_id DESC
				""", new MapSqlParameterSource());
	}

	private List<Map<String, Object>> findAllMessagesForAdmin() {
		return jdbcTemplate.queryForList("""
				SELECT
					m.message_id AS "messageId",
					m.item_id AS "itemId",
					COALESCE(i.name, '商品なし') AS "itemName",
					sender_user.name AS "senderName",
					receiver_user.name AS "receiverName",
					m.content AS "content",
					TO_CHAR(m.created_at, 'YYYY-MM-DD HH24:MI') AS "createdAt"
				FROM messages m
				INNER JOIN users sender_user
					ON m.sender_id = sender_user.user_id
				INNER JOIN users receiver_user
					ON m.receiver_id = receiver_user.user_id
				LEFT JOIN items i
					ON m.item_id = i.item_id
				ORDER BY m.message_id DESC
				""", new MapSqlParameterSource());
	}

	private int deleteItemWithRelations(Integer itemId) {
		MapSqlParameterSource params = new MapSqlParameterSource("itemId", itemId);

		jdbcTemplate.update("DELETE FROM reports WHERE target_type = 0 AND target_id = :itemId", params);
		jdbcTemplate.update("DELETE FROM messages WHERE item_id = :itemId", params);
		jdbcTemplate.update("DELETE FROM transactions WHERE item_id = :itemId", params);
		jdbcTemplate.update("DELETE FROM applications WHERE item_id = :itemId", params);
		jdbcTemplate.update("DELETE FROM item_images WHERE item_id = :itemId", params);

		return jdbcTemplate.update("""
				DELETE FROM items
				WHERE item_id = :itemId
				""", params);
	}

	private int deleteMessageById(Integer messageId) {
		jdbcTemplate.update("DELETE FROM reports WHERE target_type = 1 AND target_id = :messageId",
				new MapSqlParameterSource("messageId", messageId));

		return jdbcTemplate.update("""
				DELETE FROM messages
				WHERE message_id = :messageId
				""", new MapSqlParameterSource("messageId", messageId));
	}

	private Long countTable(String tableName) {
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM " + tableName,
				new MapSqlParameterSource(),
				Long.class);
	}

	private Long countItemsByStatus(Integer status) {
		return jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM items
				WHERE status = :status
				""", new MapSqlParameterSource("status", status), Long.class);
	}

	// 成約数: transactions.status = 1 の件数
	private long countDeals() {
		Long count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM transactions
				WHERE status = 1
				""", new MapSqlParameterSource(), Long.class);
		return count == null ? 0L : count;
	}

	// 譲渡完了数: completed_at IS NOT NULL の件数（参考値）
	private long countCompletedDeals() {
		Long count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM transactions
				WHERE completed_at IS NOT NULL
				""", new MapSqlParameterSource(), Long.class);
		return count == null ? 0L : count;
	}

	// 成約率 = 分子 / 分母 * 100（分母が0なら0.0、小数第1位まで）
	private double calculateRate(long numerator, long denominator) {
		if (denominator == 0) {
			return 0.0;
		}
		return Math.round(numerator * 1000.0 / denominator) / 10.0;
	}

	// 集計結果リストから指定キーの最大値を求める（グラフのスケール用）
	private long maxValue(
			List<Map<String, Object>> categoryStats,
			List<Map<String, Object>> timeStats,
			String key) {

		long max = 0;
		for (List<Map<String, Object>> list : List.of(categoryStats, timeStats)) {
			for (Map<String, Object> row : list) {
				Object value = row.get(key);
				if (value instanceof Number number && number.longValue() > max) {
					max = number.longValue();
				}
			}
		}
		return max;
	}

	private boolean isAdmin(HttpSession session) {
		User loginUser = getLoginUser(session);
		return loginUser != null && Integer.valueOf(1).equals(loginUser.getRole());
	}

	private String safeBackUrl(String back) {
		if (back == null || back.isBlank() || !back.startsWith("/")) {
			return "/admin/users";
		}

		if (back.startsWith("//")) {
			return "/admin/users";
		}

		return back;
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
}
