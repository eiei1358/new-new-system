package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.service.MailUtil;
import com.example.demo.entity.User;

@Controller
public class MessageController {

	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final JavaMailSender mailSender;

	public MessageController(
			NamedParameterJdbcTemplate jdbcTemplate,
			ObjectProvider<JavaMailSender> mailSenderProvider) {

		this.jdbcTemplate = jdbcTemplate;
		this.mailSender = mailSenderProvider.getIfAvailable();
	}

	/**
	 * メッセージ一覧を表示します。
	 *
	 * detail.html または auction-detail.html から itemId を受け取った場合は、
	 * その商品の出品者とのチャットを一覧の先頭へ表示します。
	 */
	@GetMapping("/messages")
	public String showMessages(
			@RequestParam(required = false) Integer itemId,
			HttpSession session,
			Model model,
			RedirectAttributes redirectAttributes) {

		User loginUser = getLoginUser(session);

		if (loginUser == null) {
			return "redirect:/login";
		}

		MapSqlParameterSource params = new MapSqlParameterSource(
				"userId",
				loginUser.getUserId());

		List<Map<String, Object>> messages = new ArrayList<>(jdbcTemplate.queryForList("""
				SELECT *
				FROM (
					SELECT DISTINCT ON (
						m.item_id,
						LEAST(m.sender_id, m.receiver_id),
						GREATEST(m.sender_id, m.receiver_id)
					)
						m.item_id AS "itemId",
						CASE
							WHEN m.sender_id = :userId THEN m.receiver_id
							ELSE m.sender_id
						END AS "otherUserId",
						COALESCE(i.name, '商品なし') AS "itemName",
						CASE
							WHEN m.sender_id = :userId THEN receiver_user.name
							ELSE sender_user.name
						END AS "otherUserName",
						m.content AS "latestMessage",
						TO_CHAR(m.created_at, 'YYYY-MM-DD HH24:MI') AS "latestCreatedAt",
						m.created_at AS "latestSortAt",
						(
							SELECT ii.image_url
							FROM item_images ii
							WHERE ii.item_id = m.item_id
							ORDER BY ii.image_id
							LIMIT 1
						) AS "imageUrl"
					FROM messages m
					INNER JOIN users sender_user
						ON m.sender_id = sender_user.user_id
					INNER JOIN users receiver_user
						ON m.receiver_id = receiver_user.user_id
					LEFT JOIN items i
						ON m.item_id = i.item_id
					WHERE m.sender_id = :userId
					   OR m.receiver_id = :userId
					ORDER BY
						m.item_id,
						LEAST(m.sender_id, m.receiver_id),
						GREATEST(m.sender_id, m.receiver_id),
						m.created_at DESC,
						m.message_id DESC
				) message_list
				ORDER BY "latestSortAt" DESC NULLS LAST
				""", params));

		if (itemId != null) {
			List<Map<String, Object>> selectedRoomList = findNewRoom(
					itemId,
					loginUser.getUserId());

			if (selectedRoomList.isEmpty()) {
				redirectAttributes.addFlashAttribute(
						"errorMessage",
						"この商品のチャットを開始できません。");
				return "redirect:/detail/" + itemId;
			}

			Map<String, Object> selectedRoom = selectedRoomList.get(0);
			Integer selectedOtherUserId = numberToInteger(
					selectedRoom.get("otherUserId"));

			int existingIndex = findRoomIndex(
					messages,
					itemId,
					selectedOtherUserId);

			if (existingIndex >= 0) {
				Map<String, Object> existingRoom = messages.remove(existingIndex);
				messages.add(0, existingRoom);
			} else {
				messages.add(0, selectedRoom);
			}

			model.addAttribute("selectedItemId", itemId);
			model.addAttribute("selectedOtherUserId", selectedOtherUserId);
		}

		model.addAttribute("loginUser", loginUser);
		model.addAttribute("messages", messages);

		return "list";
	}

