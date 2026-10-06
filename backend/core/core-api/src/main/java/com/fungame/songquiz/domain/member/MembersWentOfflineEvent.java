package com.fungame.songquiz.domain.member;

import java.util.Set;

public record MembersWentOfflineEvent(Set<Long> memberIds) {
}
