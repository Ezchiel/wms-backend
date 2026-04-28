package vn.edu.hcmuaf.fit.wms.service;

import org.springframework.data.domain.Page;
import vn.edu.hcmuaf.fit.wms.entity.Partner;
import vn.edu.hcmuaf.fit.wms.entity.enums.PartnerType;

import java.util.List;

public interface PartnerService {
    Page<Partner> getAllPartners(String keyword, PartnerType type, int page, int size, String sortBy, String sortDir);
    List<Partner> getPartnersByType(PartnerType type);
    Partner getPartnerById(Long id);
    Partner createPartner(Partner partner);
    Partner updatePartner(Long id, Partner partnerDetails);
    void deletePartner(Long id);
}
