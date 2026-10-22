
package dj_platform;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByRoomIdOrderByCreatedAtAsc(Long roomId);

    List<ChatMessage> findBySentimentIsNull();
}
