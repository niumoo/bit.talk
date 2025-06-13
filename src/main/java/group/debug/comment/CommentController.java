package group.debug.comment;

import com.alibaba.fastjson2.JSON;
import group.debug.comment.ProofOfWorkUtils.Challenge;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.List;

@RestController
@ResponseBody
@CrossOrigin(origins = {"https://www.wdbyte.com", "https://bing.wdbyte.com", "http://localhost:8000"})
public class CommentController {

    private static Logger log = LoggerFactory.getLogger(CommentController.class);

    private final Pageable limit = Pageable.ofSize(100);

    record PageResult(Long total, List<Comment> data) { }

    private final CommentRepository commentRepository;

    private final AuthUtils authUtils;

    public CommentController(CommentRepository commentRepository,AuthUtils authUtils) {
        this.commentRepository = commentRepository;
        this.authUtils = authUtils;
    }

    @GetMapping("/api/v1/pow/generate")
    public ResponseEntity<Challenge> generateChallenge() {
        return ResponseEntity.ok(ProofOfWorkUtils.getInstance().generateChallenge());
    }

    @PostMapping("/api/v1/comment")
    public ResponseEntity<String> createComment(@RequestBody @Valid Comment comment,
        @RequestHeader("c_timestamp") String timestamp,
        @RequestHeader("c_random") String random,
        @RequestHeader("c_nonce") String nonce) {

        if (!ProofOfWorkUtils.getInstance().validate(timestamp, random, nonce)) {
            return new ResponseEntity<>("验证失败", HttpStatus.UNAUTHORIZED);
        }
        log.info("create comment:{}", comment);
        if (StringUtils.hasLength(comment.getWebsite())) {
            try {
                URL url = new URL(comment.getWebsite());
                // 2. 验证协议
                String protocol = url.getProtocol().toLowerCase();
                if (!protocol.equals("http") && !protocol.equals("https")) {
                    return ResponseEntity.badRequest().body("只允许 HTTP 和 HTTPS 协议");
                }
                comment.setWebsite("%s://%s".formatted(url.getProtocol(), url.getHost()));
            } catch (MalformedURLException e) {
                return ResponseEntity.badRequest().body("URL 格式不正确！");
            }
        }
        commentRepository.save(comment);
        return ResponseEntity.ok("评论创建成功！");
    }

    @GetMapping("/api/v1/comment/{url}")
    public ResponseEntity<List<Comment>> findByUrl(@PathVariable String url) {
        log.info("find comment by url:{}", url);
        String decodedUrl = new String(Base64.getDecoder().decode(url));
        List<Comment> comments = commentRepository.findByUrlAndStatusOrderByIdDesc(decodedUrl, CommentStatus.PASSED, limit);
        comments.forEach(comment -> {
            comment.setEmail(null);
            comment.setStatus(null);
        });
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/api/v1/comment/status/{status}")
    public ResponseEntity<String> findByStatus(@PathVariable String status, @RequestHeader("Authorization") String auth) {
        if (!authUtils.checkAuth(auth)) {
            return new ResponseEntity<>("认证失败", HttpStatus.UNAUTHORIZED);
        }
        log.info("find comment by status:{}", status);
        CommentStatus commentStatus = CommentStatus.valueOf(status.toUpperCase());
        Long total = commentRepository.countByStatus(commentStatus);
        List<Comment> comments = commentRepository.findByStatusOrderByIdDesc(commentStatus, limit);
        PageResult pageResult = new PageResult(total, comments);
        return ResponseEntity.ok(JSON.toJSONString(pageResult));
    }


    @PostMapping("/api/v1/comment/{id}/{action}")
    public ResponseEntity<String> updateCommentStatus(@PathVariable Long id,
        @PathVariable String action,
        @RequestHeader("Authorization") String auth) {
        if (!authUtils.checkAuth(auth)) {
            return new ResponseEntity<>("认证失败", HttpStatus.UNAUTHORIZED);
        }
        log.info("{} comment by id:{}", action, id);
        CommentStatus newStatus = CommentStatus.valueOf(action.toUpperCase());
        updateCommentStatus(id, newStatus);
        return ResponseEntity.ok("success");
    }

    private void updateCommentStatus(Long id, CommentStatus status) {
        commentRepository.findById(id).ifPresent(comment -> {
            comment.setStatus(status);
            commentRepository.save(comment);
        });
    }
}
