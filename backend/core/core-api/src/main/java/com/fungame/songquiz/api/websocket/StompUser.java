package com.fungame.songquiz.api.websocket;

import com.fungame.songquiz.domain.member.MemberAdapter;
import java.security.Principal;
import org.springframework.security.core.Authentication;

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
