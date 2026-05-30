package vn.edu.hcmuaf.fit.wms.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.wms.common.ApiResponse;
import vn.edu.hcmuaf.fit.wms.dto.ProductRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.ProductResponseDTO;
import vn.edu.hcmuaf.fit.wms.service.ProductService;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponseDTO>>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách sản phẩm thành công",
                service.getAllProducts(keyword, page, size, sortBy, sortDir)
        ));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ProductResponseDTO>>> getAllWithoutPagination() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách tất cả sản phẩm thành công",
                service.getAllProducts()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy sản phẩm thành công",
                service.getProductById(id)
        ));
    }

    @GetMapping("/lpncode/{lpnCode}")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> getByLpnCode(@PathVariable String lpnCode) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy sản phẩm theo LPN code thành công",
                service.getProductByLpnCode(lpnCode)
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponseDTO>> create(@RequestBody ProductRequestDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tạo sản phẩm thành công",
                service.createProduct(dto)
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDTO>> update(@PathVariable Long id, @RequestBody ProductRequestDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật sản phẩm thành công",
                service.updateProduct(id, dto)
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Xoá sản phẩm thành công"));
    }
}
