package me.iabot.web;

import me.iabot.Main;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.security.SecureRandom;
import java.math.BigInteger;

public class AuthManager {
    private Main plugin;
    private Map<String, String> sessions; // session_id -> username
    private Map<String, Long> sessionExpirations;
    private String adminPassword;

    public AuthManager(Main plugin) {
        this.plugin = plugin;
        this.sessions = new ConcurrentHashMap<>();
        this.sessionExpirations = new ConcurrentHashMap<>();
        this.adminPassword = plugin.getConfig().getString("web.admin-password", "admin123");
    }

    public boolean authenticate(String username, String password) {
        // Apenas admin por enquanto
        return "admin".equals(username) && adminPassword.equals(password);
    }

    public String createSession(String username) {
        String sessionId = generateSessionId();
        long expiration = System.currentTimeMillis() + 
            (plugin.getConfig().getInt("web.session-timeout", 3600) * 1000L);
        
        sessions.put(sessionId, username);
        sessionExpirations.put(sessionId, expiration);
        
        return sessionId;
    }

    public boolean isValidSession(String sessionId) {
        if (sessionId == null) return false;
        
        Long expiration = sessionExpirations.get(sessionId);
        if (expiration == null) return false;
        
        if (System.currentTimeMillis() > expiration) {
            // Sessão expirada
            sessions.remove(sessionId);
            sessionExpirations.remove(sessionId);
            return false;
        }
        
        return true;
    }

    public void invalidateSession(String sessionId) {
        sessions.remove(sessionId);
        sessionExpirations.remove(sessionId);
    }

    public String getUsername(String sessionId) {
        return sessions.get(sessionId);
    }

    private String generateSessionId() {
        SecureRandom random = new SecureRandom();
        return new BigInteger(130, random).toString(32);
    }

    public void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        sessionExpirations.entrySet().removeIf(entry -> {
            if (entry.getValue() < now) {
                sessions.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
}