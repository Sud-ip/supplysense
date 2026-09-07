package com.supplysense.backend.inventory.domain;

import com.supplysense.backend.auth.domain.Role;
import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.auth.domain.User;
import com.supplysense.backend.catalog.domain.Location;
import com.supplysense.backend.catalog.domain.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockLedgerEntryTest {

    @Test
    void rejectsZeroQuantityDelta() {
        Tenant tenant = new Tenant("Acme");
        User user = new User(tenant, "a@b.com", "hash", "A User", Role.OWNER);
        Product product = new Product(tenant, "SKU-1", "Widget", null, BigDecimal.ONE, BigDecimal.TEN);
        Location location = new Location(tenant, "Main WH", null);

        assertThatThrownBy(() -> new StockLedgerEntry(
                tenant, product, location, 0, StockChangeReason.ADJUSTMENT, "TEST", null, user))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
