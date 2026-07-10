package vn.edu.hcmuaf.fit.wms.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.hcmuaf.fit.wms.entity.Partner;
import vn.edu.hcmuaf.fit.wms.entity.Product;
import vn.edu.hcmuaf.fit.wms.entity.enums.PartnerType;
import vn.edu.hcmuaf.fit.wms.repository.PartnerRepository;
import vn.edu.hcmuaf.fit.wms.repository.ProductRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OcrReceiptMatchingService - Unit Tests")
class OcrReceiptMatchingServiceTest {

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OcrReceiptMatchingService matchingService;

    private Partner supplier1;
    private Partner supplier2;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        // Setup suppliers
        supplier1 = new Partner();
        supplier1.setId(1L);
        supplier1.setName("Công ty TNHH Thực phẩm Sạch Việt");
        supplier1.setType(PartnerType.SUPPLIER);
        supplier1.setTaxCode("0123456789");

        supplier2 = new Partner();
        supplier2.setId(2L);
        supplier2.setName("Tập đoàn Phân phối Hàng tiêu dùng ABC");
        supplier2.setType(PartnerType.SUPPLIER);
        supplier2.setTaxCode(null);

        // Setup products
        product1 = new Product();
        product1.setId(10L);
        product1.setProductCode("SP-001");
        product1.setProductName("Gạo tẻ Tám Xoan 5kg");

        product2 = new Product();
        product2.setId(20L);
        product2.setProductCode("SP-002");
        product2.setProductName("Dầu ăn thực vật Neptune 1 lít");
    }

    // ================================================================
    // SUPPLIER MATCHING TESTS
    // ================================================================

    @Test
    @DisplayName("Match supplier chính xác theo taxCode")
    void matchSupplier_exactTaxCode_returnsHighConfidence() {
        when(partnerRepository.findByTypeOrderByName(PartnerType.SUPPLIER))
                .thenReturn(List.of(supplier1, supplier2));

        // OCR trích xuất được mã số thuế trong tên
        var result = matchingService.matchSupplier("Cty Thực phẩm Sạch Việt MST: 0123456789");

        assertThat(result.partnerId()).isEqualTo(1L);
        assertThat(result.confidence()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Fuzzy match supplier theo tên — tên gần giống")
    void matchSupplier_fuzzyName_returnsMatch() {
        when(partnerRepository.findByTypeOrderByName(PartnerType.SUPPLIER))
                .thenReturn(List.of(supplier1, supplier2));

        // Tên trên phiếu viết tắt nhưng vẫn nhận ra được
        var result = matchingService.matchSupplier("Công ty Thực phẩm Sạch Việt");

        assertThat(result.partnerId()).isEqualTo(1L);
        assertThat(result.confidence()).isGreaterThan(0.35);
    }

    @Test
    @DisplayName("Không match khi tên hoàn toàn khác")
    void matchSupplier_noMatch_returnsNull() {
        when(partnerRepository.findByTypeOrderByName(PartnerType.SUPPLIER))
                .thenReturn(List.of(supplier1, supplier2));

        var result = matchingService.matchSupplier("Amazon Web Services Inc");

        assertThat(result.partnerId()).isNull();
        assertThat(result.confidence()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Supplier name null → trả về không match")
    void matchSupplier_nullInput_returnsEmpty() {
        var result = matchingService.matchSupplier(null);

        assertThat(result.partnerId()).isNull();
        assertThat(result.confidence()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Không có supplier nào trong DB → trả về không match")
    void matchSupplier_emptyDb_returnsEmpty() {
        when(partnerRepository.findByTypeOrderByName(PartnerType.SUPPLIER))
                .thenReturn(Collections.emptyList());

        var result = matchingService.matchSupplier("Công ty ABC");

        assertThat(result.partnerId()).isNull();
    }

    // ================================================================
    // PRODUCT MATCHING TESTS
    // ================================================================

    @Test
    @DisplayName("Match product chính xác theo productCode")
    void matchProduct_exactCode_returnsFullConfidence() {
        when(productRepository.findAll()).thenReturn(List.of(product1, product2));

        var result = matchingService.matchProduct("Gạo tẻ", "SP-001");

        assertThat(result.productId()).isEqualTo(10L);
        assertThat(result.confidence()).isEqualTo(1.0);
        assertThat(result.productCode()).isEqualTo("SP-001");
    }

    @Test
    @DisplayName("Match product theo mã không phân biệt hoa thường")
    void matchProduct_codeCase_insensitive() {
        when(productRepository.findAll()).thenReturn(List.of(product1, product2));

        var result = matchingService.matchProduct(null, "sp-001");

        assertThat(result.productId()).isEqualTo(10L);
        assertThat(result.confidence()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Fuzzy match product theo tên — tên có từ khóa chính")
    void matchProduct_fuzzyName_returnsMatch() {
        when(productRepository.findAll()).thenReturn(List.of(product1, product2));

        var result = matchingService.matchProduct("Dầu ăn Neptune 1L", null);

        assertThat(result.productId()).isEqualTo(20L);
        assertThat(result.confidence()).isGreaterThan(0.30);
    }

    @Test
    @DisplayName("Không match khi cả tên và mã đều null")
    void matchProduct_bothNull_returnsEmpty() {
        var result = matchingService.matchProduct(null, null);

        assertThat(result.productId()).isNull();
        assertThat(result.confidence()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Không match khi sản phẩm không có trong DB")
    void matchProduct_notInDb_returnsNull() {
        when(productRepository.findAll()).thenReturn(List.of(product1, product2));

        var result = matchingService.matchProduct("iPhone 15 Pro Max 256GB", "APPLE-15PM");

        assertThat(result.productId()).isNull();
        assertThat(result.confidence()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Partial code match khi mã chứa nhau")
    void matchProduct_partialCode_returnsPartialMatch() {
        when(productRepository.findAll()).thenReturn(List.of(product1, product2));

        // OCR đọc thêm prefix
        var result = matchingService.matchProduct("Gạo tẻ", "VN-SP-001-BAG");

        // SP-001 contained in VN-SP-001-BAG
        assertThat(result.productId()).isEqualTo(10L);
        assertThat(result.confidence()).isGreaterThanOrEqualTo(0.85);
    }
}
