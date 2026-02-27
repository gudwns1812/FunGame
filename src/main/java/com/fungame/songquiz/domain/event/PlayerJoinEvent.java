package com.fungame.songquiz.domain.event;

import java.util.List;

public record PlayerJoinEvent(String roomId, List<String> players) {
}
