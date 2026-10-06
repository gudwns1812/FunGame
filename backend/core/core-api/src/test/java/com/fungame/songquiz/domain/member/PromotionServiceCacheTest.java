package com.fungame.songquiz.domain.member;

import com.fungame.songquiz.enums.Role;
import com.fungame.songquiz.support.MemberFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("권한이 바뀌면 프로필 캐시도 같이 바뀐다")
class PromotionServiceCacheTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long REQUEST_ID = 9L;

    private final PromotionRequestReader promotionRequestReader = mock(PromotionRequestReader.class);
    private final PromotionRequestWriter promotionRequestWriter = mock(PromotionRequestWriter.class);
    private final MemberReader memberReader = mock(MemberReader.class);
    private final MemberWriter memberWriter = mock(MemberWriter.class);
    private final CacheManager cacheManager = new ConcurrentMapCacheManager(MemberProfileCache.CACHE_NAME);
    private final MemberProfileCache memberProfileCache = new MemberProfileCache(memberReader, cacheManager);
    private final PromotionService promotionService = new PromotionService(
            promotionRequestReader, promotionRequestWriter, memberReader, memberWriter, memberProfileCache);

    @Test
    @DisplayName("승급을 승인하면 캐시에 남아 있던 옛 역할이 새 역할로 바뀐다.")
    void refreshProfileOnApprove() {
        Member member = MemberFixture.withId(MEMBER_ID, "반달");
        given(memberReader.findMember(MEMBER_ID)).willReturn(member);
        given(memberReader.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(promotionRequestReader.findById(REQUEST_ID)).willReturn(Optional.of(
                PromotionRequest.open(member.getInfo())));

        // 승인 전에 누군가 프로필을 읽어 캐시에 USER 가 올라와 있다
        assertThat(memberProfileCache.of(MEMBER_ID).role()).isEqualTo(Role.USER);

        promotionService.approveRequest(REQUEST_ID);

        assertThat(memberProfileCache.of(MEMBER_ID).role()).isEqualTo(Role.ADMIN);
    }
}
