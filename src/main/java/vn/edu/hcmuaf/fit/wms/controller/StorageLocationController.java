package vn.edu.hcmuaf.fit.wms.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.wms.common.ApiResponse;
import vn.edu.hcmuaf.fit.wms.entity.StorageLocation;
import vn.edu.hcmuaf.fit.wms.service.StorageLocationService;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class StorageLocationController {

    private final StorageLocationService locationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StorageLocation>>> getAllLocations(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách vị trí kho thành công",
                locationService.getAllLocations(keyword, page, size, sortBy, sortDir)
        ));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<StorageLocation>>> getAvailableLocations(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách vị trí kho còn trống thành công",
                locationService.getAvailableLocations(keyword, page, size, sortBy, sortDir)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StorageLocation>> getLocationById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy vị trí kho thành công",
                locationService.getLocationById(id)
        ));
    }

    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<ApiResponse<StorageLocation>> getLocationByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy vị trí kho theo Barcode thành công",
                locationService.getLocationByBarcode(barcode)
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StorageLocation>> createLocation(@RequestBody StorageLocation location) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tạo vị trí kho thành công",
                locationService.createLocation(location)
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StorageLocation>> updateLocation(@PathVariable Long id, @RequestBody StorageLocation location) {
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
    public ResponseEntity<ApiResponse<List<StorageLocation>>> createMultipleLocations(@RequestBody List<StorageLocation> locations) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tạo danh sách vị trí kho thành công",
                locationService.createMultipleLocations(locations)
        ));
    }
}
