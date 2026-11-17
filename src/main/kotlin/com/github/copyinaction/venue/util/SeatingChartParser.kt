package com.github.copyinaction.venue.util

import com.github.copyinaction.common.exception.CustomException
import com.github.copyinaction.common.exception.ErrorCode
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.copyinaction.venue.domain.SeatGrade
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 좌석 배치도 JSON 파싱 유틸
 *
 * JSON 형식:
 * {
 *   "seatTypes": {
 *     "VIP": { "positions": ["1:", "2:"] },
 *     "R": { "positions": ["3:", "4:"] }
 *   },
 *   "columns": 20,
 *   "disabledSeats": [{ "row": 1, "col": 1 }]
 * }
 */
@Component
class SeatingChartParser(
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 요청된 좌석 등급이 사용 가능한 등급 목록에 존재하는지 검증
     */
    fun validateSeatGrades(availableGrades: Set<SeatGrade>, requestedGrades: Set<SeatGrade>) {
        val invalidGrades = requestedGrades - availableGrades

        if (invalidGrades.isNotEmpty()) {
            throw CustomException(
                ErrorCode.SEAT_GRADE_NOT_FOUND_IN_VENUE,
                "공연장에 존재하지 않는 좌석 등급이 포함되어 있습니다: ${invalidGrades.joinToString { it.name }}"
            )
        }
    }

    /**
     * seatingChart JSON에서 특정 좌석(row, col)의 등급을 반환
     */
    fun getSeatGrade(seatingChartJson: String?, rowNum: Int, colNum: Int): SeatGrade? {
        if (seatingChartJson.isNullOrBlank()) return null

        return try {
            val rootNode = objectMapper.readTree(seatingChartJson)
            val seatTypesNode = rootNode.get("seatTypes") ?: return null

            val fields = seatTypesNode.fields()
            while (fields.hasNext()) {
                val entry = fields.next()
                val gradeName = entry.key
                val positionsNode = entry.value.get("positions")

                if (positionsNode != null && positionsNode.isArray) {
                    for (posNode in positionsNode) {
                        val position = posNode.asText()
                        val row = parseRowFromPosition(position)
                        if (row == rowNum) {
                            return try {
                                SeatGrade.valueOf(gradeName)
                            } catch (e: IllegalArgumentException) {
                                logger.warn("Unknown SeatGrade: {}", gradeName)
                                null
                            }
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            logger.error("Failed to parse seatingChartJson for getSeatGrade", e)
            null
        }
    }

    /**
     * seatingChart JSON에서 등급별 좌석 수를 계산
     */
    fun countSeatsByGrade(seatingChartJson: String?): Map<SeatGrade, Int> {
        if (seatingChartJson.isNullOrBlank()) {
            logger.warn("seatingChartJson is null or blank")
            return emptyMap()
        }

        return try {
            val rootNode = objectMapper.readTree(seatingChartJson)
            val columns = rootNode.get("columns")?.asInt() ?: run {
                logger.warn("Missing 'columns' field")
                return emptyMap()
            }

            val seatTypesNode = rootNode.get("seatTypes") ?: run {
                logger.warn("Missing 'seatTypes' field")
                return emptyMap()
            }

            val disabledSeats = parseDisabledSeats(rootNode.get("disabledSeats"))
            val result = mutableMapOf<SeatGrade, Int>()

            val fields = seatTypesNode.fields()
            while (fields.hasNext()) {
                val entry = fields.next()
                val gradeName = entry.key
                val positionsNode = entry.value.get("positions")

                val grade = try {
                    SeatGrade.valueOf(gradeName)
                } catch (e: IllegalArgumentException) {
                    logger.warn("Unknown SeatGrade: {}", gradeName)
                    continue
                }

                if (positionsNode != null && positionsNode.isArray) {
                    for (posNode in positionsNode) {
                        val position = posNode.asText()
                        val row = parseRowFromPosition(position)

                        if (row < 1) continue

                        // disabledSeats는 0-based(FE 규약), row/col 루프는 1-based이므로 변환하여 비교
                        var count = 0
                        for (col in 1..columns) {
                            if (!disabledSeats.contains(Pair(row - 1, col - 1))) {
                                count++
                            }
                        }

                        result[grade] = (result[grade] ?: 0) + count
                    }
                }
            }

            result
        } catch (e: Exception) {
            logger.error("Failed to count seats by grade", e)
            emptyMap()
        }
    }

    /**
     * 비활성화 좌석 여부 확인 (rowNum/colNum은 1-based 좌석 좌표)
     * - JSON의 disabledSeats는 0-based 이므로 내부에서 (row-1, col-1)로 변환하여 비교한다.
     */
    fun isSeatDisabled(seatingChartJson: String?, rowNum: Int, colNum: Int): Boolean {
        return getDisabledSeats(seatingChartJson).contains(Pair(rowNum, colNum))
    }

    /**
     * 비활성화 좌석 목록을 **1-based 좌석 좌표**(rowNum, colNum)로 변환하여 반환
     * - JSON의 disabledSeats(0-based)에 +1을 적용한다.
     * - 좌석 상태/예매 좌표계(1-based)와 직접 비교할 수 있도록 하기 위함.
     */
    fun getDisabledSeats(seatingChartJson: String?): Set<Pair<Int, Int>> {
        if (seatingChartJson.isNullOrBlank()) return emptySet()

        return try {
            val rootNode = objectMapper.readTree(seatingChartJson)
            parseDisabledSeats(rootNode.get("disabledSeats"))
                .map { (row, col) -> Pair(row + 1, col + 1) }
                .toSet()
        } catch (e: Exception) {
            logger.error("Failed to parse disabledSeats", e)
            emptySet()
        }
    }

    /**
     * 좌석 배치도를 좌석 단위 레이아웃(1-based)으로 파싱한다.
     * - 데모 시딩/시뮬레이터가 유효 좌석 풀을 구성할 때 사용한다.
     * - 파싱 불가 시 null 반환
     */
    fun parseLayout(seatingChartJson: String?): SeatLayout? {
        if (seatingChartJson.isNullOrBlank()) return null

        return try {
            val rootNode = objectMapper.readTree(seatingChartJson)
            val columns = rootNode.get("columns")?.asInt() ?: return null
            val seatTypesNode = rootNode.get("seatTypes") ?: return null

            val rowGrades = mutableMapOf<Int, SeatGrade>()
            val fields = seatTypesNode.fields()
            while (fields.hasNext()) {
                val entry = fields.next()
                val grade = runCatching { SeatGrade.valueOf(entry.key) }.getOrNull() ?: continue
                val positionsNode = entry.value.get("positions")
                if (positionsNode == null || !positionsNode.isArray) continue
                for (posNode in positionsNode) {
                    val row = parseRowFromPosition(posNode.asText())
                    if (row >= 1) rowGrades[row] = grade
                }
            }
            if (rowGrades.isEmpty()) return null

            val rows = rootNode.get("rows")?.asInt() ?: rowGrades.keys.max()
            val disabled = getDisabledSeats(seatingChartJson)

            // columnSpacers: 0-based 인덱스 "직전" 삽입 -> 1-based 열 s와 s+1 사이에 통로가 존재
            val columnSpacers = rootNode.get("columnSpacers")
                ?.takeIf { it.isArray }
                ?.map { it.asInt() }
                ?.toSet()
                ?: emptySet()

            SeatLayout(
                rows = rows,
                columns = columns,
                rowGrades = rowGrades,
                disabledSeats = disabled,
                columnSpacers = columnSpacers
            )
        } catch (e: Exception) {
            logger.error("Failed to parse seating layout", e)
            null
        }
    }

    // Helper: "1:" -> 1
    private fun parseRowFromPosition(position: String): Int {
        return try {
            val numStr = position.replace(":", "").trim()
            numStr.toInt()
        } catch (e: NumberFormatException) {
            -1
        }
    }

    private fun parseDisabledSeats(disabledSeatsNode: JsonNode?): Set<Pair<Int, Int>> {
        if (disabledSeatsNode == null || !disabledSeatsNode.isArray) {
            return emptySet()
        }
        return disabledSeatsNode.mapNotNull { seat ->
            val row = seat.get("row")?.asInt()
            val col = seat.get("col")?.asInt()
            if (row != null && col != null) Pair(row, col) else null
        }.toSet()
    }
}


/**
 * 좌석 배치도의 좌석 단위 레이아웃 (모든 좌표 1-based)
 *
 * @property rows 전체 행 수
 * @property columns 전체 열 수
 * @property rowGrades 행 번호 -> 좌석 등급
 * @property disabledSeats 비활성화 좌석 (rowNum, colNum) - 1-based
 * @property columnSpacers 열 통로 위치. 값 s는 1-based 열 s와 s+1 사이에 통로가 있음을 의미
 */
data class SeatLayout(
    val rows: Int,
    val columns: Int,
    val rowGrades: Map<Int, SeatGrade>,
    val disabledSeats: Set<Pair<Int, Int>>,
    val columnSpacers: Set<Int> = emptySet()
) {
    /**
     * 예매 가능한 모든 좌석(등급이 지정된 행 중 비활성화되지 않은 좌석)을 반환한다.
     */
    fun availableSeats(): List<LayoutSeat> {
        val seats = mutableListOf<LayoutSeat>()
        for (row in 1..rows) {
            val grade = rowGrades[row] ?: continue
            for (col in 1..columns) {
                if (!disabledSeats.contains(Pair(row, col))) {
                    seats.add(LayoutSeat(row, col, grade))
                }
            }
        }
        return seats
    }

    fun gradeOf(row: Int): SeatGrade? = rowGrades[row]

    /**
     * 같은 행의 [startCol, startCol + size - 1] 구간이 통로(columnSpacers)를 가로지르는지 여부
     */
    fun crossesColumnSpacer(startCol: Int, size: Int): Boolean {
        val endCol = startCol + size - 1
        return columnSpacers.any { spacer -> spacer in startCol until endCol }
    }
}

/**
 * 배치도 상의 좌석 1석 (1-based)
 */
data class LayoutSeat(
    val row: Int,
    val col: Int,
    val grade: SeatGrade
)
