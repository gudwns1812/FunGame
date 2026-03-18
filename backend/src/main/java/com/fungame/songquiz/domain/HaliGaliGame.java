package com.fungame.songquiz.domain;

import com.fungame.songquiz.domain.dto.GameAnswerDto;
import com.fungame.songquiz.domain.dto.GameContentDto;
import com.fungame.songquiz.domain.dto.GameInfo;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class HaliGaliGame implements Game {

    private final List<String> players = new ArrayList<>();
    private final Map<String, Deque<Card>> playerDecks = new HashMap<>();
    private final Map<String, Deque<Card>> openCards = new HashMap<>();
    private final AtomicBoolean processing = new AtomicBoolean(false);
    private int currentRound = 1;
    private final int totalRounds = 1000;

    // 종 위치 동기화를 위한 좌표
    private int bellX = 50;
    private int bellY = 50;
    private final Random random = new Random();

    public enum Fruit {
        STRAWBERRY, BANANA, LIME, GRAPE
    }

    public record Card(Fruit fruit, int count) {
        @Override
        public String toString() {
            return fruit.name() + ":" + count;
        }
    }

    @Override
    public void setPlayers(List<String> players) {
        this.players.clear();
        this.players.addAll(players);
        this.currentRound = 1;
        this.updateBellPosition();
        distributeCards();
    }

    private void distributeCards() {
        List<Card> allCards = new ArrayList<>();
        for (Fruit fruit : Fruit.values()) {
            addCards(allCards, fruit, 1, 5);
            addCards(allCards, fruit, 2, 3);
            addCards(allCards, fruit, 3, 3);
            addCards(allCards, fruit, 4, 2);
            addCards(allCards, fruit, 5, 1);
        }
        Collections.shuffle(allCards);

        int playerCount = players.size();
        if (playerCount == 0) return;

        int cardsPerPlayer = allCards.size() / playerCount;
        for (int i = 0; i < playerCount; i++) {
            String player = players.get(i);
            Deque<Card> deck = new ArrayDeque<>(allCards.subList(i * cardsPerPlayer, (i + 1) * cardsPerPlayer));
            playerDecks.put(player, deck);
            openCards.put(player, new ArrayDeque<>());
        }
    }

    private void addCards(List<Card> cards, Fruit fruit, int count, int times) {
        for (int i = 0; i < times; i++) {
            cards.add(new Card(fruit, count));
        }
    }

    private void updateBellPosition() {
        // 종이 나타날 범위를 20% ~ 80% 사이로 설정
        this.bellX = random.nextInt(60) + 20;
        this.bellY = random.nextInt(60) + 20;
    }

    @Override
    public ActionResult handleAction(GameAction action) {
        ActionResult result = switch (action.type()) {
            case FLIP_CARD -> flipCard(action.playerName());
            case PRESS_BELL -> pressBell(action.playerName());
            default -> ActionResult.WRONG;
        };

        // 액션이 발생할 때마다 종 위치를 랜덤하게 변경하여 동기화된 좌표 전송
        updateBellPosition();
        return result;
    }

    private ActionResult flipCard(String playerName) {
        if (!getCurrentPlayer().equals(playerName)) {
            return ActionResult.WRONG;
        }

        Deque<Card> deck = playerDecks.get(playerName);
        if (deck.isEmpty()) {
            nextRound();
            return ActionResult.WRONG;
        }

        Card card = deck.pollFirst();
        openCards.get(playerName).addFirst(card);
        nextRound();
        return ActionResult.CORRECT;
    }

    private boolean isPlayerActive(String player) {
        return !playerDecks.get(player).isEmpty() || !openCards.get(player).isEmpty();
    }

    @Override
    public void nextRound() {
        resetRoundState();
        int initialRound = currentRound;
        do {
            currentRound++;
            if (!playerDecks.get(getCurrentPlayer()).isEmpty()) {
                break;
            }
        } while (currentRound - initialRound < players.size());
    }

    private ActionResult pressBell(String playerName) {
        if (isFiveAnyFruit()) {
            collectCards(playerName);
            return ActionResult.CORRECT;
        } else {
            penalty(playerName);
            return ActionResult.WRONG;
        }
    }

    private boolean isFiveAnyFruit() {
        Map<Fruit, Integer> totals = new EnumMap<>(Fruit.class);
        for (Deque<Card> stack : openCards.values()) {
            if (!stack.isEmpty()) {
                Card top = stack.peekFirst();
                totals.put(top.fruit(), totals.getOrDefault(top.fruit(), 0) + top.count());
            }
        }
        return totals.values().stream().anyMatch(count -> count == 5);
    }

    private void collectCards(String winner) {
        Deque<Card> winnerDeck = playerDecks.get(winner);
        for (Deque<Card> stack : openCards.values()) {
            while (!stack.isEmpty()) {
                winnerDeck.addLast(stack.pollLast());
            }
        }
    }

    private void penalty(String loser) {
        Deque<Card> loserDeck = playerDecks.get(loser);
        if (loserDeck.isEmpty()) return;

        for (String player : players) {
            if (!player.equals(loser) && !loserDeck.isEmpty()) {
                if (isPlayerActive(player)) {
                    playerDecks.get(player).addLast(loserDeck.pollFirst());
                }
            }
        }
    }

    @Override
    public GameContentDto getStatus() {
        List<String> statusData = new ArrayList<>();
        statusData.add("TURN:" + getCurrentPlayer());
        statusData.add("ROUND:" + currentRound);
        statusData.add("BELL:" + bellX + ":" + bellY);

        for (String player : players) {
            Card top = openCards.get(player).peekFirst();
            String cardInfo = (top != null) ? top.toString() : "NONE:0";
            statusData.add(player + ":" + cardInfo + ":" + playerDecks.get(player).size());
        }

        return new GameContentDto(this, statusData);
    }

    @Override
    public GameInfo getGameInfo() {
        return new GameInfo(getType().name(), "BOARD", 56);
    }

    @Override
    public GameType getType() {
        return GameType.HALLIGALLI;
    }

    @Override
    public boolean startProcessing() {
        return processing.compareAndSet(false, true);
    }

    @Override
    public void resetRoundState() {
        processing.set(false);
    }

    @Override
    public GameAnswerDto getAnswer() {
        return GameAnswerDto.from(this, "BELL");
    }

    @Override
    public boolean isLast() {
        long activePlayers = players.stream().filter(this::isPlayerActive).count();
        return activePlayers <= 1;
    }

    @Override
    public int getCurrentRound() {
        return currentRound;
    }

    @Override
    public int getTotalRound() {
        return totalRounds;
    }

    @Override
    public String getHint() {
        return "";
    }

    // --- Test Helpers ---
    public int getPlayerDeckSize(String playerName) {
        return playerDecks.getOrDefault(playerName, new ArrayDeque<>()).size();
    }

    public String getCurrentPlayer() {
        if (players.isEmpty()) return "";
        int index = (currentRound - 1) % players.size();
        return players.get(index);
    }

    public int getOpenCardCount() {
        return openCards.values().stream().mapToInt(Deque::size).sum();
    }

    public void forceSetOpenCard(String playerName, String fruitName, int count) {
        Fruit fruit = Fruit.valueOf(fruitName);
        openCards.get(playerName).addFirst(new Card(fruit, count));
    }
}
