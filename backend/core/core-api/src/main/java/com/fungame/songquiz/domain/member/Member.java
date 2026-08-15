package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.enums.PlayerStatus;
import com.fungame.songquiz.enums.Role;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.Assert;

@Getter
public class Member {

    private final Long id;
    private final String loginId;
    private final String email;
    private String password;
    private String nickname;
    private Role role;
    private PlayerStatus status;
    private Long currentRoomId;

    private Member(Long id, String loginId, String password, String nickname, String email, Role role,
                   PlayerStatus status, Long currentRoomId) {
        this.id = id;
        this.loginId = loginId;
        this.password = password;
        this.nickname = nickname;
        this.email = email;
        this.role = role;
        this.status = status;
        this.currentRoomId = currentRoomId;
    }

    @Builder
    private static Member signUp(String loginId, String password, String nickname, String email, Role role) {
        Assert.hasText(loginId, "로그인 아이디는 필수입니다.");
        Assert.hasText(password, "비밀번호는 필수입니다.");
        Assert.hasText(nickname, "닉네임은 필수입니다.");
        Assert.hasText(email, "이메일은 필수입니다.");
        Assert.notNull(role, "역할은 필수입니다.");

        return new Member(null, loginId, password, nickname, email, role, PlayerStatus.LOBBY, null);
    }

    public static Member restore(Long id, String loginId, String password, String nickname, String email, Role role,
                                 PlayerStatus status, Long currentRoomId) {
        return new Member(id, loginId, password, nickname, email, role, status, currentRoomId);
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
