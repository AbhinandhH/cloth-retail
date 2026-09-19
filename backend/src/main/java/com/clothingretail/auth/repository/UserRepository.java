package com.clothingretail.auth.repository;

import com.clothingretail.auth.RoleName;
import com.clothingretail.auth.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Powers the "never leave the store with zero ADMIN accounts" guardrail in AuthServiceImpl. Counts only enabled accounts - a disabled admin doesn't count toward the minimum. */
    long countByRoles_NameAndEnabledTrue(RoleName roleName);

    /** Staff list for AdminStaff.tsx - every ADMIN and EMPLOYEE account (never SUPER_ADMIN or CUSTOMER). */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name IN :roleNames ORDER BY u.fullName ASC")
    List<User> findByRoles_NameIn(@Param("roleNames") List<RoleName> roleNames);
}
