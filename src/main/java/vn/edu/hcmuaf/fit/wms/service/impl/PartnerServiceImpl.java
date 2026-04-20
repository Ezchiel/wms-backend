package vn.edu.hcmuaf.fit.wms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.hcmuaf.fit.wms.entity.Partner;
import vn.edu.hcmuaf.fit.wms.entity.enums.PartnerType;
import vn.edu.hcmuaf.fit.wms.repository.PartnerRepository;
import vn.edu.hcmuaf.fit.wms.service.PartnerService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PartnerServiceImpl implements PartnerService {

    private final PartnerRepository partnerRepository;

    public List<Partner> getAllPartners() {
        return partnerRepository.findAll();
    }

    public List<Partner> getPartnersByType(PartnerType type) {
        return partnerRepository.findByType(type);
    }

    public Partner getPartnerById(Long id) {
        return partnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đối tác với ID: " + id));
    }

    public Partner createPartner(Partner partner) {
        if (partnerRepository.existsByPhone(partner.getPhone())) {
            throw new RuntimeException("Số điện thoại đã tồn tại!");
        }
        return partnerRepository.save(partner);
    }

    public Partner updatePartner(Long id, Partner partnerDetails) {
        Partner existingPartner = getPartnerById(id);

        existingPartner.setName(partnerDetails.getName());
        existingPartner.setType(partnerDetails.getType());
        existingPartner.setPhone(partnerDetails.getPhone());
        existingPartner.setEmail(partnerDetails.getEmail());
        existingPartner.setAddress(partnerDetails.getAddress());
        existingPartner.setTaxCode(partnerDetails.getTaxCode());

        return partnerRepository.save(existingPartner);
    }

    public void deletePartner(Long id) {
        Partner existingPartner = getPartnerById(id);
        partnerRepository.delete(existingPartner);
    }
}
