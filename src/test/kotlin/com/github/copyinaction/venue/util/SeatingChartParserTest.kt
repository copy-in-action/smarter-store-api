package com.github.copyinaction.venue.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.copyinaction.venue.domain.SeatGrade
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SeatingChartParserTest {

    private val objectMapper = ObjectMapper()
    private val parser = SeatingChartParser(objectMapper)

    @Test
    fun `countSeatsByGrade parses valid json correctly`() {
        // Given
        val json = """
            {
                "rows": 5,
                "columns": 10,
                "seatGrades": [
                    { "grade": "VIP", "rows": [0, 1] },
                    { "grade": "R", "startRow": 2, "endRow": 4 }
                ],
                "disabledSeats": [
                    { "row": 0, "col": 0 },
                    { "row": 2, "col": 5 }
                ]
            }
        """.trimIndent()

        // When
        val result = parser.countSeatsByGrade(json)

        // Then
        // VIP: 2 rows (0, 1) * 10 cols = 20 seats. 1 disabled (0,0) -> 19
        // R: 3 rows (2, 3, 4) * 10 cols = 30 seats. 1 disabled (2,5) -> 29
        assertEquals(19, result[SeatGrade.VIP])
        assertEquals(29, result[SeatGrade.R])
    }

    @Test
    fun `countSeatsByGrade returns empty map for null or blank json`() {
        assertTrue(parser.countSeatsByGrade(null).isEmpty())
        assertTrue(parser.countSeatsByGrade("").isEmpty())
    }

    @Test
    fun `getSeatGrade returns correct grade`() {
        val json = """
            {
                "rows": 5,
                "columns": 10,
                "seatGrades": [
                    { "grade": "VIP", "rows": [0, 1] },
                    { "grade": "R", "startRow": 2, "endRow": 4 }
                ]
            }
        """.trimIndent()

        assertEquals(SeatGrade.VIP, parser.getSeatGrade(json, 0, 5))
        assertEquals(SeatGrade.VIP, parser.getSeatGrade(json, 1, 9))
        assertEquals(SeatGrade.R, parser.getSeatGrade(json, 2, 0))
        assertEquals(SeatGrade.R, parser.getSeatGrade(json, 4, 9))
        assertNull(parser.getSeatGrade(json, 5, 0)) // Out of range
    }
}
