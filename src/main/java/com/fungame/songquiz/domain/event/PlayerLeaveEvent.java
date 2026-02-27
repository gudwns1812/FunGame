package com.fungame.songquiz.domain.event;

import java.util.List;

public record PlayerLeaveEvent(String roomId, List<String> players) {
}
