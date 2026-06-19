package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

	Optional<User> findByEmail(String email);

	Optional<User> findByEmailAndPassword(String email, String password);

	// status=0（有効ユーザー）だけをログイン対象にする場合に使用
	Optional<User> findByEmailAndPasswordAndStatus(
			String email,
			String password,
			Integer status);
}
