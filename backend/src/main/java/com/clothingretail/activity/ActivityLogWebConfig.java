package com.clothingretail.activity;

import com.clothingretail.activity.service.ActivityLogService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers {@link ActivityLoggingInterceptor} for every admin console request. */
@Configuration
public class ActivityLogWebConfig implements WebMvcConfigurer {

    private final ActivityLogService activityLogService;

    public ActivityLogWebConfig(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ActivityLoggingInterceptor(activityLogService)).addPathPatterns("/api/admin/**");
    }
}
