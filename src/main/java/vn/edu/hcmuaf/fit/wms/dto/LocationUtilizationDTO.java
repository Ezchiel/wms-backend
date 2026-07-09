package vn.edu.hcmuaf.fit.wms.dto;

public record LocationUtilizationDTO(
        String zone,
        int totalLocations,
        int fullLocations,
        int emptyLocations,
        long totalQuantity,
        long totalCapacity,
        double utilizationRate
) {}
