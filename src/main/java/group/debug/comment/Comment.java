package group.debug.comment;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
@Table(name = "comment", indexes = {
    @Index(name = "idx_id", columnList = "id"),
    @Index(name = "idx_url", columnList = "url")
})
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotBlank(message = "URL 不能为空")
    @Size(min = 3, max = 512, message = "URL 长度应在 3～512 个字符")
    @Column(name = "url", nullable = false, length = 512)
    private String url;

    @Column(name = "parent_id", nullable = true)
    private Long parentId;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 64, message = "用户名长度应在 3～64 个字符")
    @Column(name = "username", nullable = false, unique = true, length = 64)
    private String username;

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    @Size(max = 128, message = "邮箱长度不能超过 128 个字符")
    @Column(name = "email", nullable = false, unique = true, length = 128)
    private String email;

    @Size(max = 255, message = "个人网站长度不能超过 255 个字符")
    @Column(name = "website", nullable = true, length = 255)
    private String website;

    @NotBlank(message = "评论内容不能为空")
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private CommentStatus status = CommentStatus.WAITING;
}

