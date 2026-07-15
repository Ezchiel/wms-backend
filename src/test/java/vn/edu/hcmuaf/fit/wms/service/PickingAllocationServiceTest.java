package vn.edu.hcmuaf.fit.wms.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.hcmuaf.fit.wms.dto.AllocationResultDTO;
import vn.edu.hcmuaf.fit.wms.entity.InventoryStock;
import vn.edu.hcmuaf.fit.wms.entity.StorageLocation;
import vn.edu.hcmuaf.fit.wms.entity.enums.LocationType;
import vn.edu.hcmuaf.fit.wms.repository.InventoryStockRepository;
import vn.edu.hcmuaf.fit.wms.service.impl.PickingAllocationServiceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PickingAllocationService - Unit Tests")
class PickingAllocationServiceTest {

    @Mock
    private InventoryStockRepository stockRepository;

    @InjectMocks
    private PickingAllocationServiceImpl allocationService;

    private StorageLocation locA, locB, locC;

    @BeforeEach
    void setUp() {
        locA = buildLocation(1L, "LOC-A", 1);  // pathSequence 1 (gần cửa nhất)
        locB = buildLocation(2L, "LOC-B", 2);  // pathSequence 2
        locC = buildLocation(3L, "LOC-C", 5);  // pathSequence 5 (xa nhất)
    }

    // ===========================================================
    // Test FEFO
    // ===========================================================

