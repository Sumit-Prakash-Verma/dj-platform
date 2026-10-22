
package dj_platform;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "soundclash_rounds")
public class SoundclashRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Integer roomId;

    @Column(name = "song_a_title", nullable = false, length = 200)
    private String songATitle;

    @Column(name = "song_a_artist", length = 200)
    private String songAArtist;

    @Column(name = "song_b_title", nullable = false, length = 200)
    private String songBTitle;

    @Column(name = "song_b_artist", length = 200)
    private String songBArtist;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean active = true;

    public SoundclashRound() {
    }

    @PrePersist
    public void beforeSave() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public String getSongATitle() {
        return songATitle;
    }

    public void setSongATitle(String songATitle) {
        this.songATitle = songATitle;
    }

    public String getSongAArtist() {
        return songAArtist;
    }

    public void setSongAArtist(String songAArtist) {
        this.songAArtist = songAArtist;
    }

    public String getSongBTitle() {
        return songBTitle;
    }

    public void setSongBTitle(String songBTitle) {
        this.songBTitle = songBTitle;
    }

    public String getSongBArtist() {
        return songBArtist;
    }

    public void setSongBArtist(String songBArtist) {
        this.songBArtist = songBArtist;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
