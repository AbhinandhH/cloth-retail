package com.clothingretail.masterdata.service;

import com.clothingretail.common.AuditorNameResolver;
import com.clothingretail.common.BadRequestException;
import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.SizeChart;
import com.clothingretail.masterdata.SizeChartRow;
import com.clothingretail.masterdata.dto.SizeChartAdminRequest;
import com.clothingretail.masterdata.dto.SizeChartAdminResponse;
import com.clothingretail.masterdata.dto.SizeChartRowRequest;
import com.clothingretail.masterdata.dto.SizeChartRowResponse;
import com.clothingretail.masterdata.repository.SizeChartRepository;
import com.clothingretail.product.repository.ProductRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
@Log4j2
public class SizeChartServiceImpl implements SizeChartService {

    private final SizeChartRepository repository;
    private final ProductRepository productRepository;
    private final AuditorNameResolver auditorNameResolver;

    public SizeChartServiceImpl(
            SizeChartRepository repository, ProductRepository productRepository, AuditorNameResolver auditorNameResolver) {
        this.repository = repository;
        this.productRepository = productRepository;
        this.auditorNameResolver = auditorNameResolver;
    }

    @Override
    public List<SizeChartAdminResponse> listAdmin(String q, Boolean active) {
        log.info("[2070] Listing size charts query={} active={}", q, active);
        List<SizeChart> charts = repository.findAll().stream()
                .filter(c -> matchesQuery(c, q))
                .filter(c -> active == null || c.isActive() == active)
                .toList();
        Map<Long, String> names = auditorNameResolver.resolveNames(
                charts.stream().flatMap(c -> Stream.of(c.getCreatedBy(), c.getUpdatedBy())).toList());
        return charts.stream().map(c -> toResponse(c, names)).toList();
    }

    @Override
    public SizeChartAdminResponse getAdmin(Long id) {
        log.info("[2071] Fetching size chart id={}", id);
        SizeChart chart = find(id);
        return toResponse(chart, auditorNameResolver.resolveNames(chart.getCreatedBy(), chart.getUpdatedBy()));
    }

    @Override
    @Transactional
    public SizeChartAdminResponse create(SizeChartAdminRequest request) {
        log.info("[2072] Creating size chart name={} displayOrder={} active={} columns={} rowCount={}",
                request.name(), request.displayOrder(), request.active(), request.columns(),
                request.rows() != null ? request.rows().size() : 0);
        if (repository.existsByNameIgnoreCase(request.name())) {
            log.error("[2073] Cannot create size chart - name={} already exists", request.name());
            throw new ConflictException("A size chart with this name already exists");
        }
        SizeChart chart = new SizeChart();
        apply(chart, request);
        SizeChart saved = repository.save(chart);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Override
    @Transactional
    public SizeChartAdminResponse update(Long id, SizeChartAdminRequest request) {
        log.info("[2074] Updating size chart id={} name={} displayOrder={} active={} columns={} rowCount={}",
                id, request.name(), request.displayOrder(), request.active(), request.columns(),
                request.rows() != null ? request.rows().size() : 0);
        SizeChart chart = find(id);
        if (!chart.getName().equalsIgnoreCase(request.name()) && repository.existsByNameIgnoreCase(request.name())) {
            log.error("[2075] Cannot update size chart id={} - name={} already exists", id, request.name());
            throw new ConflictException("A size chart with this name already exists");
        }
        apply(chart, request);
        SizeChart saved = repository.save(chart);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("[2076] Deleting size chart id={}", id);
        SizeChart chart = find(id);
        long usageCount = productRepository.countBySizeChartId(id);
        if (usageCount > 0) {
            log.error("[2077] Cannot delete size chart id={} - {} products depend on it", id, usageCount);
            throw new ConflictException(
                    "This size chart is currently used by " + usageCount + " products and cannot be deleted. Deactivate it instead.");
        }
        repository.delete(chart);
    }

    private SizeChart find(Long id) {
        return repository.findById(id).orElseThrow(() -> {
            log.error("[2078] Size chart not found id={}", id);
            return new NotFoundException("Size chart not found: " + id);
        });
    }

    private boolean matchesQuery(SizeChart chart, String q) {
        return !StringUtils.hasText(q) || chart.getName().toLowerCase().contains(q.toLowerCase());
    }

    private void apply(SizeChart chart, SizeChartAdminRequest request) {
        chart.setName(request.name());
        chart.setDescription(request.description());
        chart.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        chart.setActive(request.active());

        List<String> columns = request.columns();
        chart.setColumns(new ArrayList<>(columns));

        List<SizeChartRowRequest> rowRequests = request.rows() != null ? request.rows() : List.of();
        for (SizeChartRowRequest rowRequest : rowRequests) {
            int valueCount = rowRequest.values() != null ? rowRequest.values().size() : 0;
            if (valueCount != columns.size()) {
                log.error("[2079] Cannot save size chart - row '{}' has {} value(s), chart has {} column(s)",
                        rowRequest.sizeLabel(), valueCount, columns.size());
                throw new BadRequestException(
                        "Row \"" + rowRequest.sizeLabel() + "\" has " + valueCount + " value(s) but the chart has "
                                + columns.size() + " column(s) - every row must have exactly one value per column");
            }
        }

        chart.getRows().clear();
        int order = 0;
        for (SizeChartRowRequest rowRequest : rowRequests) {
            SizeChartRow row = new SizeChartRow();
            row.setSizeLabel(rowRequest.sizeLabel());
            row.setValues(rowRequest.values() != null ? new ArrayList<>(rowRequest.values()) : new ArrayList<>());
            row.setDisplayOrder(order++);
            chart.addRow(row);
        }
        log.info("[2080] Assigned display order for {} row(s) in size chart", rowRequests.size());
    }

    private SizeChartAdminResponse toResponse(SizeChart chart, Map<Long, String> names) {
        List<SizeChartRowResponse> rows = chart.getRows().stream()
                .sorted(Comparator.comparing(SizeChartRow::getDisplayOrder))
                .map(r -> new SizeChartRowResponse(r.getId(), r.getSizeLabel(), List.copyOf(r.getValues()), r.getDisplayOrder()))
                .toList();
        return new SizeChartAdminResponse(
                chart.getId(),
                chart.getName(),
                chart.getDescription(),
                chart.getDisplayOrder(),
                chart.isActive(),
                List.copyOf(chart.getColumns()),
                rows,
                names.get(chart.getCreatedBy()),
                names.get(chart.getUpdatedBy()),
                chart.getCreatedAt(),
                chart.getUpdatedAt());
    }
}
