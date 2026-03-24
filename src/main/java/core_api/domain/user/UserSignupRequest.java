package core_api.domain.user;

import lombok.Getter;

@Getter
public class UserSignupRequest {
    private String email;
    private String password;
    private String nickname;
}
