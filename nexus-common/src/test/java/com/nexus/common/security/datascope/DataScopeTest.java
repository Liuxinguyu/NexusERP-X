package com.nexus.common.security.datascope;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataScopeTest {

    @Test
    void orgAndSubShopsCodeUnchanged() {
        assertThat(DataScope.ORG_AND_SUB_SHOPS.getCode()).isEqualTo(3);
        assertThat(DataScope.fromCode(3)).isEqualTo(DataScope.ORG_AND_SUB_SHOPS);
    }
}
