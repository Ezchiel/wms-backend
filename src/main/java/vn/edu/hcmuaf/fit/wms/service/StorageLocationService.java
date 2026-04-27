package vn.edu.hcmuaf.fit.wms.service;

import org.springframework.data.domain.Page;
import vn.edu.hcmuaf.fit.wms.entity.StorageLocation;

import java.util.List;

public interface StorageLocationService {
    Page<StorageLocation> getAllLocations(String keyword, int page, int size, String sortBy, String sortDir);
    Page<StorageLocation> getAvailableLocations(String keyword, int page, int size, String sortBy, String sortDir);
    StorageLocation getLocationById(Long id);
    StorageLocation getLocationByBarcode(String barcode);
    StorageLocation createLocation(StorageLocation location);
    StorageLocation updateLocation(Long id, StorageLocation locationDetails);
    void deleteLocation(Long id);
    List<StorageLocation> createMultipleLocations(List<StorageLocation> locations);
}
