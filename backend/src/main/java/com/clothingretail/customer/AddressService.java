package com.clothingretail.customer;

import com.clothingretail.common.NotFoundException;
import com.clothingretail.customer.dto.AddressRequest;
import com.clothingretail.customer.dto.AddressResponse;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD for a customer's own saved addresses, always scoped to the caller's {@link CustomerProfile}
 * (resolved from the authenticated user id - see {@code CustomerAddressController}). Every
 * operation on a specific address id re-verifies ownership; a mismatch is reported as a plain 404
 * (never leaking whether the address exists for someone else).
 *
 * create()/update() retry on a transient lock failure: concurrent calls for DIFFERENT customers
 * can spuriously deadlock in MySQL (InnoDB gap-locking on clearDefault()'s non-unique
 * customer_profile_id index under REPEATABLE READ - the rows involved don't actually conflict,
 * but the locks briefly do; confirmed live under a 100-concurrent-request test). A deadlock
 * aborts the whole transaction, so a retry has to re-run create0()/update0() as a genuinely fresh
 * transaction, not just re-run some statements inside the aborted one.
 */
@Service
@Log4j2
public class AddressService {

    private static final int MAX_LOCK_ATTEMPTS = 6;
    private static final long RETRY_BACKOFF_BASE_MILLIS = 30;
    private static final long RETRY_BACKOFF_JITTER_MILLIS = 40;

    private final AddressRepository addressRepository;
    private final CustomerProfileRepository customerProfileRepository;
    // This bean's own Spring proxy (not `this`) - routing retries through `self.create0(...)`
    // re-enters the @Transactional interceptor on every attempt. A same-class `this.create0(...)`
    // call would skip that interceptor entirely (Spring's proxy-based AOP never intercepts
    // self-invocation), so a retry would silently continue inside the SAME already-aborted
    // transaction instead of starting a fresh one. @Lazy breaks the circular-construction
    // dependency this self-reference would otherwise create.
    private final AddressService self;

    public AddressService(
            AddressRepository addressRepository,
            CustomerProfileRepository customerProfileRepository,
            @Lazy AddressService self) {
        this.addressRepository = addressRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.self = self;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(Long userId) {
        log.info("[1200] List addresses userId={}", userId);
        CustomerProfile profile = resolveProfile(userId);
        List<AddressResponse> addresses = addressRepository.findByCustomerProfileId(profile.getId()).stream().map(this::toResponse).toList();
        log.info("[1201] Addresses listed userId={} profileId={} count={}", userId, profile.getId(), addresses.size());
        return addresses;
    }

    public AddressResponse create(Long userId, AddressRequest request) {
        return withLockRetry(() -> self.create0(userId, request));
    }

    @Transactional
    AddressResponse create0(Long userId, AddressRequest request) {
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

    public AddressResponse update(Long userId, Long addressId, AddressRequest request) {
        return withLockRetry(() -> self.update0(userId, addressId, request));
    }

    @Transactional
    AddressResponse update0(Long userId, Long addressId, AddressRequest request) {
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

    /**
     * Retries a create0()/update0() call a few times when it fails with a transient lock/deadlock
     * error - see this class's own doc comment for why that happens here. Anything else (not
     * found, validation, etc.) propagates immediately, unretried.
     */
    private <T> T withLockRetry(Supplier<T> action) {
        for (int attempt = 1; ; attempt++) {
            try {
                return action.get();
            } catch (PessimisticLockingFailureException ex) {
                if (attempt >= MAX_LOCK_ATTEMPTS) {
                    log.error("[1213] Giving up after {} attempts, transient lock failure persisted: {}", attempt, ex.getMessage());
                    throw ex;
                }
                log.error("[1214] Transient lock failure on attempt {}/{}, retrying: {}", attempt, MAX_LOCK_ATTEMPTS, ex.getMessage());
                sleepBackoff(attempt);
            }
        }
    }

    /**
     * Random jitter matters here, not just an increasing delay: every thread that loses the same
     * deadlock tends to hit it at nearly the same instant, so a purely deterministic backoff (the
     * same delay for every attempt N) lets them all retry in lockstep and collide again. Jitter
     * spreads retries out so the herd stops re-colliding on every pass.
     */
    private void sleepBackoff(int attempt) {
        long jitter = java.util.concurrent.ThreadLocalRandom.current().nextLong(RETRY_BACKOFF_JITTER_MILLIS);
        try {
            Thread.sleep(RETRY_BACKOFF_BASE_MILLIS * attempt + jitter);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
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
