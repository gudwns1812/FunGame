package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.enums.Role;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.Assert;

@Getter
public class Member {

    private MemberInfo info;
    private String password;

    private Member(MemberInfo info, String password) {
        this.info = info;
        this.password = password;
    }

    @Builder
    private static Member signUp(String loginId, String password, String nickname, String email, Role role) {
        Assert.hasText(loginId, "로그인 아이디는 필수입니다.");
        Assert.hasText(password, "비밀번호는 필수입니다.");
        Assert.hasText(nickname, "닉네임은 필수입니다.");
        Assert.hasText(email, "이메일은 필수입니다.");
        Assert.notNull(role, "역할은 필수입니다.");

        return new Member(new MemberInfo(null, loginId, nickname, email, role), password);
    }

    public static Member restore(Long id, String loginId, String password, String nickname, String email, Role role) {
        return new Member(new MemberInfo(id, loginId, nickname, email, role), password);
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
