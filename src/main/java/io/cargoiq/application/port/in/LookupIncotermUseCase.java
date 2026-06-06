package io.cargoiq.application.port.in;

import io.cargoiq.domain.model.Incoterm;

/**
 * Inbound port: look up a single INCOTERM 2020 rule by code.
 *
 * <p>Backed by hard-coded reference data — the 11-rule ICC ruleset is static.
 * Exposed as both a REST endpoint and an MCP tool, which makes this use case
 * a clean demonstration of "one piece of business logic, multiple inbound
 * adapters" — the headline of hexagonal architecture.
 */
public interface LookupIncotermUseCase {
    IncotermDetail lookup(String code);

    record IncotermDetail(
            Incoterm rule,
            String summary,
            String sellerObligations,
            String buyerObligations,
            String riskTransfer,
            String costTransfer,
            String typicalUseCase) {}
}
