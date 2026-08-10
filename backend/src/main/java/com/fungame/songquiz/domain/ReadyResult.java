package com.fungame.songquiz.domain;

/**
 * 준비 상태 토글의 결과.
 *
 * @param ready      토글 후 이 플레이어의 준비 상태
 * @param isAllReady 토글 후 방의 모든 인원이 준비되었는지 여부
 */
public record ReadyResult(boolean ready, boolean isAllReady) {
}
