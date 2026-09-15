package com.clothingretail.masterdata.controller;

import com.clothingretail.masterdata.dto.AvailableSizeResponse;
import com.clothingretail.masterdata.service.SizeAvailabilityService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kept out of PublicMasterDataController (which stays a flat list of "here's every master's
 * plain listing") since this one endpoint has real logic behind it - see
 * SizeAvailabilityService.
 */
@RestController
public class PublicSizeAvailabilityController {

    private final SizeAvailabilityService sizeAvailabilityService;

    public PublicSizeAvailabilityController(SizeAvailabilityService sizeAvailabilityService) {
        this.sizeAvailabilityService = sizeAvailabilityService;
    }

    @GetMapping("/api/categories/{categoryId}/available-sizes")
    public List<AvailableSizeResponse> availableSizes(@PathVariable Long categoryId) {
        return sizeAvailabilityService.availableSizesForCategory(categoryId);
    }
}
