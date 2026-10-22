
package dj_platform;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SongRepository extends JpaRepository<Song, Long> {

    List<Song> findByGenreIgnoreCase(String genre);

    List<Song> findByGenreInIgnoreCase(List<String> genres);
}
