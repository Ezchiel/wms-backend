package vn.edu.hcmuaf.fit.wms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.edu.hcmuaf.fit.wms.dto.ProductRequestDTO;
import vn.edu.hcmuaf.fit.wms.entity.Product;
import vn.edu.hcmuaf.fit.wms.entity.ProductGroup;
import vn.edu.hcmuaf.fit.wms.repository.ProductGroupRepository;
import vn.edu.hcmuaf.fit.wms.repository.ProductRepository;
import vn.edu.hcmuaf.fit.wms.service.ProductService;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductGroupRepository productGroupRepository;

    public Page<Product> getAllProducts(String keyword, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page - 1, size, sort);

        return productRepository.searchProducts(keyword, pageable);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
    }

    public Product createProduct(ProductRequestDTO dto) {
        if (productRepository.existsByProductCode(dto.getProductCode())) {
            throw new RuntimeException("Mã sản phẩm đã tồn tại!");
        }

        ProductGroup group = productGroupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Nhóm sản phẩm với ID: " + dto.getGroupId()));

        Product product = new Product();
        product.setProductCode(dto.getProductCode());
        product.setProductName(dto.getProductName());
        product.setUnit(dto.getUnit());
        product.setDescription(dto.getDescription());
        product.setMinStockLevel(dto.getMinStockLevel());
        product.setProductGroup(group);

        return productRepository.save(product);
    }

    @Override
    public Product updateProduct(Long id, ProductRequestDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));

        product.setProductCode(dto.getProductCode());
        product.setProductName(dto.getProductName());
        product.setUnit(dto.getUnit());
        product.setDescription(dto.getDescription());
        product.setMinStockLevel(dto.getMinStockLevel());

        ProductGroup group = productGroupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Nhóm sản phẩm với ID: " + dto.getGroupId()));

        product.setProductGroup(group);

        return productRepository.save(product);
    }

    @Override
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
