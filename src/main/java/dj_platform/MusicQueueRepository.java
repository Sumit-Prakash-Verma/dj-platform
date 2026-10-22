
package dj_platform;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MusicQueueRepository
        extends JpaRepository<MusicQueueItem, Long> {

    List<MusicQueueItem> findByRoomIdOrderByAddedAtAsc(
            Integer roomId);
}
