package com.aicopilot.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    private String email;
    private String phone;
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度6-32位")
    private String password;
    @NotBlank(message = "昵称不能为空")
    private String nickname;
    /** 团队邀请码（可选） */
    private String inviteCode;
}