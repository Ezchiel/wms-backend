package vn.edu.hcmuaf.fit.wms.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Service gọi Google Gemini Vision API để OCR ảnh phiếu nhập kho.
 * Trả về chuỗi JSON thuần (đã được ép schema qua prompt).
 * KHÔNG tự ý ghi DB. Chỉ trích xuất và trả dữ liệu thô.
 */
@Service
@Slf4j
public class GeminiVisionService {

    private static final String SYSTEM_PROMPT = """
            Bạn là hệ thống trích xuất dữ liệu phiếu nhập kho cho hệ thống WMS.
            Hãy đọc kỹ ảnh phiếu giao hàng/packing slip/hóa đơn được cung cấp.
            Trả về DUY NHẤT một đối tượng JSON hợp lệ (không có markdown, không có ```json, không có giải thích thêm) theo đúng schema sau:
            {
              "supplierName": "tên nhà cung cấp đầy đủ, hoặc null nếu không tìm thấy",
              "notes": "ghi chú tổng phiếu nếu có, hoặc null",
              "items": [
                {
                  "productName": "tên sản phẩm/hàng hóa",
                  "productCode": "mã sản phẩm/SKU nếu có, hoặc null",
                  "quantity": số_nguyên_dương,
                  "unitPrice": số_thực_hoặc_null,
                  "batchNo": "số lô/batch nếu có, hoặc null",
                  "expiryDate": "ngày hết hạn định dạng yyyy-MM-dd nếu có, hoặc null",
                  "serialNumber": "số serial nếu có, hoặc null"
                }
              ],
              "overallConfidence": số_thực_từ_0_đến_1
            }
            Quy tắc bắt buộc:
            - KHÔNG tự bịa dữ liệu. Nếu trường nào không đọc được rõ ràng, để null.
            - overallConfidence phản ánh mức độ rõ ràng của ảnh và chất lượng thông tin đọc được.
            - Nếu đây không phải phiếu giao hàng hoặc ảnh không rõ, trả về items rỗng [] và overallConfidence thấp (dưới 0.2).
            - Chỉ trả về JSON, không có bất kỳ văn bản nào khác.
            """;

    private final RestTemplate geminiRestTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    @Value("${gemini.api.url}")
    private String apiUrl;

    public GeminiVisionService(
            @Qualifier("geminiRestTemplate") RestTemplate geminiRestTemplate,
            ObjectMapper objectMapper) {
        this.geminiRestTemplate = geminiRestTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Gọi Gemini Vision API để trích xuất thông tin từ ảnh phiếu nhập kho.
     *
     * @param imageBase64 chuỗi base64 của ảnh (không có data URI prefix)
     * @param mimeType    MIME type của ảnh, ví dụ "image/jpeg"
     * @return chuỗi JSON kết quả từ Gemini
     * @throws GeminiApiException nếu API lỗi hoặc không parse được kết quả
     */
    public String extractFromImage(String imageBase64, String mimeType) {
        String url = apiUrl + "/" + model + ":generateContent?key=" + apiKey;

        // Xây dựng request body theo Gemini REST API format
        Map<String, Object> requestBody = buildRequestBody(imageBase64, mimeType);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // Thử gọi, nếu lỗi thì retry 1 lần
        try {
            return callGeminiApi(url, entity);
        } catch (RestClientException e) {
            log.warn("Gemini API lần 1 thất bại: {}. Đang thử lại...", e.getMessage());
            try {
                return callGeminiApi(url, entity);
            } catch (RestClientException retryException) {
                log.error("Gemini API thất bại sau 2 lần thử: {}", retryException.getMessage());
                throw new GeminiApiException("Không thể kết nối Gemini API: " + retryException.getMessage());
            }
        }
    }

    private String callGeminiApi(String url, HttpEntity<Map<String, Object>> entity) {
        ResponseEntity<GeminiResponse> response = geminiRestTemplate.exchange(
                url, HttpMethod.POST, entity, GeminiResponse.class);

        if (response.getBody() == null
                || response.getBody().getCandidates() == null
                || response.getBody().getCandidates().isEmpty()) {
            throw new GeminiApiException("Gemini trả về kết quả rỗng");
        }

        GeminiResponse.Candidate candidate = response.getBody().getCandidates().get(0);
        if (candidate.getContent() == null
                || candidate.getContent().getParts() == null
                || candidate.getContent().getParts().isEmpty()) {
            throw new GeminiApiException("Gemini không trả về nội dung text");
        }

        String rawText = candidate.getContent().getParts().get(0).getText();
        log.debug("Gemini raw response: {}", rawText);
        return rawText;
    }

    private Map<String, Object> buildRequestBody(String imageBase64, String mimeType) {
        // Part 1: inline image
        Map<String, Object> inlineData = Map.of(
                "mimeType", mimeType,
                "data", imageBase64
        );
        Map<String, Object> imagePart = Map.of("inlineData", inlineData);

        // Part 2: system prompt text
        Map<String, Object> textPart = Map.of("text", SYSTEM_PROMPT);

        // Content object
        Map<String, Object> content = Map.of(
                "role", "user",
                "parts", List.of(imagePart, textPart)
        );

        // Generation config — nhiệt độ thấp để giảm "hallucination"
        Map<String, Object> generationConfig = Map.of(
                "temperature", 0.1,
                "responseMimeType", "application/json"
        );

        return Map.of(
                "contents", List.of(content),
                "generationConfig", generationConfig
        );
    }

    // ----------------------------------------------------------------
    // Inner classes để deserialize Gemini REST response
    // ----------------------------------------------------------------

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeminiResponse {
        private List<Candidate> candidates;

        @Getter
        @Setter
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Candidate {
            private Content content;

            @Getter
            @Setter
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Content {
                private List<Part> parts;

                @Getter
                @Setter
                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class Part {
                    private String text;
                }
            }
        }
    }

    // Custom exception để phân biệt lỗi Gemini với lỗi hệ thống
    public static class GeminiApiException extends RuntimeException {
        public GeminiApiException(String message) {
            super(message);
        }
    }
}
