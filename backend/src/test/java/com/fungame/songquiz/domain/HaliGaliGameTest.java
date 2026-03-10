package com.fungame.songquiz.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HaliGaliGameTest {

    private HaliGaliGame game;
    private List<String> players;

    @BeforeEach
    void setUp() {
        game = new HaliGaliGame();
        players = List.of("player1", "player2", "player3", "player4");
        game.setPlayers(players);
    }

    @Test
    @DisplayName("초기 카드 분배 시 각 플레이어는 14장의 카드를 가져야 한다")
    void should_distribute_cards_evenly() {
        // given & when
        // setUp에서 수행

        // then
        // HaliGaliGame의 내부 상태를 확인하는 메서드가 필요함 (ex: getPlayerDeckSize)
        assertThat(game.getPlayerDeckSize("player1")).isEqualTo(14);
        assertThat(game.getPlayerDeckSize("player2")).isEqualTo(14);
        assertThat(game.getPlayerDeckSize("player3")).isEqualTo(14);
        assertThat(game.getPlayerDeckSize("player4")).isEqualTo(14);
    }

    @Test
    @DisplayName("카드를 뒤집으면 현재 플레이어의 덱에서 카드가 줄어들고 바닥에 카드가 쌓여야 한다")
    void should_flip_card_correctly() {
        // given
        String currentPlayer = game.getCurrentPlayer();
        int initialDeckSize = game.getPlayerDeckSize(currentPlayer);

        // when
        game.handleAction(new GameAction(currentPlayer, ActionType.FLIP_CARD, null));

        // then
        assertThat(game.getPlayerDeckSize(currentPlayer)).isEqualTo(initialDeckSize - 1);
        assertThat(game.getOpenCardCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("바닥 과일 합이 5일 때 종을 울리면 모든 바닥 카드를 가져간다")
    void should_take_all_cards_when_bell_rings_at_five() {
        // given
        // 합이 5가 되도록 강제로 카드 설정 (테스트를 위한 모킹/상태 주입 필요)
        game.forceSetOpenCard("player1", "STRAWBERRY", 3);
        game.forceSetOpenCard("player2", "STRAWBERRY", 2);
        
        int initialDeckSize = game.getPlayerDeckSize("player3");
        int expectedGain = 2; // 바닥에 있는 카드 수

        // when
        ActionResult result = game.handleAction(new GameAction("player3", ActionType.PRESS_BELL, null));

        // then
        assertThat(result).isEqualTo(ActionResult.CORRECT);
        assertThat(game.getPlayerDeckSize("player3")).isEqualTo(initialDeckSize + expectedGain);
        assertThat(game.getOpenCardCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("바닥 과일 합이 5가 아닐 때 종을 울리면 벌칙으로 다른 플레이어에게 카드를 한 장씩 준다")
    void should_give_penalty_when_bell_rings_incorrectly() {
        // given
        game.forceSetOpenCard("player1", "STRAWBERRY", 3);
        game.forceSetOpenCard("player2", "STRAWBERRY", 1); // 합이 4
        
        int initialDeckSize = game.getPlayerDeckSize("player3");
        int otherPlayer1Size = game.getPlayerDeckSize("player1");

        // when
        ActionResult result = game.handleAction(new GameAction("player3", ActionType.PRESS_BELL, null));

        // then
        assertThat(result).isEqualTo(ActionResult.WRONG);
        assertThat(game.getPlayerDeckSize("player3")).isEqualTo(initialDeckSize - 3); // 3명에게 줌
        assertThat(game.getPlayerDeckSize("player1")).isEqualTo(otherPlayer1Size + 1);
    }
}
