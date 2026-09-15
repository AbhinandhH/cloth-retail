package com.clothingretail.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * In-memory read cache for data that's fetched on nearly every request but
 * changes only via occasional admin CRUD - the site configuration/theme
 * (every storefront page load) and public master data lookups (categories,
 * sub-categories, sizes, colors, vendors - read on nearly every storefront
 * and admin page). Cache names are wired up per-method with @Cacheable in
 * each owning service, evicted with @CacheEvict on that same service's admin
 * write paths (create/update/delete/activate).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String SITE_CONFIG_PUBLIC = "siteConfigPublic";
    public static final String CATEGORIES_PUBLIC = "categoriesPublic";
    public static final String SUB_CATEGORIES_PUBLIC = "subCategoriesPublic";
    public static final String SIZES_PUBLIC = "sizesPublic";
    public static final String COLORS_PUBLIC = "colorsPublic";
    public static final String VENDORS_PUBLIC = "vendorsPublic";

    @Bean
    @Profile("!test")
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                SITE_CONFIG_PUBLIC, CATEGORIES_PUBLIC, SUB_CATEGORIES_PUBLIC, SIZES_PUBLIC, COLORS_PUBLIC, VENDORS_PUBLIC);
        manager.setCaffeine(Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(1000));
        return manager;
    }

    // Integration tests roll each test method back via @Transactional, but the cache above is a
    // plain singleton bean, not transactional - a value it caches during one test's transaction
    // survives that rollback and leaks into the next test (which sees stale data instead of the
    // fresh state its own transaction just set up). A no-op manager keeps every @Cacheable read
    // going straight to the DB under the test profile, so tests observe writes immediately, the
    // same way they did before caching existed.
    @Bean
    @Profile("test")
    public CacheManager testCacheManager() {
        return new NoOpCacheManager();
    }
}
