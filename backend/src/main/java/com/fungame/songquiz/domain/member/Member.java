package com.fungame.songquiz.domain.member;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Member(String loginId, String password, String nickname, Role role) {
        Assert.hasText(loginId, "로그인 아이디는 필수입니다.");
        Assert.hasText(password, "비밀번호는 필수입니다.");
        Assert.hasText(nickname, "닉네임은 필수입니다.");
        Assert.notNull(role, "역할은 필수입니다.");

        this.loginId = loginId;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
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
