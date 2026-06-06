package io.cargoiq.application.port.in;

import io.cargoiq.domain.model.HsCode;

import java.util.List;

/**
 * Inbound port: search the HS (Harmonised System) taxonomy.
 *
 * <p>Two modes:
 * <ul>
 *   <li>Exact code lookup ({@code byCode("8517")})</li>
 *   <li>Free-text description search ({@code search("smartphone")})</li>
 * </ul>
 */
public interface LookupHsCodeUseCase {
    HsCode byCode(String code);
    List<HsCode> search(String description, int limit);
}
