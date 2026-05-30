package vn.edu.hcmuaf.fit.wms.service;

import org.springframework.data.domain.Page;
import vn.edu.hcmuaf.fit.wms.dto.ProductRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.ProductResponseDTO;

import java.util.List;

public interface ProductService {
    Page<ProductResponseDTO> getAllProducts(String keyword, int page, int size, String sortBy, String sortDir);
    List<ProductResponseDTO> getAllProducts();
    ProductResponseDTO getProductById(Long id);
    ProductResponseDTO getProductByLpnCode(String lpnCode);
    ProductResponseDTO createProduct(ProductRequestDTO dto);
    ProductResponseDTO updateProduct(Long id, ProductRequestDTO dto);
    void deleteProduct(Long id);
}