	/**
	 * 商品と相手ユーザーを指定してチャット詳細を表示します。
	 */
	@GetMapping("/messages/{itemId}/{otherUserId}")
	public String openMessage(
			@PathVariable Integer itemId,
			@PathVariable Integer otherUserId,
			HttpSession session,
			Model model,
			RedirectAttributes redirectAttributes) {

		User loginUser = getLoginUser(session);

		if (loginUser == null) {
			return "redirect:/login";
		}

		List<Map<String, Object>> itemList = findItemChatInfo(itemId);

		if (itemList.isEmpty()) {
			redirectAttributes.addFlashAttribute(
					"errorMessage",
					"商品が見つかりません。");
			return "redirect:/messages";
		}

		Map<String, Object> item = itemList.get(0);
		Integer sellerId = numberToInteger(item.get("sellerId"));

		if (!canOpenRoom(
				loginUser.getUserId(),
				otherUserId,
				sellerId,
				itemId,
				Boolean.TRUE.equals(item.get("availableForNewChat")))) {

			redirectAttributes.addFlashAttribute(
					"errorMessage",
					"このチャットを開くことはできません。");
			return "redirect:/messages";
		}

		List<Map<String, Object>> otherUserList = jdbcTemplate.queryForList("""
				SELECT
					user_id AS "userId",
					name AS "name"
				FROM users
				WHERE user_id = :otherUserId
				  AND status = 0
				""", new MapSqlParameterSource("otherUserId", otherUserId));

		if (otherUserList.isEmpty()) {
			redirectAttributes.addFlashAttribute(
					"errorMessage",
					"相手ユーザーが見つかりません。");
			return "redirect:/messages";
		}

		MapSqlParameterSource messageParams = new MapSqlParameterSource();
		messageParams.addValue("itemId", itemId);
		messageParams.addValue("loginUserId", loginUser.getUserId());
		messageParams.addValue("otherUserId", otherUserId);

		List<Map<String, Object>> messageDetails = jdbcTemplate.queryForList("""
				SELECT
					m.message_id AS "messageId",
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
				WHERE m.item_id = :itemId
				  AND (
					(m.sender_id = :loginUserId AND m.receiver_id = :otherUserId)
					OR
					(m.sender_id = :otherUserId AND m.receiver_id = :loginUserId)
				  )
				ORDER BY m.created_at ASC, m.message_id ASC
				""", messageParams);

		model.addAttribute("loginUser", loginUser);
		model.addAttribute("item", item);
		model.addAttribute("otherUser", otherUserList.get(0));
		model.addAttribute("messageDetails", messageDetails);
		model.addAttribute("itemId", itemId);
		model.addAttribute("otherUserId", otherUserId);

		return "detail_message";
	}

	/**
	 * チャット詳細画面からメッセージを送信します。
	 * 出品者と購入希望者のどちらからでも送信できます。
	 */
	@PostMapping("/messages/{itemId}/{otherUserId}/send")
	public String sendMessage(
			@PathVariable Integer itemId,
			@PathVariable Integer otherUserId,
			@RequestParam String content,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		User loginUser = getLoginUser(session);

		if (loginUser == null) {
			return "redirect:/login";
		}

		List<Map<String, Object>> itemList = findItemChatInfo(itemId);

		if (itemList.isEmpty()) {
			redirectAttributes.addFlashAttribute(
					"errorMessage",
					"商品が見つかりません。");
			return "redirect:/messages";
		}

		Map<String, Object> item = itemList.get(0);
		Integer sellerId = numberToInteger(item.get("sellerId"));

		if (!canOpenRoom(
				loginUser.getUserId(),
				otherUserId,
				sellerId,
				itemId,
				Boolean.TRUE.equals(item.get("availableForNewChat")))) {

			redirectAttributes.addFlashAttribute(
					"errorMessage",
					"このチャットへ送信することはできません。");
			return "redirect:/messages";
		}

		if (content == null || content.isBlank()) {
			redirectAttributes.addFlashAttribute(
					"errorMessage",
					"メッセージを入力してください。");
			return redirectToRoom(itemId, otherUserId);
		}

		if (containsNgKeyword(content)) {
			redirectAttributes.addFlashAttribute(
					"errorMessage",
					"NGキーワードが含まれています。");
			return redirectToRoom(itemId, otherUserId);
		}

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("senderId", loginUser.getUserId());
		params.addValue("receiverId", otherUserId);
		params.addValue("itemId", itemId);
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

		sendMessageNotification(otherUserId, loginUser.getName(), content.trim());

		redirectAttributes.addFlashAttribute(
				"successMessage",
				"メッセージを送信しました。");

		return redirectToRoom(itemId, otherUserId);
	}

