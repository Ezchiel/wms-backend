package vn.edu.hcmuaf.fit.wms.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.wms.common.ApiResponse;
import vn.edu.hcmuaf.fit.wms.dto.StorageLocationRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.StorageLocationResponseDTO;
import vn.edu.hcmuaf.fit.wms.service.StorageLocationService;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@Tag(name = "Storage Locations", description = "Các API quản lý vị trí kho")
public class StorageLocationController {

    private final StorageLocationService locationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StorageLocationResponseDTO>>> getAllLocations(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String locationType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách vị trí kho thành công",
                locationService.getAllLocations(keyword, locationType, page, size, sortBy, sortDir)
        ));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<StorageLocationResponseDTO>>> getAvailableLocations(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String locationType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách vị trí kho còn trống thành công",
                locationService.getAvailableLocations(keyword, locationType, page, size, sortBy, sortDir)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StorageLocationResponseDTO>> getLocationById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy vị trí kho thành công",
                locationService.getLocationById(id)
        ));
    }

    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<ApiResponse<StorageLocationResponseDTO>> getLocationByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy vị trí kho theo Barcode thành công",
                locationService.getLocationByBarcode(barcode)
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StorageLocationResponseDTO>> createLocation(@RequestBody StorageLocationRequestDTO location) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tạo vị trí kho thành công",
                locationService.createLocation(location)
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StorageLocationResponseDTO>> updateLocation(@PathVariable Long id, @RequestBody StorageLocationRequestDTO location) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật vị trí kho thành công",
                locationService.updateLocation(id, location)
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLocation(@PathVariable Long id) {
        locationService.deleteLocation(id);
        return ResponseEntity.ok(ApiResponse.success("Xoá vị trí kho thành công"));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<StorageLocationResponseDTO>>> createMultipleLocations(@RequestBody List<StorageLocationRequestDTO> locations) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tạo danh sách vị trí kho thành công",
                locationService.createMultipleLocations(locations)
        ));
    }
}
