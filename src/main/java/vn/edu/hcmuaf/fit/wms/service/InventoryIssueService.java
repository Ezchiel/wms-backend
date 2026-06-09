package vn.edu.hcmuaf.fit.wms.service;

import org.springframework.data.domain.Page;
import vn.edu.hcmuaf.fit.wms.dto.IssueRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.IssueResponseDTO;
import vn.edu.hcmuaf.fit.wms.entity.enums.IssueStatus;

public interface InventoryIssueService {

    Page<IssueResponseDTO> getAllIssues(String keyword, IssueStatus status,
                                        int page, int size, String sortBy, String sortDir);

    IssueResponseDTO getIssueById(Long id);

    IssueResponseDTO createIssue(IssueRequestDTO requestDTO);

    IssueResponseDTO approveIssue(Long issueId);

    IssueResponseDTO confirmIssue(Long issueId);

    IssueResponseDTO cancelIssue(Long issueId);
}
