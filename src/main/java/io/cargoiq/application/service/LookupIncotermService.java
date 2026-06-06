package io.cargoiq.application.service;

import io.cargoiq.application.port.in.LookupIncotermUseCase;
import io.cargoiq.domain.exception.IncotermNotFoundException;
import io.cargoiq.domain.model.Incoterm;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

/**
 * Use case: lookup an INCOTERM 2020 rule.
 *
 * <p>The reference data is in-code on purpose. The 11-rule ICC ruleset is
 * static and well-known; reaching for a database would be over-engineering.
 * If you ever ship INCOTERMS 2030 here, you change a class, not a schema
 * migration.
 *
 * <p>Each rule is summarised below in the canonical seller/buyer obligation
 * framing used by ICC. The summaries are paraphrased and intentionally short
 * — for the full canonical text the user should be directed to ICC's
 * publication.
 */
@Service
public class LookupIncotermService implements LookupIncotermUseCase {

    private static final Map<Incoterm, IncotermDetail> RULES = new EnumMap<>(Incoterm.class);

    static {
        RULES.put(Incoterm.EXW, new IncotermDetail(Incoterm.EXW,
                "Seller makes goods available at their own premises; buyer arranges everything from there.",
                "Make goods available at named place (typically the factory).",
                "Collect goods; arrange export clearance, all transport, and import clearance.",
                "Transfers when goods are placed at buyer's disposal at seller's premises.",
                "Buyer bears all transport and clearance cost.",
                "Domestic-feel trades where the buyer has freight expertise and infrastructure."));

        RULES.put(Incoterm.FCA, new IncotermDetail(Incoterm.FCA,
                "Seller delivers cleared-for-export goods to a carrier nominated by the buyer at a named place.",
                "Export clearance; delivery to nominated carrier at named place.",
                "Main carriage; insurance (optional); import clearance.",
                "On delivery to the nominated carrier at the named place.",
                "Seller pays to named place; buyer pays from there.",
                "Containerised cargo where buyer controls main carriage — replaces FOB for boxes."));

        RULES.put(Incoterm.CPT, new IncotermDetail(Incoterm.CPT,
                "Seller pays freight to the named destination; risk passes when goods are handed to the first carrier.",
                "Export clearance, main carriage paid to destination.",
                "Insurance from handover to first carrier; import clearance.",
                "On hand-over to first carrier.",
                "Seller pays carriage to destination.",
                "Multimodal trades where the seller arranges freight but the buyer accepts in-transit risk."));

        RULES.put(Incoterm.CIP, new IncotermDetail(Incoterm.CIP,
                "Like CPT but seller must also procure insurance (Institute Cargo Clauses A — all-risks — under 2020 update).",
                "Export clearance, main carriage paid, all-risks insurance.",
                "Import clearance.",
                "On hand-over to first carrier (despite seller's insurance obligation).",
                "Seller pays carriage and insurance to destination.",
                "Buyer wants seller to procure top-tier insurance but takes on-arrival risk."));

        RULES.put(Incoterm.DAP, new IncotermDetail(Incoterm.DAP,
                "Seller delivers goods ready for unloading at the named place of destination.",
                "Export clearance; all transport to the named destination.",
                "Unloading at destination; import clearance.",
                "At the named destination, ready for unloading.",
                "Seller pays all transport to destination.",
                "Door-to-door deliveries where the seller controls inbound freight."));

        RULES.put(Incoterm.DPU, new IncotermDetail(Incoterm.DPU,
                "Seller delivers goods unloaded at the named place of destination. (Replaces 2010's DAT.)",
                "Export clearance; transport; unloading at destination.",
                "Import clearance.",
                "After unloading at destination.",
                "Seller pays transport and unloading.",
                "Project cargo, machinery — anything where the seller's people unload."));

        RULES.put(Incoterm.DDP, new IncotermDetail(Incoterm.DDP,
                "Seller delivers cleared-for-import goods at the named destination — maximum seller obligation.",
                "Everything: export clearance, transport, import clearance, duties, taxes.",
                "Accept delivery.",
                "At the named destination, ready for unloading.",
                "Seller pays everything to destination including import duties.",
                "Distance sales / e-commerce where buyer wants a single landed price."));

        RULES.put(Incoterm.FAS, new IncotermDetail(Incoterm.FAS,
                "Seller delivers goods alongside the vessel at the named port of shipment. Sea / inland-waterway only.",
                "Export clearance; deliver alongside vessel.",
                "Loading, main carriage, insurance, import clearance.",
                "Alongside vessel at named port.",
                "Seller pays to alongside-ship; buyer pays from there.",
                "Bulk and break-bulk cargo (commodities, oversized) where buyer charters the vessel."));

        RULES.put(Incoterm.FOB, new IncotermDetail(Incoterm.FOB,
                "Seller delivers goods on board the vessel nominated by the buyer at the named port of shipment.",
                "Export clearance; deliver on board vessel.",
                "Main carriage, insurance, import clearance.",
                "When goods are on board the vessel.",
                "Seller pays through loading; buyer pays from there.",
                "Bulk commodities. Avoid for containerised cargo — use FCA instead."));

        RULES.put(Incoterm.CFR, new IncotermDetail(Incoterm.CFR,
                "Seller pays freight to the named destination port; risk passes when goods are on board at origin.",
                "Export clearance; main carriage paid to destination port.",
                "Insurance from on-board; import clearance.",
                "When goods are on board the vessel at origin port.",
                "Seller pays freight to destination port.",
                "Bulk commodities where the seller arranges sea freight but buyer carries cargo risk."));

        RULES.put(Incoterm.CIF, new IncotermDetail(Incoterm.CIF,
                "Like CFR but seller also procures insurance (Institute Cargo Clauses C — minimum cover — under 2020).",
                "Export clearance; main carriage; minimum-cover marine insurance.",
                "Import clearance; upgrade insurance if more cover wanted.",
                "When goods are on board the vessel at origin port (despite insurance).",
                "Seller pays freight and insurance to destination port.",
                "Traditional bulk shipping. Note the minimum-cover trap — buyer should top up."));
    }

    @Override
    public IncotermDetail lookup(String code) {
        Incoterm rule = Incoterm.parse(code)
                .orElseThrow(() -> new IncotermNotFoundException(code));
        return RULES.get(rule);
    }
}
