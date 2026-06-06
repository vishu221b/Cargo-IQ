package io.cargoiq.application.port.out;

import io.cargoiq.domain.model.HsCode;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port: read-only reference data (HS schedule).
 *
 * <p>INCOTERMS are already enum-modelled in {@link io.cargoiq.domain.model.Incoterm},
 * so they don't need a port. HS codes are a sizeable taxonomy (5,000+ at the
 * 6-digit level, 17,000+ at national-extension level) that we want to load
 * from a CSV resource — hence the port + adapter.
 */
public interface ReferenceDataPort {

    Optional<HsCode> findHsCode(String code);

    List<HsCode> searchHsCodes(String descriptionLike, int limit);
}
