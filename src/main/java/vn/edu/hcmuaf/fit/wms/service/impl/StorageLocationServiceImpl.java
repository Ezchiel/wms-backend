package vn.edu.hcmuaf.fit.wms.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hcmuaf.fit.wms.dto.StorageLocationRequestDTO;
import vn.edu.hcmuaf.fit.wms.dto.StorageLocationResponseDTO;
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
    public Page<StorageLocationResponseDTO> getAllLocations(String keyword, String type, int page, int size, String sortBy, String sortDir) {
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

        return locationRepository.searchLocations(keyword, false, locType, pageable)
                .map(StorageLocationResponseDTO::fromEntity);
    }

    @Override
    public Page<StorageLocationResponseDTO> getAvailableLocations(String keyword, String type, int page, int size, String sortBy, String sortDir) {
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

        return locationRepository.searchLocations(keyword, true, locType, pageable)
                .map(StorageLocationResponseDTO::fromEntity);
    }

    @Override
    public StorageLocationResponseDTO getLocationById(Long id) {
        StorageLocation location = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vị trí lưu trữ với ID: " + id));
        return StorageLocationResponseDTO.fromEntity(location);
    }

    @Override
    public StorageLocationResponseDTO getLocationByBarcode(String barcode) {
        StorageLocation location = locationRepository.findByBarcode(barcode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vị trí với mã vạch: " + barcode));
        return StorageLocationResponseDTO.fromEntity(location);
    }

    @Override
    public StorageLocationResponseDTO createLocation(StorageLocationRequestDTO dto) {
        if (locationRepository.existsByBarcode(dto.barcode())) {
            throw new RuntimeException("Mã vạch vị trí đã tồn tại!");
        }
        StorageLocation location = dto.toEntity();
        return StorageLocationResponseDTO.fromEntity(locationRepository.save(location));
    }

    @Override
    public StorageLocationResponseDTO updateLocation(Long id, StorageLocationRequestDTO dto) {
        StorageLocation existingLocation = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vị trí lưu trữ với ID: " + id));

        existingLocation.setZone(dto.zone());
        existingLocation.setRack(dto.rack());
        existingLocation.setShelf(dto.shelf());
        existingLocation.setDescription(dto.description());
        existingLocation.setFull(dto.isFull());

        if (dto.locationType() != null) {
            existingLocation.setLocationType(dto.locationType());
        }

        if (!existingLocation.getBarcode().equals(dto.barcode())
                && locationRepository.existsByBarcode(dto.barcode())) {
            throw new RuntimeException("Mã vạch vị trí đã tồn tại!");
        }
        existingLocation.setBarcode(dto.barcode());

        return StorageLocationResponseDTO.fromEntity(locationRepository.save(existingLocation));
    }

    @Override
    public void deleteLocation(Long id) {
        StorageLocation existingLocation = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vị trí lưu trữ với ID: " + id));
        locationRepository.delete(existingLocation);
    }

    @Override
    @Transactional
    public List<StorageLocationResponseDTO> createMultipleLocations(List<StorageLocationRequestDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            throw new RuntimeException("Danh sách vị trí trống!");
        }

        // Check duplicate barcode in location list
        Set<String> uniqueBarcodes = new HashSet<>();
        for (StorageLocationRequestDTO dto : dtos) {
            if (!uniqueBarcodes.add(dto.barcode())) {
                throw new RuntimeException("Lỗi: File import chứa mã vạch trùng lặp (" + dto.barcode() + ")");
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

        // Save all and map to DTO
        List<StorageLocation> locations = dtos.stream().map(StorageLocationRequestDTO::toEntity).toList();
        return locationRepository.saveAll(locations)
                .stream()
                .map(StorageLocationResponseDTO::fromEntity)
                .toList();
    }
}
