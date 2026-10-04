package com.fungame.songquiz.api.controller.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PasswordResetLinkRequest {
    private String loginId;
    private String email;
}
