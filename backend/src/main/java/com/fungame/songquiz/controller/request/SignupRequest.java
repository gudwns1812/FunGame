package com.fungame.songquiz.controller.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {
    private String loginId;
    private String password;
    private String nickname;
}
