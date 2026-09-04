package com.clothingretail.customer;

import com.clothingretail.common.NotFoundException;
import com.clothingretail.customer.dto.AddressRequest;
import com.clothingretail.customer.dto.AddressResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD for a customer's own saved addresses, always scoped to the caller's {@link CustomerProfile}
 * (resolved from the authenticated user id - see {@code CustomerAddressController}). Every
 * operation on a specific address id re-verifies ownership; a mismatch is reported as a plain 404
 * (never leaking whether the address exists for someone else).
 */
@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final CustomerProfileRepository customerProfileRepository;

    public AddressService(AddressRepository addressRepository, CustomerProfileRepository customerProfileRepository) {
        this.addressRepository = addressRepository;
        this.customerProfileRepository = customerProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(Long userId) {
        CustomerProfile profile = resolveProfile(userId);
        return addressRepository.findByCustomerProfileId(profile.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AddressResponse create(Long userId, AddressRequest request) {
        CustomerProfile profile = resolveProfile(userId);
        if (Boolean.TRUE.equals(request.isDefault())) {
            addressRepository.clearDefault(profile.getId());
        }
        Address address = new Address();
        address.setCustomerProfile(profile);
        applyRequest(address, request);
        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse update(Long userId, Long addressId, AddressRequest request) {
        CustomerProfile profile = resolveProfile(userId);
        Address address = findOwned(profile.getId(), addressId);
        if (Boolean.TRUE.equals(request.isDefault())) {
            addressRepository.clearDefault(profile.getId());
        }
        applyRequest(address, request);
        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public void delete(Long userId, Long addressId) {
        CustomerProfile profile = resolveProfile(userId);
        Address address = findOwned(profile.getId(), addressId);
        addressRepository.delete(address);
    }

    private Address findOwned(Long profileId, Long addressId) {
        return addressRepository.findByIdAndCustomerProfileId(addressId, profileId)
                .orElseThrow(() -> new NotFoundException("Address not found: " + addressId));
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
        return customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Customer profile not found for user: " + userId));
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
