package com.queuesmart.repository;

import com.queuesmart.model.User;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class UserRepository {

    private final Map<String, User> usersById = new ConcurrentHashMap<>();
    private final Map<String, User> usersByEmail = new ConcurrentHashMap<>();

    public User save(User user) {
        usersById.put(user.getId(), user);
        usersByEmail.put(user.getEmail(), user);
        return user;
    }

    public Optional<User> findById(String id) {
        return Optional.ofNullable(usersById.get(id));
    }

    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(usersByEmail.get(email));
    }

    public boolean existsByEmail(String email) {
        return usersByEmail.containsKey(email);
    }

    public boolean existsByUsername(String username) {
        return usersById.values().stream()
                .anyMatch(u -> u.getUsername().equalsIgnoreCase(username));
    }

    public List<User> findAll() {
        return new ArrayList<>(usersById.values());
    }

    public void delete(String id) {
        User user = usersById.remove(id);
        if (user != null) {
            usersByEmail.remove(user.getEmail());
        }
    }

    public int count() {
        return usersById.size();
    }
}
