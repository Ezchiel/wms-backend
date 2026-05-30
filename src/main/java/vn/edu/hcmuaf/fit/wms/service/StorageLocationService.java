package vn.edu.hcmuaf.fit.wms.service;

import org.springframework.data.domain.Page;
import vn.edu.hcmuaf.fit.wms.dto.StorageLocationRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.StorageLocationResponseDTO;

import java.util.List;

public interface StorageLocationService {
    Page<StorageLocationResponseDTO> getAllLocations(String keyword, String type, int page, int size, String sortBy, String sortDir);
    Page<StorageLocationResponseDTO> getAvailableLocations(String keyword, String type, int page, int size, String sortBy, String sortDir);
    StorageLocationResponseDTO getLocationById(Long id);
    StorageLocationResponseDTO getLocationByBarcode(String barcode);
    StorageLocationResponseDTO createLocation(StorageLocationRequestDTO dto);
    StorageLocationResponseDTO updateLocation(Long id, StorageLocationRequestDTO dto);
    void deleteLocation(Long id);
    List<StorageLocationResponseDTO> createMultipleLocations(List<StorageLocationRequestDTO> dtos);
}
