package com.fungame.songquiz.domain.event;

public record HostChangeEvent(
        Long roomId, String newHost
){
}
