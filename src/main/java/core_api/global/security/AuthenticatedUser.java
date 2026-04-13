package core_api.global.security;

import core_api.domain.user.Role;

// 필터에서 인증이 끝난 뒤 SecurityContext에 넣어둘 최소 사용자 정보
// 현재 프로젝트에서는 userId와 role만 있으면 관리자 API 인가에 충분
public record AuthenticatedUser(Long userId, Role role) {
}
