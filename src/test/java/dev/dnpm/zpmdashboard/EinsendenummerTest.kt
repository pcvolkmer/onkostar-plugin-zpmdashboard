package dev.dnpm.zpmdashboard

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class EinsendenummerTest {

    @Test
    fun test() {
        assertThat(
            Einsendenummer("A/20/123").matches(Einsendenummer("A/2020/123"))
        ).isTrue()

        assertThat(
            Einsendenummer("A123-20").matches(Einsendenummer("A/2020/123"))
        ).isTrue()

        assertThat(
            Einsendenummer("A/20/123").matches(Einsendenummer("A0123-20"))
        ).isTrue()

        assertThat(
            Einsendenummer("A/20/0123").matches(Einsendenummer("A/2020/123"))
        ).isTrue()

        assertThat(
            Einsendenummer("A/2020/123").matches(Einsendenummer("A/2020/123"))
        ).isTrue()

        assertThat(
            Einsendenummer("A/2020/123.100").matches(Einsendenummer("A/2020/123"))
        ).isTrue()

        assertThat(
            Einsendenummer("A/20/123.100").matches(Einsendenummer("A/2020/123"))
        ).isTrue()

        assertThat(
            Einsendenummer(null).matches(Einsendenummer(null))
        ).isFalse

        assertThat(
            Einsendenummer("").matches(Einsendenummer("A/2020/123"))
        ).isFalse
    }

}