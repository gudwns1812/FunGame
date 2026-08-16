package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.enums.PlayerStatus;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberPresenceService {

    private final MemberReader memberReader;
    private final MemberWriter memberWriter;
    private final ApplicationEventPublisher applicationEventPublisher;

    public void enterWaitingRoom(Long memberId, Long roomId) {
        memberWriter.moveToWaitingRoom(memberId, roomId);

        announcePresenceChange();
    }

    public void enterPlayingRoom(Long memberId, Long roomId) {
        memberWriter.moveToPlayingRoom(memberId, roomId);

        announcePresenceChange();
    }

    public void leaveRoom(Long memberId) {
        memberWriter.moveToLobby(memberId);

        announcePresenceChange();
    }

    public void markRoomPlaying(Long roomId) {
        memberWriter.movePresenceOfRoom(roomId, PlayerStatus.PLAYING);
        announcePresenceChange();
    }

    public void markRoomWaiting(Long roomId) {
        memberWriter.movePresenceOfRoom(roomId, PlayerStatus.WAITING);
        announcePresenceChange();
    }

    public void clearEveryLocation() {
        int cleared = memberWriter.clearEveryLocation();

        if (cleared > 0) {
            log.info("기동 시점에 남아 있던 회원 위치 {} 건을 로비로 되돌린다", cleared);
        }
    }

    public List<Member> findAllIn(Collection<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return List.of();
        }

        return memberReader.findAllInOrderByNickname(memberIds);
    }

    public Member findMember(Long memberId) {
        return memberReader.findById(memberId)
                .orElseThrow(() -> new CoreException(ErrorType.MEMBER_NOT_FOUND));
    }

    private void announcePresenceChange() {
        applicationEventPublisher.publishEvent(new MemberPresenceChangedEvent());
    }
}
