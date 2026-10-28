package com.cinema.payment.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class PaymentProviderRegistryTest {

    @Test
    void returnsProviderById() {
        PaymentProvider vnpay = provider(ProviderId.VNPAY);
        PaymentProviderRegistry registry = new PaymentProviderRegistry(List.of(vnpay));

        assertThat(registry.getRequired(ProviderId.VNPAY)).isSameAs(vnpay);
    }

    @Test
    void rejectsUnconfiguredProvider() {
        PaymentProviderRegistry registry = new PaymentProviderRegistry(List.of());

        assertThatThrownBy(() -> registry.getRequired(ProviderId.MOMO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MOMO");
    }

    @Test
    void rejectsDuplicateProviderRegistration() {
        assertThatThrownBy(() -> new PaymentProviderRegistry(
                List.of(provider(ProviderId.VNPAY), provider(ProviderId.VNPAY))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("VNPAY");
    }

    private static PaymentProvider provider(ProviderId id) {
        return new PaymentProvider() {
            @Override
            public ProviderId id() {
                return id;
            }

            @Override
            public CheckoutResult createCheckout(CheckoutCommand command) {
                throw new UnsupportedOperationException("Not used by this test");
            }
        };
    }
}
