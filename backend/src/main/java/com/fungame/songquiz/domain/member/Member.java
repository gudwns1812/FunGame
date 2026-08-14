package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.enums.PlayerStatus;
import com.fungame.songquiz.enums.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.Assert;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50, name = "login_id")
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 100)
    private String nickname;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlayerStatus status;

    @Column(name = "current_room_id")
    private Long currentRoomId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Member(String loginId, String password, String nickname, String email, Role role) {
        Assert.hasText(loginId, "로그인 아이디는 필수입니다.");
        Assert.hasText(password, "비밀번호는 필수입니다.");
        Assert.hasText(nickname, "닉네임은 필수입니다.");
        Assert.hasText(email, "이메일은 필수입니다.");
        Assert.notNull(role, "역할은 필수입니다.");

        this.loginId = loginId;
        this.password = password;
        this.nickname = nickname;
        this.email = email;
        this.role = role;
        this.status = PlayerStatus.LOBBY;
    }

    public void enterWaitingRoom(Long roomId) {
        Assert.notNull(roomId, "방 번호는 필수입니다.");
        this.status = PlayerStatus.WAITING;
        this.currentRoomId = roomId;
    }

    public void enterPlayingRoom(Long roomId) {
        Assert.notNull(roomId, "방 번호는 필수입니다.");
        this.status = PlayerStatus.PLAYING;
        this.currentRoomId = roomId;
    }

    public void leaveRoom() {
        this.status = PlayerStatus.LOBBY;
        this.currentRoomId = null;
    }

    public boolean isInLobby() {
        return currentRoomId == null;
    }

    public boolean isWaitingIn(Long roomId) {
        return status == PlayerStatus.WAITING && roomId.equals(currentRoomId);
    }

    public void changeNickname(String newNickname) {
        Assert.hasText(newNickname, "새 닉네임은 비어있을 수 없습니다.");
        this.nickname = newNickname;
    }

    public void changePassword(String newPassword) {
        Assert.hasText(newPassword, "새 비밀번호는 비어있을 수 없습니다.");
        this.password = newPassword;
    }

    public void updateRole(Role role) {
        Assert.notNull(role, "역할은 필수입니다.");
        this.role = role;
    }
}
