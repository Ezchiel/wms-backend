package vn.edu.hcmuaf.fit.wms.service;

import vn.edu.hcmuaf.fit.wms.dto.CheckRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.CheckResponseDTO;

import java.util.List;

public interface InventoryCheckService {
    List<CheckResponseDTO> getAllChecks();
    CheckResponseDTO createCheck(CheckRequestDTO requestDTO);
    CheckResponseDTO confirmCheck(Long checkId);
}
