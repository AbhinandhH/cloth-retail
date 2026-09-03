package com.clothingretail.siteconfig.dto;

/** Public-shaped theme, nested inside {@link PublicConfigurationResponse}. */
public record ThemeResponse(
        String name,
        String primaryColor,
        String secondaryColor,
        String accentColor,
        String backgroundColor,
        String textColor) {}
