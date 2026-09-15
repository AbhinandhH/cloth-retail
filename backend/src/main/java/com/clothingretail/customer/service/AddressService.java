package com.clothingretail.customer.service;

import com.clothingretail.customer.dto.AddressRequest;
import com.clothingretail.customer.dto.AddressResponse;
import java.util.List;

/**
 * CRUD for a customer's own saved addresses, always scoped to the caller's CustomerProfile
 * (resolved from the authenticated user id). Every operation on a specific address id re-verifies
 * ownership; a mismatch is reported as a plain 404 (never leaking whether the address exists for
 * someone else). create()/update() retry on a transient MySQL deadlock - see AddressServiceImpl's
 * own doc comment.
 */
public interface AddressService {

    List<AddressResponse> list(Long userId);

    AddressResponse create(Long userId, AddressRequest request);

    AddressResponse update(Long userId, Long addressId, AddressRequest request);

    void delete(Long userId, Long addressId);

    /**
     * Internal - always call {@link #create(Long, AddressRequest)} instead, which wraps this with
     * deadlock retry. Only on this interface (not package-private on the impl) because the retry
     * loop re-enters it via this bean's own proxy - see AddressServiceImpl's doc comment - and
     * Spring proxies an interface-implementing bean as a JDK dynamic proxy, which only exposes
     * interface methods.
     */
    AddressResponse create0(Long userId, AddressRequest request);

    /** Internal - see {@link #create0(Long, AddressRequest)}'s doc comment; same reasoning, for update(). */
    AddressResponse update0(Long userId, Long addressId, AddressRequest request);
}
