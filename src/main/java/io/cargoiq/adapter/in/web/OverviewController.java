package io.cargoiq.adapter.in.web;

import io.cargoiq.adapter.in.web.dto.OverviewResponse;
import io.cargoiq.application.port.in.GetCorpusOverviewUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST adapter exposing a corpus overview — the data behind the dashboard.
 * Any authenticated user can read it.
 *
 * @author Vishal Dogra
 */
@RestController
@RequestMapping("/api/v1/overview")
@Tag(name = "overview", description = "Corpus statistics for dashboards")
public class OverviewController {

    private final GetCorpusOverviewUseCase overview;

    public OverviewController(GetCorpusOverviewUseCase overview) {
        this.overview = overview;
    }

    @Operation(summary = "Corpus totals and breakdowns by document type and INCOTERM")
    @GetMapping
    public OverviewResponse get() {
        return OverviewResponse.from(overview.overview());
    }
}
