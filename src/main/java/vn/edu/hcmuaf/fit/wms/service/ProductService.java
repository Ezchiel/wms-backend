package vn.edu.hcmuaf.fit.wms.service;

import org.springframework.data.domain.Page;
import vn.edu.hcmuaf.fit.wms.dto.ProductRequestDTO;
import vn.edu.hcmuaf.fit.wms.entity.Product;

public interface ProductService {
    Page<Product> getAllProducts(String keyword, int page, int size, String sortBy, String sortDir);
    Product getProductById(Long id);
    Product createProduct(ProductRequestDTO dto);
    Product updateProduct(Long id, ProductRequestDTO dto);
    void deleteProduct(Long id);
}
