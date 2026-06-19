package vn.edu.hcmuaf.fit.wms.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmuaf.fit.wms.common.ApiResponse;
import vn.edu.hcmuaf.fit.wms.dto.PutawayRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.PutawaySuggestionDTO;
import vn.edu.hcmuaf.fit.wms.service.PutawayService;

@RestController
@RequestMapping("/api/putaway")
@RequiredArgsConstructor
@Tag(name = "Putaway", description = "Các API thao tác cất hàng nhập kho")
public class PutawayController {
    private final PutawayService putawayService;

    @GetMapping("/suggest/{lpnCode}")
    public ResponseEntity<ApiResponse<PutawaySuggestionDTO>> suggestLocation(@PathVariable String lpnCode) {
        return ResponseEntity.ok(ApiResponse.success("Đã tìm thấy vị trí gợi ý",
                putawayService.getSuggestion(lpnCode)));
    }

    @Deprecated
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPutaway(@RequestBody PutawayRequestDTO request) {
        putawayService.confirm(request);
        return ResponseEntity.ok(ApiResponse.success("Cất hàng thành công"));
    }
}
