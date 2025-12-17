export interface SeatChartConfig {
    /** 총 행 수 */
    rows: number;
    /** 총 열 수 */
    columns: number;
    /** 차트 모드 */
    mode: SeatChartMode;
    /** 좌석 타입들 */
    seatTypes: Record<string, SeatType>;
    /** 좌석 등급 설정들 */
    seatGrades?: SeatGradeConfig[];
    /** 비활성화된 좌석들 */
    disabledSeats: SeatPosition[];
    /** 예약된 좌석들 */
    reservedSeats: SeatPosition[];
    /** 구매 진행 중인 좌석들 */
    pendingSeats: SeatPosition[];
    /** 선택된 좌석들 */
    selectedSeats: SeatPosition[];
    /** 행 간격 추가 위치들 */
    rowSpacers: number[];
    /** 열 간격 추가 위치들 */
    columnSpacers: number[];
}
const config: SeatChartConfig = {
    "rows": 10,
    "columns": 10,
    "mode": "edit",
    "seatTypes": {
        "default": {
            "label": "Economy",
            "cssClass": "economy",
            "price": 15,
            "color": "#10B981"
        }
    },
    "seatGrades": [],
    "disabledSeats": [
        {
            "row": 0,
            "col": 0
        },
        {
            "row": 1,
            "col": 0
        },
        {
            "row": 2,
            "col": 0
        },
        {
            "row": 0,
            "col": 9
        },
        {
            "row": 1,
            "col": 9
        },
        {
            "row": 2,
            "col": 9
        }
    ],
    "reservedSeats": [],
    "pendingSeats": [
        {
            "row": 3,
            "col": 3
        },
        {
            "row": 4,
            "col": 4
        }
    ],
    "selectedSeats": [],
    "rowSpacers": [],
    "columnSpacers": []
}