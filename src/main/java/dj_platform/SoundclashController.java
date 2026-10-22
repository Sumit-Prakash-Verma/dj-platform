
package dj_platform;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;

@RestController
@RequestMapping("/api/soundclash")
public class SoundclashController {

    private final SoundclashRoundRepository roundRepository;
    private final SoundclashVoteRepository voteRepository;
    private final RoomRepository roomRepository;
    private final RoomMembershipRepository membershipRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public SoundclashController(
            SoundclashRoundRepository roundRepository,
            SoundclashVoteRepository voteRepository,
            RoomRepository roomRepository,
            RoomMembershipRepository membershipRepository,
            SimpMessagingTemplate messagingTemplate) {

        this.roundRepository = roundRepository;
        this.voteRepository = voteRepository;
        this.roomRepository = roomRepository;
        this.membershipRepository = membershipRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // CREATE A NEW ROUND
    @PostMapping("/rooms/{roomId}/rounds")
    public ResponseEntity<?> createRound(
            @PathVariable Integer roomId,
            @RequestBody CreateRoundRequest request,
            Authentication authentication) {

        if (authentication == null) {
            return error(HttpStatus.UNAUTHORIZED, "Please login first.");
        }

        if (!roomRepository.existsById(roomId)) {
            return error(HttpStatus.NOT_FOUND, "Room not found.");
        }

        if (!isMember(authentication.getName(), roomId)) {
            return error(HttpStatus.FORBIDDEN,
                    "Join the room before creating a round.");
        }

        if (request == null
                || blank(request.getSongATitle())
                || blank(request.getSongBTitle())) {
            return error(HttpStatus.BAD_REQUEST,
                    "Both song titles are required.");
        }

        String titleA = request.getSongATitle().trim();
        String titleB = request.getSongBTitle().trim();

        if (titleA.length() > 200
                || titleB.length() > 200
                || tooLong(request.getSongAArtist())
                || tooLong(request.getSongBArtist())) {
            return error(HttpStatus.BAD_REQUEST,
                    "Song titles and artists must be 200 characters or fewer.");
        }

        if (titleA.equalsIgnoreCase(titleB)) {
            return error(HttpStatus.BAD_REQUEST,
                    "Choose two different song titles.");
        }

        // Close any previous active round in this room.
        roundRepository.closeActiveRounds(roomId);

        SoundclashRound round = new SoundclashRound();
        round.setRoomId(roomId);
        round.setSongATitle(titleA);
        round.setSongAArtist(clean(request.getSongAArtist()));
        round.setSongBTitle(titleB);
        round.setSongBArtist(clean(request.getSongBArtist()));
        round.setCreatedBy(authentication.getName());
        round.setActive(true);

        SoundclashRound saved = roundRepository.save(round);

        Map<String, Object> result = buildResult(saved);

        broadcastResult(roomId, result);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // GET CURRENT ACTIVE ROUND AND RESULTS
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<?> getCurrentRound(
            @PathVariable Integer roomId,
            Authentication authentication) {

        if (authentication == null) {
            return error(HttpStatus.UNAUTHORIZED, "Please login first.");
        }

        if (!roomRepository.existsById(roomId)) {
            return error(HttpStatus.NOT_FOUND, "Room not found.");
        }

        if (!isMember(authentication.getName(), roomId)) {
            return error(HttpStatus.FORBIDDEN,
                    "Join the room to view Soundclash.");
        }

        return roundRepository
                .findTopByRoomIdAndActiveTrueOrderByIdDesc(roomId)
                .<ResponseEntity<?>>map(
                        round -> ResponseEntity.ok(buildResult(round)))
                .orElseGet(() -> ResponseEntity.ok(
                        Map.of(
                                "active", false,
                                "message", "No active Soundclash round.")));
    }

    // VOTE FOR SONG A OR SONG B
    @PostMapping("/rooms/{roomId}/vote")
    @Transactional
    public ResponseEntity<?> castVote(
            @PathVariable Integer roomId,
            @RequestBody VoteRequest request,
            Authentication authentication) {

        if (authentication == null) {
            return error(HttpStatus.UNAUTHORIZED, "Please login first.");
        }

        if (!roomRepository.existsById(roomId)) {
            return error(HttpStatus.NOT_FOUND, "Room not found.");
        }

        String userName = authentication.getName();

        if (!isMember(userName, roomId)) {
            return error(HttpStatus.FORBIDDEN,
                    "Join the room before voting.");
        }

        if (request == null
                || request.getRoundId() == null
                || request.getSongChoice() == null) {
            return error(HttpStatus.BAD_REQUEST,
                    "Round ID and song choice are required.");
        }

        String choice = request.getSongChoice().trim().toUpperCase();

        if (!choice.equals("A") && !choice.equals("B")) {
            return error(HttpStatus.BAD_REQUEST,
                    "Song choice must be A or B.");
        }

        SoundclashRound round = roundRepository
                .findById(request.getRoundId())
                .orElse(null);

        if (round == null
                || !round.getRoomId().equals(roomId)
                || !round.isActive()) {
            return error(HttpStatus.BAD_REQUEST,
                    "This Soundclash round is not active.");
        }

        // Prevent a user from voting more than once per round.
        if (voteRepository.existsByRoundIdAndUserName(
                round.getId(), userName)) {
            return error(HttpStatus.CONFLICT,
                    "You have already voted in this round.");
        }

        SoundclashVote vote = new SoundclashVote();
        vote.setRoundId(round.getId());
        vote.setUserName(userName);
        vote.setSongChoice(choice);

        try {
            voteRepository.saveAndFlush(vote);
        } catch (DataIntegrityViolationException ex) {
            return error(
                    HttpStatus.CONFLICT,
                    "You have already voted in this round.");
        }

        Map<String, Object> result = buildResult(round);

        // Send updated vote counts to all subscribers.
        broadcastResult(roomId, result);

        return ResponseEntity.ok(result);
    }

    // BUILD VOTE COUNTS AND WINNER STATUS
    private Map<String, Object> buildResult(SoundclashRound round) {

        long votesA = voteRepository.countByRoundIdAndSongChoice(
                round.getId(), "A");

        long votesB = voteRepository.countByRoundIdAndSongChoice(
                round.getId(), "B");

        long totalVotes = votesA + votesB;

        String winner;

        if (totalVotes == 0) {
            winner = "PENDING";
        } else if (votesA > votesB) {
            winner = "A";
        } else if (votesB > votesA) {
            winner = "B";
        } else {
            winner = "TIE";
        }

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("roundId", round.getId());
        result.put("roomId", round.getRoomId());
        result.put("songATitle", round.getSongATitle());
        result.put("songAArtist", round.getSongAArtist());
        result.put("songBTitle", round.getSongBTitle());
        result.put("songBArtist", round.getSongBArtist());
        result.put("votesA", votesA);
        result.put("votesB", votesB);
        result.put("totalVotes", totalVotes);
        result.put("winner", winner);
        result.put("active", round.isActive());

        return result;
    }

    // BROADCAST RESULTS USING WEBSOCKET

    private void broadcastResult(
            Integer roomId,
            Map<String, Object> result) {

        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomId + "/soundclash",
                (Object) result);
    }

    private boolean isMember(String userName, Integer roomId) {
        return membershipRepository
                .existsByUserNameAndRoomId(userName, roomId);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private boolean tooLong(String value) {
        return value != null && value.length() > 200;
    }

    private String clean(String value) {
        return blank(value) ? null : value.trim();
    }

    private ResponseEntity<Map<String, String>> error(
            HttpStatus status, String message) {

        return ResponseEntity.status(status)
                .body(Map.of("message", message));
    }

    // REQUEST DTO FOR CREATING A ROUND
    public static class CreateRoundRequest {

        private String songATitle;
        private String songAArtist;
        private String songBTitle;
        private String songBArtist;

        public String getSongATitle() {
            return songATitle;
        }

        public void setSongATitle(String value) {
            songATitle = value;
        }

        public String getSongAArtist() {
            return songAArtist;
        }

        public void setSongAArtist(String value) {
            songAArtist = value;
        }

        public String getSongBTitle() {
            return songBTitle;
        }

        public void setSongBTitle(String value) {
            songBTitle = value;
        }

        public String getSongBArtist() {
            return songBArtist;
        }

        public void setSongBArtist(String value) {
            songBArtist = value;
        }
    }

    // REQUEST DTO FOR VOTING
    public static class VoteRequest {

        private Long roundId;
        private String songChoice;

        public Long getRoundId() {
            return roundId;
        }

        public void setRoundId(Long value) {
            roundId = value;
        }

        public String getSongChoice() {
            return songChoice;
        }

        public void setSongChoice(String value) {
            songChoice = value;
        }
    }
}
