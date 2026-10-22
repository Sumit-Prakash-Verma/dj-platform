
package dj_platform;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/memberships")
public class RoomMembershipController {

        private final RoomMembershipRepository membershipRepository;
        private final RoomRepository roomRepository;
        private final SimpMessagingTemplate messagingTemplate;

        public RoomMembershipController(
                        RoomMembershipRepository membershipRepository,
                        RoomRepository roomRepository,
                        SimpMessagingTemplate messagingTemplate) {

                this.membershipRepository = membershipRepository;
                this.roomRepository = roomRepository;
                this.messagingTemplate = messagingTemplate;
        }

        // JOIN ROOM API
        @PostMapping("/join")
        public ResponseEntity<?> joinRoom(
                        @RequestBody JoinRoomRequest request,
                        Authentication authentication) {

                String userName = authentication.getName();

                if (request.getRoomId() == null) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of("message", "Room ID is required"));
                }

                Integer roomId = request.getRoomId();

                if (!roomRepository.existsById(roomId)) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(Map.of("message", "Room not found"));
                }

                boolean alreadyJoined = membershipRepository
                                .existsByUserNameAndRoomId(userName, roomId);

                if (alreadyJoined) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(Map.of("message", "Already joined this room"));
                }

                RoomMembership membership = new RoomMembership(userName, roomId);

                RoomMembership saved = membershipRepository.save(membership);

                // Notify all subscribers about updated members
                broadcastMembersUpdate(roomId);

                return ResponseEntity.status(HttpStatus.CREATED).body(
                                Map.of(
                                                "message", "Room joined successfully",
                                                "membershipId", saved.getId(),
                                                "userName", saved.getUserName(),
                                                "roomId", saved.getRoomId()));
        }

        // LEAVE ROOM API
        @DeleteMapping("/leave")
        public ResponseEntity<?> leaveRoom(
                        @RequestBody JoinRoomRequest request,
                        Authentication authentication) {

                String userName = authentication.getName();

                if (request.getRoomId() == null) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of("message", "Room ID is required"));
                }

                Integer roomId = request.getRoomId();

                if (!roomRepository.existsById(roomId)) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(Map.of("message", "Room not found"));
                }

                long deleted = membershipRepository
                                .deleteByUserNameAndRoomId(userName, roomId);

                if (deleted == 0) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(Map.of("message", "Membership not found"));
                }

                // Notify all subscribers about updated members
                broadcastMembersUpdate(roomId);

                return ResponseEntity.ok(
                                Map.of(
                                                "message", "Room left successfully",
                                                "userName", userName,
                                                "roomId", roomId));
        }

        // GET ROOM MEMBERS API
        @GetMapping("/room/{roomId}")
        public ResponseEntity<?> getRoomMembers(
                        @PathVariable Integer roomId) {

                if (!roomRepository.existsById(roomId)) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(Map.of("message", "Room not found"));
                }

                var members = membershipRepository.findByRoomId(roomId);

                return ResponseEntity.ok(
                                members.stream()
                                                .map(member -> Map.of(
                                                                "userName", member.getUserName(),
                                                                "roomId", member.getRoomId()))
                                                .toList());
        }

        // BROADCAST UPDATED MEMBERS TO ALL CONNECTED USERS
        private void broadcastMembersUpdate(Integer roomId) {

                List<Map<String, Object>> members = membershipRepository.findByRoomId(roomId)
                                .stream()
                                .map(member -> Map.<String, Object>of(
                                                "userName", member.getUserName(),
                                                "roomId", member.getRoomId()))
                                .toList();

                Map<String, Object> event = Map.of(
                                "type", "MEMBERS_UPDATED",
                                "roomId", roomId,
                                "listeners", members.size(),
                                "members", members);

                messagingTemplate.convertAndSend(
                                "/topic/rooms/" + roomId + "/members",
                                (Object) event);
        }

        // REQUEST DTO
        public static class JoinRoomRequest {

                private Integer roomId;

                public JoinRoomRequest() {
                }

                public Integer getRoomId() {
                        return roomId;
                }

                public void setRoomId(Integer roomId) {
                        this.roomId = roomId;
                }
        }
}
