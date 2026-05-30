package vn.edu.hcmuaf.fit.wms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.edu.hcmuaf.fit.wms.dto.ProductRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.ProductResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.Lpn;
import vn.edu.hcmuaf.fit.wms.entity.Product;
import vn.edu.hcmuaf.fit.wms.entity.ProductGroup;
import vn.edu.hcmuaf.fit.wms.repository.LpnRepository;
import vn.edu.hcmuaf.fit.wms.repository.ProductGroupRepository;
import vn.edu.hcmuaf.fit.wms.repository.ProductRepository;
import vn.edu.hcmuaf.fit.wms.service.ProductService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductGroupRepository productGroupRepository;
    private final LpnRepository lpnRepository;

    @Override
    public Page<ProductResponseDTO> getAllProducts(String keyword, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page - 1, size, sort);

        return productRepository.searchProducts(keyword, pageable)
                .map(ProductResponseDTO::fromEntity);
    }

    @Override
    public List<ProductResponseDTO> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(ProductResponseDTO::fromEntity)
                .toList();
    }

    @Override
    public ProductResponseDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
        return ProductResponseDTO.fromEntity(product);
    }

    @Override
    public ProductResponseDTO getProductByLpnCode(String lpnCode) {
        Lpn lpn = lpnRepository.findByLpnCode(lpnCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin với LpnCode: " + lpnCode));
        return ProductResponseDTO.fromEntityWithBatch(lpn.getProduct(), lpn.getBatchNo());
    }


    @Override
    public ProductResponseDTO createProduct(ProductRequestDTO dto) {
        if (productRepository.existsByProductCode(dto.productCode())) {
            throw new RuntimeException("Mã sản phẩm đã tồn tại!");
        }

        ProductGroup group = productGroupRepository.findById(dto.groupId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Nhóm sản phẩm với ID: " + dto.groupId()));

        Product product = new Product();
        product.setProductCode(dto.productCode());
        product.setProductName(dto.productName());
        product.setUnit(dto.unit());
        product.setDescription(dto.description());
        product.setMinStockLevel(dto.minStockLevel());
        product.setProductGroup(group);

        return ProductResponseDTO.fromEntity(productRepository.save(product));
    }

    @Override
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));

        product.setProductCode(dto.productCode());
        product.setProductName(dto.productName());
        product.setUnit(dto.unit());
        product.setDescription(dto.description());
        product.setMinStockLevel(dto.minStockLevel());

        ProductGroup group = productGroupRepository.findById(dto.groupId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Nhóm sản phẩm với ID: " + dto.groupId()));

        product.setProductGroup(group);

        return ProductResponseDTO.fromEntity(productRepository.save(product));
    }

    @Override
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
