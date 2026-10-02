package com.fungame.songquiz.domain.member;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MemberProfiles {

    public static final String CACHE_NAME = "memberProfiles";

    private final MemberReader memberReader;
    private final CacheManager cacheManager;

    public MemberProfile of(Long memberId) {
        MemberProfile cached = cache().get(memberId, MemberProfile.class);
        if (cached != null) {
            return cached;
        }

        return put(memberReader.findMember(memberId));
    }

    public Map<Long, MemberProfile> allOf(Collection<Long> memberIds) {
        Map<Long, MemberProfile> found = new HashMap<>();
        Set<Long> missing = new LinkedHashSet<>();

        for (Long memberId : memberIds) {
            MemberProfile cached = cache().get(memberId, MemberProfile.class);
            if (cached == null) {
                missing.add(memberId);
                continue;
            }
            found.put(memberId, cached);
        }

        if (missing.isEmpty()) {
            return Map.copyOf(found);
        }

        List<Member> members = memberReader.findAllInOrderByNickname(missing);
        members.forEach(member -> found.put(member.getId(), put(member)));

        return Map.copyOf(found);
    }

    public MemberProfile refresh(Member member) {
        return put(member);
    }

    public void forget(Long memberId) {
        cache().evict(memberId);
    }

    private MemberProfile put(Member member) {
        MemberProfile profile = MemberProfile.from(member);
        cache().put(member.getId(), profile);

        return profile;
    }

    private Cache cache() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            throw new IllegalStateException("'%s' 캐시가 없다. CacheConfig 를 확인한다.".formatted(CACHE_NAME));
        }

        return cache;
    }
}
