package com.github.copyinaction.venue.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.copyinaction.venue.domain.SeatGrade
import org.springframework.stereotype.Component

/**
 * 좌석 배치도 JSON 파싱 유틸
 */
@Component
class SeatingChartParser(
    private val objectMapper: ObjectMapper
) {

    /**
     * seatingChart JSON에서 특정 좌석(row, col)의 등급을 반환
     * seatGrades 설정이 없으면 null 반환
     */
    fun getSeatGrade(seatingChartJson: String?, rowNum: Int, colNum: Int): SeatGrade? {
        if (seatingChartJson.isNullOrBlank()) return null

        return try {
            val rootNode = objectMapper.readTree(seatingChartJson)
            val seatGrades = rootNode.get("seatGrades") ?: return null

            if (!seatGrades.isArray || seatGrades.isEmpty) return null

            for (gradeConfig in seatGrades) {
                if (isRowInGradeRange(gradeConfig, rowNum)) {
                    val gradeName = gradeConfig.get("grade")?.asText() ?: continue
                    return try {
                        SeatGrade.valueOf(gradeName)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * seatingChart JSON에서 등급별 좌석 수를 계산
     * - 총 좌석 수에서 disabledSeats를 제외
     * - seatGrades 설정에 따라 등급별로 분류
     */
    fun countSeatsByGrade(seatingChartJson: String?): Map<SeatGrade, Int> {
        if (seatingChartJson.isNullOrBlank()) return emptyMap()

        return try {
            val rootNode = objectMapper.readTree(seatingChartJson)
            val rows = rootNode.get("rows")?.asInt() ?: return emptyMap()
            val columns = rootNode.get("columns")?.asInt() ?: return emptyMap()
            val disabledSeats = parseDisabledSeats(rootNode.get("disabledSeats"))
            val seatGrades = rootNode.get("seatGrades")

            if (seatGrades == null || !seatGrades.isArray || seatGrades.isEmpty) {
                return emptyMap()
            }

            val result = mutableMapOf<SeatGrade, Int>()

            for (gradeConfig in seatGrades) {
                val gradeName = gradeConfig.get("grade")?.asText() ?: continue
                val grade = try {
                    SeatGrade.valueOf(gradeName)
                } catch (e: IllegalArgumentException) {
                    continue
                }

                val gradeRows = getRowsForGrade(gradeConfig, rows)
                var count = 0
                for (row in gradeRows) {
                    for (col in 0 until columns) {
                        if (!disabledSeats.contains(Pair(row, col))) {
                            count++
                        }
                    }
                }
                result[grade] = count
            }

            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun isRowInGradeRange(gradeConfig: JsonNode, rowNum: Int): Boolean {
        // rows 배열 형식: { grade: "VIP", rows: [0, 1, 2] }
        val rowsNode = gradeConfig.get("rows")
        if (rowsNode != null && rowsNode.isArray) {
            return rowsNode.any { it.asInt() == rowNum }
        }

        // startRow/endRow 형식: { grade: "VIP", startRow: 0, endRow: 2 }
        val startRow = gradeConfig.get("startRow")?.asInt()
        val endRow = gradeConfig.get("endRow")?.asInt()
        if (startRow != null && endRow != null) {
            return rowNum in startRow..endRow
        }

        return false
    }

    private fun getRowsForGrade(gradeConfig: JsonNode, totalRows: Int): List<Int> {
        // rows 배열 형식
        val rowsNode = gradeConfig.get("rows")
        if (rowsNode != null && rowsNode.isArray) {
            return rowsNode.map { it.asInt() }
        }

        // startRow/endRow 형식
        val startRow = gradeConfig.get("startRow")?.asInt() ?: return emptyList()
        val endRow = gradeConfig.get("endRow")?.asInt() ?: return emptyList()
        return (startRow..minOf(endRow, totalRows - 1)).toList()
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
