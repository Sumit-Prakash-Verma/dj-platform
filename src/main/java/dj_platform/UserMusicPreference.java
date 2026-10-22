
package dj_platform;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_music_preferences")
public class UserMusicPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "genre", nullable = false, length = 100)
    private String genre;

    public UserMusicPreference() {
    }

    public UserMusicPreference(String username, String genre) {
        this.username = username;
        this.genre = genre;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getGenre() {
        return genre;
    }
}
