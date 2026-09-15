package com.clothingretail.customer.controller;

import com.clothingretail.customer.dto.AddressRequest;
import com.clothingretail.customer.dto.AddressResponse;
import com.clothingretail.customer.service.AddressService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/addresses")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerAddressController {

    private final AddressService addressService;

    public CustomerAddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public List<AddressResponse> list(Authentication authentication) {
        return addressService.list(userId(authentication));
    }

    @PostMapping
    public AddressResponse create(Authentication authentication, @Valid @RequestBody AddressRequest request) {
        return addressService.create(userId(authentication), request);
    }

    @PutMapping("/{id}")
    public AddressResponse update(
            Authentication authentication, @PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        return addressService.update(userId(authentication), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        addressService.delete(userId(authentication), id);
        return ResponseEntity.noContent().build();
    }

    private Long userId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
