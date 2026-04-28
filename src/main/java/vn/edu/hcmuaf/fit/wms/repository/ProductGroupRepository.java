package vn.edu.hcmuaf.fit.wms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.hcmuaf.fit.wms.entity.ProductGroup;

@Repository
public interface ProductGroupRepository extends JpaRepository<ProductGroup, Long> {
    boolean existsByGroupCode(String groupCode);

    @Query("SELECT pg FROM ProductGroup pg WHERE " +
            ":keyword IS NULL OR LOWER(pg.groupCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(pg.groupName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(pg.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<ProductGroup> searchProductGroups(@Param("keyword") String keyword, Pageable pageable);
}
