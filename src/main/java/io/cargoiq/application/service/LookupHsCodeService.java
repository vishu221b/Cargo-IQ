package io.cargoiq.application.service;

import io.cargoiq.application.port.in.LookupHsCodeUseCase;
import io.cargoiq.application.port.out.ReferenceDataPort;
import io.cargoiq.domain.exception.HsCodeNotFoundException;
import io.cargoiq.domain.model.HsCode;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LookupHsCodeService implements LookupHsCodeUseCase {

    private final ReferenceDataPort referenceData;

    public LookupHsCodeService(ReferenceDataPort referenceData) {
        this.referenceData = referenceData;
    }

    @Override
    public HsCode byCode(String code) {
        return referenceData.findHsCode(code)
                .orElseThrow(() -> new HsCodeNotFoundException(code));
    }

    @Override
    public List<HsCode> search(String description, int limit) {
        return referenceData.searchHsCodes(description, limit);
    }
}
