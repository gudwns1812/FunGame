package com.fungame.songquiz.domain;

public record HostChangeEvent(
        String roomId, String newHost
){
}
