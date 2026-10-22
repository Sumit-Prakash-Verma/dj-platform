package dj_platform;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface RoomMembershipRepository
                extends JpaRepository<RoomMembership, Long> {

        boolean existsByUserNameAndRoomId(
                        String userName, Integer roomId);

        long countByRoomId(Integer roomId);

        // Interface ke andar ye method add karein
        List<RoomMembership> findByRoomId(Integer roomId);

        @Transactional
        long deleteByUserNameAndRoomId(
                        String userName, Integer roomId);
}