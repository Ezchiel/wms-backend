package vn.edu.hcmuaf.fit.wms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Cấu hình RestTemplate riêng cho Gemini Vision API với timeout phù hợp.
 * Connect timeout: 10 giây, Read timeout: 25 giây (Gemini có thể chậm khi xử lý ảnh lớn).
 */
@Configuration
public class GeminiConfig {

    @Bean(name = "geminiRestTemplate")
    public RestTemplate geminiRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000); // 10 seconds
        factory.setReadTimeout(25_000);    // 25 seconds
        return new RestTemplate(factory);
    }
}
