package core_api.domain.chat;

import core_api.domain.chat.dto.AiChatResponse;
import core_api.domain.document.dto.AiSummaryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class AiWorkerClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai-worker.url}")
    private String aiWorkerUrl;

    public AiSummaryResponse extractPdfSummary(MultipartFile file) throws IOException {
        String url = aiWorkerUrl + "/api/v1/pdf/extract";

        // 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // 바디 설정 (pdf 파일을 java로 변환)
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // 기존 파일명을 유지
        ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename(); // 기존 PDF 파일명 유지
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

        // 파이썬이 준 요약 결과물 반환
        return response.getBody();
    }

    public AiChatResponse askQuestion(String question) {
        String endpoint = aiWorkerUrl + "/api/v1/chat/";

        // JSON 형태로 보낼 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 바디에 질문(question) 저장
        Map<String, String> body = new HashMap<>();
        body.put("question", question);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

        return restTemplate.postForObject(endpoint, requestEntity, AiChatResponse.class);
    }

}
