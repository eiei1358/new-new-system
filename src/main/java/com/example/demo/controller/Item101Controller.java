package com.example.demo.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;

import org.springframework.data.domain.Sort;
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
import com.example.demo.repository.ItemRepository;
import com.example.demo.entity.Item;
import com.example.demo.entity.User;

@Controller
public class Item101Controller {

	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final CategoryRepository categoryRepository;
	private final ItemRepository itemRepository;

	public Item101Controller(
			NamedParameterJdbcTemplate jdbcTemplate,
			CategoryRepository categoryRepository,
			ItemRepository itemRepository) {

		this.jdbcTemplate = jdbcTemplate;
		this.categoryRepository = categoryRepository;
		this.itemRepository = itemRepository;
	}

	@GetMapping({ "/item-101", "/items" })
	public String showItemList(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Integer categoryId,
			@RequestParam(required = false) Integer status,
			@RequestParam(defaultValue = "newest") String sort,
			HttpSession session,
			Model model) {

		User loginUser = getLoginUser(session);

		if (loginUser == null) {
			return "redirect:/login";
		}

		StringBuilder sql = new StringBuilder("""
				SELECT
					i.item_id,
					i.name,
					i.description,
					i.condition,
					i.status,
					i.price,
					i.deadline,
					i.place,
					c.name AS category_name,
					CASE i.status
						WHEN 0 THEN '出品中'
						WHEN 1 THEN '成約中'
						WHEN 2 THEN '譲渡完了'
						WHEN 3 THEN '通報済み'
						ELSE '不明'
					END AS status_name,
					CASE i.condition
						WHEN 0 THEN '新品'
						WHEN 1 THEN '中古'
						WHEN 2 THEN '傷あり'
						ELSE '不明'
					END AS condition_name,
					(
						SELECT ii.image_url
						FROM item_images ii
						WHERE ii.item_id = i.item_id
						ORDER BY ii.image_id
						LIMIT 1
					) AS image_url
				FROM items i
				LEFT JOIN categories c
					ON i.category_id = c.category_id
				WHERE 1 = 1
				""");

		MapSqlParameterSource params = new MapSqlParameterSource();

		if (keyword != null && !keyword.isBlank()) {
			sql.append("""
					AND (
						i.name ILIKE :keyword
						OR COALESCE(i.description, '') ILIKE :keyword
					)
					""");

			params.addValue("keyword", "%" + keyword.trim() + "%");
		}

		if (categoryId != null) {
			sql.append(" AND i.category_id = :categoryId ");
			params.addValue("categoryId", categoryId);
		}

		if (status != null) {
			sql.append(" AND i.status = :status ");
			params.addValue("status", status);
		}

		if ("deadline".equals(sort)) {
			sql.append(" ORDER BY i.deadline ASC, i.item_id DESC ");
		} else {
			sql.append(" ORDER BY i.created_at DESC, i.item_id DESC ");
		}

		List<Map<String, Object>> items = jdbcTemplate.queryForList(
				sql.toString(),
				params);

		model.addAttribute("loginUser", loginUser);
		model.addAttribute("previousLoginAt", session.getAttribute("previousLoginAt"));
		model.addAttribute("items", items);
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("resultCount", items.size());
		model.addAttribute("keyword", keyword);
		model.addAttribute("categoryId", categoryId);
		model.addAttribute("status", status);
		model.addAttribute("sort", sort);

		return "item101";
	}

