
package dj_platform;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.transaction.annotation.Transactional;

public interface SoundclashRoundRepository
        extends JpaRepository<SoundclashRound, Long> {

    Optional<SoundclashRound> findTopByRoomIdAndActiveTrueOrderByIdDesc(Integer roomId);

    List<SoundclashRound> findByRoomIdOrderByIdDesc(Integer roomId);

    @Modifying
    @Transactional
    @Query("""
                update SoundclashRound r
                set r.active = false
                where r.roomId = :roomId and r.active = true
            """)
    int closeActiveRounds(@Param("roomId") Integer roomId);
}
