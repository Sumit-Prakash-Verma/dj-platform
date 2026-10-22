
package dj_platform;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "soundclash_votes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_soundclash_round_user", columnNames = { "round_id", "user_name" })
})
public class SoundclashVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @Column(name = "song_choice", nullable = false, length = 1)
    private String songChoice;

    @Column(name = "voted_at", nullable = false)
    private LocalDateTime votedAt;

    public SoundclashVote() {
    }

    @PrePersist
    public void beforeSave() {
        if (votedAt == null) {
            votedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getRoundId() {
        return roundId;
    }

    public void setRoundId(Long roundId) {
        this.roundId = roundId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getSongChoice() {
        return songChoice;
    }

    public void setSongChoice(String songChoice) {
        this.songChoice = songChoice;
    }

    public LocalDateTime getVotedAt() {
        return votedAt;
    }
}