	@GetMapping("/search")
	public String searchItems(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String priceRange,
			@RequestParam(required = false) Integer condition,
			@RequestParam(required = false) Integer categoryId,
			@RequestParam(required = false) Integer place,
			@RequestParam(required = false) String sort,
			HttpSession session,
			Model model) {

		User loginUser = getLoginUser(session);

		if (loginUser == null) {
			return "redirect:/login";
		}

		BigDecimal minPrice = null;
		BigDecimal maxPrice = null;

		if ("1".equals(priceRange)) {
			maxPrice = BigDecimal.valueOf(1000);
		} else if ("2".equals(priceRange)) {
			minPrice = BigDecimal.valueOf(1000);
			maxPrice = BigDecimal.valueOf(5000);
		} else if ("3".equals(priceRange)) {
			minPrice = BigDecimal.valueOf(5000);
			maxPrice = BigDecimal.valueOf(10000);
		} else if ("4".equals(priceRange)) {
			minPrice = BigDecimal.valueOf(10000);
		}

		Sort itemSort;

		if ("high".equals(sort)) {
			itemSort = Sort.by(
					Sort.Order.desc("price"),
					Sort.Order.desc("itemId"));
		} else if ("low".equals(sort)) {
			itemSort = Sort.by(
					Sort.Order.asc("price"),
					Sort.Order.desc("itemId"));
		} else {
			itemSort = Sort.by(
					Sort.Order.desc("createdAt"),
					Sort.Order.desc("itemId"));
		}

		List<Item> items = itemRepository.searchItems(
				keyword,
				categoryId,
				condition,
				place,
				minPrice,
				maxPrice,
				itemSort);

		model.addAttribute("loginUser", loginUser);
		model.addAttribute("previousLoginAt", session.getAttribute("previousLoginAt"));
		model.addAttribute("items", items);
		model.addAttribute("categories", categoryRepository.findAll());
		model.addAttribute("resultCount", items.size());
		model.addAttribute("keyword", keyword);
		model.addAttribute("priceRange", priceRange);
		model.addAttribute("condition", condition);
		model.addAttribute("categoryId", categoryId);
		model.addAttribute("place", place);
		model.addAttribute("sort", sort);

		return "search_104";
	}

