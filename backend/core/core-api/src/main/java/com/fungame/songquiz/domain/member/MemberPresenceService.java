package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.domain.event.MemberPresenceChangedEvent;
import com.fungame.songquiz.support.error.CoreException;
import com.fungame.songquiz.support.error.ErrorType;
import com.fungame.songquiz.enums.PlayerStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberPresenceService {

    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public void enterWaitingRoom(Long memberId, Long roomId) {
        loadMember(memberId).enterWaitingRoom(roomId);
        announcePresenceChange();
    }

    @Transactional
    public void enterPlayingRoom(Long memberId, Long roomId) {
        loadMember(memberId).enterPlayingRoom(roomId);
        announcePresenceChange();
    }

    @Transactional
    public void leaveRoom(Long memberId) {
        loadMember(memberId).leaveRoom();
        announcePresenceChange();
    }

    @Transactional
    public void markRoomPlaying(Long roomId) {
        memberRepository.updateStatusOfRoom(roomId, PlayerStatus.PLAYING);
        announcePresenceChange();
    }

    @Transactional
    public void markRoomWaiting(Long roomId) {
        memberRepository.updateStatusOfRoom(roomId, PlayerStatus.WAITING);
        announcePresenceChange();
    }

    @Transactional
    public void clearEveryLocation() {
        int cleared = memberRepository.clearEveryLocation();

        if (cleared > 0) {
            log.info("기동 시점에 남아 있던 회원 위치 {} 건을 로비로 되돌린다", cleared);
        }
    }

    @Transactional(readOnly = true)
    public List<Member> findAllIn(Collection<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return List.of();
        }

        return memberRepository.findAllByIdInOrderByNicknameAsc(memberIds);
    }

    @Transactional(readOnly = true)
    public Member findMember(Long memberId) {
        return loadMember(memberId);
    }

    private void announcePresenceChange() {
        applicationEventPublisher.publishEvent(new MemberPresenceChangedEvent());
    }

    private Member loadMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CoreException(ErrorType.MEMBER_NOT_FOUND));
    }
}
