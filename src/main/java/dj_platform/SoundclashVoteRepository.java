
package dj_platform;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SoundclashVoteRepository
        extends JpaRepository<SoundclashVote, Long> {

    boolean existsByRoundIdAndUserName(
            Long roundId, String userName);

    long countByRoundIdAndSongChoice(
            Long roundId, String songChoice);
}
