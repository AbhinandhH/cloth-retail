package com.clothingretail.auth.repository;

import com.clothingretail.auth.Role;
import com.clothingretail.auth.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
