package vn.edu.hcmuaf.fit.wms.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.hcmuaf.fit.wms.entity.enums.LpnStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lpns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lpn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lpn_code", unique = true, nullable = false, length = 50)
    private String lpnCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private InventoryReceipt receipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_detail_id", nullable = false)
    private InventoryReceiptDetail receiptDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "batch_no", length = 50)
    private String batchNo;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "serial_number", length = 100, unique = true)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LpnStatus status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}