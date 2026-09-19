package com.clothingretail.masterdata.controller;

import com.clothingretail.masterdata.dto.ColorAdminRequest;
import com.clothingretail.masterdata.dto.ColorAdminResponse;
import com.clothingretail.masterdata.service.ColorService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/colors")
@PreAuthorize("hasRole('ADMIN') or (hasRole('EMPLOYEE') and @modulePermission.hasAccess('MASTERS'))")
public class AdminColorController {

    private final ColorService colorService;

    public AdminColorController(ColorService colorService) {
        this.colorService = colorService;
    }

    @GetMapping
    public List<ColorAdminResponse> list(
            @RequestParam(required = false) String q, @RequestParam(required = false) Boolean active) {
        return colorService.listAdmin(q, active);
    }

    @GetMapping("/{id}")
    public ColorAdminResponse get(@PathVariable Long id) {
        return colorService.getAdmin(id);
    }

    @PostMapping
    public ResponseEntity<ColorAdminResponse> create(@Valid @RequestBody ColorAdminRequest request) {
        return ResponseEntity.ok(colorService.create(request));
    }

    @PutMapping("/{id}")
    public ColorAdminResponse update(@PathVariable Long id, @Valid @RequestBody ColorAdminRequest request) {
        return colorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        colorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
