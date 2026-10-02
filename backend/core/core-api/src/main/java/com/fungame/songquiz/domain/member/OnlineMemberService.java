package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.domain.room.MemberLocations;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OnlineMemberService {

    private final MemberConnectionTracker memberConnectionTracker;
    private final MemberProfiles memberProfiles;
    private final GameRoomService gameRoomService;

    public OnlineMembers findAllOnline() {
        Set<Long> onlineMemberIds = memberConnectionTracker.onlineMemberIds();
        MemberLocations locations = gameRoomService.findEveryLocation();

        return new OnlineMembers(memberProfiles.allOf(onlineMemberIds).values().stream()
                .sorted(Comparator.comparing(MemberProfile::nickname))
                .map(profile -> OnlineMemberInfo.of(profile, locations.of(profile.memberId())))
                .toList());
    }

    public List<OnlineMemberInfo> findOthersOnline(Long viewerMemberId) {
        return findAllOnline().excluding(viewerMemberId);
    }
}
