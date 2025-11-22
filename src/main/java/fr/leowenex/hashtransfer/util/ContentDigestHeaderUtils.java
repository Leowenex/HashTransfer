package fr.leowenex.hashtransfer.util;

import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class ContentDigestHeaderUtils {

    public static final String CONTENT_DIGEST_HEADER = "Content-Digest";

    public static final String SHA256_ALGORITHM = "sha-256";
    public static final String SHA512_ALGORITHM = "sha-512";
    public static final String SHA1_ALGORITHM = "sha";
    public static final String MD5_ALGORITHM = "md5";

    public static final String SHA256_ALGORITHM_PREFIX = SHA256_ALGORITHM + "=";
    public static final String SHA512_ALGORITHM_PREFIX = SHA512_ALGORITHM + "=";
    public static final String SHA1_ALGORITHM_PREFIX = SHA1_ALGORITHM + "=";
    public static final String MD5_ALGORITHM_PREFIX = MD5_ALGORITHM + "=";

    public static Map<String, String> parseContentDigestHeader(String headerValue) {
        Map<String, String> digestMap = new HashMap<>();
        if (headerValue == null || headerValue.isEmpty()) {
            return digestMap;
        }
        String[] parts = headerValue.split(",");
        for (String part : parts) {
            String[] algoAndDigest = part.trim().split("=");
            if (algoAndDigest.length == 2) {
                digestMap.put(algoAndDigest[0].trim(), algoAndDigest[1].trim());
            }
        }
        return digestMap;
    }
}