    @Test
    @DisplayName("FEFO - chọn lô có expiryDate sớm nhất trước")
    void allocate_fefo_earliestExpiryFirst() {
        // locB có lô hết hạn sớm hơn (2025-01), locA có lô hết hạn muộn hơn (2025-06)
        InventoryStock stockExpireLater  = buildStock(1L, locA, "BATCH-A", LocalDate.of(2025, 6, 1), 100);
        InventoryStock stockExpireSooner = buildStock(2L, locB, "BATCH-B", LocalDate.of(2025, 1, 1), 100);

        when(stockRepository.findStockForAllocationWithLock(eq(10L), any()))
                .thenReturn(new ArrayList<>(List.of(stockExpireLater, stockExpireSooner)));

        List<AllocationResultDTO> results = allocationService.allocate(10L, 50, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getBatchNo()).isEqualTo("BATCH-B"); // lô sắp hết hạn được ưu tiên
        assertThat(results.get(0).getQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("FEFO - lô null expiryDate (không có hạn) ưu tiên cuối cùng")
    void allocate_fefo_nullExpiryLast() {
        InventoryStock stockNoExpiry  = buildStock(1L, locA, "BATCH-A", null, 100);
        InventoryStock stockWithExpiry = buildStock(2L, locB, "BATCH-B", LocalDate.of(2026, 3, 15), 100);

        when(stockRepository.findStockForAllocationWithLock(eq(10L), any()))
                .thenReturn(new ArrayList<>(List.of(stockNoExpiry, stockWithExpiry)));

        List<AllocationResultDTO> results = allocationService.allocate(10L, 50, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getBatchNo()).isEqualTo("BATCH-B"); // lô có hạn ưu tiên hơn lô null
    }

    // ===========================================================
    // Test pathSequence (proximity)
    // ===========================================================

    @Test
    @DisplayName("pathSequence - cùng expiryDate → ưu tiên vị trí gần cửa hơn")
    void allocate_sameExpiry_closerLocationFirst() {
        LocalDate sameExpiry = LocalDate.of(2026, 6, 1);
        InventoryStock stockFar   = buildStock(1L, locC, "BATCH-X", sameExpiry, 100); // pathSequence=5
        InventoryStock stockClose = buildStock(2L, locA, "BATCH-Y", sameExpiry, 100); // pathSequence=1

        when(stockRepository.findStockForAllocationWithLock(eq(10L), any()))
                .thenReturn(new ArrayList<>(List.of(stockFar, stockClose)));

        List<AllocationResultDTO> results = allocationService.allocate(10L, 50, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getLocationId()).isEqualTo(1L); // locA gần hơn
    }

    // ===========================================================
    // Test split allocation (nhiều vị trí)
    // ===========================================================

    @Test
    @DisplayName("Split - chia lấy hàng từ nhiều vị trí khi 1 vị trí không đủ")
    void allocate_split_multipleLocations() {
        InventoryStock stockFirst  = buildStock(1L, locA, "BATCH-A", LocalDate.of(2025, 3, 1), 30); // chỉ có 30
        InventoryStock stockSecond = buildStock(2L, locB, "BATCH-B", LocalDate.of(2025, 6, 1), 50); // có 50

        when(stockRepository.findStockForAllocationWithLock(eq(10L), any()))
                .thenReturn(new ArrayList<>(List.of(stockFirst, stockSecond)));

        List<AllocationResultDTO> results = allocationService.allocate(10L, 70, null);

        assertThat(results).hasSize(2);
        // Kết quả phân bổ: BATCH-A (hết hạn sớm) 30 trước + BATCH-B 40 sau
        AllocationResultDTO first = results.get(0);
        AllocationResultDTO second = results.get(1);
        assertThat(first.getBatchNo()).isEqualTo("BATCH-A");
        assertThat(first.getQuantity()).isEqualTo(30);
        assertThat(second.getBatchNo()).isEqualTo("BATCH-B");
        assertThat(second.getQuantity()).isEqualTo(40);
    }

    @Test
    @DisplayName("Vừa đủ - 1 vị trí đủ số lượng")
    void allocate_exactFit_singleLocation() {
        InventoryStock stock = buildStock(1L, locA, "BATCH-A", LocalDate.of(2025, 12, 31), 100);

        when(stockRepository.findStockForAllocationWithLock(eq(10L), any()))
                .thenReturn(new ArrayList<>(List.of(stock)));

        List<AllocationResultDTO> results = allocationService.allocate(10L, 100, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getQuantity()).isEqualTo(100);
    }

    // ===========================================================
    // Test batch filter
    // ===========================================================

    @Test
    @DisplayName("Lọc theo batchNo - chỉ phân bổ từ lô được chỉ định")
    void allocate_withBatchNoFilter_usesSpecificBatch() {
        InventoryStock stockB1 = buildStock(1L, locA, "BATCH-001", LocalDate.of(2025, 1, 1), 100);
        // stockRepository được mock để chỉ trả BATCH-001 khi batchNo="BATCH-001"
        when(stockRepository.findStockForAllocationWithLock(eq(10L), eq("BATCH-001")))
                .thenReturn(new ArrayList<>(List.of(stockB1)));

        List<AllocationResultDTO> results = allocationService.allocate(10L, 50, "BATCH-001");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getBatchNo()).isEqualTo("BATCH-001");
    }

    // ===========================================================
    // Test insufficient stock
    // ===========================================================

    @Test
    @DisplayName("Thiếu hàng - tổng tồn kho không đủ → throw RuntimeException")
    void allocate_insufficientStock_throwsException() {
        InventoryStock stock = buildStock(1L, locA, "BATCH-A", null, 10); // chỉ có 10

        when(stockRepository.findStockForAllocationWithLock(eq(10L), any()))
                .thenReturn(new ArrayList<>(List.of(stock)));

        assertThatThrownBy(() -> allocationService.allocate(10L, 100, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không đủ tồn kho");
    }

    @Test
    @DisplayName("Không có tồn kho nào → throw RuntimeException")
    void allocate_emptyStock_throwsException() {
        when(stockRepository.findStockForAllocationWithLock(eq(10L), any()))
                .thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> allocationService.allocate(10L, 1, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không đủ tồn kho");
    }

    // ===========================================================
    // Helpers
    // ===========================================================

    private StorageLocation buildLocation(Long id, String barcode, int pathSequence) {
        StorageLocation loc = new StorageLocation();
        loc.setId(id);
        loc.setBarcode(barcode);
        loc.setZone("Z1");
        loc.setRack("R1");
        loc.setShelf("S1");
        loc.setLocationType(LocationType.STORAGE);
        loc.setPathSequence(pathSequence);
        return loc;
    }

    private InventoryStock buildStock(Long id, StorageLocation location, String batchNo,
                                      LocalDate expiryDate, int quantity) {
        InventoryStock s = new InventoryStock();
        s.setId(id);
        s.setLocation(location);
        s.setBatchNo(batchNo);
        s.setExpiryDate(expiryDate);
        s.setQuantity(quantity);
        return s;
    }
}
