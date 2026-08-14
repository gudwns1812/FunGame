package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.storage.MemberEntity;
import com.fungame.songquiz.storage.MemberStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MemberReader {

    private final MemberStore memberStore;

    public Optional<Member> findById(Long memberId) {
        return memberStore.findById(memberId).map(MemberReader::toMember);
    }

    public Optional<Member> findByLoginId(String loginId) {
        return memberStore.findByLoginId(loginId).map(MemberReader::toMember);
    }

    public Optional<Member> findByLoginIdAndEmail(String loginId, String email) {
        return memberStore.findByLoginIdAndEmail(loginId, email).map(MemberReader::toMember);
    }

    public Optional<Member> findByIdForUpdate(Long memberId) {
        return memberStore.findByIdForUpdate(memberId).map(MemberReader::toMember);
    }

    public List<Member> findAllInOrderByNickname(Collection<Long> memberIds) {
        return memberStore.findAllByIdInOrderByNicknameAsc(memberIds).stream()
                .map(MemberReader::toMember)
                .toList();
    }

    public boolean existsByLoginId(String loginId) {
        return memberStore.existsByLoginId(loginId);
    }

    public boolean existsByNickname(String nickname) {
        return memberStore.existsByNickname(nickname);
    }

    public boolean existsByEmail(String email) {
        return memberStore.existsByEmail(email);
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
