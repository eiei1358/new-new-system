package com.example.demo.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.Item;

public interface ItemRepository extends JpaRepository<Item, Integer> {

	@Query("""
			SELECT i
			FROM Item i
			WHERE (
				:keyword IS NULL
				OR :keyword = ''
				OR LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
				OR LOWER(COALESCE(i.description, ''))
					LIKE LOWER(CONCAT('%', :keyword, '%'))
			)
			AND (:categoryId IS NULL OR i.categoryId = :categoryId)
			AND (:condition IS NULL OR i.condition = :condition)
			AND (:place IS NULL OR i.place = :place)
			AND (:minPrice IS NULL OR i.price >= :minPrice)
			AND (:maxPrice IS NULL OR i.price <= :maxPrice)
			""")
	List<Item> searchItems(
			@Param("keyword") String keyword,
			@Param("categoryId") Integer categoryId,
			@Param("condition") Integer condition,
			@Param("place") Integer place,
			@Param("minPrice") BigDecimal minPrice,
			@Param("maxPrice") BigDecimal maxPrice,
			Sort sort);

	List<Item> findByUserIdOrderByCreatedAtDesc(Integer userId);
}