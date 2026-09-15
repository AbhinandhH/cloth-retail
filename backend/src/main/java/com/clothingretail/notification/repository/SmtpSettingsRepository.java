package com.clothingretail.notification.repository;

import com.clothingretail.notification.SmtpSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmtpSettingsRepository extends JpaRepository<SmtpSettings, Long> {}
