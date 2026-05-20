package com.jobqueue.repository;

import com.jobqueue.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    void save(User user);
    Optional<User> findByUsername(String username);
    Optional<User> findById(Integer id);
    List<User> findAll();
    boolean existsByUsername(String username);
}
