# JPA Auditing 가이드: 생성/수정일자 자동화

이 문서는 Spring Data JPA의 Auditing 기능을 사용하여 엔티티의 생성 및 수정 일자를 자동으로 관리하는 방법에 대해 상세히 설명합니다.

## 1. JPA Auditing이란?

JPA Auditing은 엔티티의 특정 필드(예: `createdAt`, `updatedAt`, `createdBy`, `lastModifiedBy`)를 자동으로 채워주는 기능입니다. 이를 통해 개발자는 반복적인 Auditing 정보를 수동으로 관리할 필요 없이 비즈니스 로직에 집중할 수 있습니다.

## 2. Auditing 적용 방법

프로젝트에서는 다음 세 가지 단계를 통해 JPA Auditing을 적용했습니다.

### 2.1. `@EnableJpaAuditing` 활성화

메인 애플리케이션 클래스(`SmarterStoreApiApplication.kt`)에 `@EnableJpaAuditing` 어노테이션을 추가하여 JPA Auditing 기능을 활성화합니다.

```kotlin
// SmarterStoreApiApplication.kt 예시
package com.github.copyinaction

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing // 이 import 필요

@EnableJpaAuditing // JPA Auditing 기능 활성화
@SpringBootApplication
class SmarterStoreApiApplication

fun main(args: Array<String>) {
	runApplication<SmarterStoreApiApplication>(*args)
}
```

### 2.2. `BaseEntity` 정의 (`domain/BaseEntity.kt`)

모든 엔티티에서 공통으로 사용할 Auditing 필드를 포함하는 추상 기본 엔티티(`BaseEntity`)를 정의합니다.

*   **`@MappedSuperclass`**: 자식 엔티티 클래스들이 이 클래스의 매핑 정보를 상속받도록 합니다. 이 클래스 자체는 데이터베이스 테이블과 매핑되지 않습니다.
*   **`@EntityListeners(AuditingEntityListener::class)`**: JPA 엔티티의 생명주기 이벤트를 감지하여 Auditing 정보를 처리하는 리스너를 등록합니다.
*   **`@CreatedDate`**: 엔티티가 생성될 때 현재 시간을 자동으로 주입합니다.
*   **`@LastModifiedDate`**: 엔티티가 수정될 때마다 현재 시간을 자동으로 주입합니다.

```kotlin
// BaseEntity.kt 예시
package com.github.copyinaction.domain

import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass // 자식 엔티티들이 이 클래스의 매핑 정보를 상속받음
@EntityListeners(AuditingEntityListener::class) // Auditing 리스너 등록
abstract class BaseEntity {
    @CreatedDate // 엔티티 생성 시 현재 시간 자동 주입
    var createdAt: LocalDateTime? = null

    @LastModifiedDate // 엔티티 수정 시 현재 시간 자동 주입
    var updatedAt: LocalDateTime? = null
}
```

### 2.3. 엔티티에서 `BaseEntity` 상속

생성 및 수정 일자를 자동으로 관리하고자 하는 모든 엔티티 클래스는 `BaseEntity`를 상속받습니다.

```kotlin
// Product.kt 예시
package com.github.copyinaction.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    var name: String,

    var price: Double,
) : BaseEntity() { // BaseEntity 상속
    fun update(name: String, price: Double) {
        this.name = name
        this.price = price
    }
}
```

## 3. 동작 확인

위 설정이 완료되면, `ProductService`에서 `productRepository.save()` 또는 `product.update()`와 같은 작업을 수행할 때, `createdAt`과 `updatedAt` 필드에 현재 시각이 자동으로 기록되는 것을 확인할 수 있습니다.

이 기능을 통해 데이터의 이력 관리 및 추적을 효율적으로 수행할 수 있습니다.
