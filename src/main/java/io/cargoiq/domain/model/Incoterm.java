package io.cargoiq.domain.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * The 11 INCOTERMS 2020 rules published by the International Chamber of Commerce.
 *
 * <p>Two groups: rules for <b>any mode of transport</b> (E/F/C/D prefixes are
 * historical groupings, all 7 here apply to any mode incl. multimodal), and
 * rules for <b>sea/inland waterway only</b> (the 4 maritime-specific ones).
 *
 * <p>These values are the canonical reference data used by
 * {@code LookupIncotermUseCase} and the corresponding MCP tool. Hard-coded
 * intentionally — the 2020 ruleset doesn't change, and externalising it to a
 * database would be cargo-culted abstraction.
 */
public enum Incoterm {

    // ---- Any mode of transport ----
    EXW("Ex Works",         Mode.ANY,      Risk.SELLER_AT_PREMISES),
    FCA("Free Carrier",     Mode.ANY,      Risk.HANDS_TO_CARRIER),
    CPT("Carriage Paid To", Mode.ANY,      Risk.HANDS_TO_CARRIER),
    CIP("Carriage and Insurance Paid To", Mode.ANY, Risk.HANDS_TO_CARRIER),
    DAP("Delivered at Place",     Mode.ANY, Risk.AT_DESTINATION),
    DPU("Delivered at Place Unloaded", Mode.ANY, Risk.AT_DESTINATION_UNLOADED),
    DDP("Delivered Duty Paid",    Mode.ANY, Risk.AT_DESTINATION),

    // ---- Sea & inland waterway only ----
    FAS("Free Alongside Ship",    Mode.SEA, Risk.ALONGSIDE_VESSEL),
    FOB("Free On Board",          Mode.SEA, Risk.ON_BOARD_VESSEL),
    CFR("Cost and Freight",       Mode.SEA, Risk.ON_BOARD_VESSEL),
    CIF("Cost, Insurance and Freight", Mode.SEA, Risk.ON_BOARD_VESSEL);

    public enum Mode { ANY, SEA }

    public enum Risk {
        SELLER_AT_PREMISES, HANDS_TO_CARRIER, ALONGSIDE_VESSEL,
        ON_BOARD_VESSEL, AT_DESTINATION, AT_DESTINATION_UNLOADED
    }

    private final String fullName;
    private final Mode mode;
    private final Risk riskTransferPoint;

    Incoterm(String fullName, Mode mode, Risk riskTransferPoint) {
        this.fullName = fullName;
        this.mode = mode;
        this.riskTransferPoint = riskTransferPoint;
    }

    public String fullName() { return fullName; }
    public Mode mode() { return mode; }
    public Risk riskTransferPoint() { return riskTransferPoint; }

    public static Optional<Incoterm> parse(String code) {
        if (code == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(t -> t.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }
}
