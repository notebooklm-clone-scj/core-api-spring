package core_api.domain.user;

import lombok.Getter;

@Getter
public class UserLoginRequest {
    private String email;
    private String password;
}
