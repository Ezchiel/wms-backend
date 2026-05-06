package vn.edu.hcmuaf.fit.wms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hcmuaf.fit.wms.entity.StorageLocation;
import vn.edu.hcmuaf.fit.wms.entity.enums.LocationType;
import vn.edu.hcmuaf.fit.wms.repository.StorageLocationRepository;
import vn.edu.hcmuaf.fit.wms.service.StorageLocationService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StorageLocationServiceImpl implements StorageLocationService {

    private final StorageLocationRepository locationRepository;

    @Override
    public Page<StorageLocation> getAllLocations(String keyword, String type, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        // Convert type from String to Enum
        LocationType locType = null;
        if (type != null && !type.trim().isEmpty()) {
            try {
                locType = LocationType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Loại vị trí không hợp lệ");
            }
        }

        return locationRepository.searchLocations(keyword, false, locType, pageable);
    }

    @Override
    public Page<StorageLocation> getAvailableLocations(String keyword, String type, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        // Convert type from String to Enum
        LocationType locType = null;
        if (type != null && !type.trim().isEmpty()) {
            try {
                locType = LocationType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Loại vị trí không hợp lệ");
            }
        }

        return locationRepository.searchLocations(keyword, true, locType, pageable);
    }

    @Override
    public StorageLocation getLocationById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vị trí lưu trữ với ID: " + id));
    }

    @Override
    public StorageLocation getLocationByBarcode(String barcode) {
        return locationRepository.findByBarcode(barcode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vị trí với mã vạch: " + barcode));
    }

    @Override
    public StorageLocation createLocation(StorageLocation location) {
        if (locationRepository.existsByBarcode(location.getBarcode())) {
            throw new RuntimeException("Mã vạch vị trí đã tồn tại!");
        }
        return locationRepository.save(location);
    }

    @Override
    public StorageLocation updateLocation(Long id, StorageLocation locationDetails) {
        StorageLocation existingLocation = getLocationById(id);

        existingLocation.setZone(locationDetails.getZone());
        existingLocation.setRack(locationDetails.getRack());
        existingLocation.setShelf(locationDetails.getShelf());
        existingLocation.setDescription(locationDetails.getDescription());
        existingLocation.setFull(locationDetails.isFull());

        if(locationDetails.getLocationType() != null) {
            existingLocation.setLocationType(locationDetails.getLocationType());
        }

        if (!existingLocation.getBarcode().equals(locationDetails.getBarcode())
                && locationRepository.existsByBarcode(locationDetails.getBarcode())) {
            throw new RuntimeException("Mã vạch vị trí đã tồn tại!");
        }
        existingLocation.setBarcode(locationDetails.getBarcode());

        return locationRepository.save(existingLocation);
    }

    @Override
    public void deleteLocation(Long id) {
        StorageLocation existingLocation = getLocationById(id);
        locationRepository.delete(existingLocation);
    }

    @Override
    @Transactional
    public List<StorageLocation> createMultipleLocations(List<StorageLocation> locations) {
        if (locations == null || locations.isEmpty()) {
            throw new RuntimeException("Danh sách vị trí trống!");
        }

        // Check duplicate barcode in location list
        Set<String> uniqueBarcodes = new HashSet<>();
        for (StorageLocation loc : locations) {
            if (!uniqueBarcodes.add(loc.getBarcode())) {
                throw new RuntimeException("Lỗi: File import chứa mã vạch trùng lặp (" + loc.getBarcode() + ")");
            }
        }

        // Check duplicate in Database
        List<StorageLocation> existingLocations = locationRepository.findByBarcodeIn(new ArrayList<>(uniqueBarcodes));
        if (!existingLocations.isEmpty()) {
            String duplicateBarcodes = existingLocations.stream()
                    .map(StorageLocation::getBarcode)
                    .collect(Collectors.joining(", "));
            throw new RuntimeException("Lỗi: Các mã vạch sau đã tồn tại trong hệ thống: " + duplicateBarcodes);
        }

        // Save all
        return locationRepository.saveAll(locations);
    }
}
