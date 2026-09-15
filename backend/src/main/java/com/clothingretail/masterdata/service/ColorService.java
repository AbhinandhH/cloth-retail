package com.clothingretail.masterdata.service;

import com.clothingretail.masterdata.dto.ColorAdminRequest;
import com.clothingretail.masterdata.dto.ColorAdminResponse;
import com.clothingretail.masterdata.dto.ColorResponse;
import java.util.List;

public interface ColorService {

    List<ColorAdminResponse> listAdmin(String q, Boolean active);

    ColorAdminResponse getAdmin(Long id);

    List<ColorResponse> listPublic();

    ColorAdminResponse create(ColorAdminRequest request);

    ColorAdminResponse update(Long id, ColorAdminRequest request);

    void delete(Long id);
}
