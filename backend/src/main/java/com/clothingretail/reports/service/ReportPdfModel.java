package com.clothingretail.reports.service;

import java.util.List;

/**
 * Generic shape every report is reduced to before rendering, so a single Thymeleaf template
 * (templates/reports/report.html) serves all report types - each {@link ReportPdfServiceImpl}
 * method's job is entirely "fetch this report's data and shape it into one of these," not
 * template plumbing.
 */
record ReportPdfModel(
        String businessName,
        String title,
        String filterSummary,
        String generatedAt,
        List<String> columnHeaders,
        List<List<String>> rows,
        List<String> totalsRow,
        boolean truncated) {}
