package com.clothingretail.siteconfig.dto;

public record ThemeAdminResponse(
        Long id,
        String name,
        String primaryColor,
        String secondaryColor,
        String accentColor,
        String backgroundColor,
        String textColor,
        int displayOrder,
        boolean richAmbient,
        String motif) {}
