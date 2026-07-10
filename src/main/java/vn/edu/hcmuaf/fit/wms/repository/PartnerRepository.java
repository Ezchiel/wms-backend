package vn.edu.hcmuaf.fit.wms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.wms.entity.Partner;
import vn.edu.hcmuaf.fit.wms.entity.enums.PartnerType;

import java.util.List;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, Long> {
    List<Partner> findByType(PartnerType type);
    List<Partner> findByTypeOrderByName(PartnerType type);
    boolean existsByPhone(String phone);

    @Query("SELECT p FROM Partner p WHERE " +
            "(:type IS NULL OR p.type = :type) AND (" +
            ":keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Partner> searchPartners(@Param("keyword") String keyword, @Param("type") PartnerType type, Pageable pageable);
}
