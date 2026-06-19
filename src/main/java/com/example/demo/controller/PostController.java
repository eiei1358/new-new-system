package com.example.demo.controller;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ItemRepository;
import com.example.demo.entity.Item;
import com.example.demo.entity.User;

@Controller
@RequestMapping("/item")
public class PostController {

	private static final String PENDING_FORM = "pendingPostForm";
	private static final String PENDING_IMAGES = "pendingImageUrls";

	private final ItemRepository itemRepository;
	private final CategoryRepository categoryRepository;
	private final NamedParameterJdbcTemplate jdbcTemplate;

	public PostController(
			ItemRepository itemRepository,
			CategoryRepository categoryRepository,
			NamedParameterJdbcTemplate jdbcTemplate) {

		this.itemRepository = itemRepository;
		this.categoryRepository = categoryRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

	@GetMapping("/post")
	public String showPostPage(
			HttpSession session,
			Model model) {

		User loginUser = getLoginUser(session);

		if (loginUser == null) {
			return "redirect:/login";
		}

		PostForm postForm = new PostForm();
		Object pending = session.getAttribute(PENDING_FORM);

		if (pending instanceof PostForm savedForm) {
			postForm = savedForm;
		} else {
			postForm.setType(0);
		}

		setPostPageModel(model, loginUser, postForm);

		return "post";
	}

	@PostMapping("/confirm")
	public String confirm(
			@ModelAttribute("postForm") PostForm postForm,
			@RequestParam(value = "images", required = false)
			MultipartFile[] images,
			HttpSession session,
			Model model) {

		User loginUser = getLoginUser(session);

		if (loginUser == null) {
			return "redirect:/login";
		}

		String error = validate(postForm);

		if (error != null) {
			setPostPageModel(model, loginUser, postForm);
			model.addAttribute("error", error);
			return "post";
		}

		try {
			List<String> imageUrls = saveUploadedImages(images);

			// 編集画面から戻って画像を選択しなかった場合は、前回の画像を保持する
			if (imageUrls.isEmpty()) {
				imageUrls = getPendingImageUrls(session);
			}

			session.setAttribute(PENDING_FORM, postForm);
			session.setAttribute(PENDING_IMAGES, imageUrls);

			setConfirmPageModel(model, loginUser, postForm, imageUrls);

			return "confirm";

		} catch (IOException e) {
			setPostPageModel(model, loginUser, postForm);
			model.addAttribute("error", "画像の保存に失敗しました。");
			return "post";
		}
	}

	@PostMapping("/complete")
	@Transactional
	public String complete(
			@ModelAttribute("postForm") PostForm postForm,
			HttpSession session,
			Model model) {

		User loginUser = getLoginUser(session);

		if (loginUser == null) {
			return "redirect:/login";
		}

		String error = validate(postForm);
		List<String> imageUrls = getPendingImageUrls(session);

		if (error == null && containsNgKeyword(postForm)) {
			error = "商品名または説明にNGキーワードが含まれています。";
		}

		if (error != null) {
			setConfirmPageModel(model, loginUser, postForm, imageUrls);
			model.addAttribute("error", error);
			return "confirm";
		}

		Item item = new Item();
		item.setUserId(loginUser.getUserId());
		item.setName(postForm.getName().trim());
		item.setDescription(normalizeDescription(postForm.getDescription()));
		item.setCondition(postForm.getCondition());
		item.setType(postForm.getType());
		item.setStatus(0);
		item.setCategoryId(postForm.getCategoryId());
		item.setDeadline(postForm.getDeadline());
		item.setCreatedAt(LocalDateTime.now());
		item.setPlace(postForm.getPlace());

		if (Integer.valueOf(0).equals(postForm.getType())) {
			item.setPrice(BigDecimal.ZERO);
		} else {
			item.setPrice(postForm.getPrice());
		}

		Item savedItem = itemRepository.saveAndFlush(item);

		for (String imageUrl : imageUrls) {
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("itemId", savedItem.getItemId());
			params.addValue("imageUrl", imageUrl);

			jdbcTemplate.update("""
					INSERT INTO item_images (item_id, image_url)
					VALUES (:itemId, :imageUrl)
					""", params);
		}

		session.removeAttribute(PENDING_FORM);
		session.removeAttribute(PENDING_IMAGES);

		return "redirect:/mypage";
	}

	private void setPostPageModel(
			Model model,
			User loginUser,
			PostForm postForm) {

		model.addAttribute("loginUser", loginUser);
		model.addAttribute("postForm", postForm);
		model.addAttribute("categories", categoryRepository.findAll());
	}

	private void setConfirmPageModel(
			Model model,
			User loginUser,
			PostForm postForm,
			List<String> imageUrls) {

		model.addAttribute("loginUser", loginUser);
		model.addAttribute("postForm", postForm);
		model.addAttribute("imageUrls", imageUrls);
		model.addAttribute(
				"categoryName",
				categoryRepository.findById(postForm.getCategoryId())
						.map(category -> category.getName())
						.orElse("不明"));
	}

	private String validate(PostForm postForm) {

		if (postForm.getName() == null || postForm.getName().isBlank()) {
			return "物品名を入力してください。";
		}

		if (postForm.getCategoryId() == null
				|| !categoryRepository.existsById(postForm.getCategoryId())) {
			return "カテゴリを選択してください。";
		}

		if (postForm.getCondition() == null
				|| postForm.getCondition() < 0
				|| postForm.getCondition() > 2) {
			return "物品状態を選択してください。";
		}

		if (postForm.getPlace() == null
				|| postForm.getPlace() < 0
				|| postForm.getPlace() > 1) {
			return "受け取り場所を選択してください。";
		}

		if (postForm.getType() == null
				|| postForm.getType() < 0
				|| postForm.getType() > 2) {
			return "価格条件を選択してください。";
		}

		if (!Integer.valueOf(0).equals(postForm.getType())) {
			if (postForm.getPrice() == null) {
				return "有償またはオークションの場合は金額を入力してください。";
			}

			if (postForm.getPrice().compareTo(BigDecimal.ZERO) < 0) {
				return "金額は0円以上で入力してください。";
			}
		}

		if (postForm.getDeadline() == null) {
			return "出品終了日を入力してください。";
		}

		return null;
	}

	private List<String> saveUploadedImages(MultipartFile[] images)
			throws IOException {

		List<String> imageUrls = new ArrayList<>();

		if (images == null || images.length == 0) {
			return imageUrls;
		}

		Path uploadDirectory = Paths.get(
				System.getProperty("user.dir"),
				"public",
				"img")
				.toAbsolutePath()
				.normalize();

		Files.createDirectories(uploadDirectory);

		for (MultipartFile image : images) {
			if (image == null || image.isEmpty()) {
				continue;
			}

			String contentType = image.getContentType();

			if (contentType == null
					|| !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
				throw new IOException("画像以外のファイルが選択されています。");
			}

			String extension = getSafeExtension(image.getOriginalFilename());
			String fileName = UUID.randomUUID() + extension;
			Path destination = uploadDirectory.resolve(fileName).normalize();

			if (!destination.startsWith(uploadDirectory)) {
				throw new IOException("不正なファイル名です。");
			}

			image.transferTo(destination);
			imageUrls.add("/img/" + fileName);
		}

		return imageUrls;
	}

	private String getSafeExtension(String originalFilename) {

		if (originalFilename == null) {
			return "";
		}

		String fileName = Paths.get(originalFilename).getFileName().toString();
		int dotIndex = fileName.lastIndexOf('.');

		if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
			return "";
		}

		String extension = fileName.substring(dotIndex).toLowerCase(Locale.ROOT);

		if (!extension.matches("\\.[a-z0-9]{1,8}")) {
			return "";
		}

		return extension;
	}

