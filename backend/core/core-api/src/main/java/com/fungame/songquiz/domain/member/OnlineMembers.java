package com.fungame.songquiz.domain.member;

import java.util.List;

public record OnlineMembers(List<OnlineMemberInfo> members) {

    public List<OnlineMemberInfo> excluding(Long viewerMemberId) {
        return members.stream()
                .filter(member -> !member.memberId().equals(viewerMemberId))
                .toList();
    }
}
