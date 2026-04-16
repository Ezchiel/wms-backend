package vn.edu.hcmuaf.fit.wms.service;

import vn.edu.hcmuaf.fit.wms.entity.Partner;
import vn.edu.hcmuaf.fit.wms.entity.PartnerType;

import java.util.List;

public interface PartnerService {
    List<Partner> getAllPartners();
    List<Partner> getPartnersByType(PartnerType type);
    Partner getPartnerById(Long id);
    Partner createPartner(Partner partner);
    Partner updatePartner(Long id, Partner partnerDetails);
    void deletePartner(Long id);
}