	/**
	 * メッセージ一覧で代表メッセージIDから開くための互換URLです。
	 * 実際のチャット画面は、商品ID＋相手ユーザーIDのURLへ寄せます。
	 */
	@GetMapping("/messages/{messageId}")
	public String openMessageByMessageId(
			@PathVariable Integer messageId,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		User loginUser = getLoginUser(session);

		if (loginUser == null) {
			return "redirect:/login";
		}

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("messageId", messageId);
		params.addValue("loginUserId", loginUser.getUserId());

		List<Map<String, Object>> rooms = jdbcTemplate.queryForList("""
				SELECT
					m.message_id AS "messageId",
					m.item_id AS "itemId",
					i.user_id AS "sellerId",
					CASE
						WHEN i.user_id = :loginUserId THEN
							CASE
								WHEN m.sender_id = :loginUserId THEN m.receiver_id
								ELSE m.sender_id
							END
						WHEN m.sender_id = :loginUserId THEN m.receiver_id
						ELSE m.sender_id
					END AS "otherUserId"
				FROM messages m
				INNER JOIN items i
					ON m.item_id = i.item_id
				WHERE m.message_id = :messageId
				  AND (
					m.sender_id = :loginUserId
					OR m.receiver_id = :loginUserId
					OR i.user_id = :loginUserId
				  )
				""", params);

		if (rooms.isEmpty()) {
			redirectAttributes.addFlashAttribute(
					"errorMessage",
					"このメッセージは閲覧できません。");
			return "redirect:/messages";
		}

		Map<String, Object> room = rooms.get(0);
		Integer itemId = numberToInteger(room.get("itemId"));
		Integer otherUserId = numberToInteger(room.get("otherUserId"));

		if (itemId == null || otherUserId == null) {
			redirectAttributes.addFlashAttribute(
					"errorMessage",
					"このメッセージを開けません。");
			return "redirect:/messages";
		}

		return redirectToRoom(itemId, otherUserId);
	}

	/**
	 * 商品詳細からメッセージ画面を直接開くためのURLです。
	 */
	@GetMapping("/messages/item/{itemId}")
	public String openItemMessage(
			@PathVariable Integer itemId,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		User loginUser = getLoginUser(session);

		if (loginUser == null) {
			return "redirect:/login";
		}

		List<Map<String, Object>> itemList = findItemChatInfo(itemId);

		if (itemList.isEmpty()) {
			redirectAttributes.addFlashAttribute(
					"errorMessage",
					"商品が見つかりません。");
			return "redirect:/item-101";
		}

		Integer sellerId = numberToInteger(itemList.get(0).get("sellerId"));

		if (sellerId == null) {
			redirectAttributes.addFlashAttribute(
					"errorMessage",
					"出品者情報が見つかりません。");
			return "redirect:/detail/" + itemId;
		}

		if (loginUser.getUserId().equals(sellerId)) {
			redirectAttributes.addFlashAttribute(
					"errorMessage",
					"出品者はメッセージ一覧から相手を選んでください。");
			return "redirect:/messages";
		}

		return redirectToRoom(itemId, sellerId);
	}

	@PostMapping("/messages/item/{itemId}/send")
	public String sendItemMessage(
			@PathVariable Integer itemId,
			@RequestParam Integer otherUserId,
			@RequestParam String content,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		return sendMessage(
				itemId,
				otherUserId,
				content,
				session,
				redirectAttributes);
	}

	private void sendMessageNotification(
			Integer receiverId,
			String senderName,
			String content) {

		List<Map<String, Object>> receivers = jdbcTemplate.queryForList("""
				SELECT name, email
				FROM users
				WHERE user_id = :receiverId
				""", new MapSqlParameterSource("receiverId", receiverId));

		if (receivers.isEmpty()) {
			return;
		}

		Map<String, Object> receiver = receivers.get(0);
		MailUtil.sendMessageNotificationMail(
				mailSender,
				String.valueOf(receiver.get("email")),
				String.valueOf(receiver.get("name")),
				senderName,
				content);
	}

	private List<Map<String, Object>> findNewRoom(
			Integer itemId,
			Integer loginUserId) {

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("itemId", itemId);
		params.addValue("userId", loginUserId);

		return jdbcTemplate.queryForList("""
				SELECT
					i.item_id AS "itemId",
					i.user_id AS "otherUserId",
					i.name AS "itemName",
					u.name AS "otherUserName",
					NULL::VARCHAR AS "latestMessage",
					NULL::VARCHAR AS "latestCreatedAt",
					NULL::TIMESTAMP AS "latestSortAt",
					(
						SELECT ii.image_url
						FROM item_images ii
						WHERE ii.item_id = i.item_id
						ORDER BY ii.image_id
						LIMIT 1
					) AS "imageUrl"
				FROM items i
				INNER JOIN users u
					ON i.user_id = u.user_id
				WHERE i.item_id = :itemId
				  AND i.user_id <> :userId
				  AND (
					(i.status = 0 AND i.deadline >= CURRENT_DATE)
					OR (
						i.status = 1
						AND EXISTS (
							SELECT 1
							FROM applications a
							WHERE a.item_id = i.item_id
							  AND a.user_id = :userId
							  AND a.status = 0
							  AND a.bid_price IS NULL
						)
					)
				  )
				""", params);
	}

