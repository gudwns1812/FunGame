package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.domain.room.GameRoomService;
import com.fungame.songquiz.domain.room.MemberLocations;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OnlineMemberService {

    private final MemberConnectionTracker memberConnectionTracker;
    private final MemberReader memberReader;
    private final GameRoomService gameRoomService;

    public OnlineMembers findAllOnline() {
        Set<Long> onlineMemberIds = memberConnectionTracker.onlineMemberIds();
        MemberLocations locations = gameRoomService.findEveryLocation();

        return new OnlineMembers(memberReader.findAllInOrderByNickname(onlineMemberIds).stream()
                .map(member -> OnlineMemberInfo.of(member, locations.of(member.getId())))
                .toList());
    }

    public List<OnlineMemberInfo> findOthersOnline(Long viewerMemberId) {
        return findAllOnline().excluding(viewerMemberId);
    }
}
