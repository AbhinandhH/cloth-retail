package com.clothingretail.auth.service;

import com.clothingretail.auth.AdminModule;
import com.clothingretail.auth.ModulePermission;
import com.clothingretail.auth.User;
import com.clothingretail.auth.dto.ModulePermissionRow;
import com.clothingretail.auth.repository.ModulePermissionRepository;
import com.clothingretail.auth.repository.UserRepository;
import com.clothingretail.common.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component("modulePermission")
@Transactional(readOnly = true)
@Log4j2
public class ModulePermissionServiceImpl implements ModulePermissionService {

    private final ModulePermissionRepository modulePermissionRepository;
    private final UserRepository userRepository;

    public ModulePermissionServiceImpl(ModulePermissionRepository modulePermissionRepository, UserRepository userRepository) {
        this.modulePermissionRepository = modulePermissionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public boolean hasAccess(String module) {
        Long userId = currentUserId();
        if (userId == null) {
            return false;
        }
        AdminModule parsed;
        try {
            parsed = AdminModule.valueOf(module);
        } catch (IllegalArgumentException ex) {
            log.error("[1930] hasAccess called with unknown module={}", module);
            return false;
        }
        boolean mutating = isMutatingRequest();
        boolean allowed = mutating ? hasEdit(userId, parsed) : hasView(userId, parsed);
        log.info("[1931] Permission check userId={}, module={}, mutating={}, allowed={}", userId, parsed, mutating, allowed);
        return allowed;
    }

    @Override
    public boolean hasView(Long userId, AdminModule module) {
        return modulePermissionRepository.findByUser_IdAndModule(userId, module)
                .map(ModulePermission::isCanView)
                .orElse(false);
    }

    @Override
    public boolean hasEdit(Long userId, AdminModule module) {
        return modulePermissionRepository.findByUser_IdAndModule(userId, module)
                .map(ModulePermission::isCanEdit)
                .orElse(false);
    }

    @Override
    public List<ModulePermissionRow> listForUser(Long userId) {
        Map<AdminModule, ModulePermission> byModule = modulePermissionRepository.findByUser_Id(userId).stream()
                .collect(Collectors.toMap(ModulePermission::getModule, p -> p));
        return java.util.Arrays.stream(AdminModule.values())
                .map(m -> {
                    ModulePermission p = byModule.get(m);
                    return new ModulePermissionRow(m, p != null && p.isCanView(), p != null && p.isCanEdit());
                })
                .toList();
    }

    @Override
    @Transactional
    public void replaceForUser(Long userId, List<ModulePermissionRow> grants) {
        log.info("[1932] Replacing module permissions userId={}, grants={}", userId, grants);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        Map<AdminModule, ModulePermission> existing = modulePermissionRepository.findByUser_Id(userId).stream()
                .collect(Collectors.toMap(ModulePermission::getModule, p -> p));
        Map<AdminModule, ModulePermissionRow> requested =
                grants.stream().collect(Collectors.toMap(ModulePermissionRow::module, g -> g));

        for (AdminModule module : AdminModule.values()) {
            ModulePermissionRow requestedRow = requested.get(module);
            boolean canView = requestedRow != null && requestedRow.canView();
            boolean canEdit = requestedRow != null && requestedRow.canEdit();
            ModulePermission permission = existing.get(module);
            if (permission == null) {
                permission = new ModulePermission();
                permission.setUser(user);
                permission.setModule(module);
            }
            permission.setCanView(canView || canEdit);
            permission.setCanEdit(canEdit);
            modulePermissionRepository.save(permission);
        }
        log.info("[1933] Module permissions replaced userId={}", userId);
    }

    @Override
    @Transactional
    public void grantAllModules(Long userId) {
        log.info("[1934] Granting full module access userId={}", userId);
        List<ModulePermissionRow> allGranted = java.util.Arrays.stream(AdminModule.values())
                .map(m -> new ModulePermissionRow(m, true, true))
                .toList();
        replaceForUser(userId, allGranted);
    }

    private Long currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof Long id ? id : null;
    }

    private boolean isMutatingRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
            // No request in scope (e.g. called outside an HTTP request) - default to the safer,
            // stricter check rather than silently allowing a write.
            return true;
        }
        HttpServletRequest request = servletAttrs.getRequest();
        return !"GET".equalsIgnoreCase(request.getMethod());
    }
}
