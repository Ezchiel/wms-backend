package vn.edu.hcmuaf.fit.wms.service;

import org.springframework.data.domain.Page;
import vn.edu.hcmuaf.fit.wms.entity.ProductGroup;

import java.util.List;

public interface ProductGroupService {
    Page<ProductGroup> getAllProductGroups(String keyword, int page, int size, String sortBy, String sortDir);
    List<ProductGroup> getAllProductGroups();
    ProductGroup getProductGroupById(Long id);
    ProductGroup createProductGroup(ProductGroup productGroup);
    ProductGroup updateProductGroup(Long id, ProductGroup productGroupDetails);
    void deleteProductGroup(Long id);
}
