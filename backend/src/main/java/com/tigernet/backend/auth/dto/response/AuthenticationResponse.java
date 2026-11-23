package com.tigernet.backend.auth.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationResponse {
    String token;          // access token, gửi trong header Authorization: Bearer
    String refreshToken;   // chỉ dùng cho POST /auth/refresh
    long expiresIn;        // số giây access token còn hiệu lực
    boolean authenticated;
    // Quản trị: đúng mật khẩu rồi còn phải nhập mã 6 số (mfaRequired) hoặc cài app Authenticator lần đầu
    // (mfaSetupRequired); mfaToken (5 phút) chỉ dùng cho /auth/mfa/**, chưa có token gọi API
    boolean mfaRequired;
    boolean mfaSetupRequired;
    String mfaToken;
    List<String> recoveryCodes; // chỉ trả đúng 1 lần, ngay khi vừa bật MFA
}
