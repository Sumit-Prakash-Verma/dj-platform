
package dj_platform;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface UserMusicPreferenceRepository
        extends JpaRepository<UserMusicPreference, Long> {

    List<UserMusicPreference> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndGenreIgnoreCase(
            String username, String genre);

    @Transactional
    void deleteByUsernameIgnoreCase(String username);
}
