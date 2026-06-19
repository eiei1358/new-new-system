package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.User;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

@Controller
public class QrController {

	private static final String DEFAULT_TRANSFER_BASE_URL = "http://192.168.19.144:8080";

	@Value("${app.transfer-base-url:http://192.168.19.144:8080}")
	private String transferBaseUrl;

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public QrController(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@GetMapping("/transfer/item/{itemId}")
	public String showTransferQr(
			@PathVariable Integer itemId,
			HttpSession session,
			HttpServletRequest request,
			Model model,
			RedirectAttributes redirectAttributes) {

		User loginUser = getLoginUser(session);
		if (loginUser == null) {
			return "redirect:/login";
		}

		Map<String, Object> transaction = findUserTransaction(itemId, loginUser.getUserId());
		if (transaction == null) {
			redirectAttributes.addFlashAttribute(
					"errorMessage",
					"この物品の譲渡情報はまだ作成されていません。");
			return "redirect:/detail/" + itemId;
		}

		String transferCode = ensureTransferCode(transaction);
		String transferUrl = buildTransferUrl(request, transferCode);

		model.addAttribute("loginUser", loginUser);
		model.addAttribute("transaction", transaction);
		model.addAttribute("transferCode", transferCode);
		model.addAttribute("transferUrl", transferUrl);

		return "transfer-qr";
	}

	@GetMapping("/transfer/qr/{transferCode}")
	public void showQr(
			@PathVariable String transferCode,
			HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		String transferUrl = buildTransferUrl(request, transferCode);

		QRCodeWriter writer = new QRCodeWriter();
		BitMatrix matrix = writer.encode(
				transferUrl,
				BarcodeFormat.QR_CODE,
				250,
				250);

		response.setContentType("image/png");
		MatrixToImageWriter.writeToStream(
				matrix,
				"PNG",
				response.getOutputStream());
	}

	@GetMapping("/transfer/status/{transferCode}")
	public String transferStatus(
			@PathVariable String transferCode,
			Model model) {

		Map<String, Object> transaction = findTransactionByCode(transferCode);
		if (transaction == null) {
			model.addAttribute("error", "譲渡情報が見つかりません。");
			model.addAttribute("transferCode", transferCode);
			return "transfer-status";
		}

		model.addAttribute("transaction", transaction);
		model.addAttribute("transferCode", transferCode);

		return "transfer-status";
	}

	@PostMapping("/transfer/status/{transferCode}/seller-done")
	public String sellerDone(@PathVariable String transferCode) {
		updateCompletion(transferCode, true);
		return "redirect:/transfer/status/" + transferCode;
	}

	@PostMapping("/transfer/status/{transferCode}/buyer-done")
	public String buyerDone(@PathVariable String transferCode) {
		updateCompletion(transferCode, false);
		return "redirect:/transfer/status/" + transferCode;
	}

	private Map<String, Object> findUserTransaction(
			Integer itemId,
			Integer userId) {

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("itemId", itemId);
		params.addValue("userId", userId);

		List<Map<String, Object>> transactions = jdbcTemplate.queryForList("""
				SELECT
					t.transaction_id AS "transactionId",
					t.item_id AS "itemId",
					t.seller_id AS "sellerId",
					t.buyer_id AS "buyerId",
					t.status AS "status",
					t.seller_completed AS "sellerCompleted",
					t.buyer_completed AS "buyerCompleted",
					t.transfer_code AS "transferCode",
					t.completed_at AS "completedAt",
					i.name AS "itemName",
					seller.name AS "sellerName",
					buyer.name AS "buyerName"
				FROM transactions t
				INNER JOIN items i
					ON t.item_id = i.item_id
				INNER JOIN users seller
					ON t.seller_id = seller.user_id
				INNER JOIN users buyer
					ON t.buyer_id = buyer.user_id
				WHERE t.item_id = :itemId
				  AND (t.seller_id = :userId OR t.buyer_id = :userId)
				ORDER BY t.transaction_id DESC
				LIMIT 1
				""", params);

		return transactions.isEmpty() ? null : transactions.get(0);
	}

	private Map<String, Object> findTransactionByCode(String transferCode) {

		MapSqlParameterSource params = new MapSqlParameterSource("transferCode", transferCode);

		List<Map<String, Object>> transactions = jdbcTemplate.queryForList("""
				SELECT
					t.transaction_id AS "transactionId",
					t.item_id AS "itemId",
					t.seller_id AS "sellerId",
					t.buyer_id AS "buyerId",
					t.status AS "status",
					t.seller_completed AS "sellerCompleted",
					t.buyer_completed AS "buyerCompleted",
					t.transfer_code AS "transferCode",
					t.completed_at AS "completedAt",
					i.name AS "itemName",
					seller.name AS "sellerName",
					buyer.name AS "buyerName"
				FROM transactions t
				INNER JOIN items i
					ON t.item_id = i.item_id
				INNER JOIN users seller
					ON t.seller_id = seller.user_id
				INNER JOIN users buyer
					ON t.buyer_id = buyer.user_id
				WHERE t.transfer_code = :transferCode
				""", params);

		return transactions.isEmpty() ? null : transactions.get(0);
	}

	private String ensureTransferCode(Map<String, Object> transaction) {

		Object value = transaction.get("transferCode");
		if (value != null && !String.valueOf(value).isBlank()) {
			return String.valueOf(value);
		}

		String transferCode = generateTransferCode();
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("transactionId", transaction.get("transactionId"));
		params.addValue("transferCode", transferCode);

		jdbcTemplate.update("""
				UPDATE transactions
				SET transfer_code = :transferCode
				WHERE transaction_id = :transactionId
				""", params);

		transaction.put("transferCode", transferCode);
		return transferCode;
	}

	private void updateCompletion(
			String transferCode,
			boolean sellerSide) {

		Map<String, Object> transaction = findTransactionByCode(transferCode);
		if (transaction == null) {
			return;
		}

		MapSqlParameterSource params = new MapSqlParameterSource("transferCode", transferCode);

		if (sellerSide) {
			jdbcTemplate.update("""
					UPDATE transactions
					SET seller_completed = 1
					WHERE transfer_code = :transferCode
					""", params);
		} else {
			jdbcTemplate.update("""
					UPDATE transactions
					SET buyer_completed = 1
					WHERE transfer_code = :transferCode
					""", params);
		}

		Map<String, Object> updated = findTransactionByCode(transferCode);
		if (updated == null) {
			return;
		}

		Integer sellerCompleted = numberToInteger(updated.get("sellerCompleted"));
		Integer buyerCompleted = numberToInteger(updated.get("buyerCompleted"));
		Integer itemId = numberToInteger(updated.get("itemId"));

		if (Integer.valueOf(1).equals(sellerCompleted)
				&& Integer.valueOf(1).equals(buyerCompleted)) {

			MapSqlParameterSource completeParams = new MapSqlParameterSource();
			completeParams.addValue("transferCode", transferCode);
			completeParams.addValue("itemId", itemId);
			completeParams.addValue("completedAt", LocalDateTime.now());

			jdbcTemplate.update("""
					UPDATE transactions
					SET status = 2,
						completed_at = :completedAt
					WHERE transfer_code = :transferCode
					""", completeParams);

			jdbcTemplate.update("""
					UPDATE items
					SET status = 2
					WHERE item_id = :itemId
					""", completeParams);
		}
	}

	private String buildTransferUrl(
			HttpServletRequest request,
			String transferCode) {

		String baseUrl = transferBaseUrl;
		if (baseUrl == null || baseUrl.isBlank()) {
			baseUrl = DEFAULT_TRANSFER_BASE_URL;
		}

		if (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}

		return baseUrl
				+ request.getContextPath()
				+ "/transfer/status/"
				+ transferCode;
	}

	private String generateTransferCode() {
		return UUID.randomUUID()
				.toString()
				.replace("-", "")
				.substring(0, 16);
	}

	private Integer numberToInteger(Object value) {
		if (value instanceof Number number) {
			return number.intValue();
		}
		return null;
	}

	private User getLoginUser(HttpSession session) {
		Object value = session.getAttribute("loginUser");
		if (value instanceof User user) {
			return user;
		}
		return null;
	}
}
