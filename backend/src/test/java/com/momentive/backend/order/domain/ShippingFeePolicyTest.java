package com.momentive.backend.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ShippingFeePolicyTest {

    @Test
    void base_fee_when_below_threshold_and_not_jeju() {
        assertThat(ShippingFeePolicy.calculate(69_999, "12345")).isEqualTo(3_400);
    }

    @Test
    void free_shipping_when_at_or_above_threshold_and_not_jeju() {
        assertThat(ShippingFeePolicy.calculate(70_000, "12345")).isEqualTo(0);
    }

    @Test
    void jeju_surcharge_added_on_top_of_base_fee_when_below_threshold() {
        assertThat(ShippingFeePolicy.calculate(69_999, "63000")).isEqualTo(7_400);
    }

    @Test
    void only_jeju_surcharge_remains_when_at_or_above_threshold() {
        assertThat(ShippingFeePolicy.calculate(70_000, "63644")).isEqualTo(4_000);
    }

    @Test
    void zipcode_just_below_jeju_range_is_not_treated_as_jeju() {
        assertThat(ShippingFeePolicy.calculate(0, "62999")).isEqualTo(3_400);
    }

    @Test
    void zipcode_at_jeju_range_lower_bound_is_treated_as_jeju() {
        assertThat(ShippingFeePolicy.calculate(0, "63000")).isEqualTo(7_400);
    }

    @Test
    void zipcode_at_jeju_range_upper_bound_is_treated_as_jeju() {
        assertThat(ShippingFeePolicy.calculate(0, "63644")).isEqualTo(7_400);
    }

    @Test
    void zipcode_just_above_jeju_range_is_not_treated_as_jeju() {
        assertThat(ShippingFeePolicy.calculate(0, "63645")).isEqualTo(3_400);
    }

    @Test
    void non_numeric_zipcode_is_safely_treated_as_not_jeju() {
        assertThat(ShippingFeePolicy.calculate(0, "제주시")).isEqualTo(3_400);
    }

    @Test
    void null_zipcode_is_safely_treated_as_not_jeju() {
        assertThat(ShippingFeePolicy.calculate(0, null)).isEqualTo(3_400);
    }
}
