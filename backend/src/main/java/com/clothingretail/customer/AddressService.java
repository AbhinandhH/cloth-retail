package com.clothingretail.customer;

import com.clothingretail.common.NotFoundException;
import com.clothingretail.customer.dto.AddressRequest;
import com.clothingretail.customer.dto.AddressResponse;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD for a customer's own saved addresses, always scoped to the caller's {@link CustomerProfile}
 * (resolved from the authenticated user id - see {@code CustomerAddressController}). Every
 * operation on a specific address id re-verifies ownership; a mismatch is reported as a plain 404
 * (never leaking whether the address exists for someone else).
 */
@Service
@Log4j2
public class AddressService {

    private final AddressRepository addressRepository;
    private final CustomerProfileRepository customerProfileRepository;

    public AddressService(AddressRepository addressRepository, CustomerProfileRepository customerProfileRepository) {
        this.addressRepository = addressRepository;
        this.customerProfileRepository = customerProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(Long userId) {
        log.info("[1200] List addresses userId={}", userId);
        CustomerProfile profile = resolveProfile(userId);
        List<AddressResponse> addresses = addressRepository.findByCustomerProfileId(profile.getId()).stream().map(this::toResponse).toList();
        log.info("[1201] Addresses listed userId={} profileId={} count={}", userId, profile.getId(), addresses.size());
        return addresses;
    }

    @Transactional
    public AddressResponse create(Long userId, AddressRequest request) {
        log.info("[1202] Create address userId={} label={}", userId, request.label());
        CustomerProfile profile = resolveProfile(userId);
        if (Boolean.TRUE.equals(request.isDefault())) {
            log.info("[1203] Clearing previous default address profileId={}", profile.getId());
            addressRepository.clearDefault(profile.getId());
        }
        Address address = new Address();
        address.setCustomerProfile(profile);
        applyRequest(address, request);
        AddressResponse response = toResponse(addressRepository.save(address));
        log.info("[1204] Address created addressId={} profileId={}", response.id(), profile.getId());
        return response;
    }

    @Transactional
    public AddressResponse update(Long userId, Long addressId, AddressRequest request) {
        log.info("[1205] Update address userId={} addressId={}", userId, addressId);
        CustomerProfile profile = resolveProfile(userId);
        Address address = findOwned(profile.getId(), addressId);
        if (Boolean.TRUE.equals(request.isDefault())) {
            log.info("[1206] Clearing previous default address profileId={}", profile.getId());
            addressRepository.clearDefault(profile.getId());
        }
        applyRequest(address, request);
        AddressResponse response = toResponse(addressRepository.save(address));
        log.info("[1207] Address updated addressId={} profileId={}", addressId, profile.getId());
        return response;
    }

    @Transactional
    public void delete(Long userId, Long addressId) {
        log.info("[1208] Delete address userId={} addressId={}", userId, addressId);
        CustomerProfile profile = resolveProfile(userId);
        Address address = findOwned(profile.getId(), addressId);
        addressRepository.delete(address);
        log.info("[1209] Address deleted addressId={} profileId={}", addressId, profile.getId());
    }

    private Address findOwned(Long profileId, Long addressId) {
        return addressRepository.findByIdAndCustomerProfileId(addressId, profileId)
                .orElseThrow(() -> {
                    log.error("[1210] Address not found addressId={} profileId={}", addressId, profileId);
                    return new NotFoundException("Address not found: " + addressId);
                });
    }

    private void applyRequest(Address address, AddressRequest request) {
        address.setLabel(request.label());
        address.setAddressLine1(request.addressLine1());
        address.setAddressLine2(request.addressLine2());
        address.setCity(request.city());
        address.setState(request.state());
        address.setPostalCode(request.postalCode());
        address.setCountry(request.country());
        address.setDefault(Boolean.TRUE.equals(request.isDefault()));
    }

    private CustomerProfile resolveProfile(Long userId) {
        CustomerProfile profile = customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("[1211] No customer profile for userId={}", userId);
                    return new NotFoundException("Customer profile not found for user: " + userId);
                });
        log.info("[1212] Resolved customer profile userId={} profileId={}", userId, profile.getId());
        return profile;
    }

    private AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getLabel(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry(),
                address.isDefault());
    }
}
