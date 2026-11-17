package com.github.copyinaction.venue.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.copyinaction.venue.domain.SeatGrade
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SeatingChartParserTest {

    private val objectMapper = ObjectMapper()
    private val parser = SeatingChartParser(objectMapper)

    /**
     * 5행 x 10열
     * - VIP: 1~2행, R: 3~5행 (positions는 1-based 행 번호)
     * - disabledSeats는 0-based: (0,0) = 1행 1열, (2,5) = 3행 6열
     * - columnSpacers [5]: 5열과 6열 사이 통로
     */
    private val chartJson = """
        {
            "rows": 5,
            "columns": 10,
            "seatTypes": {
                "VIP": { "positions": ["1:", "2:"] },
                "R": { "positions": ["3:", "4:", "5:"] }
            },
            "disabledSeats": [
                { "row": 0, "col": 0 },
                { "row": 2, "col": 5 }
            ],
            "rowSpacers": [2],
            "columnSpacers": [5]
        }
    """.trimIndent()

    @Test
    fun `countSeatsByGrade excludes 0-based disabledSeats`() {
        val result = parser.countSeatsByGrade(chartJson)

        // VIP: 2행 x 10열 = 20석, 1행1열 비활성 -> 19
        // R: 3행 x 10열 = 30석, 3행6열 비활성 -> 29
        assertEquals(19, result[SeatGrade.VIP])
        assertEquals(29, result[SeatGrade.R])
    }

    @Test
    fun `countSeatsByGrade returns empty map for null or blank json`() {
        assertTrue(parser.countSeatsByGrade(null).isEmpty())
        assertTrue(parser.countSeatsByGrade("").isEmpty())
    }

    @Test
    fun `getSeatGrade returns grade by 1-based row`() {
        assertEquals(SeatGrade.VIP, parser.getSeatGrade(chartJson, 1, 5))
        assertEquals(SeatGrade.VIP, parser.getSeatGrade(chartJson, 2, 9))
        assertEquals(SeatGrade.R, parser.getSeatGrade(chartJson, 3, 1))
        assertEquals(SeatGrade.R, parser.getSeatGrade(chartJson, 5, 10))
        assertNull(parser.getSeatGrade(chartJson, 6, 1)) // 범위 밖
    }

    @Test
    fun `isSeatDisabled converts 0-based disabledSeats to 1-based seat coordinates`() {
        assertTrue(parser.isSeatDisabled(chartJson, 1, 1))
        assertTrue(parser.isSeatDisabled(chartJson, 3, 6))
        assertFalse(parser.isSeatDisabled(chartJson, 0, 0))
        assertFalse(parser.isSeatDisabled(chartJson, 2, 1))
        assertFalse(parser.isSeatDisabled(null, 1, 1))
    }

    @Test
    fun `getDisabledSeats returns 1-based coordinates`() {
        assertEquals(setOf(Pair(1, 1), Pair(3, 6)), parser.getDisabledSeats(chartJson))
        assertTrue(parser.getDisabledSeats("").isEmpty())
    }

    @Test
    fun `parseLayout builds available seats consistent with countSeatsByGrade`() {
        val layout = parser.parseLayout(chartJson)!!

        assertEquals(5, layout.rows)
        assertEquals(10, layout.columns)
        assertEquals(setOf(5), layout.columnSpacers)

        val seats = layout.availableSeats()
        assertEquals(48, seats.size)
        assertEquals(19, seats.count { it.grade == SeatGrade.VIP })
        assertEquals(29, seats.count { it.grade == SeatGrade.R })
        assertTrue(seats.none { it.row == 1 && it.col == 1 })
        assertTrue(seats.none { it.row == 3 && it.col == 6 })
        assertTrue(seats.all { it.row in 1..5 && it.col in 1..10 })
    }

    @Test
    fun `parseLayout returns null for invalid json`() {
        assertNull(parser.parseLayout(null))
        assertNull(parser.parseLayout("{}"))
        assertNull(parser.parseLayout("not-json"))
    }

    @Test
    fun `crossesColumnSpacer detects aisle between columns`() {
        val layout = parser.parseLayout(chartJson)!!

        assertFalse(layout.crossesColumnSpacer(4, 2))  // 4,5열: 통로 이전까지
        assertTrue(layout.crossesColumnSpacer(5, 2))   // 5,6열: 통로를 가로지름
        assertFalse(layout.crossesColumnSpacer(6, 3))  // 6~8열: 통로 이후
    }
}
