package dfs.server;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FIX 4: Handles registration and login with salted SHA-256 hashing.
 * Issues expiring session tokens instead of plaintext passwords.
 */
public class AuthManager {

    // FIX 4: Store salted hashes, NOT plaintext passwords
    private final ConcurrentHashMap<String, String> userStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> saltStore = new ConcurrentHashMap<>();

    // FIX 4: Sessions expire after 30 minutes
    private final ConcurrentHashMap<String, Long> sessions = new ConcurrentHashMap<>();
    private static final long TOKEN_TTL_MS = 30 * 60 * 1000L;

    public String register(String username, String password) {
        if (userStore.containsKey(username))
            return "ERROR: Username already exists";
        try {
            String salt = generateSalt();
            String hash = hashPassword(password, salt);
            saltStore.put(username, salt);
            userStore.put(username, hash);
            return "OK";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public String login(String username, String password) {
        try {
            if (!userStore.containsKey(username))
                return "ERROR: Invalid credentials";
            String salt = saltStore.get(username);
            String hash = hashPassword(password, salt);
            if (!hash.equals(userStore.get(username)))
                return "ERROR: Invalid credentials";
            String token = UUID.randomUUID().toString();
            sessions.put(token, System.currentTimeMillis() + TOKEN_TTL_MS);
            return token;
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public void requireAuth(String token) throws SecurityException {
        Long expiry = sessions.get(token);
        if (expiry == null)
            throw new SecurityException("Invalid session token");
        if (System.currentTimeMillis() > expiry) {
            sessions.remove(token);
            throw new SecurityException("Session expired — please login again");
        }
    }

    private String generateSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String hashPassword(String password, String salt) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update((salt + password).getBytes());
        return Base64.getEncoder().encodeToString(md.digest());
    }
}