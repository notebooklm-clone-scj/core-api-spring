package core_api.global.jwt;

// JWT는 access / refresh 두 종류로 운영합니다.
// 같은 서명 키를 써도 용도를 구분해야 refresh token이 API 호출에 오용되지 않습니다.
public enum TokenType {
    ACCESS,
    REFRESH
}
