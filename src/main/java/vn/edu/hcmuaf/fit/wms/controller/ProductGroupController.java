package vn.edu.hcmuaf.fit.wms.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.wms.common.ApiResponse;
import vn.edu.hcmuaf.fit.wms.entity.ProductGroup;
import vn.edu.hcmuaf.fit.wms.service.ProductGroupService;

import java.util.List;

@RestController
@RequestMapping("/api/product-groups")
@RequiredArgsConstructor
@Tag(name = "Product Groups", description = "Các API quản lý nhóm sản phẩm")
public class ProductGroupController {

    private final ProductGroupService productGroupService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductGroup>>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách nhóm sản phẩm thành công",
                productGroupService.getAllProductGroups(keyword, page, size, sortBy, sortDir)
        ));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ProductGroup>>> getAllWithoutPagination() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách tất cả nhóm sản phẩm thành công",
                productGroupService.getAllProductGroups()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductGroup>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy nhóm sản phẩm thành công",
                productGroupService.getProductGroupById(id)
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductGroup>> create(@RequestBody ProductGroup productGroup) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tạo nhóm sản phẩm thành công",
                productGroupService.createProductGroup(productGroup)
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductGroup>> update(@PathVariable Long id, @RequestBody ProductGroup productGroup) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật nhóm sản phẩm thành công",
                productGroupService.updateProductGroup(id, productGroup)
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        productGroupService.deleteProductGroup(id);
        return ResponseEntity.ok(ApiResponse.success("Xoá nhóm sản phẩm thành công"));
    }
}
