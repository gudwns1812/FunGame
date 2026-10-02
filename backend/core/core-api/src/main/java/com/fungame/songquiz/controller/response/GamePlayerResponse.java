package com.fungame.songquiz.controller.response;

import com.fungame.songquiz.domain.member.MemberProfile;
import com.fungame.songquiz.domain.member.MemberProfiles;
import com.fungame.songquiz.domain.room.GamePlayer;

import java.util.List;
import java.util.Map;

public record GamePlayerResponse(
        Long memberId,
        String nickname,
        boolean isReady
) {

    public static GamePlayerResponse from(GamePlayer player, MemberProfiles profiles) {
        return new GamePlayerResponse(player.memberId(), profiles.of(player.memberId()).nickname(), player.isReady());
    }

    public static List<GamePlayerResponse> listFrom(List<GamePlayer> players, MemberProfiles profiles) {
        Map<Long, MemberProfile> found = profiles.allOf(players.stream().map(GamePlayer::memberId).toList());

        return players.stream()
                .map(player -> new GamePlayerResponse(
                        player.memberId(), nicknameOf(found, player.memberId()), player.isReady()))
                .toList();
    }

    private static String nicknameOf(Map<Long, MemberProfile> found, Long memberId) {
        MemberProfile profile = found.get(memberId);
        return profile == null ? null : profile.nickname();
    }
}
