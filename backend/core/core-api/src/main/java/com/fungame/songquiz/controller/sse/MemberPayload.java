package com.fungame.songquiz.controller.sse;

@FunctionalInterface
public interface MemberPayload {

    Object of(Long memberId);
}
