package dj_platform;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_memberships", uniqueConstraints = @UniqueConstraint(name = "unique_user_room", columnNames = {
        "user_name", "room_id" }))
public class RoomMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @Column(name = "room_id", nullable = false)
    private Integer roomId;

    @Column(name = "joined_at", insertable = false, updatable = false)
    private LocalDateTime joinedAt;

    public RoomMembership() {
    }

    public RoomMembership(String userName, Integer roomId) {
        this.userName = userName;
        this.roomId = roomId;
    }

    public Long getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}