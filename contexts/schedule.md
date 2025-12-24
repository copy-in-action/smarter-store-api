공연 회차 선택 프로세스

공연 상세페이지 이동

하단의 예매하기 버튼 클릭

캘린더가 보여지며 회차가 있는 날만 선택이 가능함.

날짜 선택 시 해당 일의 공연 회차 리스트 표시

공연 회차 선택 후 예매하기를 눌러 좌석배치도 페이지로 이동

필요 API

해당 공연의 공연 회차 날짜 리스트

예매 가능한 공연 날짜만 - 지금을 기준으로 티켓판매를 시작했고 공연을 아직 하지 않은 공연 회차

ex)

[
"2025-12-26T09:39:00",
"2025-12-27T14:22:00",
"2025-12-27T14:23:00",
"2025-12-29T14:23:00"
]

선택한 날짜의 공연 회차 리스트

공연시간 내림차순으로 정렬

각 좌석 등급의 잔여석 수

선택을 할때 마다 잔여석을 최신화 하기 위해서 별도의 API로 처리.

ex)

[
{
id: 18,
showDateTime: "2025-12-26T09:39:00",
ticketOptions: [
{
seatGrade: "R",
remainingSeats: 10,
},
{
seatGrade: "S",
remainingSeats: 10,
},
{
seatGrade: "A",
remainingSeats: 10,
},
{
seatGrade: "B",
remainingSeats: 10,
},
],
},
{
id: 18,
showDateTime: "2025-12-26T12:39:00",
ticketOptions: [
{
seatGrade: "R",
remainingSeats: 10,
},
{
seatGrade: "S",
remainingSeats: 10,
},
{
seatGrade: "A",
remainingSeats: 10,
},
{
seatGrade: "B",
remainingSeats: 10,
},
],
},
]

