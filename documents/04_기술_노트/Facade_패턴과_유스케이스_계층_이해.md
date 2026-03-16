# Facade 패턴과 유스케이스 계층 이해

| 버전 | 날짜 | 작성자 | 내용 |
|------|------|--------|------|
| 1.1  | 2026-03-16 | Gemini | 실 프로젝트 적용 사례(Payment/Booking Facade)를 바탕으로 상세화 |
| 1.0  | 2026-03-07 | Gemini | 초기 리팩토링 로드맵 수립 시 작성 |

---

## 1. 배경: '비대한 서비스(Fat Service)'의 문제점

비즈니스가 복잡해짐에 따라 단일 서비스 클래스가 너무 많은 책임을 갖게 되는 현상을 **God Class** 또는 **Fat Service**라고 합니다. 이는 다음과 같은 유지보수 비용을 발생시킵니다.

1.  **높은 결합도(Tight Coupling)**: 하나의 서비스가 여러 도메인의 Repository와 Service를 의존하게 되어, 특정 도메인 변경 시 영향 범위를 파악하기 어렵습니다.
2.  **순환 참조 위험**: 서비스 간에 서로를 참조하게 될 확률이 높아지며, 이는 애플리케이션 기동 실패로 이어집니다.
3.  **트랜잭션 경계 모호**: 여러 도메인의 상태를 변경하는 거대한 트랜잭션이 서비스에 위치하게 되어, 일부 로직 실패 시의 롤백 정책을 제어하기 까다로워집니다.
4.  **낮은 응집도**: 서비스 클래스 내부에 도메인 비즈니스 로직과 단순 오케스트레이션(순서 제어) 로직이 섞여 코드의 의도를 파악하기 힘들어집니다.

---

## 2. Facade (Application Service) 계층의 역할

Facade 계층은 **"누가 무엇을 할지"**를 결정하는 **지휘자(Conductor)** 역할을 수행합니다.

### 핵심 원칙
- **순서 조율(Orchestration)**: 여러 도메인 서비스를 호출하여 하나의 비즈니스 시나리오(유스케이스)를 완성합니다.
- **도메인 로직 포함 금지**: "금액을 계산한다"거나 "상태를 변경한다"와 같은 비즈니스 규칙은 도메인 엔티티나 도메인 서비스에 있어야 합니다. Facade는 단지 그들을 호출할 뿐입니다.
- **트랜잭션 관리**: 하나의 유스케이스가 성공하거나 실패해야 하는 단위를 `@Transactional`로 묶어 관리합니다.
- **인프라/외부 시스템 연동**: 이벤트 발행(`ApplicationEventPublisher`), 외부 API 호출 등을 담당하여 도메인 서비스를 순수하게 유지합니다.

---

## 3. 실 프로젝트 적용 사례

### 3.1 PaymentFacade (결제 오케스트레이션)
결제는 **예매, 쿠폰, 결제** 세 가지 도메인이 복잡하게 얽히는 유스케이스입니다.

```kotlin
@Component
class PaymentFacade(
    private val paymentService: PaymentService, // 결제 도메인 로직 담당
    private val couponService: CouponService,   // 쿠폰 도메인 로직 담당
    private val bookingRepository: BookingRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    @Transactional
    fun createPayment(userId: Long, request: PaymentCreateRequest): PaymentResponse {
        // 1. 도메인 서비스 호출 (엔티티 생성 및 기본 검증)
        val payment = paymentService.preparePayment(userId, request, booking)

        // 2. 다른 도메인 서비스 조율 (할인/쿠폰 처리)
        paymentService.processDiscountsWithCoupons(userId, payment, booking, request)

        // 3. 최종 저장 및 반환
        val savedPayment = paymentService.savePayment(payment)
        return PaymentResponse.from(savedPayment)
    }

    @Transactional
    fun completePayment(paymentId: UUID, request: PaymentCompleteRequest): PaymentResponse {
        // 1. 상태 변경 (도메인 서비스 위임)
        val payment = paymentService.completePaymentInternal(paymentId, request)
        
        // 2. 외부 이벤트 발행 (Facade의 책임)
        eventPublisher.publishEvent(PaymentCompletedEvent(...))
        
        return PaymentResponse.from(payment)
    }
}
```

### 3.2 BookingFacade (예매 및 환불 조율)
예매 취소 시 **결제 환불**이 함께 일어나야 하는 오케스트레이션을 담당합니다.

```kotlin
@Component
class BookingFacade(
    private val bookingService: BookingService,
    private val paymentFacade: PaymentFacade, // Facade가 다른 Facade를 참조하여 시나리오 확장
    private val eventPublisher: ApplicationEventPublisher
) {
    @Transactional
    fun cancelBooking(bookingId: UUID, userId: Long, ...): BookingResponse {
        // 1. 결제 상태 확인 및 환불 조율 (도메인 간 상호작용)
        if (booking.status == CONFIRMED) {
            paymentFacade.cancelPayment(bookingId, ...) 
        }

        // 2. 좌석 점유 해제 이벤트 발행
        eventPublisher.publishEvent(BookingCancelledEvent(...))

        // 3. 예매 상태 변경 (도메인 서비스 위임)
        bookingService.cancelBookingInternal(booking)

        return BookingResponse.from(booking)
    }
}
```

---

## 4. 도입 효과 (Refactoring Benefits)

1.  **도메인 서비스의 순수성**: `PaymentService`가 더 이상 `CouponService`나 `EventPublisher`를 의존하지 않습니다. 코드 라인이 대폭 줄어들고 테스트가 쉬워졌습니다.
2.  **명확한 비즈니스 흐름**: Facade 클래스만 읽어도 "결제 시 어떤 일들이 일어나는지"를 한눈에 파악할 수 있습니다.
3.  **결합도 해소**: Controller는 이제 `Facade` 하나만 의존하면 됩니다. 여러 Repository와 Service를 주입받던 Controller가 단순해졌습니다.
4.  **재사용성**: `PaymentFacade.cancelPayment`는 사용자 취소뿐만 아니라 시스템 자동 취소 등 다양한 곳에서 재사용될 수 있습니다.

---

## 5. 설계 가이드: Facade vs Domain Service

| 구분 | Facade (Application) | Domain Service |
| :--- | :--- | :--- |
| **의존성** | 여러 Service, Repository, Facade | 자신의 도메인 Repository, Entity |
| **로직 내용** | "A하고 B한 뒤 이벤트를 보낸다" (How to coordinate) | "상태를 A에서 B로 바꾼다", "할인율을 계산한다" (What is the rule) |
| **트랜잭션** | 전체 유스케이스를 하나의 단위로 묶음 | 주로 Facade에 의해 호출되어 트랜잭션 내에서 실행 |
| **노출 범위** | **Controller가 직접 호출하는 유일한 계층** | 주로 Facade 내부에서만 호출됨 |

---

## 💡 결론
Facade 계층의 도입은 단순히 클래스를 하나 더 만드는 것이 아니라, **"오케스트레이션(조율)"과 "비즈니스 규칙"을 명확히 분리**하여 시스템의 복잡도를 관리하는 핵심 전략입니다. 우리 프로젝트에서는 이 구조를 통해 DDD 아키텍처를 더욱 견고하게 유지하고 있습니다.
