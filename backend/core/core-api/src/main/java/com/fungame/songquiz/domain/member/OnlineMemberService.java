package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.domain.dto.OnlineMemberInfo;
import com.fungame.songquiz.domain.dto.OnlineMembers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OnlineMemberService {

    private final MemberConnectionTracker memberConnectionTracker;
    private final MemberPresenceService memberPresenceService;

    public OnlineMembers findAllOnline() {
        Set<Long> onlineMemberIds = memberConnectionTracker.onlineMemberIds();

        return new OnlineMembers(memberPresenceService.findAllIn(onlineMemberIds).stream()
                .map(OnlineMemberInfo::from)
                .toList());
    }

    public List<OnlineMemberInfo> findOthersOnline(Long viewerMemberId) {
        return findAllOnline().excluding(viewerMemberId);
    }
}
