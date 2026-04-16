package vn.edu.hcmuaf.fit.wms.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.wms.common.ApiResponse;
import vn.edu.hcmuaf.fit.wms.entity.Partner;
import vn.edu.hcmuaf.fit.wms.entity.PartnerType;
import vn.edu.hcmuaf.fit.wms.service.PartnerService;

import java.util.List;

@RestController
@RequestMapping("/api/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Partner>>> getAllPartners() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách đối tác thành công",
                partnerService.getAllPartners()
        ));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<Partner>>> getPartnersByType(@PathVariable PartnerType type) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách đối tác theo loại thành công",
                partnerService.getPartnersByType(type)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Partner>> getPartnerById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy đối tác thành công",
                partnerService.getPartnerById(id)
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Partner>> createPartner(@RequestBody Partner partner) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tạo đối tác thành công",
                partnerService.createPartner(partner)
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Partner>> updatePartner(@PathVariable Long id, @RequestBody Partner partnerDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật đối tác thành công",
                partnerService.updatePartner(id, partnerDetails)
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePartner(@PathVariable Long id) {
        partnerService.deletePartner(id);
        return ResponseEntity.ok(ApiResponse.success("Xoá đối tác thành công"));
    }
}
