package com.clothingretail.auth.repository;

import com.clothingretail.auth.AdminModule;
import com.clothingretail.auth.ModulePermission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModulePermissionRepository extends JpaRepository<ModulePermission, Long> {

    Optional<ModulePermission> findByUser_IdAndModule(Long userId, AdminModule module);

    List<ModulePermission> findByUser_Id(Long userId);

    void deleteByUser_Id(Long userId);
}
