
package dj_platform;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
public class MusicRecommendationController {

    private final SongRepository songRepository;
    private final UserMusicPreferenceRepository preferenceRepository;

    public MusicRecommendationController(
            SongRepository songRepository,
            UserMusicPreferenceRepository preferenceRepository) {
        this.songRepository = songRepository;
        this.preferenceRepository = preferenceRepository;
    }

    // Save or replace the logged-in user's favourite genres
    @PostMapping("/preferences")
    @Transactional
    public ResponseEntity<?> savePreferences(
            @RequestBody PreferenceRequest request,
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please log in first"));
        }

        if (request.getGenres() == null || request.getGenres().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Select at least one genre"));
        }

        List<String> allowedGenres = List.of(
                "EDM", "Bollywood", "Lo-Fi");

        List<String> genres = request.getGenres().stream()
                .filter(genre -> genre != null)
                .map(String::trim)
                .filter(genre -> !genre.isEmpty())
                .map(genre -> allowedGenres.stream()
                        .filter(allowed -> allowed.equalsIgnoreCase(genre))
                        .findFirst()
                        .orElse(null))
                .filter(genre -> genre != null)
                .distinct()
                .toList();

        if (genres.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Choose EDM, Bollywood or Lo-Fi"));
        }

        if (genres.size() != request.getGenres().stream()
                .filter(genre -> genre != null && !genre.isBlank())
                .count()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message",
                            "Only EDM, Bollywood and Lo-Fi are supported"));
        }

        String username = authentication.getName();

        preferenceRepository.deleteByUsernameIgnoreCase(username);

        List<UserMusicPreference> saved = genres.stream()
                .map(genre -> new UserMusicPreference(username, genre))
                .map(preferenceRepository::save)
                .toList();

        return ResponseEntity.ok(Map.of(
                "message", "Music preferences saved",
                "genres", saved.stream()
                        .map(UserMusicPreference::getGenre)
                        .toList()));
    }

    // Get the logged-in user's recommended songs
    @GetMapping
    public ResponseEntity<?> getRecommendations(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please log in first"));
        }

        String username = authentication.getName();

        List<String> genres = preferenceRepository
                .findByUsernameIgnoreCase(username)
                .stream()
                .map(UserMusicPreference::getGenre)
                .distinct()
                .toList();

        if (genres.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "message", "Please save your favourite genres first",
                    "genres", List.of(),
                    "songs", List.of()));
        }

        List<Song> songs = songRepository.findByGenreInIgnoreCase(genres);

        return ResponseEntity.ok(Map.of(
                "genres", genres,
                "songs", songs));
    }

    public static class PreferenceRequest {

        private List<String> genres;

        public PreferenceRequest() {
        }

        public List<String> getGenres() {
            return genres;
        }

        public void setGenres(List<String> genres) {
            this.genres = genres;
        }
    }
}
