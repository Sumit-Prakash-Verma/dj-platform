
package dj_platform;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "music_queue")
public class MusicQueueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Integer roomId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 200)
    private String artist;

    @Column(name = "added_by", nullable = false, length = 100)
    private String addedBy;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    @Column
    private Integer bpm;

    @Column(length = 50)
    private String genre;

    public MusicQueueItem() {
    }

    public MusicQueueItem(
            Integer roomId,
            String title,
            String artist,
            String addedBy) {
        this.roomId = roomId;
        this.title = title;
        this.artist = artist;
        this.addedBy = addedBy;
    }

    @PrePersist
    public void setAddedAtBeforeSave() {
        if (addedAt == null) {
            addedAt = LocalDateTime.now();
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public Integer getBpm() {
        return bpm;
    }

    public void setBpm(Integer bpm) {
        this.bpm = bpm;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }
}
