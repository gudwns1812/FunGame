package com.fungame.songquiz.domain.room;

import java.util.Map;

public record MemberLocations(Map<Long, MemberLocation> locationsByMember) {

    public MemberLocation of(Long memberId) {
        return locationsByMember.getOrDefault(memberId, MemberLocation.lobby());
    }
}
