package vn.edu.hcmuaf.fit.wms.service;

import org.springframework.data.domain.Page;
import vn.edu.hcmuaf.fit.wms.dto.CheckRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.CheckResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.enums.CheckStatus;

import java.time.LocalDate;

public interface InventoryCheckService {
    Page<CheckResponseDTO> getAllChecks(String keyword, CheckStatus status, Boolean createdByMe,
            LocalDate fromDate, LocalDate toDate, int page, int size, String sortBy, String sortDir);

    CheckResponseDTO getCheckById(Long id);

    CheckResponseDTO createCheck(CheckRequestDTO requestDTO);

    CheckResponseDTO confirmCheck(Long checkId);

    CheckResponseDTO cancelCheck(Long checkId);
}
