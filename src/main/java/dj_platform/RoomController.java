
package dj_platform;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoomController {

    private final RoomRepository roomRepository;
    private final RoomMembershipRepository membershipRepository;

    public RoomController(
            RoomRepository roomRepository,
            RoomMembershipRepository membershipRepository) {
        this.roomRepository = roomRepository;
        this.membershipRepository = membershipRepository;
    }

    @GetMapping("/api/rooms")
    public List<Map<String, Object>> getRooms() {

        return roomRepository.findAll()
                .stream()
                .map(room -> Map.<String, Object>of(
                        "id", room.getId(),
                        "name", room.getName(),
                        "genre", room.getGenre(),
                        "listeners",
                        membershipRepository.countByRoomId(room.getId())))
                .toList();
    }
}
