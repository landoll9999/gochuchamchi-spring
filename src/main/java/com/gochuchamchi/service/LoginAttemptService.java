package com.gochuchamchi.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Locale;

/** Account-based login throttling backed by Redis. IP-wide throttling is handled by WAF. */
@Service
public class LoginAttemptService {

    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);
    private static final int THROTTLE_START_FAILURES = 5;
    private static final String FAILURE_KEY_PREFIX = "login:failure:user:";
    private static final String COOLDOWN_KEY_PREFIX = "login:cooldown:user:";

    private final StringRedisTemplate redisTemplate;

    public LoginAttemptService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void ensureNotThrottled(String username) {
        if (isThrottled(username)) {
            throw new LockedException("Login attempts are temporarily limited.");
        }
    }

    public void recordFailure(String username) {
        if (username == null || username.isBlank()) return;

        String identity = identity(username);
        Long failures = redisTemplate.opsForValue().increment(FAILURE_KEY_PREFIX + identity);
        redisTemplate.expire(FAILURE_KEY_PREFIX + identity, FAILURE_WINDOW);

        if (failures != null && failures >= THROTTLE_START_FAILURES) {
            redisTemplate.opsForValue().set(COOLDOWN_KEY_PREFIX + identity, "1", cooldownFor(failures));
        }
    }

    public void reset(String username) {
        if (username == null || username.isBlank()) return;

        String identity = identity(username);
        redisTemplate.delete(FAILURE_KEY_PREFIX + identity);
        redisTemplate.delete(COOLDOWN_KEY_PREFIX + identity);
    }

    public boolean isThrottled(String username) {
        return username != null && !username.isBlank()
                && Boolean.TRUE.equals(redisTemplate.hasKey(COOLDOWN_KEY_PREFIX + identity(username)));
    }

    private Duration cooldownFor(long failures) {
        if (failures == 5) return Duration.ofSeconds(30);
        if (failures == 6) return Duration.ofMinutes(1);
        if (failures == 7) return Duration.ofMinutes(2);
        return Duration.ofMinutes(15);
    }

    private String identity(String username) {
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
