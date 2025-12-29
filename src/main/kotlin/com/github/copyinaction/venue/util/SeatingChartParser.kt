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
 */
@Component
class SeatingChartParser(
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 요청된 좌석 등급이 사용 가능한 등급 목록에 존재하는지 검증
     * @throws CustomException(SEAT_GRADE_NOT_FOUND_IN_VENUE)
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

            // 1. seatTypes 파싱 (key -> label)
            val seatTypesNode = rootNode.get("seatTypes") ?: return null
            val typeToGradeMap = parseSeatTypes(seatTypesNode)

            // 2. seatGrades 파싱 (position -> seatTypeKey)
            val seatGradesNode = rootNode.get("seatGrades")
            if (seatGradesNode != null && seatGradesNode.isArray) {
                for (node in seatGradesNode) {
                    val position = node.get("position")?.asText() ?: continue
                    val targetRow = parseRowFromPosition(position)

                    if (targetRow == rowNum) {
                        val typeKey = node.get("seatTypeKey")?.asText()
                        return typeToGradeMap[typeKey]
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
            
            // 1. seatTypes 파싱
            val seatTypesNode = rootNode.get("seatTypes") ?: run {
                logger.warn("Missing 'seatTypes' field")
                return emptyMap()
            }
            val typeToGradeMap = parseSeatTypes(seatTypesNode)

            // 2. Disabled Seats 파싱
            val disabledSeats = parseDisabledSeats(rootNode.get("disabledSeats"))

            // 3. seatGrades 순회하며 계산
            val seatGradesNode = rootNode.get("seatGrades") ?: run {
                logger.warn("Missing 'seatGrades' field")
                return emptyMap()
            }

            val result = mutableMapOf<SeatGrade, Int>()

            if (seatGradesNode.isArray) {
                for (node in seatGradesNode) {
                    val typeKey = node.get("seatTypeKey")?.asText() ?: continue
                    val grade = typeToGradeMap[typeKey] ?: continue
                    
                    val position = node.get("position")?.asText() ?: continue
                    val row = parseRowFromPosition(position)
                    
                    if (row < 1) continue

                    // 해당 행(row)의 유효 좌석 수 계산 (1-based)
                    var count = 0
                    for (col in 1..columns) {
                        if (!disabledSeats.contains(Pair(row, col))) {
                            count++
                        }
                    }
                    
                    result[grade] = (result[grade] ?: 0) + count
                }
            }

            result
        } catch (e: Exception) {
            logger.error("Failed to count seats by grade", e)
            emptyMap()
        }
    }

    // Helper: seatTypes 맵핑 (예: "SEAT_CLASS_1" -> SeatGrade.R)
    private fun parseSeatTypes(seatTypesNode: JsonNode): Map<String, SeatGrade> {
        val map = mutableMapOf<String, SeatGrade>()
        val fields = seatTypesNode.fields()
        while (fields.hasNext()) {
            val entry = fields.next()
            val key = entry.key // e.g., "SEAT_CLASS_1"
            val label = entry.value.get("label")?.asText() // e.g., "R"
            
            if (label != null) {
                try {
                    map[key] = SeatGrade.valueOf(label)
                } catch (e: IllegalArgumentException) {
                    logger.warn("Unknown SeatGrade label: {}", label)
                }
            }
        }
        return map
    }

    // Helper: "1:" -> 1 (1-based 그대로 유지)
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
