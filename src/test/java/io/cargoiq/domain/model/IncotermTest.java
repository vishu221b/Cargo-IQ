package io.cargoiq.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IncotermTest {

    @Test
    void recognisesAllElevenRules() {
        assertThat(Incoterm.values()).hasSize(11);
    }

    @Test
    void parseIsCaseInsensitive() {
        assertThat(Incoterm.parse("cif")).contains(Incoterm.CIF);
        assertThat(Incoterm.parse("CIF")).contains(Incoterm.CIF);
        assertThat(Incoterm.parse(" CiF ")).contains(Incoterm.CIF);
    }

    @Test
    void parseReturnsEmptyForBogusCodes() {
        assertThat(Incoterm.parse(null)).isEmpty();
        assertThat(Incoterm.parse("")).isEmpty();
        assertThat(Incoterm.parse("XYZ")).isEmpty();
    }

    @Test
    void seaOnlyRulesAreCorrectlyClassified() {
        assertThat(Incoterm.FAS.mode()).isEqualTo(Incoterm.Mode.SEA);
        assertThat(Incoterm.FOB.mode()).isEqualTo(Incoterm.Mode.SEA);
        assertThat(Incoterm.CFR.mode()).isEqualTo(Incoterm.Mode.SEA);
        assertThat(Incoterm.CIF.mode()).isEqualTo(Incoterm.Mode.SEA);
    }

    @Test
    void anyModeRulesAreCorrectlyClassified() {
        for (var t : new Incoterm[]{
                Incoterm.EXW, Incoterm.FCA, Incoterm.CPT, Incoterm.CIP,
                Incoterm.DAP, Incoterm.DPU, Incoterm.DDP}) {
            assertThat(t.mode()).isEqualTo(Incoterm.Mode.ANY);
        }
    }
}
