package com.fungame.songquiz.domain;

/**
 * 방 참가 시도의 결과.
 *
 * @param playerNumber 참가 후 방의 인원 수
 * @param newlyJoined  이번 호출로 실제 방에 추가되었는지 여부.
 *                     false 면 이미 방에 있던 사람의 재참가이므로 입장 알림을 보내면 안 된다.
 */
public record JoinResult(int playerNumber, boolean newlyJoined) {
}
