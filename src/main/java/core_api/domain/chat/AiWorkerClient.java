package core_api.domain.chat;

import core_api.domain.chat.dto.AiChatResponse;
import core_api.domain.chat.dto.AiConversationSummaryResponse;
import core_api.domain.document.dto.AiSummaryResponse;
import core_api.global.exception.CustomException;
import core_api.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AiWorkerClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai-worker.url}")
    private String aiWorkerUrl;

    public AiSummaryResponse extractPdfSummary(byte[] fileBytes, String filename) throws IOException {
        String url = aiWorkerUrl + "/api/v1/pdf/extract";

        try {
            // 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // 바디 설정 (pdf 파일을 java로 변환)
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // 기존 파일명을 유지
            ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return filename; // 기존 PDF 파일명 유지
                }
            };

            body.add("file", fileResource); // 파이썬의 file: UploadFile 파라미터 이름과 일치

            // 헤더와 파일을 하나의 Entity로 변경
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 파이썬 서버로 post 요청 (AiSummaryResponse로 응답 받음)
            ResponseEntity<AiSummaryResponse> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    AiSummaryResponse.class
            );

            if (response.getBody() == null) {
                throw new CustomException(ErrorCode.AI_RESPONSE_EMPTY);
            }

            // 파이썬이 준 요약 결과물 반환
            return response.getBody();

        } catch (RestClientException e) {
            throw new CustomException(ErrorCode.AI_WORKER_ERROR);
        }
    }

    public AiChatResponse askQuestionWithHistory(
            String question,
            String conversationSummary,
            List<ChatHistory> historyList) {

        String endpoint = aiWorkerUrl + "/api/v1/chat/";

        try {
            // JSON 형태로 보낼 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);




            // Body에 내용물(question, history) 저장
            Map<String, Object> body = new HashMap<>();
            body.put("question", question);
            body.put("conversation_summary", conversationSummary);
            body.put("history", formatHistory(historyList));

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            AiChatResponse response = restTemplate.postForObject(endpoint, requestEntity, AiChatResponse.class);

            if (response == null) {
                throw new CustomException(ErrorCode.AI_RESPONSE_EMPTY);
            }

            return response;

        } catch (RestClientException e) {
            throw new CustomException(ErrorCode.AI_WORKER_ERROR);
        }
    }

    public String summarizeConversation(String existingSummary, List<ChatHistory> histories) {
        String endpoint = aiWorkerUrl + "/api/v1/chat/summary";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("existing_summary", existingSummary);
            body.put("history", formatHistory(histories));

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            AiConversationSummaryResponse response = restTemplate.postForObject(endpoint, requestEntity, AiConversationSummaryResponse.class);

            if (response == null || response.getSummary() == null || response.getSummary().isBlank()) {
                throw new CustomException(ErrorCode.AI_RESPONSE_EMPTY);
            }

            return response.getSummary();

        } catch (RestClientException e) {
            throw new CustomException(ErrorCode.AI_WORKER_ERROR);
        }
    }

    // Spring의 ChatHistory 객체들을 파이썬이 읽기 편한 간단한 Map 구조로 변환
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
}
