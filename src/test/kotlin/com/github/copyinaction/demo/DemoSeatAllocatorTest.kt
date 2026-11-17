package com.github.copyinaction.demo

import com.github.copyinaction.demo.service.DemoOccupancyPreset
import com.github.copyinaction.demo.service.DemoScheduleRollingService
import com.github.copyinaction.demo.service.DemoSeatAllocator
import com.github.copyinaction.venue.domain.SeatGrade
import com.github.copyinaction.venue.util.SeatLayout
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

class DemoSeatAllocatorTest {

    /** 4행 x 10열, 1~2행 VIP / 3~4행 R, 5열-6열 사이 통로, (1,1) 비활성 */
    private val layout = SeatLayout(
        rows = 4,
        columns = 10,
        rowGrades = mapOf(1 to SeatGrade.VIP, 2 to SeatGrade.VIP, 3 to SeatGrade.R, 4 to SeatGrade.R),
        disabledSeats = setOf(Pair(1, 1)),
        columnSpacers = setOf(5)
    )

    @Test
    fun `allocate returns adjacent seats in the same row without crossing aisle`() {
        val allocator = DemoSeatAllocator(layout, emptyList(), Random(42))

        repeat(30) {
            val seats = allocator.allocate(4) ?: return@repeat
            assertEquals(4, seats.size)
            assertEquals(1, seats.map { it.row }.toSet().size)
            val cols = seats.map { it.col }.sorted()
            assertEquals(cols.first()..cols.last(), cols.first()..(cols.first() + 3))
            assertFalse(layout.crossesColumnSpacer(cols.first(), 4))
            assertTrue(seats.none { it.row == 1 && it.col == 1 })
        }
    }

    @Test
    fun `allocate never returns occupied or duplicated seats`() {
        val occupied = listOf(Pair(2, 5), Pair(2, 6))
        val allocator = DemoSeatAllocator(layout, occupied, Random(7))
        val allocated = mutableSetOf<Pair<Int, Int>>()

        while (true) {
            val seats = allocator.allocateBestEffort(3) ?: break
            seats.forEach { seat ->
                val pos = Pair(seat.row, seat.col)
                assertFalse(occupied.contains(pos), "점유 좌석 재할당: $pos")
                assertTrue(allocated.add(pos), "중복 할당: $pos")
            }
        }

        // 가용 좌석(40 - 비활성 1 - 점유 2 = 37)을 모두 소진해야 한다.
        assertEquals(37, allocated.size)
        assertEquals(0, allocator.freeCount())
    }

    @Test
    fun `allocate prefers center columns on average`() {
        val wideLayout = SeatLayout(1, 25, mapOf(1 to SeatGrade.R), emptySet())
        val centers = (1..200).map {
            DemoSeatAllocator(wideLayout, emptyList(), Random(it)).allocate(1)!!.first().col
        }

        val average = centers.average()
        assertTrue(average in 10.0..16.0, "가운데(13) 근처에 몰려야 합니다. 평균=$average")
    }

    @Test
    fun `allocate returns null when no window fits`() {
        val tiny = SeatLayout(1, 3, mapOf(1 to SeatGrade.R), emptySet())
        val allocator = DemoSeatAllocator(tiny, emptyList(), Random(1))

        assertNull(allocator.allocate(4))
        assertNotNull(allocator.allocate(3))
        assertNull(allocator.allocateBestEffort(1))
    }

    @Test
    fun `target ratio follows weekend weekday and far-future presets`() {
        val today = LocalDate.of(2026, 11, 17) // 화요일
        val saturdayNight = LocalDateTime.of(2026, 11, 21, 19, 30)
        val weekdayNight = LocalDateTime.of(2026, 11, 19, 19, 30)
        val weekdayDay = LocalDateTime.of(2026, 11, 19, 14, 0)
        val farFuture = LocalDateTime.of(2027, 1, 5, 19, 30)

        repeat(100) { seed ->
            val random = Random(seed)
            assertTrue(DemoOccupancyPreset.targetRatio(saturdayNight, today, random) in 0.0..0.80)
            assertTrue(DemoOccupancyPreset.targetRatio(weekdayNight, today, random) in 0.0..0.55)
            assertTrue(DemoOccupancyPreset.targetRatio(weekdayDay, today, random) in 0.0..0.30)
            assertTrue(DemoOccupancyPreset.targetRatio(farFuture, today, random) in 0.05..0.25)
        }
    }

    @Test
    fun `group size is always between 1 and 4`() {
        repeat(500) {
            assertTrue(DemoOccupancyPreset.pickGroupSize(Random(it)) in 1..4)
        }
    }

    @Test
    fun `target end date is spread by performance id within 15 days`() {
        val today = LocalDate.of(2026, 11, 17)

        assertEquals(today.plusDays(56), DemoScheduleRollingService.targetEndDate(today, 56, 15))
        assertEquals(today.plusDays(57), DemoScheduleRollingService.targetEndDate(today, 56, 1))
        assertEquals(today.plusDays(70), DemoScheduleRollingService.targetEndDate(today, 56, 14))
    }
}
