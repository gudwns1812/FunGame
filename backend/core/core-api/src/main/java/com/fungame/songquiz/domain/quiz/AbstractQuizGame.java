package com.fungame.songquiz.domain.quiz;

import com.fungame.songquiz.domain.room.GamePlayer;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractQuizGame implements Game {
    protected final AtomicBoolean isRoundProcessing = new AtomicBoolean(false);

    @Override
    public void dropPlayer(Long memberId) {
    }

    @Override
    public void takeBackPlayer(GamePlayer player) {
    }

    @Override
    public boolean startProcessing() {
        return isRoundProcessing.compareAndSet(false, true);
    }

    @Override
    public void resetRoundState() {
        isRoundProcessing.set(false);
    }
}
