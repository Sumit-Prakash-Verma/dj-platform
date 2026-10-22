
package dj_platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/music-queue")
public class MusicQueueController {

    private final MusicQueueRepository queueRepository;
    private final RoomRepository roomRepository;
    private final RoomMembershipRepository membershipRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final DjTransitionService transitionService;

    public MusicQueueController(
            MusicQueueRepository queueRepository,
            RoomRepository roomRepository,
            RoomMembershipRepository membershipRepository,
            SimpMessagingTemplate messagingTemplate,
            DjTransitionService transitionService) {

        this.queueRepository = queueRepository;
        this.roomRepository = roomRepository;
        this.membershipRepository = membershipRepository;
        this.messagingTemplate = messagingTemplate;
        this.transitionService = transitionService;
    }

    // CHECK LOGIN
    private boolean isLoggedIn(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName());
    }

    // CHECK ROOM MEMBERSHIP
    private boolean isRoomMember(String userName, Integer roomId) {
        return membershipRepository
                .existsByUserNameAndRoomId(userName, roomId);
    }

    // BROADCAST UPDATED QUEUE
    private void broadcastQueue(Integer roomId) {
        List<MusicQueueItem> updatedQueue = queueRepository.findByRoomIdOrderByAddedAtAsc(roomId);

        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomId + "/music-queue",
                (Object) updatedQueue);
    }

    // VALIDATE SONG DETAILS
    private String validateSongRequest(AddSongRequest request) {

        if (request == null
                || request.getTitle() == null
                || request.getTitle().isBlank()) {
            return "Song title is required";
        }

        if (request.getTitle().trim().length() > 200) {
            return "Song title must be 200 characters or fewer";
        }

        String artist = request.getArtist();
        if (artist != null && artist.trim().length() > 200) {
            return "Artist must be 200 characters or fewer";
        }

        String genre = request.getGenre();
        if (genre != null && genre.trim().length() > 50) {
            return "Genre must be 50 characters or fewer";
        }

        Integer bpm = request.getBpm();
        if (bpm != null && (bpm < 30 || bpm > 300)) {
            return "BPM must be between 30 and 300, or leave it empty";
        }

        return null;
    }

    // ADD SONG TO QUEUE
    @PostMapping("/{roomId}")
    public ResponseEntity<?> addSong(
            @PathVariable Integer roomId,
            @RequestBody AddSongRequest request,
            Authentication authentication) {

        if (!isLoggedIn(authentication)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please log in first"));
        }

        String userName = authentication.getName();

        if (!roomRepository.existsById(roomId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Room not found"));
        }

        if (!isRoomMember(userName, roomId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "Join the room before adding a song"));
        }

        String validationError = validateSongRequest(request);

        if (validationError != null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", validationError));
        }

        String title = request.getTitle().trim();
        String artist = cleanValue(request.getArtist());
        String genre = cleanValue(request.getGenre());

        MusicQueueItem song = new MusicQueueItem(
                roomId,
                title,
                artist,
                userName);

        song.setBpm(request.getBpm());
        song.setGenre(genre);

        MusicQueueItem saved = queueRepository.save(song);

        broadcastQueue(roomId);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // GET SONGS FOR A ROOM
    @GetMapping("/{roomId}")
    public ResponseEntity<?> getQueue(
            @PathVariable Integer roomId) {

        if (!roomRepository.existsById(roomId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Room not found"));
        }

        List<MusicQueueItem> songs = queueRepository.findByRoomIdOrderByAddedAtAsc(roomId);

        return ResponseEntity.ok(songs);
    }

    // EDIT SONG IN QUEUE
    @PutMapping("/{roomId}/{songId}")
    public ResponseEntity<?> editSong(
            @PathVariable Integer roomId,
            @PathVariable Long songId,
            @RequestBody AddSongRequest request,
            Authentication authentication) {

        if (!isLoggedIn(authentication)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please log in first"));
        }

        String userName = authentication.getName();

        if (!roomRepository.existsById(roomId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Room not found"));
        }

        if (!isRoomMember(userName, roomId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Join the room first"));
        }

        String validationError = validateSongRequest(request);

        if (validationError != null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", validationError));
        }

        MusicQueueItem song = queueRepository.findById(songId).orElse(null);

        if (song == null || !roomId.equals(song.getRoomId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Song not found in this room"));
        }

        // Only the original song contributor can edit it.
        if (!userName.equals(song.getAddedBy())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "You can edit only your own songs"));
        }

        song.setTitle(request.getTitle().trim());
        song.setArtist(cleanValue(request.getArtist()));
        song.setBpm(request.getBpm());
        song.setGenre(cleanValue(request.getGenre()));

        MusicQueueItem updatedSong = queueRepository.save(song);

        broadcastQueue(roomId);

        return ResponseEntity.ok(updatedSong);
    }

    // DELETE SONG FROM QUEUE
    @DeleteMapping("/{roomId}/{songId}")
    public ResponseEntity<?> deleteSong(
            @PathVariable Integer roomId,
            @PathVariable Long songId,
            Authentication authentication) {

        if (!isLoggedIn(authentication)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please log in first"));
        }

        String userName = authentication.getName();

        if (!roomRepository.existsById(roomId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Room not found"));
        }

        if (!isRoomMember(userName, roomId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Join the room first"));
        }

        MusicQueueItem song = queueRepository.findById(songId).orElse(null);

        if (song == null || !roomId.equals(song.getRoomId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Song not found in this room"));
        }

        // Only the original song contributor can delete it.
        if (!userName.equals(song.getAddedBy())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "You can delete only your own songs"));
        }

        queueRepository.delete(song);

        broadcastQueue(roomId);

        return ResponseEntity.ok(
                Map.of("message", "Song deleted successfully"));
    }

    // GET TRANSITION RECOMMENDATIONS BETWEEN CONSECUTIVE SONGS
    @GetMapping("/{roomId}/transitions")
    public ResponseEntity<?> getTransitions(
            @PathVariable Integer roomId,
            Authentication authentication) {

        if (!isLoggedIn(authentication)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please log in first"));
        }

        if (!roomRepository.existsById(roomId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Room not found"));
        }

        if (!isRoomMember(authentication.getName(), roomId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "Join the room to view its transitions"));
        }

        List<MusicQueueItem> songs = queueRepository.findByRoomIdOrderByAddedAtAsc(roomId);

        List<TransitionRecommendation> recommendations = new ArrayList<>();

        for (int i = 0; i < songs.size() - 1; i++) {

            MusicQueueItem current = songs.get(i);
            MusicQueueItem next = songs.get(i + 1);

            String recommendation = transitionService.recommendTransition(current, next);

            String reason = transitionService.explainTransition(current, next);

            recommendations.add(new TransitionRecommendation(
                    current.getId(),
                    current.getTitle(),
                    current.getBpm(),
                    current.getGenre(),
                    next.getId(),
                    next.getTitle(),
                    next.getBpm(),
                    next.getGenre(),
                    recommendation,
                    reason));
        }

        return ResponseEntity.ok(recommendations);
    }

    // CONVERT EMPTY STRINGS TO NULL
    private String cleanValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    // TRANSITION RESPONSE DTO
    public static class TransitionRecommendation {

        private final Long currentSongId;
        private final String currentTitle;
        private final Integer currentBpm;
        private final String currentGenre;

        private final Long nextSongId;
        private final String nextTitle;
        private final Integer nextBpm;
        private final String nextGenre;

        private final String recommendation;
        private final String reason;

        public TransitionRecommendation(
                Long currentSongId,
                String currentTitle,
                Integer currentBpm,
                String currentGenre,
                Long nextSongId,
                String nextTitle,
                Integer nextBpm,
                String nextGenre,
                String recommendation,
                String reason) {

            this.currentSongId = currentSongId;
            this.currentTitle = currentTitle;
            this.currentBpm = currentBpm;
            this.currentGenre = currentGenre;
            this.nextSongId = nextSongId;
            this.nextTitle = nextTitle;
            this.nextBpm = nextBpm;
            this.nextGenre = nextGenre;
            this.recommendation = recommendation;
            this.reason = reason;
        }

        public Long getCurrentSongId() {
            return currentSongId;
        }

        public String getCurrentTitle() {
            return currentTitle;
        }

        public Integer getCurrentBpm() {
            return currentBpm;
        }

        public String getCurrentGenre() {
            return currentGenre;
        }

        public Long getNextSongId() {
            return nextSongId;
        }

        public String getNextTitle() {
            return nextTitle;
        }

        public Integer getNextBpm() {
            return nextBpm;
        }

        public String getNextGenre() {
            return nextGenre;
        }

        public String getRecommendation() {
            return recommendation;
        }

        public String getReason() {
            return reason;
        }
    }

    // REQUEST DTO
    public static class AddSongRequest {

        private String title;
        private String artist;
        private Integer bpm;
        private String genre;

        public AddSongRequest() {
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
}
