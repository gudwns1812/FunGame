package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.enums.PlayerStatus;
import com.fungame.songquiz.storage.MemberEntity;
import com.fungame.songquiz.storage.MemberRepository;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MemberWriter {

    private final MemberRepository memberRepository;

    @Transactional
    public Long append(Member member) {
        return memberRepository.save(MemberEntity.builder()
                        .loginId(member.getLoginId())
                        .password(member.getPassword())
                        .nickname(member.getNickname())
                        .email(member.getEmail())
                        .role(member.getRole())
                        .status(member.getStatus())
                        .currentRoomId(member.getCurrentRoomId())
                        .build())
                .getId();
    }

    @Transactional
    public void update(Member member) {
        MemberEntity entity = memberRepository.findById(member.getId())
                .orElseThrow(() -> new CoreException(ErrorType.MEMBER_NOT_FOUND));

        entity.changeNickname(member.getNickname());
        entity.changePassword(member.getPassword());
        entity.changeRole(member.getRole());
        entity.changePresence(member.getStatus(), member.getCurrentRoomId());
    }

    @Transactional
    public void movePresenceOfRoom(Long roomId, PlayerStatus status) {
        memberRepository.updateStatusOfRoom(roomId, status);
    }

    @Transactional
    public int clearEveryLocation() {
        return memberRepository.clearEveryLocation();
    }
}
