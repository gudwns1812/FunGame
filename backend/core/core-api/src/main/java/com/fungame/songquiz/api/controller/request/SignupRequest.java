package com.fungame.songquiz.api.controller.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {
    private String loginId;
    private String password;
    private String nickname;
    private String email;
}
