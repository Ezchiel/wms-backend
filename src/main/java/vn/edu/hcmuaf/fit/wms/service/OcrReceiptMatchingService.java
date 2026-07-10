package vn.edu.hcmuaf.fit.wms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.hcmuaf.fit.wms.entity.Partner;
import vn.edu.hcmuaf.fit.wms.entity.Product;
import vn.edu.hcmuaf.fit.wms.entity.enums.PartnerType;
import vn.edu.hcmuaf.fit.wms.repository.PartnerRepository;
import vn.edu.hcmuaf.fit.wms.repository.ProductRepository;

import java.util.*;

/**
 * Service thực hiện fuzzy matching giữa dữ liệu OCR trích xuất từ ảnh
 * và dữ liệu Partner/Product đã có trong hệ thống.
 *
 * Thuật toán: Jaccard Similarity trên tập từ (word-level), không phân biệt hoa thường.
 * Ưu tiên match chính xác theo mã (productCode, taxCode) trước khi dùng fuzzy.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OcrReceiptMatchingService {

    private final PartnerRepository partnerRepository;
    private final ProductRepository productRepository;

    // Ngưỡng confidence tối thiểu để coi là "matched"
    private static final double PARTNER_MATCH_THRESHOLD = 0.35;
    private static final double PRODUCT_MATCH_THRESHOLD = 0.30;

    // ----------------------------------------------------------------
    // Partner matching
    // ----------------------------------------------------------------

    public record PartnerMatchResult(Long partnerId, String partnerName, double confidence) {}

    /**
     * Tìm Partner (nhà cung cấp) khớp nhất với tên trích xuất từ ảnh.
     *
     * @param supplierNameRaw tên nhà cung cấp thô từ OCR
     * @return kết quả match (partnerId = null nếu không tìm thấy)
     */
    public PartnerMatchResult matchSupplier(String supplierNameRaw) {
        if (supplierNameRaw == null || supplierNameRaw.isBlank()) {
            return new PartnerMatchResult(null, null, 0.0);
        }

        List<Partner> suppliers = partnerRepository.findByTypeOrderByName(PartnerType.SUPPLIER);
        if (suppliers.isEmpty()) {
            return new PartnerMatchResult(null, null, 0.0);
        }

        String normalizedInput = normalize(supplierNameRaw);
        Partner bestMatch = null;
        double bestScore = 0.0;

        for (Partner partner : suppliers) {
            // Khớp chính xác taxCode (nếu OCR trích xuất được mã số thuế)
            if (partner.getTaxCode() != null && !partner.getTaxCode().isBlank()) {
                String cleanedInput = supplierNameRaw.replaceAll("\\s+", "").toLowerCase();
                String cleanedTax = partner.getTaxCode().replaceAll("\\s+", "").toLowerCase();
                if (cleanedInput.contains(cleanedTax) || cleanedTax.contains(cleanedInput)) {
                    log.debug("Partner matched by taxCode: {} -> {}", supplierNameRaw, partner.getName());
                    return new PartnerMatchResult(partner.getId(), partner.getName(), 1.0);
                }
            }

            // Fuzzy match theo tên
            double score = jaccardSimilarity(normalizedInput, normalize(partner.getName()));
            if (score > bestScore) {
                bestScore = score;
                bestMatch = partner;
            }
        }

        if (bestMatch != null && bestScore >= PARTNER_MATCH_THRESHOLD) {
            log.debug("Partner fuzzy matched: '{}' -> '{}' (score={})", supplierNameRaw, bestMatch.getName(), bestScore);
            return new PartnerMatchResult(bestMatch.getId(), bestMatch.getName(), bestScore);
        }

        log.debug("No partner match found for: '{}' (best score={})", supplierNameRaw, bestScore);
        return new PartnerMatchResult(null, null, 0.0);
    }

    // ----------------------------------------------------------------
    // Product matching
    // ----------------------------------------------------------------

    public record ProductMatchResult(Long productId, String productName, String productCode, double confidence) {}

    /**
     * Tìm Product khớp nhất với thông tin trích xuất từ ảnh.
     * Ưu tiên: match chính xác productCode > fuzzy match productName.
     *
     * @param productNameRaw tên sản phẩm thô từ OCR
     * @param productCodeRaw mã sản phẩm thô từ OCR (có thể null)
     * @return kết quả match (productId = null nếu không tìm thấy)
     */
    public ProductMatchResult matchProduct(String productNameRaw, String productCodeRaw) {
        if (productNameRaw == null && productCodeRaw == null) {
            return new ProductMatchResult(null, null, null, 0.0);
        }

        // Ưu tiên 1: match chính xác theo productCode
        if (productCodeRaw != null && !productCodeRaw.isBlank()) {
            String cleanedCode = productCodeRaw.trim().toUpperCase();
            List<Product> allProducts = productRepository.findAll();

            Optional<Product> exactMatch = allProducts.stream()
                    .filter(p -> p.getProductCode() != null
                            && p.getProductCode().trim().toUpperCase().equals(cleanedCode))
                    .findFirst();

            if (exactMatch.isPresent()) {
                Product p = exactMatch.get();
                log.debug("Product matched by exact code: {} -> {}", productCodeRaw, p.getProductName());
                return new ProductMatchResult(p.getId(), p.getProductName(), p.getProductCode(), 1.0);
            }

            // Thử partial match mã (code chứa trong nhau)
            Optional<Product> partialCodeMatch = allProducts.stream()
                    .filter(p -> p.getProductCode() != null && (
                            p.getProductCode().toUpperCase().contains(cleanedCode)
                            || cleanedCode.contains(p.getProductCode().toUpperCase())))
                    .findFirst();
            if (partialCodeMatch.isPresent()) {
                Product p = partialCodeMatch.get();
                log.debug("Product partial code match: {} -> {}", productCodeRaw, p.getProductName());
                return new ProductMatchResult(p.getId(), p.getProductName(), p.getProductCode(), 0.85);
            }
        }

        // Ưu tiên 2: fuzzy match theo tên
        if (productNameRaw != null && !productNameRaw.isBlank()) {
            List<Product> allProducts = productRepository.findAll();
            String normalizedInput = normalize(productNameRaw);

            Product bestMatch = null;
            double bestScore = 0.0;

            for (Product product : allProducts) {
                double score = jaccardSimilarity(normalizedInput, normalize(product.getProductName()));
                if (score > bestScore) {
                    bestScore = score;
                    bestMatch = product;
                }
            }

            if (bestMatch != null && bestScore >= PRODUCT_MATCH_THRESHOLD) {
                log.debug("Product fuzzy matched: '{}' -> '{}' (score={})", productNameRaw, bestMatch.getProductName(), bestScore);
                return new ProductMatchResult(bestMatch.getId(), bestMatch.getProductName(), bestMatch.getProductCode(), bestScore);
            }
        }

        log.debug("No product match found for name='{}', code='{}'", productNameRaw, productCodeRaw);
        return new ProductMatchResult(null, null, null, 0.0);
    }

    // ----------------------------------------------------------------
    // Utility: Jaccard Similarity trên tập từ
    // ----------------------------------------------------------------

    /**
     * Tính Jaccard Similarity giữa 2 chuỗi đã chuẩn hóa.
     * Similarity = |intersection| / |union| của tập các từ.
     */
    private double jaccardSimilarity(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) return 0.0;

        Set<String> setA = new HashSet<>(Arrays.asList(a.split("\\s+")));
        Set<String> setB = new HashSet<>(Arrays.asList(b.split("\\s+")));

        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);

        if (union.isEmpty()) return 0.0;
        return (double) intersection.size() / union.size();
    }

    /**
     * Chuẩn hóa chuỗi: lowercase, loại bỏ ký tự đặc biệt, chuẩn hóa khoảng trắng.
     */
    private String normalize(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replaceAll("[^a-z0-9\\sàáâãäåèéêëìíîïòóôõöùúûüýÿăđơưạảấầẩẫậắằẳẵặẹẻẽếềểễệỉịọỏốồổỗộớờởỡợụủứừửữựỳỵỷỹ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
