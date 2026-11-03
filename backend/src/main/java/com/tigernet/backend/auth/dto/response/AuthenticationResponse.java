package com.tigernet.backend.auth.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

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
}
