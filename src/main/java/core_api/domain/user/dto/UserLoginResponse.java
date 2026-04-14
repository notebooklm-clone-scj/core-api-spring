package core_api.domain.user.dto;

import core_api.domain.user.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserLoginResponse {
    private String token;
    private String refreshToken;
    private Role role;
}
