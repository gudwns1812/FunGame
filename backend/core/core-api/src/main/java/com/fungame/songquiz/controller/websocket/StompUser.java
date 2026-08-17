package com.fungame.songquiz.controller.websocket;

import com.fungame.songquiz.domain.member.MemberAdapter;
import org.springframework.security.core.Authentication;

import java.security.Principal;

final class StompUser {

    private StompUser() {
    }

    static Long memberIdOf(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof MemberAdapter member) {
            return member.getId();
        }

        return null;
    }
}
