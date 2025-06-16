package group.debug.comment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author niulang
 * @date 2025/06/13
 */
@Component
public class AuthUtils {

    private static final Logger log = LoggerFactory.getLogger(AuthUtils.class);

    @Value("#{T(group.debug.comment.AuthUtils).convertPassword('${password}')}")
    private String password;

    public static String convertPassword(String pwd) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            pwd = ProofOfWorkUtils.sha256(pwd);
        }
        log.info("password:{},cost:{}ms", pwd, System.currentTimeMillis() - start);
        return pwd;
    }

    public boolean checkAuth(String auth) {
        return password.equals(auth);
    }
}
