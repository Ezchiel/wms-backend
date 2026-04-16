package vn.edu.hcmuaf.fit.wms.service;

import vn.edu.hcmuaf.fit.wms.entity.StorageLocation;

import java.util.List;

public interface StorageLocationService {
    List<StorageLocation> getAllLocations();
    StorageLocation getLocationById(Long id);
    StorageLocation getLocationByBarcode(String barcode);
    StorageLocation createLocation(StorageLocation location);
    StorageLocation updateLocation(Long id, StorageLocation locationDetails);
    void deleteLocation(Long id);
}
