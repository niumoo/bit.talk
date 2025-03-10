package group.debug.comment;

import group.debug.comment.ProofOfWorkUtils.Challenge;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author niumoo
 * @date 2025/03/10
 */
class ProofOfWorkUtilsTest {

    private Logger log = LoggerFactory.getLogger(ProofOfWorkUtilsTest.class);

    @Test
    void generateChallenge() {
    }

    @Test
    void validate() throws InterruptedException {

        long start = System.currentTimeMillis();
        Challenge challenge = ProofOfWorkUtils.getInstance().generateChallenge();
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            String hash = ProofOfWorkUtils.sha256(challenge.timestamp() + challenge.random() + i);
            if (hash.startsWith("0".repeat(challenge.difficulty()))) {
                log.info("hash:{}", hash);
                log.info("cost:{}", System.currentTimeMillis() - start);
                //Thread.sleep(6000);
                log.info(ProofOfWorkUtils.getInstance().validate(challenge.timestamp(), challenge.random(), i + "")+"");
                //log.info(ProofOfWorkUtils.getInstance().validate(challenge.timestamp(), challenge.random(), i + "")+"");
                break;
            }
        }
    }

    @Test
    void sha256() {
    }
}