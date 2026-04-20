package vn.edu.hcmuaf.fit.wms.service;

import vn.edu.hcmuaf.fit.wms.dto.IssueRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.IssueResponseDTO;

import java.util.List;

public interface InventoryIssueService {
    List<IssueResponseDTO> getAllIssues();
    IssueResponseDTO createIssue(IssueRequestDTO requestDTO);
    IssueResponseDTO confirmIssue(Long issueId);
}