	@GetMapping("/detail/{id}")
	public String showItemDetail(
			@PathVariable Integer id,
			HttpSession session,
			Model model) {

		User loginUser = getLoginUser(session);

		if (loginUser == null) {
			return "redirect:/login";
		}

		MapSqlParameterSource params = new MapSqlParameterSource("itemId", id);

		List<Map<String, Object>> itemList = jdbcTemplate.queryForList("""
				SELECT
					i.item_id AS "itemId",
					i.user_id AS "sellerId",
					i.name AS "name",
					i.description AS "description",
					i.condition AS "condition",
					i.status AS "status",
					i.price AS "price",
					i.type AS "type",
					i.place AS "place",
					i.deadline AS "deadline",
					i.deadline < CURRENT_DATE AS "expired",
					u.name AS "sellerName",
					c.name AS "categoryName",
					CASE i.condition
						WHEN 0 THEN '新品'
						WHEN 1 THEN '中古'
						WHEN 2 THEN '傷あり'
						ELSE '不明'
					END AS "conditionName",
					CASE i.status
						WHEN 0 THEN '出品中'
						WHEN 1 THEN '成約中'
						WHEN 2 THEN '譲渡完了'
						WHEN 3 THEN '通報済み'
						ELSE '不明'
					END AS "statusName",
					CASE i.type
						WHEN 0 THEN '無償'
						WHEN 1 THEN '有償'
						WHEN 2 THEN 'オークション'
						ELSE '不明'
					END AS "typeName",
					CASE i.place
						WHEN 0 THEN '神田'
						WHEN 1 THEN '横浜'
						ELSE '不明'
					END AS "placeName"
				FROM items i
				INNER JOIN users u
					ON i.user_id = u.user_id
				LEFT JOIN categories c
					ON i.category_id = c.category_id
				WHERE i.item_id = :itemId
				""", params);

		if (itemList.isEmpty()) {
			return "redirect:/item-101";
		}

		Map<String, Object> item = itemList.get(0);
		Integer sellerId = ((Number) item.get("sellerId")).intValue();
		boolean isSeller = sellerId.equals(loginUser.getUserId());

		List<String> imageUrls = jdbcTemplate.queryForList("""
				SELECT image_url
				FROM item_images
				WHERE item_id = :itemId
				ORDER BY image_id
				""", params, String.class);

		List<BigDecimal> bidPrices = jdbcTemplate.queryForList("""
				SELECT bid_price
				FROM applications
				WHERE item_id = :itemId
				  AND bid_price IS NOT NULL
				ORDER BY bid_price DESC
				LIMIT 1
				""", params, BigDecimal.class);

		BigDecimal currentPrice = (BigDecimal) item.get("price");

		if (!bidPrices.isEmpty()
				&& (currentPrice == null
						|| bidPrices.get(0).compareTo(currentPrice) > 0)) {
			currentPrice = bidPrices.get(0);
		}

		MapSqlParameterSource applicationParams = new MapSqlParameterSource();
		applicationParams.addValue("itemId", id);
		applicationParams.addValue("userId", loginUser.getUserId());

		Long applicationCount = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM applications
				WHERE item_id = :itemId
				  AND user_id = :userId
				  AND status = 0
				  AND bid_price IS NULL
				""", applicationParams, Long.class);

		List<Map<String, Object>> messages;

		if (isSeller) {
			messages = jdbcTemplate.queryForList("""
					SELECT
						m.message_id AS "messageId",
						sender_user.name AS "senderName",
						receiver_user.name AS "receiverName",
						m.content AS "content",
						TO_CHAR(m.created_at, 'YYYY-MM-DD HH24:MI') AS "createdAt"
					FROM messages m
					INNER JOIN users sender_user
						ON m.sender_id = sender_user.user_id
					INNER JOIN users receiver_user
						ON m.receiver_id = receiver_user.user_id
					WHERE m.item_id = :itemId
					ORDER BY m.created_at ASC, m.message_id ASC
					""", params);
		} else {
			MapSqlParameterSource messageParams = new MapSqlParameterSource();
			messageParams.addValue("itemId", id);
			messageParams.addValue("loginUserId", loginUser.getUserId());
			messageParams.addValue("sellerId", sellerId);

			messages = jdbcTemplate.queryForList("""
					SELECT
						m.message_id AS "messageId",
						sender_user.name AS "senderName",
						receiver_user.name AS "receiverName",
						m.content AS "content",
						TO_CHAR(m.created_at, 'YYYY-MM-DD HH24:MI') AS "createdAt"
					FROM messages m
					INNER JOIN users sender_user
						ON m.sender_id = sender_user.user_id
					INNER JOIN users receiver_user
						ON m.receiver_id = receiver_user.user_id
					WHERE m.item_id = :itemId
					  AND (
						(m.sender_id = :loginUserId AND m.receiver_id = :sellerId)
						OR
						(m.sender_id = :sellerId AND m.receiver_id = :loginUserId)
					  )
					ORDER BY m.created_at ASC, m.message_id ASC
					""", messageParams);
		}

		model.addAttribute("loginUser", loginUser);
		model.addAttribute("item", item);
		model.addAttribute("imageUrls", imageUrls);
		model.addAttribute("currentPrice", currentPrice);
		model.addAttribute("isSeller", isSeller);
		model.addAttribute("expired", Boolean.TRUE.equals(item.get("expired")));
		model.addAttribute("alreadyApplied", applicationCount != null && applicationCount > 0);
		model.addAttribute("messages", messages);
		model.addAttribute("transferInfo", findTransferForUser(id, loginUser.getUserId()));

		Number itemType = (Number) item.get("type");
		if (itemType != null && itemType.intValue() == 2) {
			return "auction-detail";
		}

		return "detail";
	}

	@GetMapping("/detail/{id}/apply-complete")
	public String showApplyComplete(
			@PathVariable Integer id,
			HttpSession session,
			Model model) {

		return showCompletePage(id, session, model, "apply-complete");
	}

	@GetMapping("/detail/{id}/bid-complete")
	public String showBidComplete(
			@PathVariable Integer id,
			HttpSession session,
			Model model) {

		return showCompletePage(id, session, model, "bid-complete");
	}

	@GetMapping("/detail/{id}/message-complete")
	public String showMessageComplete(
			@PathVariable Integer id,
			HttpSession session,
			Model model) {

		return showCompletePage(id, session, model, "message-complete");
	}

	@PostMapping("/detail/{id}/message")
	public String sendMessage(
			@PathVariable Integer id,
			@RequestParam String content,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		User loginUser = getLoginUser(session);

		if (loginUser == null) {
			return "redirect:/login";
		}

		Item item = itemRepository.findById(id).orElse(null);

		if (item == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "商品が見つかりません。");
			return "redirect:/item-101";
		}

		if (item.getUserId().equals(loginUser.getUserId())) {
			redirectAttributes.addFlashAttribute("errorMessage", "自分の出品物にはメッセージを送れません。");
			return redirectToDetail(id);
		}

		if (!Integer.valueOf(0).equals(item.getStatus())) {
			redirectAttributes.addFlashAttribute("errorMessage", "現在この商品へメッセージを送れません。");
			return redirectToDetail(id);
		}

		if (content == null || content.isBlank()) {
			redirectAttributes.addFlashAttribute("errorMessage", "メッセージを入力してください。");
			return redirectToDetail(id);
		}

		if (containsNgKeyword(content)) {
			redirectAttributes.addFlashAttribute("errorMessage", "NGキーワードが含まれています。");
			return redirectToDetail(id);
		}

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("senderId", loginUser.getUserId());
		params.addValue("receiverId", item.getUserId());
		params.addValue("itemId", id);
		params.addValue("content", content.trim());
		params.addValue("createdAt", LocalDateTime.now());

		jdbcTemplate.update("""
				INSERT INTO messages (
					sender_id,
					receiver_id,
					item_id,
					content,
					created_at
				)
				VALUES (
					:senderId,
					:receiverId,
					:itemId,
					:content,
					:createdAt
				)
				""", params);

		return "redirect:/detail/" + id + "/message-complete";
	}

	@Transactional
	@PostMapping("/detail/{id}/apply")
	public String apply(
			@PathVariable Integer id,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		User loginUser = getLoginUser(session);

		if (loginUser == null) {
			return "redirect:/login";
		}

		Item item = itemRepository.findById(id).orElse(null);
		String validationError = validateActionItem(item, loginUser);

		if (validationError != null) {
			redirectAttributes.addFlashAttribute("errorMessage", validationError);
			return item == null ? "redirect:/item-101" : redirectToDetail(id);
		}

		if (Integer.valueOf(2).equals(item.getType())) {
			redirectAttributes.addFlashAttribute("errorMessage", "オークション商品は入札してください。");
			return redirectToDetail(id);
		}

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("itemId", id);
		params.addValue("userId", loginUser.getUserId());

		Long count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM applications
				WHERE item_id = :itemId
				  AND user_id = :userId
				  AND status = 0
				  AND bid_price IS NULL
				""", params, Long.class);

