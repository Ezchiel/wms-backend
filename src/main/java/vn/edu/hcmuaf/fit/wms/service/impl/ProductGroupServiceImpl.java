package vn.edu.hcmuaf.fit.wms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.edu.hcmuaf.fit.wms.entity.ProductGroup;
import vn.edu.hcmuaf.fit.wms.repository.ProductGroupRepository;
import vn.edu.hcmuaf.fit.wms.service.ProductGroupService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductGroupServiceImpl implements ProductGroupService {

    private final ProductGroupRepository productGroupRepository;

    @Override
    public Page<ProductGroup> getAllProductGroups(String keyword, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page - 1, size, sort);

        return productGroupRepository.searchProductGroups(keyword, pageable);
    }

    @Override
    public List<ProductGroup> getAllProductGroups() {
        return productGroupRepository.findAll();
    }

    @Override
    public ProductGroup getProductGroupById(Long id) {
        ProductGroup productGroup = productGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm sản phẩm với ID: " + id));
        return productGroup;
    }

    @Override
    public ProductGroup createProductGroup(ProductGroup productGroup) {
        if (productGroupRepository.existsByGroupCode(productGroup.getGroupCode())) {
            throw new RuntimeException("Mã nhóm sản phẩm đã tồn tại!");
        }
        return productGroupRepository.save(productGroup);
    }

    @Override
    public ProductGroup updateProductGroup(Long id, ProductGroup productGroupDetails) {
        ProductGroup group = productGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm sản phẩm"));

        group.setGroupCode(productGroupDetails.getGroupCode());
        group.setGroupName(productGroupDetails.getGroupName());
        group.setDescription(productGroupDetails.getDescription());

        return productGroupRepository.save(group);
    }

    @Override
    public void deleteProductGroup(Long id) {
        productGroupRepository.deleteById(id);
    }
}