	private List<Map<String, Object>> findItemChatInfo(Integer itemId) {

		return jdbcTemplate.queryForList("""
				SELECT
					i.item_id AS "itemId",
					i.user_id AS "sellerId",
					i.name AS "name",
					i.description AS "description",
					i.price AS "price",
					i.type AS "type",
					i.status AS "status",
					i.deadline AS "deadline",
					u.name AS "sellerName",
					c.name AS "categoryName",
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
					CASE i.place
						WHEN 0 THEN '神田'
						WHEN 1 THEN '横浜'
						ELSE '不明'
					END AS "placeName",
					CASE i.status
						WHEN 0 THEN '出品中'
						WHEN 1 THEN '成約中'
						WHEN 2 THEN '譲渡完了'
						WHEN 3 THEN '通報済み'
						ELSE '不明'
					END AS "statusName",
					COALESCE(
						(
							SELECT MAX(a.bid_price)
							FROM applications a
							WHERE a.item_id = i.item_id
							  AND a.bid_price IS NOT NULL
						),
						i.price
					) AS "displayPrice",
					(
						SELECT ii.image_url
						FROM item_images ii
						WHERE ii.item_id = i.item_id
						ORDER BY ii.image_id
						LIMIT 1
					) AS "imageUrl",
					(i.status = 0 AND i.deadline >= CURRENT_DATE)
						AS "availableForNewChat"
				FROM items i
				INNER JOIN users u
					ON i.user_id = u.user_id
				LEFT JOIN categories c
					ON i.category_id = c.category_id
				WHERE i.item_id = :itemId
				""", new MapSqlParameterSource("itemId", itemId));
	}

	private boolean canOpenRoom(
			Integer loginUserId,
			Integer otherUserId,
			Integer sellerId,
			Integer itemId,
			boolean availableForNewChat) {

		if (loginUserId == null
				|| otherUserId == null
				|| sellerId == null
				|| loginUserId.equals(otherUserId)) {
			return false;
		}

		long messageCount = countRoomMessages(
				itemId,
				loginUserId,
				otherUserId);

		if (loginUserId.equals(sellerId)) {
			// 出品者は、購入希望者から一度でも連絡が来たチャットだけ開けます。
			return messageCount > 0;
		}

		if (!otherUserId.equals(sellerId)) {
			return false;
		}

		// 購入希望者は、既存チャット、受付中の商品、または自分が購入応募した
		// 成約中の商品ならチャットを開けます。
		return messageCount > 0
				|| availableForNewChat
				|| hasActivePurchaseApplication(itemId, loginUserId);
	}

	private boolean hasActivePurchaseApplication(
			Integer itemId,
			Integer userId) {

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("itemId", itemId);
		params.addValue("userId", userId);

		Long count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM applications
				WHERE item_id = :itemId
				  AND user_id = :userId
				  AND status = 0
				  AND bid_price IS NULL
				""", params, Long.class);

		return count != null && count > 0;
	}

	private long countRoomMessages(
			Integer itemId,
			Integer firstUserId,
			Integer secondUserId) {

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("itemId", itemId);
		params.addValue("firstUserId", firstUserId);
		params.addValue("secondUserId", secondUserId);

		Long count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM messages
				WHERE item_id = :itemId
				  AND (
					(sender_id = :firstUserId AND receiver_id = :secondUserId)
					OR
					(sender_id = :secondUserId AND receiver_id = :firstUserId)
				  )
				""", params, Long.class);

		return count == null ? 0 : count;
	}

	private int findRoomIndex(
			List<Map<String, Object>> messages,
			Integer itemId,
			Integer otherUserId) {

		for (int index = 0; index < messages.size(); index++) {
			Map<String, Object> room = messages.get(index);
			Integer roomItemId = numberToInteger(room.get("itemId"));
			Integer roomOtherUserId = numberToInteger(room.get("otherUserId"));

			if (itemId.equals(roomItemId)
					&& otherUserId.equals(roomOtherUserId)) {
				return index;
			}
		}

		return -1;
	}

	private Integer numberToInteger(Object value) {

		if (value instanceof Number number) {
			return number.intValue();
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

	private String redirectToRoom(
			Integer itemId,
			Integer otherUserId) {

		return "redirect:/messages/" + itemId + "/" + otherUserId;
	}

	private User getLoginUser(
			HttpSession session) {

		Object value = session.getAttribute("loginUser");

		if (value instanceof User user) {
			return user;
		}

		return null;
	}
}
