package vn.edu.hcmuaf.fit.wms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.hcmuaf.fit.wms.entity.StorageLocation;
import vn.edu.hcmuaf.fit.wms.repository.StorageLocationRepository;
import vn.edu.hcmuaf.fit.wms.service.StorageLocationService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StorageLocationServiceImpl implements StorageLocationService {
    private final StorageLocationRepository locationRepository;

    public List<StorageLocation> getAllLocations() {
        return locationRepository.findAll();
    }

    public StorageLocation getLocationById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vị trí lưu trữ với ID: " + id));
    }

    public StorageLocation getLocationByBarcode(String barcode) {
        return locationRepository.findByBarcode(barcode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vị trí với mã vạch: " + barcode));
    }

    public StorageLocation createLocation(StorageLocation location) {
        if (locationRepository.existsByBarcode(location.getBarcode())) {
            throw new RuntimeException("Mã vạch vị trí đã tồn tại!");
        }
        return locationRepository.save(location);
    }

    public StorageLocation updateLocation(Long id, StorageLocation locationDetails) {
        StorageLocation existingLocation = getLocationById(id);

        existingLocation.setZone(locationDetails.getZone());
        existingLocation.setRack(locationDetails.getRack());
        existingLocation.setShelf(locationDetails.getShelf());
        existingLocation.setDescription(locationDetails.getDescription());
        existingLocation.setFull(locationDetails.isFull());

        if (!existingLocation.getBarcode().equals(locationDetails.getBarcode())
                && locationRepository.existsByBarcode(locationDetails.getBarcode())) {
            throw new RuntimeException("Mã vạch vị trí đã tồn tại!");
        }
        existingLocation.setBarcode(locationDetails.getBarcode());

        return locationRepository.save(existingLocation);
    }

    public void deleteLocation(Long id) {
        StorageLocation existingLocation = getLocationById(id);
        locationRepository.delete(existingLocation);
    }
}
