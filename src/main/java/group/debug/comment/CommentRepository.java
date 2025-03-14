package group.debug.comment;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends CrudRepository<Comment, Long> {


    List<Comment> findByUrlAndStatusOrderByIdDesc(String url, CommentStatus status, Pageable pageable);

    List<Comment> findByStatusOrderByIdDesc(CommentStatus status, Pageable pageable);
}