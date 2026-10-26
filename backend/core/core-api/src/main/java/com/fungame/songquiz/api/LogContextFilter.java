package com.fungame.songquiz.api;

import com.fungame.songquiz.domain.member.MemberAdapter;
import com.fungame.songquiz.support.logging.LogContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class LogContextFilter extends OncePerRequestFilter {

    private static final int REQUEST_ID_LENGTH = 8;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try (LogContext context = LogContext.open()
                .put(LogContext.REQUEST_ID, newRequestId())
                .put(LogContext.MEMBER_ID, signedInMemberId())) {
            chain.doFilter(request, response);
        }
    }

    private static String newRequestId() {
        return UUID.randomUUID().toString().substring(0, REQUEST_ID_LENGTH);
    }

    private static Long signedInMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof MemberAdapter member) {
            return member.getId();
        }

        return null;
    }
}
