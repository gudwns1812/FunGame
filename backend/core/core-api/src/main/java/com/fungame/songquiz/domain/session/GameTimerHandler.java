package com.fungame.songquiz.domain.session;

import java.util.List;

public interface GameTimerHandler {

    List<GameTimerTask.Kind> timerKinds();

    void onTimer(GameTimerTask task);
}
