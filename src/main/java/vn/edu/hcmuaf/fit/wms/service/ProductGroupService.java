package vn.edu.hcmuaf.fit.wms.service;

import vn.edu.hcmuaf.fit.wms.entity.ProductGroup;

import java.util.List;
import java.util.Optional;

public interface ProductGroupService {
    List<ProductGroup> getAllProductGroups();
    ProductGroup getProductGroupById(Long id);
    ProductGroup createProductGroup(ProductGroup productGroup);
    ProductGroup updateProductGroup(Long id, ProductGroup productGroupDetails);
    void deleteProductGroup(Long id);
}
