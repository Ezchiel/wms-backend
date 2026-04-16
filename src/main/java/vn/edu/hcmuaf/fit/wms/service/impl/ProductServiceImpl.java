package vn.edu.hcmuaf.fit.wms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.hcmuaf.fit.wms.dto.ProductRequestDTO;
import vn.edu.hcmuaf.fit.wms.entity.Product;
import vn.edu.hcmuaf.fit.wms.entity.ProductGroup;
import vn.edu.hcmuaf.fit.wms.repository.ProductGroupRepository;
import vn.edu.hcmuaf.fit.wms.repository.ProductRepository;
import vn.edu.hcmuaf.fit.wms.service.ProductService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductGroupRepository productGroupRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
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

        ProductGroup group = productGroupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Nhóm sản phẩm với ID: " + dto.getGroupId()));

        product.setProductGroup(group);

        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
