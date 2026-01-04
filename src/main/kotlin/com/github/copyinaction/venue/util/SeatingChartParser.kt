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

                        var count = 0
                        for (col in 1..columns) {
                            if (!disabledSeats.contains(Pair(row, col))) {
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
