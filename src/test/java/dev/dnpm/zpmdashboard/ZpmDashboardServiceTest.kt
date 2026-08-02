/*
 * This file is part of zpm-dashboard
 *
 * Copyright (C) 2026  the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package dev.dnpm.zpmdashboard

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import javax.sql.DataSource

@RunWith(SpringJUnit4ClassRunner::class)
@ContextConfiguration(classes = [TestConfig::class])
@Sql(
    scripts = [
        "/testdata/schema.sql",
        "/testdata/patient.sql",
        "/testdata/mtb-anmeldung.sql",
        "/testdata/mtb-empfehlung.sql"
    ],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
    statements = ["DROP ALL OBJECTS;"],
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class ZpmDashboardServiceTest {

    lateinit var service: ZpmDashboardService

    @Autowired
    lateinit var dataSource: DataSource

    @Before
    fun setUp() {
        service = ZpmDashboardService(dataSource)
    }

    @Test
    fun shouldReturnCountSelectedYearIfAnmeldungPresent() {
        val actual = service.countMtbAnmeldungInYear(2026)
        assertThat(actual).isEqualTo(2)
    }

    @Test
    fun shouldReturnZeroForSelectedYearIfNoAnmeldungIsPresent() {
        val actual = service.countMtbAnmeldungInYear(2000)
        assertThat(actual).isEqualTo(0)
    }

    @Test
    fun shouldReturnCountSelectedYearIfEmpfehlungPresent() {
        val actual = service.countMtbEmpfehlungInYear(2026)
        assertThat(actual).isEqualTo(1)
    }

    @Test
    fun shouldReturnZeroForSelectedYearIfNoEmpfehlungIsPresent() {
        val actual = service.countMtbEmpfehlungInYear(2000)
        assertThat(actual).isEqualTo(0)
    }

}
