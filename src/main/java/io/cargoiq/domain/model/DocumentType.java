package io.cargoiq.domain.model;

/**
 * Kinds of documents in the trade-finance corpus.
 *
 * <p>Driven by interview-credible domain knowledge: these are the actual
 * artefacts that change hands in international shipping. Keeping the list
 * explicit (not a free-text "category" field) lets ingestion pick the right
 * parser and lets queries filter cleanly.
 */
public enum DocumentType {
    /** Master Bill of Lading or House Bill of Lading — title-of-goods document. */
    BILL_OF_LADING,
    /** Sea Waybill — non-negotiable variant. */
    SEA_WAYBILL,
    /** Commercial Invoice — what the seller bills the buyer. */
    COMMERCIAL_INVOICE,
    /** Packing List — itemised carton/pallet contents. */
    PACKING_LIST,
    /** Letter of Credit — bank-issued payment instrument. */
    LETTER_OF_CREDIT,
    /** Certificate of Origin — customs/duty determination. */
    CERTIFICATE_OF_ORIGIN,
    /** Charter Party — vessel hire contract. */
    CHARTER_PARTY,
    /** Reference material (INCOTERMS 2020 ruleset, HS schedule, etc.). */
    REFERENCE,
    /** Anything that doesn't fit above; still ingestable. */
    OTHER
}
