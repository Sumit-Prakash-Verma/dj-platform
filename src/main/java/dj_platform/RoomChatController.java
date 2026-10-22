
package dj_platform;

import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat/rooms")
public class RoomChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final RoomMembershipRepository membershipRepository;
    private final RoomRepository roomRepository;
    private final EntityManager entityManager;

    public RoomChatController(
            ChatMessageRepository chatMessageRepository,
            RoomMembershipRepository membershipRepository,
            RoomRepository roomRepository,
            EntityManager entityManager) {

        this.chatMessageRepository = chatMessageRepository;
        this.membershipRepository = membershipRepository;
        this.roomRepository = roomRepository;
        this.entityManager = entityManager;
    }

    // GET: Room messages
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<?> getMessages(
            @PathVariable Integer roomId,
            Authentication authentication) {

        ResponseEntity<?> accessError = checkRoomAccess(roomId, authentication);

        if (accessError != null) {
            return accessError;
        }

        List<ChatMessage> messages = chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(
                roomId.longValue());

        return ResponseEntity.ok(messages);
    }

    // POST: Send a room message
    @PostMapping("/{roomId}/messages")
    @Transactional
    public ResponseEntity<?> sendMessage(
            @PathVariable Integer roomId,
            @RequestBody ChatRequest request,
            Authentication authentication) {

        ResponseEntity<?> accessError = checkRoomAccess(roomId, authentication);

        if (accessError != null) {
            return accessError;
        }

        if (request == null
                || request.getMessage() == null
                || request.getMessage().isBlank()) {

            return ResponseEntity.badRequest().body(
                    Map.of("message", "Message cannot be empty."));
        }

        String message = request.getMessage().trim();

        if (message.length() > 2000) {
            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "Message cannot exceed 2000 characters."));
        }

        // Username authenticated session se liya jayega.
        String username = authentication.getName();

        ChatMessage chat = new ChatMessage();
        chat.setRoomId(roomId.longValue());
        chat.setUsername(username);
        chat.setMessage(message);

        // Message database mein save karein.
        ChatMessage saved = chatMessageRepository.saveAndFlush(chat);

        // Database-generated created_at value reload karein.
        entityManager.refresh(saved);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(saved);
    }

    // Login, room aur membership checks
    private ResponseEntity<?> checkRoomAccess(
            Integer roomId,
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message",
                            "Please log in to use room chat."));
        }

        if (!roomRepository.existsById(roomId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Room not found."));
        }

        String username = authentication.getName();

        boolean isMember = membershipRepository.existsByUserNameAndRoomId(
                username, roomId);

        if (!isMember) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message",
                            "Join this room before using its chat."));
        }

        return null;
    }

    // Request body mein sirf message hoga.
    public static class ChatRequest {

        private String message;

        public ChatRequest() {
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
