package com.fungame.songquiz.domain.member;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class PasswordResetTokenGenerator {

    public static final Duration TOKEN_TTL = Duration.ofMinutes(5);

    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final String HASH_ALGORITHM = "SHA-256";

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateRawToken() {
        byte[] token = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(token);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    public String hash(String rawToken) {
        return HexFormat.of().formatHex(digest().digest(rawToken.getBytes(StandardCharsets.UTF_8)));
    }

    private MessageDigest digest() {
        try {
            return MessageDigest.getInstance(HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(HASH_ALGORITHM + " 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}
