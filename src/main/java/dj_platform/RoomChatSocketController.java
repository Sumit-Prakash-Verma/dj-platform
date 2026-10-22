
package dj_platform;

import java.security.Principal;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class RoomChatSocketController {

        private final ChatMessageRepository chatMessageRepository;
        private final RoomMembershipRepository membershipRepository;
        private final RoomRepository roomRepository;
        private final SimpMessagingTemplate messagingTemplate;
        private final EntityManager entityManager;
        private final SentimentAnalyzer sentimentAnalyzer;

        public RoomChatSocketController(
                        ChatMessageRepository chatMessageRepository,
                        RoomMembershipRepository membershipRepository,
                        RoomRepository roomRepository,
                        SimpMessagingTemplate messagingTemplate,
                        EntityManager entityManager,
                        SentimentAnalyzer sentimentAnalyzer) {

                this.chatMessageRepository = chatMessageRepository;
                this.membershipRepository = membershipRepository;
                this.roomRepository = roomRepository;
                this.messagingTemplate = messagingTemplate;
                this.entityManager = entityManager;
                this.sentimentAnalyzer = sentimentAnalyzer;
        }

        @MessageMapping("/rooms/{roomId}/send")
        @Transactional
        public void sendMessage(
                        @DestinationVariable Integer roomId,
                        @Payload RoomChatController.ChatRequest request,
                        Principal principal) {

                if (principal == null) {
                        throw new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "Please log in to use room chat.");
                }

                if (!roomRepository.existsById(roomId)) {
                        throw new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Room not found.");
                }

                String username = principal.getName();

                boolean isMember = membershipRepository.existsByUserNameAndRoomId(
                                username, roomId);

                if (!isMember) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "Join this room before sending messages.");
                }

                if (request == null
                                || request.getMessage() == null
                                || request.getMessage().isBlank()) {

                        throw new IllegalArgumentException(
                                        "Message cannot be empty.");
                }

                String message = request.getMessage().trim();

                if (message.length() > 2000) {
                        throw new IllegalArgumentException(
                                        "Message cannot exceed 2000 characters.");
                }

                ChatMessage chat = new ChatMessage();
                chat.setRoomId(roomId.longValue());
                chat.setUsername(username);
                chat.setMessage(message);

                // Analyse the sentiment before saving.
                String sentiment = sentimentAnalyzer.analyze(message);
                chat.setSentiment(sentiment);

                ChatMessage saved = chatMessageRepository.saveAndFlush(chat);

                // Load database-generated fields such as createdAt.
                entityManager.refresh(saved);

                // Broadcast the saved message, including its sentiment.
                messagingTemplate.convertAndSend(
                                "/topic/rooms/" + roomId,
                                saved);
        }
}
