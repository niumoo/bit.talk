package group.debug.comment;

import java.util.Base64;
import java.util.List;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author https://www.wdbyte.com
 */
@Slf4j
@RestController
public class CommentController {

    private final Pageable limit = Pageable.ofSize(1000);

    @Autowired
    private CommentRepository commentRepository;

    @PostMapping("/api/v1/comment")
    public ResponseEntity<String> createComment(@RequestBody @Valid Comment comment) {
        log.info("create comment:{}", comment);
        commentRepository.save(comment);
        return ResponseEntity.ok("评论创建成功！");
    }

    @GetMapping("/api/v1/comment/{url}")
    public String findById(@PathVariable String url) {
        log.info("find comment by url:{}", url);
        // url base64 解码
        byte[] decode = Base64.getDecoder().decode(url);
        url = new String(decode);
        List<Comment> comment = commentRepository.findByUrlAndStatusOrderByIdDesc(url, CommentStatus.WAITING, limit);
        return comment.toString();
    }

}
