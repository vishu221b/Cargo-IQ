package io.cargoiq.adapter.out.reference;

import io.cargoiq.domain.model.HsCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ring-2 test for {@link HsCodeReferenceData}: it loads the real
 * {@code reference/hs-codes.csv}, so it also guards the quoted-CSV parser
 * (descriptions contain commas) and the token-aware ranked search.
 */
class HsCodeReferenceDataTest {

    private HsCodeReferenceData ref;

    @BeforeEach
    void setUp() throws Exception {
        ref = new HsCodeReferenceData();
        ref.load(); // @PostConstruct, package-private — invoked directly
    }

    @Test
    void quotedDescriptionsWithCommasParseIntoTheDescriptionNotTheChapter() {
        HsCode steel = ref.findHsCode("7208").orElseThrow();
        // "Flat-rolled products of iron or non-alloy steel, hot-rolled, in coils"
        assertThat(steel.description().toLowerCase()).contains("coils");
        assertThat(steel.chapter()).isEqualTo("72"); // not "hot-rolled..."
    }

    @Test
    void rankedSearchMatchesTermsInsideQuotedDescriptions() {
        assertThat(ref.searchHsCodes("steel coil", 5))
                .extracting(HsCode::code).contains("7208");
        assertThat(ref.searchHsCodes("smartphone", 5))
                .extracting(HsCode::code).contains("8517");
    }

    @Test
    void numericQueryIsTreatedAsCodePrefix() {
        List<HsCode> chapter85 = ref.searchHsCodes("85", 50);
        assertThat(chapter85).isNotEmpty();
        assertThat(chapter85).allSatisfy(c -> assertThat(c.code()).startsWith("85"));
    }

    @Test
    void rankingPrefersStrongerMatches() {
        List<HsCode> hits = ref.searchHsCodes("coffee", 5);
        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).code()).isEqualTo("0901"); // "Coffee ..." ranks first
    }
}
