package com.example.demo.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.service.PasswordUtil;
import com.example.demo.repository.ItemRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.entity.Item;
import com.example.demo.entity.User;

@Controller
public class MypageController {

	private final ItemRepository itemRepository;
	private final UserRepository userRepository;
	private final NamedParameterJdbcTemplate jdbcTemplate;

	public MypageController(
			ItemRepository itemRepository,
			UserRepository userRepository,
			NamedParameterJdbcTemplate jdbcTemplate) {

		this.itemRepository = itemRepository;
		this.userRepository = userRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

	@GetMapping("/mypage")
	public String showMyPage(
			HttpSession session,
			Model model) {

		User loginUser = getLoginUser(session);

		if (loginUser == null) {
			return "redirect:/login";
		}

		setMyPageModel(loginUser, model);

		return "mypage";
	}

	@PostMapping("/mypage/update")
	public String updateUser(
			@RequestParam String name,
			@RequestParam String email,
			@RequestParam(required = false) String password,
			HttpSession session,
			Model model) {

		User loginUser = getLoginUser(session);

		if (loginUser == null) {
			return "redirect:/login";
		}

		if (name == null || name.isBlank()
				|| email == null || email.isBlank()) {
			setMyPageModel(loginUser, model);
			model.addAttribute("error", "名前とメールアドレスを入力してください。");
			return "mypage";
		}

		loginUser.setName(name.trim());
		loginUser.setEmail(email.trim());

		if (password != null && !password.isBlank()) {
			loginUser.setPassword(password);
			loginUser.setTemporaryPassword(false);
			loginUser.setLastPasswordChange(LocalDateTime.now());
		}

		try {
			User updatedUser = userRepository.saveAndFlush(loginUser);
			session.setAttribute("loginUser", updatedUser);
		} catch (DataIntegrityViolationException e) {
			setMyPageModel(loginUser, model);
			model.addAttribute("error", "そのメールアドレスはすでに使用されています。");
			return "mypage";
		}

		return "redirect:/mypage";
	}

	private void setMyPageModel(
			User loginUser,
			Model model) {

		List<Item> allMyItems = itemRepository
				.findByUserIdOrderByCreatedAtDesc(loginUser.getUserId());

		List<Item> items = allMyItems.stream()
				.filter(item -> Integer.valueOf(0).equals(item.getStatus()))
				.toList();

		List<Item> sellHistory = allMyItems.stream()
				.filter(item -> !Integer.valueOf(0).equals(item.getStatus()))
				.toList();

		List<Item> buyHistory = findBuyHistory(loginUser.getUserId());
		Map<Integer, String> itemImages = findFirstImages(
				items,
				sellHistory,
				buyHistory);

		model.addAttribute("user", loginUser);
		model.addAttribute("loginUser", loginUser);
		model.addAttribute("items", items);
		model.addAttribute("sellHistory", sellHistory);
		model.addAttribute("buyHistory", buyHistory);
		model.addAttribute("itemImages", itemImages);
	}

	private List<Item> findBuyHistory(Integer userId) {

		MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);

		List<Integer> itemIds = jdbcTemplate.queryForList("""
				SELECT item_id
				FROM transactions
				WHERE buyer_id = :userId
				ORDER BY transaction_id DESC
				""", params, Integer.class);

		List<Item> buyHistory = new ArrayList<>();

		for (Integer itemId : itemIds) {
			itemRepository.findById(itemId).ifPresent(buyHistory::add);
		}

		return buyHistory;
	}

	@SafeVarargs
	private final Map<Integer, String> findFirstImages(List<Item>... itemLists) {

		List<Integer> itemIds = new ArrayList<>();

		for (List<Item> itemList : itemLists) {
			for (Item item : itemList) {
				if (!itemIds.contains(item.getItemId())) {
					itemIds.add(item.getItemId());
				}
			}
		}

		Map<Integer, String> result = new LinkedHashMap<>();

		for (Integer itemId : itemIds) {
			MapSqlParameterSource params = new MapSqlParameterSource("itemId", itemId);

			List<String> urls = jdbcTemplate.queryForList("""
					SELECT image_url
					FROM item_images
					WHERE item_id = :itemId
					ORDER BY image_id
					LIMIT 1
					""", params, String.class);

			if (!urls.isEmpty()) {
				result.put(itemId, urls.get(0));
			}
		}

		return result;
	}

	private User getLoginUser(HttpSession session) {

		Object value = session.getAttribute("loginUser");

		if (value instanceof User user) {
			return user;
		}

		return null;
	}
}
