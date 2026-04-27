package core_api.global.security;

import core_api.config.Webconfig;
import core_api.domain.admin.AdminAiCallLogController;
import core_api.domain.admin.AdminAiCallLogService;
import core_api.domain.admin.dto.AdminAiCallLogPageResponse;
import core_api.domain.notebook.NotebookController;
import core_api.domain.notebook.NotebookService;
import core_api.domain.user.Role;
import core_api.domain.user.User;
import core_api.domain.user.UserController;
import core_api.domain.user.UserRepository;
import core_api.domain.user.UserService;
import core_api.domain.user.dto.UserLoginResponse;
import core_api.global.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        UserController.class,
        NotebookController.class,
        AdminAiCallLogController.class
})
@Import({
        Webconfig.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        SecurityAuthenticationEntryPoint.class,
        SecurityAccessDeniedHandler.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private NotebookService notebookService;

    @MockBean
    private AdminAiCallLogService adminAiCallLogService;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("로그인 API는 인증 없이 호출할 수 있다")
    void loginEndpoint_isPublic() throws Exception {
        given(userService.login(any()))
                .willReturn(new UserLoginResponse("access-token", "refresh-token", Role.USER));

        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@test.com",
                                  "password": "password123!"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("일반 API는 인증이 없으면 401을 반환한다")
    void protectedEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/notebooks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CORS preflight 요청은 인증 없이 허용된다")
    void preflightRequest_isPermitted() throws Exception {
        mockMvc.perform(options("/api/v1/notebooks")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    @DisplayName("관리자 API는 USER 권한으로 접근하면 403을 반환한다")
    void adminEndpoint_forbiddenForUserRole() throws Exception {
        given(jwtProvider.extractAccessTokenUserId("user-token")).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(createUser(Role.USER)));

        mockMvc.perform(get("/api/v1/admin/ai-call-logs")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자 API는 ADMIN 권한이면 접근할 수 있다")
    void adminEndpoint_allowsAdminRole() throws Exception {
        given(jwtProvider.extractAccessTokenUserId("admin-token")).willReturn(2L);
        given(userRepository.findById(2L)).willReturn(Optional.of(createUser(Role.ADMIN)));
        given(adminAiCallLogService.getAiCallLogs(null, null, null, null, null, null))
                .willReturn(AdminAiCallLogPageResponse.builder()
                        .content(List.of())
                        .page(0)
                        .size(20)
                        .totalPages(0)
                        .totalElements(0)
                        .hasNext(false)
                        .build());

        mockMvc.perform(get("/api/v1/admin/ai-call-logs")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    private User createUser(Role role) {
        return User.builder()
                .email(role.name().toLowerCase() + "@test.com")
                .password("password123!")
                .nickname(role.name())
                .role(role)
                .build();
    }
}
