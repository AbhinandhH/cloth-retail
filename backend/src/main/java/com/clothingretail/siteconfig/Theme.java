package com.clothingretail.siteconfig;

import com.clothingretail.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A selectable storefront color scheme. Exactly one is active at a time via {@link SiteConfiguration#getActiveTheme()}. */
@Entity
@Table(name = "themes")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Theme extends BaseEntity {

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "primary_color", nullable = false, length = 7)
    private String primaryColor;

    @Column(name = "secondary_color", nullable = false, length = 7)
    private String secondaryColor;

    @Column(name = "accent_color", length = 7)
    private String accentColor;

    @Column(name = "background_color", nullable = false, length = 7)
    private String backgroundColor;

    @Column(name = "text_color", nullable = false, length = 7)
    private String textColor;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    /**
     * Whether the storefront's page-wide {@code AmbientBackground} wash shows for this theme.
     * False reproduces the original, pre-redesign look: plain white behind the product list,
     * with only BrandHero's own small local gold glow visible near the hero.
     */
    @Column(name = "rich_ambient", nullable = false)
    private boolean richAmbient = true;

    /**
     * Which visual language this theme uses, beyond just its 5 colors - "SIGNATURE" (default,
     * the storefront's existing ornate/gilded/serif look), "STUDIO" (a calmer, geometric-sans
     * look with no shimmer/glow ornamentation and a squared CTA), "ELAN" (a premium editorial
     * boutique look: a warm serif display face, the same quiet no-shimmer/no-ornament treatment
     * as Studio, and a refined, barely-rounded CTA rather than square or pill), or "PULSE" (a
     * sharp, high-energy dark-mode look: a bold geometric display face, an animated aurora
     * backdrop in place of the gold hero ornamentation, and glow-on-interaction buttons/cards -
     * see index.css's [data-motif="pulse"] rules). A plain string rather than a Java enum to
     * match this entity's existing fields, which are all validated at the DTO boundary (see
     * ThemeAdminRequest) rather than via JPA typing.
     */
    @Column(nullable = false, length = 20)
    private String motif = "SIGNATURE";
}
