package core_api.domain.user;

// 회원 권한은 일반 사용자와 관리자 두 단계만 먼저 운영합니다.
// 관리자는 회원가입으로 만들지 않고, DB에서 직접 등록하는 방식입니다.
public enum Role {
    USER,
    ADMIN
}
