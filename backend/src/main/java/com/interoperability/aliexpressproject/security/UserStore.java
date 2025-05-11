// src/main/java/com/interoperability/aliexpressproject/security/UserStore.java
package com.interoperability.aliexpressproject.security;

import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ultra‑light in‑memory user repository.
 * NOT for production – purely to satisfy the project rubric.
 */
@Component
public class UserStore {

    private final Map<String,String> users = new ConcurrentHashMap<>();
    private final BCryptPasswordEncoder enc = new BCryptPasswordEncoder();

    /** Pre‑seed one demo account so the TA can test instantly. */
    @PostConstruct
    void init() {
        addUser("demo", "demo");
    }

    /**
     * @return true if the user was created, false if the username already exists
     */
    public boolean addUser(String username, String rawPassword) {
        return users.putIfAbsent(username, enc.encode(rawPassword)) == null;
    }

    /**
     * Validates supplied credentials.
     */
    public boolean validCredentials(String username, String rawPassword) {
        String hash = users.get(username);
        return hash != null && enc.matches(rawPassword, hash);
    }
}
