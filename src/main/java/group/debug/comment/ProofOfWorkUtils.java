package group.debug.comment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 工作量证明
 *
 * @author niumoo
 * @date 2025/03/10
 */
public class ProofOfWorkUtils {

    private Logger log = LoggerFactory.getLogger(ProofOfWorkUtils.class);
    /**
     * 工作量证明难度
     */
    private static final int DIFFICULTY = 4;
    /**
     * challenge 过期时间
     */
    private static final long EXPIRATION_MILLIS = 3000;

    // 单例模式
    private static final ProofOfWorkUtils INSTANCE = new ProofOfWorkUtils();

    private ProofOfWorkUtils() {}

    public static ProofOfWorkUtils getInstance() {
        return INSTANCE;
    }

    // 使用 caffeine cache 存储已发出的 challenges
    private static Cache<String, Challenge> challenges = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.SECONDS)
        .maximumSize(10000)
        .build();

    record Challenge(String timestamp, String random, int difficulty) {}

    public Challenge generateChallenge() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString();
        Challenge challenge = new Challenge(timestamp, random, DIFFICULTY);
        // 缓存 challenge
        challenges.put(random, challenge);
        return challenge;
    }

    public boolean validate(String timestamp, String random, String nonce) {
        // 增加参数校验
        if (timestamp == null || random == null || nonce == null) {
            log.error("Invalid parameters");
            return false;
        }
        try {
            Long.parseLong(timestamp);
            Long.parseLong(nonce);
        } catch (NumberFormatException e) {
            log.error("Invalid number format", e);
            return false;
        }
        // 从缓存获取 challenge
        Challenge challenge = challenges.getIfPresent(random);
        if (challenge == null) {
            log.error("challenge not found");
            // random 不存在
            return false;
        }
        // 检查时间戳是否过期
        long ts = Long.parseLong(timestamp);
        if (System.currentTimeMillis() - ts > EXPIRATION_MILLIS) {
            log.error("challenge expired");
            return false;
        }
        // 验证时间戳匹配
        if (!challenge.timestamp().equals(timestamp)) {
            log.error("challenge timestamp not match");
            return false;
        }
        // 验证工作量证明
        String hash = sha256(timestamp + random + nonce);
        boolean valid = hash.startsWith("0".repeat(challenge.difficulty));
        if (valid) {
            // 使用后立即删除,防止重用
            challenges.invalidate(random);
        }
        return valid;
    }

    public static String sha256(String input) {
        //log.info("input:{}", input);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            // 转为16进制
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {hexString.append('0');}
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // 转换为运行时异常
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
