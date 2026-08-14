package com.fungame.songquiz.storage;

import com.fungame.songquiz.enums.PlayerStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberStore {

    private final MemberRepository memberRepository;

    public Optional<MemberEntity> findById(Long id) {
        return memberRepository.findById(id);
    }

    public List<MemberEntity> findAllById(Collection<Long> ids) {
        return memberRepository.findAllById(ids);
    }

    public MemberEntity getReferenceById(Long id) {
        return memberRepository.getReferenceById(id);
    }

    public MemberEntity save(MemberEntity entity) {
        return memberRepository.save(entity);
    }

    public Optional<MemberEntity> findByLoginId(String loginId) {
        return memberRepository.findByLoginId(loginId);
    }

    public Optional<MemberEntity> findByLoginIdAndEmail(String loginId, String email) {
        return memberRepository.findByLoginIdAndEmail(loginId, email);
    }

    public Optional<MemberEntity> findByIdForUpdate(Long id) {
        return memberRepository.findByIdForUpdate(id);
    }

    public List<MemberEntity> findAllByIdInOrderByNicknameAsc(Collection<Long> ids) {
        return memberRepository.findAllByIdInOrderByNicknameAsc(ids);
    }

    public void updateStatusOfRoom(Long roomId, PlayerStatus status) {
        memberRepository.updateStatusOfRoom(roomId, status);
    }

    public int clearEveryLocation() {
        return memberRepository.clearEveryLocation();
    }

    public boolean existsByLoginId(String loginId) {
        return memberRepository.existsByLoginId(loginId);
    }

    public boolean existsByNickname(String nickname) {
        return memberRepository.existsByNickname(nickname);
    }

    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }
}
