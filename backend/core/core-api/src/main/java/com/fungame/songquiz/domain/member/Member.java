package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.enums.PlayerStatus;
import com.fungame.songquiz.enums.Role;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.Assert;

@Getter
public class Member {

    private MemberInfo info;
    private String password;
    private PlayerStatus status;
    private Long currentRoomId;

    private Member(MemberInfo info, String password, PlayerStatus status, Long currentRoomId) {
        this.info = info;
        this.password = password;
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

        return new Member(
                new MemberInfo(null, loginId, nickname, email, role),
                password,
                PlayerStatus.LOBBY,
                null);
    }

    public static Member restore(Long id, String loginId, String password, String nickname, String email, Role role,
                                 PlayerStatus status, Long currentRoomId) {
        return new Member(new MemberInfo(id, loginId, nickname, email, role), password, status, currentRoomId);
    }

    public Long getId() {
        return info.id();
    }

    public String getLoginId() {
        return info.loginId();
    }

    public String getNickname() {
        return info.nickname();
    }

    public String getEmail() {
        return info.email();
    }

    public Role getRole() {
        return info.role();
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

    public boolean isIn(Long roomId) {
        return roomId.equals(currentRoomId);
    }

    public void changeNickname(String newNickname) {
        Assert.hasText(newNickname, "새 닉네임은 비어있을 수 없습니다.");
        this.info = info.withNickname(newNickname);
    }

    public void changePassword(String newPassword) {
        Assert.hasText(newPassword, "새 비밀번호는 비어있을 수 없습니다.");
        this.password = newPassword;
    }

    public void updateRole(Role role) {
        Assert.notNull(role, "역할은 필수입니다.");
        this.info = info.withRole(role);
    }
}
