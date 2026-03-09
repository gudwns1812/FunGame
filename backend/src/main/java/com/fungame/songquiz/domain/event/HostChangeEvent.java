package com.fungame.songquiz.domain.event;

public record HostChangeEvent(
        String roomId, String newHost
){
}
