package vn.edu.hcmuaf.fit.wms.service;

import vn.edu.hcmuaf.fit.wms.dto.PutawayRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.PutawaySuggestionDTO;

public interface PutawayService {
    PutawaySuggestionDTO getSuggestion(String lpnCode);
    void confirm(PutawayRequestDTO request);
}
