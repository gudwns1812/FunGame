package com.fungame.songquiz.domain;

/**
 * 방 이탈 처리의 결과.
 *
 * @param destroyed  마지막 인원이 나가서 방까지 정리되었는지 여부.
 *                   true 면 방이 없으므로 이탈 알림을 보낼 대상도 없다.
 * @param wasPlaying 이탈 시점에 게임이 진행 중이었는지 여부
 */
public record LeaveResult(boolean destroyed, boolean wasPlaying) {
}
