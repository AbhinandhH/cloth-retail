package com.clothingretail.auth.dto;

import java.util.List;

public record UserSummary(Long id, String fullName, String email, List<String> roles) {}