		if (count != null && count > 0) {
			redirectAttributes.addFlashAttribute("errorMessage", "すでに応募済みです。");
			return redirectToDetail(id);
		}

		params.addValue("createdAt", LocalDateTime.now());

		// 先着順で購入応募を確定し、一覧表示を「成約中」にします。
		// status = 0 の商品だけを更新することで、同時応募による二重受付も防ぎます。
		int updatedItemCount = jdbcTemplate.update("""
				UPDATE items
				SET status = 1
				WHERE item_id = :itemId
				  AND status = 0
				""", params);

		if (updatedItemCount == 0) {
			redirectAttributes.addFlashAttribute(
					"errorMessage",
					"この商品はすでに成約中です。");
			return redirectToDetail(id);
		}

		jdbcTemplate.update("""
				INSERT INTO applications (
					item_id,
					user_id,
					bid_price,
					status,
					created_at
				)
				VALUES (
					:itemId,
					:userId,
					NULL,
					0,
					:createdAt
				)
				""", params);

		MapSqlParameterSource transactionParams = new MapSqlParameterSource();
		transactionParams.addValue("itemId", id);
		transactionParams.addValue("sellerId", item.getUserId());
		transactionParams.addValue("buyerId", loginUser.getUserId());
		transactionParams.addValue("transferCode", generateTransferCode());

