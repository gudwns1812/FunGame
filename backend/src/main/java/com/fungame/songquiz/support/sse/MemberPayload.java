package com.fungame.songquiz.support.sse;

@FunctionalInterface
public interface MemberPayload {

    Object of(Long memberId);
}