	private boolean containsNgKeyword(PostForm postForm) {

		List<String> keywords = jdbcTemplate.queryForList("""
				SELECT keyword
				FROM ng_keywords
				WHERE status = 0
				""", new MapSqlParameterSource(), String.class);

		String target = (
				postForm.getName() + " "
				+ normalizeDescription(postForm.getDescription()))
				.toLowerCase(Locale.ROOT);

		for (String keyword : keywords) {
			if (keyword != null
					&& !keyword.isBlank()
					&& target.contains(keyword.toLowerCase(Locale.ROOT))) {
				return true;
			}
		}

		return false;
	}

	private String normalizeDescription(String description) {
		return description == null ? "" : description.trim();
	}

	@SuppressWarnings("unchecked")
	private List<String> getPendingImageUrls(HttpSession session) {

		Object value = session.getAttribute(PENDING_IMAGES);

		if (value instanceof List<?>) {
			return new ArrayList<>((List<String>) value);
		}

		return new ArrayList<>();
	}

	private User getLoginUser(HttpSession session) {

		Object value = session.getAttribute("loginUser");

		if (value instanceof User user) {
			return user;
		}

		return null;
	}

	public static class PostForm implements Serializable {

		private static final long serialVersionUID = 1L;

		private String name;
		private String description;
		private Integer condition;
		private BigDecimal price;
		private Integer type;
		private Integer categoryId;

		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
		private LocalDate deadline;

		private Integer place;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public Integer getCondition() {
			return condition;
		}

		public void setCondition(Integer condition) {
			this.condition = condition;
		}

		public BigDecimal getPrice() {
			return price;
		}

		public void setPrice(BigDecimal price) {
			this.price = price;
		}

		public Integer getType() {
			return type;
		}

		public void setType(Integer type) {
			this.type = type;
		}

		public Integer getCategoryId() {
			return categoryId;
		}

		public void setCategoryId(Integer categoryId) {
			this.categoryId = categoryId;
		}

		public LocalDate getDeadline() {
			return deadline;
		}

		public void setDeadline(LocalDate deadline) {
			this.deadline = deadline;
		}

		public Integer getPlace() {
			return place;
		}

		public void setPlace(Integer place) {
			this.place = place;
		}
	}
}
