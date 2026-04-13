package core_api.domain.chat;

import core_api.domain.aicall.AiCallLogService;
import core_api.domain.aicall.AiRequestType;
import core_api.domain.chat.dto.AiChatResponse;
import core_api.domain.chat.dto.AiConversationSummaryResponse;
import core_api.domain.document.dto.AiSummaryResponse;
import core_api.global.exception.CustomException;
import core_api.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
// Spring에서 FastAPI AI Worker를 호출하는 전용 클라이언트
// 단순 HTTP 호출기 역할만 하는 게 아니라 timeout / retry / requestId / 성공/실패 기록까지 함께 담당
public class AiWorkerClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final AiCallLogService aiCallLogService;

    @Value("${ai-worker.url}")
    private String aiWorkerUrl;

    // AI Worker 연결 자체가 안 될 때 얼마나 기다릴지
    @Value("${ai-worker.connect-timeout-ms:3000}")
    private int connectTimeoutMs;

    // 연결 후 응답을 얼마나 기다릴지
    // PDF 분석이나 요약은 시간이 조금 걸릴 수 있어 connect timeout보다 길게
    @Value("${ai-worker.read-timeout-ms:180000}")
    private int readTimeoutMs;

    // 채팅/대화요약 요청은 네트워크 흔들림이 있을 수 있어 1회 재시도 허용
    @Value("${ai-worker.chat-retry-count:1}")
    private int chatRetryCount;

    // RestTemplate은 기본 timeout이 사실상 무한에 가까워서 외부 AI Worker가 멈추면 Spring도 오래 기다릴 수 있음
    // 앱 시작 시 공통 timeout을 한 번 세팅
    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        restTemplate.setRequestFactory(requestFactory);
    }

    // PDF 업로드 분석 호출 -> 어떤 문서(documentId)를 분석했는지 남겨야 운영 중 실패 원인을 추적 가능
    public AiSummaryResponse extractPdfSummary(Long documentId, byte[] fileBytes, String filename) throws IOException {
        String url = aiWorkerUrl + "/api/v1/pdf/extract";

        // Spring 로그, FastAPI 로그, DB 요약 로그를 하나의 요청으로 묶는 식별자
        String requestId = generateRequestId();

        // 응답 시간(latencyMs) 계산을 위해 시작 시각 저장
        long startTime = System.currentTimeMillis();

        try {
            // multipart/form-data 요청이지만, requestId도 같이 내려서 FastAPI 쪽에서도 동일한 요청으로 인식
            HttpHeaders headers = createHeaders(MediaType.MULTIPART_FORM_DATA, requestId);

            // 파일 업로드용 body 생성
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // byte[]를 multipart 파일처럼 감싸는 객체 -> 이걸 해야 Spring이 파이썬 UploadFile에 맞는 형태로 보낼 수 있다.
            ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return filename; // 기존 PDF 파일명 유지
                }
            };

            body.add("file", fileResource); // 파이썬의 file: UploadFile 파라미터 이름과 일치

            // 헤더 + 바디를 하나의 HTTP 요청 객체로 묶는다.
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // FastAPI에 실제로 파일 업로드 요청 전송
            ResponseEntity<AiSummaryResponse> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    AiSummaryResponse.class
            );

            if (response.getBody() == null) {
                long latencyMs = elapsedSince(startTime);
                recordFailure(AiRequestType.PDF_SUMMARY, requestId, null, documentId, latencyMs,
                        ErrorCode.AI_RESPONSE_EMPTY, "Empty body from pdf extract");
                throw new CustomException(ErrorCode.AI_RESPONSE_EMPTY);
            }

            long latencyMs = elapsedSince(startTime);
            AiSummaryResponse bodyResponse = response.getBody();

            // 성공한 호출은 관리자 페이지에서 볼 수 있도록 DB에도 요약 저장
            aiCallLogService.recordSuccess(
                    AiRequestType.PDF_SUMMARY,
                    requestId,
                    null,
                    documentId,
                    latencyMs,
                    null
            );

            // 상세 분석은 DB보다 애플리케이션 로그가 더 적합하므로 info 로그도 같이 남긴다.
            log.info("event=ai_call_success requestId={} requestType={} documentId={} latencyMs={} url={} totalPages={} chunksSaved={}",
                    requestId,
                    AiRequestType.PDF_SUMMARY,
                    documentId,
                    latencyMs,
                    url,
                    bodyResponse.getTotal_pages(),
                    bodyResponse.getChunks_saved());

            // 파이썬이 준 요약 결과물 반환
            return bodyResponse;

        } catch (RestClientException e) {
            long latencyMs = elapsedSince(startTime);

            // 단순 통신 실패인지, timeout인지 구분해서 에러 코드를 더 정확하게 만든다.
            ErrorCode errorCode = resolveAiErrorCode(e);
            recordFailure(AiRequestType.PDF_SUMMARY, requestId, null, documentId, latencyMs, errorCode, e.getMessage());
            throw new CustomException(errorCode);
        }
    }

    // 노트북 채팅 호출 -> notebookId를 같이 받아 "어느 노트북의 채팅이 느린지"를 추적할 수 있게 했다.
    public AiChatResponse askQuestionWithHistory(
            Long notebookId,
            String question,
            String conversationSummary,
            List<ChatHistory> historyList) {

        String endpoint = aiWorkerUrl + "/api/v1/chat/";
        String requestId = generateRequestId();

        // 채팅은 조회 성격이 강해 1회 정도 재시도해도 부작용이 적음 (반면 PDF 업로드 분석은 중복 저장 위험이 있어 retry를 넣지 않았다)
        for (int attempt = 1; attempt <= chatRetryCount + 1; attempt++) {
            long startTime = System.currentTimeMillis();

            try {
                // JSON 요청에도 requestId를 헤더로 같이 보낸다.
                HttpHeaders headers = createHeaders(MediaType.APPLICATION_JSON, requestId);

                // FastAPI가 기대하는 JSON body 구성
                Map<String, Object> body = new HashMap<>();
                body.put("question", question);
                body.put("conversation_summary", conversationSummary);
                body.put("history", formatHistory(historyList));

                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                AiChatResponse response = restTemplate.postForObject(endpoint, requestEntity, AiChatResponse.class);

                // 응답 본문이 비어 있는 경우도 명시적으로 실패 처리
                if (response == null) {
                    long latencyMs = elapsedSince(startTime);
                    recordFailure(AiRequestType.CHAT, requestId, notebookId, null, latencyMs,
                            ErrorCode.AI_RESPONSE_EMPTY, "Empty body from chat");
                    throw new CustomException(ErrorCode.AI_RESPONSE_EMPTY);
                }

                long latencyMs = elapsedSince(startTime);

                // 챗 응답은 "근거가 몇 개 붙었는지"도 운영 지표로 가치가 있어 저장한다.
                int referenceCount = response.getReference_chunks() == null ? 0 : response.getReference_chunks().size();

                aiCallLogService.recordSuccess(
                        AiRequestType.CHAT,
                        requestId,
                        notebookId,
                        null,
                        latencyMs,
                        referenceCount
                );
                log.info("event=ai_call_success requestId={} requestType={} notebookId={} latencyMs={} attempt={} referenceCount={}",
                        requestId,
                        AiRequestType.CHAT,
                        notebookId,
                        latencyMs,
                        attempt,
                        referenceCount);

                return response;

            } catch (RestClientException e) {
                long latencyMs = elapsedSince(startTime);
                boolean willRetry = attempt <= chatRetryCount;

                // 재시도 전 단계에서도 로그를 남겨 "몇 번 시도했고, 마지막에 실패했는지"를 확인 가능
                log.warn("event=ai_call_retryable_failure requestId={} requestType={} notebookId={} latencyMs={} attempt={} willRetry={} message={}",
                        requestId,
                        AiRequestType.CHAT,
                        notebookId,
                        latencyMs,
                        attempt,
                        willRetry,
                        sanitizeMessage(e.getMessage()));

                if (!willRetry) {
                    // 마지막 시도까지 실패했을 때만 DB 실패 로그를 기록한다.
                    ErrorCode errorCode = resolveAiErrorCode(e);
                    recordFailure(AiRequestType.CHAT, requestId, notebookId, null, latencyMs, errorCode, e.getMessage());
                    throw new CustomException(errorCode);
                }
            }
        }

        throw new CustomException(ErrorCode.AI_WORKER_ERROR);
    }

    // 오래된 대화를 요약 메모리(summary memory)로 압축하는 호출 -> 장기 대화 비용/토큰 증가를 줄이기 위한 보조 AI 요청
    public String summarizeConversation(Long notebookId, String existingSummary, List<ChatHistory> histories) {
        String endpoint = aiWorkerUrl + "/api/v1/chat/summary";
        String requestId = generateRequestId();

        // 읽기성 요청이라 채팅과 같은 retry 정책을 적용
        for (int attempt = 1; attempt <= chatRetryCount + 1; attempt++) {
            long startTime = System.currentTimeMillis();

            try {
                HttpHeaders headers = createHeaders(MediaType.APPLICATION_JSON, requestId);

                // 기존 요약 + 새 대화 일부를 같이 넘겨 summary memory를 갱신
                Map<String, Object> body = new HashMap<>();
                body.put("existing_summary", existingSummary);
                body.put("history", formatHistory(histories));

                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                AiConversationSummaryResponse response = restTemplate.postForObject(endpoint, requestEntity, AiConversationSummaryResponse.class);

                if (response == null || response.getSummary() == null || response.getSummary().isBlank()) {
                    long latencyMs = elapsedSince(startTime);
                    recordFailure(AiRequestType.CHAT_SUMMARY, requestId, notebookId, null, latencyMs,
                            ErrorCode.AI_RESPONSE_EMPTY, "Empty body from chat summary");
                    throw new CustomException(ErrorCode.AI_RESPONSE_EMPTY);
                }

                long latencyMs = elapsedSince(startTime);
                aiCallLogService.recordSuccess(
                        AiRequestType.CHAT_SUMMARY,
                        requestId,
                        notebookId,
                        null,
                        latencyMs,
                        null
                );
                log.info("event=ai_call_success requestId={} requestType={} notebookId={} latencyMs={} attempt={}",
                        requestId,
                        AiRequestType.CHAT_SUMMARY,
                        notebookId,
                        latencyMs,
                        attempt);

                return response.getSummary();

            } catch (RestClientException e) {
                long latencyMs = elapsedSince(startTime);
                boolean willRetry = attempt <= chatRetryCount;

                log.warn("event=ai_call_retryable_failure requestId={} requestType={} notebookId={} latencyMs={} attempt={} willRetry={} message={}",
                        requestId,
                        AiRequestType.CHAT_SUMMARY,
                        notebookId,
                        latencyMs,
                        attempt,
                        willRetry,
                        sanitizeMessage(e.getMessage()));

                if (!willRetry) {
                    ErrorCode errorCode = resolveAiErrorCode(e);
                    recordFailure(AiRequestType.CHAT_SUMMARY, requestId, notebookId, null, latencyMs, errorCode, e.getMessage());
                    throw new CustomException(errorCode);
                }
            }
        }

        throw new CustomException(ErrorCode.AI_WORKER_ERROR);
    }

    // JPA 엔티티를 그대로 외부에 보내지 않고 FastAPI가 이해할 수 있는 단순 JSON 구조로 변경
    private List<Map<String, String>> formatHistory(List<ChatHistory> historyList) {
        return historyList.stream()
                .map(h -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("role", h.getRole());
                    map.put("message", h.getMessage());
                    return map;
                })
                .collect(Collectors.toList());
    }

    // requestId를 공통 헤더에 담아 모든 외부 호출에 붙임 -> 나중에 FastAPI 로그와 Spring 로그를 requestId 하나로 묶을 수 있다.
    private HttpHeaders createHeaders(MediaType mediaType, String requestId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.set("X-Request-Id", requestId);
        return headers;
    }

    // 단순 통신 실패와 timeout은 운영 의미가 다르기 때문에 별도 코드로 구분 -> timeout은 AI 서버가 느리다는 신호라서 따로 집계할 가치가 있다.
    private ErrorCode resolveAiErrorCode(RestClientException e) {
        if (e instanceof ResourceAccessException && e.getMessage() != null) {
            String lowerMessage = e.getMessage().toLowerCase();
            if (lowerMessage.contains("timed out")) {
                return ErrorCode.AI_WORKER_TIMEOUT;
            }
        }

        return ErrorCode.AI_WORKER_ERROR;
    }

    // 실패 시
    // 1) 관리자 화면용 DB 요약 로그
    // 2) 개발자 분석용 애플리케이션 로그
    private void recordFailure(
            AiRequestType requestType,
            String requestId,
            Long notebookId,
            Long documentId,
            long latencyMs,
            ErrorCode errorCode,
            String errorMessage
    ) {
        aiCallLogService.recordFailure(
                requestType,
                requestId,
                notebookId,
                documentId,
                latencyMs,
                errorCode.getCode(),
                sanitizeMessage(errorMessage)
        );
        log.error("event=ai_call_failure requestId={} requestType={} notebookId={} documentId={} latencyMs={} errorCode={} message={}",
                requestId,
                requestType,
                notebookId,
                documentId,
                latencyMs,
                errorCode.getCode(),
                sanitizeMessage(errorMessage));
    }

    // 시작 시각 기준으로 경과 시간(ms)을 계산
    private long elapsedSince(long startTime) {
        return System.currentTimeMillis() - startTime;
    }

    // Spring 로그, FastAPI 로그, DB 요약 로그를 같은 요청으로 묶기 위한 값
    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    // 외부 예외 메시지가 너무 길면 DB에 저장하기 부담스러우므로 길이를 제한
    private String sanitizeMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        if (message.length() > 500) {
            return message.substring(0, 500);
        }

        return message;
    }
}
