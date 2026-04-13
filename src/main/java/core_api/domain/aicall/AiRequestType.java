package core_api.domain.aicall;

// AI Worker를 어떤 목적으로 호출했는지 구분하는 enum
// -> 운영 중 "채팅이 느린 건지", "PDF 요약이 느린 건지"를 나눠서 볼 수 있다.
public enum AiRequestType {
    PDF_SUMMARY,
    CHAT,
    CHAT_SUMMARY
}
