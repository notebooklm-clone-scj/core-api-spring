package core_api.domain.user;

import core_api.domain.user.dto.UserLoginRequest;
import core_api.domain.user.dto.UserLoginResponse;
import core_api.domain.user.dto.UserSignupRequest;
import core_api.domain.user.dto.UserSignupResponse;
import core_api.domain.user.dto.UserTokenRefreshRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<UserSignupResponse> signup(@Valid @RequestBody UserSignupRequest request) {
        Long userId = userService.signup(request);

        UserSignupResponse response = new UserSignupResponse(userId, "회원가입이 완료되었습니다.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        UserLoginResponse response = userService.login(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<UserLoginResponse> refresh(@Valid @RequestBody UserTokenRefreshRequest request) {
        UserLoginResponse response = userService.refresh(request);
        return ResponseEntity.ok(response);
    }
}
