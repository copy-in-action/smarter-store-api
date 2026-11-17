package com.github.copyinaction.demo.service

import com.github.copyinaction.venue.util.LayoutSeat
import com.github.copyinaction.venue.util.SeatLayout
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.exp
import kotlin.random.Random

/**
 * 좌석 배치도에서 "자연스러운" 연석 그룹을 뽑아주는 순수 로직 (Spring 의존 없음)
 *
 * - Center-Out 가중치: 무대 정면(가운데 열)에 가까운 좌석이 먼저 팔리도록 가중치를 준다.
 * - 같은 행의 인접한 빈 좌석 1~4석을 묶어서 반환하며, 통로(columnSpacers)를 가로지르는 묶음은 제외한다.
 * - 모든 좌표는 1-based 이다.
 */
class DemoSeatAllocator(
    private val layout: SeatLayout,
    occupied: Collection<Pair<Int, Int>>,
    private val random: Random = Random.Default
) {
    private val taken: MutableSet<Pair<Int, Int>> = occupied.toHashSet()

    /** 아직 비어 있는(예매 가능) 좌석 수 */
    fun freeCount(): Int = layout.availableSeats().count { !taken.contains(Pair(it.row, it.col)) }

    /**
     * 정확히 [size]석 연석을 확보한다. 가능한 묶음이 없으면 null.
     * 확보된 좌석은 내부적으로 점유 처리되어 다음 호출에서 제외된다.
     */
    fun allocate(size: Int): List<LayoutSeat>? {
        require(size in 1..4) { "연석 크기는 1~4석이어야 합니다: $size" }

        val windows = mutableListOf<Window>()
        for ((row, grade) in layout.rowGrades) {
            if (row !in 1..layout.rows) continue
            for (startCol in 1..(layout.columns - size + 1)) {
                if (layout.crossesColumnSpacer(startCol, size)) continue
                val cols = startCol until startCol + size
                if (cols.any { col -> isUnavailable(row, col) }) continue
                windows.add(Window(row, startCol, grade, centerWeight(startCol, size)))
            }
        }
        if (windows.isEmpty()) return null

        val picked = weightedPick(windows)
        val seats = (picked.startCol until picked.startCol + size).map { col ->
            LayoutSeat(picked.row, col, picked.grade)
        }
        seats.forEach { taken.add(Pair(it.row, it.col)) }
        return seats
    }

    /**
     * [maxSize]석을 우선 시도하고, 불가능하면 한 칸씩 줄여가며 확보한다.
     */
    fun allocateBestEffort(maxSize: Int): List<LayoutSeat>? {
        for (size in maxSize.coerceIn(1, 4) downTo 1) {
            allocate(size)?.let { return it }
        }
        return null
    }

    private fun isUnavailable(row: Int, col: Int): Boolean {
        val pos = Pair(row, col)
        return layout.disabledSeats.contains(pos) || taken.contains(pos)
    }

    /** 가운데 열에 가까울수록 큰 가중치 (지수 감쇠) */
    private fun centerWeight(startCol: Int, size: Int): Double {
        val center = (layout.columns + 1) / 2.0
        val windowCenter = startCol + (size - 1) / 2.0
        val spread = layout.columns * 0.3
        return exp(-abs(windowCenter - center) / spread) + 0.05
    }

    private fun weightedPick(windows: List<Window>): Window {
        val total = windows.sumOf { it.weight }
        var cursor = random.nextDouble() * total
        for (window in windows) {
            cursor -= window.weight
            if (cursor <= 0) return window
        }
        return windows.last()
    }

    private data class Window(val row: Int, val startCol: Int, val grade: com.github.copyinaction.venue.domain.SeatGrade, val weight: Double)
}

/**
 * 데모 초기 점유율/연석 크기 프리셋 (순수 함수)
 */
object DemoOccupancyPreset {

    /**
     * 회차의 목표 점유율(0.0~1.0)을 결정한다.
     *
     * - 오늘+1 ~ +28일: 주말 70~80%, 평일 저녁(18시 이후) 40~55%, 평일 낮 15~30%, 일부(10%) 회차는 0~5%
     * - 오늘+29일 이후: 5~25% 경량 시딩
     */
    fun targetRatio(showDateTime: LocalDateTime, today: LocalDate, random: Random = Random.Default): Double {
        val daysAhead = java.time.temporal.ChronoUnit.DAYS.between(today, showDateTime.toLocalDate())

        if (daysAhead > 28) {
            return random.nextDouble(0.05, 0.25)
        }

        // 일부 회차는 거의 비어 있어야 "아직 자리가 많은" 회차도 데모에서 볼 수 있다.
        if (random.nextDouble() < 0.10) {
            return random.nextDouble(0.0, 0.05)
        }

        val isWeekend = showDateTime.dayOfWeek == DayOfWeek.SATURDAY || showDateTime.dayOfWeek == DayOfWeek.SUNDAY
        return when {
            isWeekend -> random.nextDouble(0.70, 0.80)
            showDateTime.hour >= 18 -> random.nextDouble(0.40, 0.55)
            else -> random.nextDouble(0.15, 0.30)
        }
    }

    /**
     * 연석 크기(1~4) 분포: 1석 20%, 2석 45%, 3석 20%, 4석 15%
     */
    fun pickGroupSize(random: Random = Random.Default): Int {
        val value = random.nextInt(100)
        return when {
            value < 20 -> 1
            value < 65 -> 2
            value < 85 -> 3
            else -> 4
        }
    }
}
