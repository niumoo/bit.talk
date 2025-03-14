package group.debug.comment;

import java.util.Base64;
import java.util.List;

import com.alibaba.fastjson2.JSON;

import group.debug.comment.ProofOfWorkUtils.Challenge;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ResponseBody
public class CommentController {

    private Logger log = LoggerFactory.getLogger(CommentController.class);

    @Value("${password}")
    private String password;

    private final Pageable limit = Pageable.ofSize(15);

    @Autowired
    private CommentRepository commentRepository;

    @GetMapping("/api/v1/pow/generate")
    public ResponseEntity<Challenge> generateChallenge() {
        return ResponseEntity.ok(ProofOfWorkUtils.getInstance().generateChallenge());
    }

    @PostMapping("/api/v1/comment")
    public ResponseEntity<String> createComment(@RequestBody @Valid Comment comment,
        @RequestHeader("c_timestamp") String timestamp,
        @RequestHeader("c_random") String random,
        @RequestHeader("c_nonce") String nonce) {

        log.info("create comment:{}", comment);
        if (!ProofOfWorkUtils.getInstance().validate(timestamp, random, nonce)) {
            return new ResponseEntity<>("验证失败", HttpStatus.UNAUTHORIZED);
        }
        commentRepository.save(comment);
        return ResponseEntity.ok("评论创建成功！");
    }

    @GetMapping("/api/v1/comment/{url}")
    public ResponseEntity<List<Comment>> findById(@PathVariable String url) {
        log.info("find comment by url:{}", url);
        String decodedUrl = new String(Base64.getDecoder().decode(url));
        List<Comment> comments = commentRepository.findByUrlAndStatusOrderByIdDesc(decodedUrl, CommentStatus.PASSED, limit);
        comments.forEach(comment -> comment.setEmail(null));
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/api/v1/comment/status/{status}")
    public ResponseEntity<String> findByStatus(@PathVariable String status, @RequestHeader("Authorization") String auth) {
        if (!isAuthorized(auth)) {
            return new ResponseEntity<>("认证失败", HttpStatus.UNAUTHORIZED);
        }
        log.info("find comment by status:{}", status);
        CommentStatus commentStatus = CommentStatus.valueOf(status.toUpperCase());
        List<Comment> comments = commentRepository.findByStatusOrderByIdDesc(commentStatus, limit);
        return ResponseEntity.ok(JSON.toJSONString(comments));
    }

    @PostMapping("/api/v1/comment/{id}/{action}")
    public ResponseEntity<String> updateCommentStatus(@PathVariable Long id,
        @PathVariable String action,
        @RequestHeader("Authorization") String auth) {
        if (!isAuthorized(auth)) {
            return new ResponseEntity<>("认证失败", HttpStatus.UNAUTHORIZED);
        }
        log.info("{} comment by id:{}", action, id);
        CommentStatus newStatus = CommentStatus.valueOf(action.toUpperCase());
        updateCommentStatus(id, newStatus);
        return ResponseEntity.ok("success");
    }

    private boolean isAuthorized(String auth) {
        return password.equals(auth);
    }

    private void updateCommentStatus(Long id, CommentStatus status) {
        commentRepository.findById(id).ifPresent(comment -> {
            comment.setStatus(status);
            commentRepository.save(comment);
        });
    }
}
