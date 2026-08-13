package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.domain.dto.OnlineMemberInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OnlineMemberService {

    private final MemberConnectionTracker memberConnectionTracker;
    private final MemberPresenceService memberPresenceService;

    public List<OnlineMemberInfo> findOthersOnline(Long viewerMemberId) {
        Set<Long> otherMemberIds = new HashSet<>(memberConnectionTracker.onlineMemberIds());
        otherMemberIds.remove(viewerMemberId);

        return memberPresenceService.findAllIn(otherMemberIds).stream()
                .map(OnlineMemberInfo::from)
                .toList();
    }
}
