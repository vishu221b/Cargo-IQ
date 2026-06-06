package io.cargoiq.domain.exception;

public class IncotermNotFoundException extends DomainException {
    public IncotermNotFoundException(String code) {
        super("No such INCOTERM 2020 rule: '" + code +
                "'. Expected one of: EXW, FCA, CPT, CIP, DAP, DPU, DDP, FAS, FOB, CFR, CIF.");
    }
}