		jdbcTemplate.update("""
				INSERT INTO transactions (
					item_id,
					seller_id,
					buyer_id,
					status,
					seller_completed,
					buyer_completed,
					transfer_code,
					completed_at
				)
				VALUES (
					:itemId,
					:sellerId,
					:buyerId,
					1,
					0,
					0,
					:transferCode,
					NULL
				)
				""", transactionParams);

		return "redirect:/detail/" + id + "/apply-complete";
	}

	@PostMapping("/detail/{id}/bid")
	public String bid(
			@PathVariable Integer id,
			@RequestParam BigDecimal bidPrice,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		User loginUser = getLoginUser(session);

		if (loginUser == null) {
			return "redirect:/login";
		}

		Item item = itemRepository.findById(id).orElse(null);
		String validationError = validateActionItem(item, loginUser);

		if (validationError != null) {
			redirectAttributes.addFlashAttribute("errorMessage", validationError);
			return item == null ? "redirect:/item-101" : redirectToDetail(id);
		}

		if (!Integer.valueOf(2).equals(item.getType())) {
			redirectAttributes.addFlashAttribute("errorMessage", "この商品はオークションではありません。");
			return redirectToDetail(id);
		}

		if (bidPrice == null || bidPrice.compareTo(BigDecimal.ZERO) <= 0) {
			redirectAttributes.addFlashAttribute("errorMessage", "入札額は1円以上で入力してください。");
			return redirectToDetail(id);
		}

		MapSqlParameterSource params = new MapSqlParameterSource("itemId", id);
		List<BigDecimal> prices = jdbcTemplate.queryForList("""
				SELECT bid_price
				FROM applications
				WHERE item_id = :itemId
				  AND bid_price IS NOT NULL
				ORDER BY bid_price DESC
				LIMIT 1
				""", params, BigDecimal.class);

		BigDecimal currentPrice = item.getPrice();

		if (!prices.isEmpty()
				&& (currentPrice == null
						|| prices.get(0).compareTo(currentPrice) > 0)) {
			currentPrice = prices.get(0);
		}

		if (currentPrice != null && bidPrice.compareTo(currentPrice) <= 0) {
			redirectAttributes.addFlashAttribute(
					"errorMessage",
					"現在価格より高い金額を入力してください。");
			return redirectToDetail(id);
		}

		params.addValue("userId", loginUser.getUserId());
		params.addValue("bidPrice", bidPrice);
		params.addValue("createdAt", LocalDateTime.now());

		jdbcTemplate.update("""
				INSERT INTO applications (
					item_id,
					user_id,
					bid_price,
					status,
					created_at
				)
				VALUES (
					:itemId,
					:userId,
					:bidPrice,
					0,
					:createdAt
				)
				""", params);

		return "redirect:/detail/" + id + "/bid-complete";
	}

	@PostMapping("/detail/{id}/report")
	public String report(
			@PathVariable Integer id,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		User loginUser = getLoginUser(session);

		if (loginUser == null) {
			return "redirect:/login";
		}

		Item item = itemRepository.findById(id).orElse(null);

		if (item == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "商品が見つかりません。");
			return "redirect:/item-101";
		}

		if (item.getUserId().equals(loginUser.getUserId())) {
			redirectAttributes.addFlashAttribute("errorMessage", "自分の出品物は通報できません。");
			return redirectToDetail(id);
		}

		if (Integer.valueOf(3).equals(item.getStatus())) {
			redirectAttributes.addFlashAttribute("errorMessage", "この商品はすでに通報済みです。");
			return redirectToDetail(id);
		}

		item.setStatus(3);
		itemRepository.save(item);

		MapSqlParameterSource reportParams = new MapSqlParameterSource();
		reportParams.addValue("targetType", 0);
		reportParams.addValue("targetId", id);
		reportParams.addValue("userId", loginUser.getUserId());
		reportParams.addValue("reason", "商品詳細画面からの通報");
		reportParams.addValue("createdAt", LocalDateTime.now());

		jdbcTemplate.update("""
				INSERT INTO reports (target_type, target_id, user_id, reason, status, created_at)
				VALUES (:targetType, :targetId, :userId, :reason, 0, :createdAt)
				""", reportParams);

		redirectAttributes.addFlashAttribute("successMessage", "商品を通報しました。管理者の通報確認に登録しました。");
		return redirectToDetail(id);
	}

	private Map<String, Object> findTransferForUser(
			Integer itemId,
			Integer userId) {

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("itemId", itemId);
		params.addValue("userId", userId);

		List<Map<String, Object>> transfers = jdbcTemplate.queryForList("""
				SELECT
					t.transaction_id AS "transactionId",
					t.item_id AS "itemId",
					t.seller_id AS "sellerId",
					t.buyer_id AS "buyerId",
					t.status AS "status",
					t.seller_completed AS "sellerCompleted",
					t.buyer_completed AS "buyerCompleted",
					t.transfer_code AS "transferCode",
					t.completed_at AS "completedAt"
				FROM transactions t
				WHERE t.item_id = :itemId
				  AND (t.seller_id = :userId OR t.buyer_id = :userId)
				ORDER BY t.transaction_id DESC
				LIMIT 1
				""", params);

		if (transfers.isEmpty()) {
			return null;
		}

		Map<String, Object> transfer = transfers.get(0);
		if (transfer.get("transferCode") == null
				|| String.valueOf(transfer.get("transferCode")).isBlank()) {
			String transferCode = generateTransferCode();
			MapSqlParameterSource updateParams = new MapSqlParameterSource();
			updateParams.addValue("transactionId", transfer.get("transactionId"));
			updateParams.addValue("transferCode", transferCode);
			jdbcTemplate.update("""
					UPDATE transactions
					SET transfer_code = :transferCode
					WHERE transaction_id = :transactionId
					""", updateParams);
			transfer.put("transferCode", transferCode);
		}

		return transfer;
	}

	private String generateTransferCode() {
		return UUID.randomUUID()
				.toString()
				.replace("-", "")
				.substring(0, 16);
	}

	private String validateActionItem(Item item, User loginUser) {

		if (item == null) {
			return "商品が見つかりません。";
		}

		if (item.getUserId().equals(loginUser.getUserId())) {
			return "自分の出品物には応募できません。";
		}

		if (!Integer.valueOf(0).equals(item.getStatus())) {
			return "この商品は現在受付中ではありません。";
		}

		if (item.getDeadline() != null
				&& item.getDeadline().isBefore(LocalDate.now())) {
			return "応募期限が終了しています。";
		}

		return null;
	}

	private boolean containsNgKeyword(String text) {

		List<String> keywords = jdbcTemplate.queryForList("""
				SELECT keyword
				FROM ng_keywords
				WHERE status = 0
				""", new MapSqlParameterSource(), String.class);

		String normalizedText = text.toLowerCase(Locale.ROOT);

		for (String keyword : keywords) {
			if (keyword != null
					&& !keyword.isBlank()
					&& normalizedText.contains(keyword.toLowerCase(Locale.ROOT))) {
				return true;
			}
		}

		return false;
	}

	private String showCompletePage(
			Integer itemId,
			HttpSession session,
			Model model,
			String templateName) {

		User loginUser = getLoginUser(session);

		if (loginUser == null) {
			return "redirect:/login";
		}

		if (!itemRepository.existsById(itemId)) {
			return "redirect:/item-101";
		}

		model.addAttribute("loginUser", loginUser);
		model.addAttribute("itemId", itemId);
		return templateName;
	}

	private String redirectToDetail(Integer itemId) {
		return "redirect:/detail/" + itemId;
	}

	private User getLoginUser(HttpSession session) {

		Object value = session.getAttribute("loginUser");

		if (value instanceof User user) {
			return user;
		}

		return null;
	}
}
