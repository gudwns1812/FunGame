package com.fungame.songquiz.support;

import com.fungame.songquiz.domain.member.MemberAdapter;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;

public final class StompMessages {

    private static final byte[] EMPTY_BODY = new byte[0];

    private StompMessages() {
    }

    public static Principal loggedIn(Long memberId) {
        MemberAdapter member = new MemberAdapter(MemberFixture.withId(memberId, "회원" + memberId));
        return new UsernamePasswordAuthenticationToken(member, null, member.getAuthorities());
    }

    public static Message<byte[]> subscribe(String sessionId, String destination, Principal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionId(sessionId);
        accessor.setDestination(destination);
        accessor.setUser(user);

        return MessageBuilder.createMessage(EMPTY_BODY, accessor.getMessageHeaders());
    }

    public static Message<byte[]> session(String sessionId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
        accessor.setSessionId(sessionId);

        return MessageBuilder.createMessage(EMPTY_BODY, accessor.getMessageHeaders());
    }
}
