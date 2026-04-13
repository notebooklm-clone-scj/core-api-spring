package core_api.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserTokenRefreshRequest {

    @NotBlank(message = "리프레시 토큰은 필수입니다.")
    private String refreshToken;
}
