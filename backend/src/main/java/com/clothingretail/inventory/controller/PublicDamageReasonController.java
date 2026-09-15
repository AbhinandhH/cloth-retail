package com.clothingretail.inventory.controller;

import com.clothingretail.inventory.dto.DamageReasonResponse;
import com.clothingretail.inventory.service.DamageReasonService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, read-only, active-only lookup used by the admin damage-recording modal (see
 * PublicMasterDataController's class comment for why this follows the same
 * public/minimal-shape convention as /api/colors etc. even though only an admin screen
 * consumes it - no need to gate it behind auth).
 */
@RestController
public class PublicDamageReasonController {

    private final DamageReasonService damageReasonService;

    public PublicDamageReasonController(DamageReasonService damageReasonService) {
        this.damageReasonService = damageReasonService;
    }

    @GetMapping("/api/damage-reasons")
    public List<DamageReasonResponse> damageReasons() {
        return damageReasonService.listPublic();
    }
}
