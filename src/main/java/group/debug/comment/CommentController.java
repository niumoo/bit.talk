package group.debug.comment;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.alibaba.fastjson2.JSON;

import group.debug.comment.ProofOfWorkUtils.Challenge;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author https://www.wdbyte.com
 */
@RestController
@ResponseBody
public class CommentController {
    public static final String PASSWORD = "Basic d2RieXRlOmRlbGV0ZQ==";
    private Logger log = LoggerFactory.getLogger(CommentController.class);

    private final Pageable limit = Pageable.ofSize(1000);

    @Autowired
    private CommentRepository commentRepository;

    @GetMapping("/api/v1/pow/generate")
    public ResponseEntity<Challenge> generateChallenge() {
        return ResponseEntity.ok(ProofOfWorkUtils.getInstance().generateChallenge());
    }

    @PostMapping("/api/v1/comment")
    public ResponseEntity<String> createComment(@RequestBody @Valid Comment comment,HttpServletRequest request) {
        log.info("create comment:{}", comment);
        String timestamp = request.getHeader("c_timestamp");
        String random = request.getHeader("c_random");
        String nonce = request.getHeader("c_nonce");
        if (!ProofOfWorkUtils.getInstance().validate(timestamp, random, nonce)) {
            return new ResponseEntity<>("验证失败", HttpStatus.UNAUTHORIZED);
        }
        commentRepository.save(comment);
        return ResponseEntity.ok("评论创建成功！");
    }

    @GetMapping("/api/v1/comment/{url}")
    public ResponseEntity<List<Comment>> findById(@PathVariable String url) {
        log.info("find comment by url:{}", url);
        // url base64 解码
        byte[] decode = Base64.getDecoder().decode(url);
        url = new String(decode);
        List<Comment> comments = commentRepository.findByUrlAndStatusOrderByIdDesc(url, CommentStatus.WAITING, limit);
        for (Comment comment : comments) {comment.setEmail(null);}
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/api/v1/comment/status/{waiting}")
    public ResponseEntity<String> findByStatus(@PathVariable String waiting, HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (!PASSWORD.equals(auth)) {
            return new ResponseEntity<>("认证失败", HttpStatus.UNAUTHORIZED);
        }
        log.info("find comment by status:{}", waiting);
        List<Comment> comment = new ArrayList<>(0);
        if (CommentStatus.WAITING.name().toLowerCase().equals(waiting)) {
            comment = commentRepository.findByStatusOrderByIdDesc(CommentStatus.WAITING, limit);
        }
        if (CommentStatus.PASSED.name().toLowerCase().equals(waiting)) {
            comment = commentRepository.findByStatusOrderByIdDesc(CommentStatus.PASSED, limit);
        }
        return ResponseEntity.ok(JSON.toJSONString(comment));
    }
}
