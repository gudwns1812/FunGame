package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.storage.MemberEntity;
import com.fungame.songquiz.storage.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MemberReader {

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public Optional<Member> findById(Long memberId) {
        return memberRepository.findById(memberId).map(MemberReader::toMember);
    }

    @Transactional(readOnly = true)
    public Optional<Member> findByLoginId(String loginId) {
        return memberRepository.findByLoginId(loginId).map(MemberReader::toMember);
    }

    @Transactional(readOnly = true)
    public Optional<Member> findByLoginIdAndEmail(String loginId, String email) {
        return memberRepository.findByLoginIdAndEmail(loginId, email).map(MemberReader::toMember);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Member> findByIdForUpdate(Long memberId) {
        return memberRepository.findByIdForUpdate(memberId).map(MemberReader::toMember);
    }

    @Transactional(readOnly = true)
    public List<Member> findAllInOrderByNickname(Collection<Long> memberIds) {
        return memberRepository.findAllByIdInOrderByNicknameAsc(memberIds).stream()
                .map(MemberReader::toMember)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean existsByLoginId(String loginId) {
        return memberRepository.existsByLoginId(loginId);
    }

    @Transactional(readOnly = true)
    public boolean existsByNickname(String nickname) {
        return memberRepository.existsByNickname(nickname);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    static Member toMember(MemberEntity entity) {
        return Member.restore(
                entity.getId(),
                entity.getLoginId(),
                entity.getPassword(),
                entity.getNickname(),
                entity.getEmail(),
                entity.getRole(),
                entity.getStatus(),
                entity.getCurrentRoomId());
    }
}
