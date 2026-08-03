package dfs.server;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BONUS FIX: Prevents replay attacks on write operations.
 * Each write request must carry a unique nonce + timestamp.
 */
public class NonceManager {

    private static final long WINDOW_MS = 5 * 60 * 1000L; // 5-minute window

    // FIX: Java 8 compatible anonymous LinkedHashMap (no diamond <> on anonymous class)
    private final Map<String, Long> usedNonces = Collections.synchronizedMap(
        new LinkedHashMap<String, Long>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                return size() > 10000; // FIX: no underscore in number literal for Java 8
            }
        }
    );

    public void validate(String nonce, long timestamp) throws Exception {
        long now = System.currentTimeMillis();

        if (Math.abs(now - timestamp) > WINDOW_MS)
            throw new Exception("REPLAY REJECTED: Request outside 5-minute time window.");

        if (usedNonces.containsKey(nonce))
            throw new Exception("REPLAY REJECTED: Nonce already used - replay attack blocked.");

        usedNonces.put(nonce, timestamp);
    }
}