# 홈 화면 API 가이드

## 개정이력
| 버전 | 일자 | 작성자 | 내용 |
|------|------|--------|------|
| 1.0 | 2026-01-25 | Claude | 최초 작성 |
| 1.1 | 2026-01-25 | Claude | 관리자용 페이지 구성 가이드 추가 |

---

## 1. 개요

이 문서는 홈 화면에 공연을 섹션별로 노출하기 위한 API 사용 방법을 설명합니다.

### 핵심 개념

- **Section (섹션)**: 홈 화면의 대분류 영역 (인기티켓, 데이트코스 등)
- **Tag (태그)**: 섹션 내 하위 분류 (뮤지컬, 콘서트, 연극 등)
- **순서 관리**: 관리자가 지정한 순서대로 공연이 노출됨

### 섹션 구조

```
홈 화면
├── 인기티켓 (POPULAR_TICKET)
│   ├── 금주오픈티켓 (WEEKLY_OPEN)
│   ├── 뮤지컬 (MUSICAL)
│   ├── 콘서트 (CONCERT)
│   ├── 연극 (THEATER)
│   └── 전시/행사 (EXHIBITION)
│
├── 데이트코스 (DATE_COURSE)
│   ├── 뮤지컬 (DATE_MUSICAL)
│   ├── 연극 (DATE_THEATER)
│   ├── 클래식 (DATE_CLASSIC)
│   └── 전시 (DATE_EXHIBITION)
│
├── 이런 티켓은 어때요? (RECOMMENDED)
│   ├── 한정특가 (LIMITED_SALE)
│   ├── 아이와 함께 (WITH_KIDS)
│   └── 대학로공연 (DAEHAKRO)
│
└── 어디로 떠나볼까요? (REGION)
    ├── 서울 (REGION_SEOUL)
    ├── 경기 (REGION_GYEONGGI)
    ├── 부산 (REGION_BUSAN)
    ├── 대구 (REGION_DAEGU)
    ├── 대전 (REGION_DAEJEON)
    └── 전국 (REGION_NATIONWIDE)
```

---

## 2. API 엔드포인트

### 2.1 홈 전체 섹션 조회

홈 화면에 필요한 모든 데이터를 한 번에 조회합니다.

```http
GET /api/home/sections
```

**응답 예시:**
```json
{
  "sections": [
    {
      "section": "POPULAR_TICKET",
      "displayName": "인기티켓",
      "displayOrder": 1,
      "tags": [
        {
          "tag": "WEEKLY_OPEN",
          "displayName": "금주오픈티켓",
          "displayOrder": 1,
          "performances": [
            {
              "id": 1,
              "title": "뮤지컬 위키드",
              "mainImageUrl": "https://example.com/wicked.jpg",
              "startDate": "2026-02-01",
              "endDate": "2026-03-31",
              "venueName": "블루스퀘어"
            },
            {
              "id": 2,
              "title": "레미제라블",
              "mainImageUrl": "https://example.com/les.jpg",
              "startDate": "2026-02-15",
              "endDate": "2026-04-30",
              "venueName": "예술의전당"
            }
          ]
        },
        {
          "tag": "MUSICAL",
          "displayName": "뮤지컬",
          "displayOrder": 2,
          "performances": [...]
        }
      ]
    },
    {
      "section": "DATE_COURSE",
      "displayName": "데이트코스",
      "displayOrder": 2,
      "tags": [...]
    }
  ]
}
```

### 2.2 특정 섹션 조회

특정 섹션의 태그와 공연 목록만 조회합니다.

```http
GET /api/home/sections/{section}
```

**Path Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| section | enum | 섹션 코드 (POPULAR_TICKET, DATE_COURSE, RECOMMENDED, REGION) |

**요청 예시:**
```http
GET /api/home/sections/POPULAR_TICKET
```

**응답 예시:**
```json
{
  "section": "POPULAR_TICKET",
  "displayName": "인기티켓",
  "displayOrder": 1,
  "tags": [
    {
      "tag": "WEEKLY_OPEN",
      "displayName": "금주오픈티켓",
      "displayOrder": 1,
      "performances": [...]
    }
  ]
}
```

### 2.3 특정 태그의 공연 목록 조회

특정 태그에 등록된 공연 목록만 조회합니다. "더보기" 기능 구현 시 사용합니다.

```http
GET /api/home/tags/{tag}/performances
```

**Path Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| tag | enum | 태그 코드 (MUSICAL, CONCERT, REGION_SEOUL 등) |

**요청 예시:**
```http
GET /api/home/tags/MUSICAL/performances
```

**응답 예시:**
```json
{
  "tag": "MUSICAL",
  "displayName": "뮤지컬",
  "displayOrder": 2,
  "performances": [
    {
      "id": 1,
      "title": "뮤지컬 위키드",
      "mainImageUrl": "https://example.com/wicked.jpg",
      "startDate": "2026-02-01",
      "endDate": "2026-03-31",
      "venueName": "블루스퀘어"
    }
  ]
}
```

---

## 3. 태그 코드 목록

### 3.1 인기티켓 (POPULAR_TICKET)

| 태그 코드 | 화면 표시명 |
|----------|------------|
| WEEKLY_OPEN | 금주오픈티켓 |
| MUSICAL | 뮤지컬 |
| CONCERT | 콘서트 |
| THEATER | 연극 |
| EXHIBITION | 전시/행사 |

### 3.2 데이트코스 (DATE_COURSE)

| 태그 코드 | 화면 표시명 |
|----------|------------|
| DATE_MUSICAL | 뮤지컬 |
| DATE_THEATER | 연극 |
| DATE_CLASSIC | 클래식 |
| DATE_EXHIBITION | 전시 |

### 3.3 이런 티켓은 어때요? (RECOMMENDED)

| 태그 코드 | 화면 표시명 |
|----------|------------|
| LIMITED_SALE | 한정특가 |
| WITH_KIDS | 아이와 함께 |
| DAEHAKRO | 대학로공연 |

### 3.4 어디로 떠나볼까요? (REGION)

| 태그 코드 | 화면 표시명 |
|----------|------------|
| REGION_SEOUL | 서울 |
| REGION_GYEONGGI | 경기 |
| REGION_BUSAN | 부산 |
| REGION_DAEGU | 대구 |
| REGION_DAEJEON | 대전 |
| REGION_NATIONWIDE | 전국 |

---

## 4. 타입 정의 (TypeScript)

```typescript
// types/home.ts

// 섹션 코드
type HomeSection =
  | 'POPULAR_TICKET'
  | 'DATE_COURSE'
  | 'RECOMMENDED'
  | 'REGION';

// 태그 코드
type HomeSectionTag =
  // 인기티켓
  | 'WEEKLY_OPEN' | 'MUSICAL' | 'CONCERT' | 'THEATER' | 'EXHIBITION'
  // 데이트코스
  | 'DATE_MUSICAL' | 'DATE_THEATER' | 'DATE_CLASSIC' | 'DATE_EXHIBITION'
  // 이런 티켓은 어때요?
  | 'LIMITED_SALE' | 'WITH_KIDS' | 'DAEHAKRO'
  // 어디로 떠나볼까요?
  | 'REGION_SEOUL' | 'REGION_GYEONGGI' | 'REGION_BUSAN'
  | 'REGION_DAEGU' | 'REGION_DAEJEON' | 'REGION_NATIONWIDE';

// 홈 화면용 공연 정보
interface HomePerformance {
  id: number;
  title: string;
  mainImageUrl: string | null;
  startDate: string;  // ISO date format (YYYY-MM-DD)
  endDate: string;    // ISO date format (YYYY-MM-DD)
  venueName: string | null;
}

// 태그별 공연 목록
interface HomeTagWithPerformances {
  tag: HomeSectionTag;
  displayName: string;
  displayOrder: number;
  performances: HomePerformance[];
}

// 섹션 응답
interface HomeSectionResponse {
  section: HomeSection;
  displayName: string;
  displayOrder: number;
  tags: HomeTagWithPerformances[];
}

// 전체 섹션 응답
interface HomeSectionsResponse {
  sections: HomeSectionResponse[];
}
```

---

## 5. 프론트엔드 구현 예시

### 5.1 데이터 페칭 (React Query)

```typescript
// hooks/useHomeData.ts
import { useQuery } from '@tanstack/react-query';

const fetchHomeSections = async (): Promise<HomeSectionsResponse> => {
  const response = await fetch('/api/home/sections');
  if (!response.ok) throw new Error('Failed to fetch home data');
  return response.json();
};

export const useHomeSections = () => {
  return useQuery({
    queryKey: ['home', 'sections'],
    queryFn: fetchHomeSections,
    staleTime: 1000 * 60 * 5, // 5분 캐싱
  });
};

// 특정 태그의 공연 더보기
const fetchTagPerformances = async (tag: HomeSectionTag): Promise<HomeTagWithPerformances> => {
  const response = await fetch(`/api/home/tags/${tag}/performances`);
  if (!response.ok) throw new Error('Failed to fetch tag performances');
  return response.json();
};

export const useTagPerformances = (tag: HomeSectionTag) => {
  return useQuery({
    queryKey: ['home', 'tags', tag],
    queryFn: () => fetchTagPerformances(tag),
  });
};
```

### 5.2 홈 화면 컴포넌트

```tsx
// pages/HomePage.tsx
import { useHomeSections } from '@/hooks/useHomeData';

const HomePage = () => {
  const { data, isLoading, error } = useHomeSections();

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message="홈 데이터를 불러오지 못했습니다." />;

  return (
    <div className="home-page">
      {data?.sections.map((section) => (
        <HomeSection key={section.section} section={section} />
      ))}
    </div>
  );
};
```

### 5.3 섹션 컴포넌트

```tsx
// components/HomeSection.tsx
interface HomeSectionProps {
  section: HomeSectionResponse;
}

const HomeSection = ({ section }: HomeSectionProps) => {
  const [activeTag, setActiveTag] = useState(section.tags[0]?.tag);

  const activeTagData = section.tags.find((t) => t.tag === activeTag);

  return (
    <section className="home-section">
      <h2 className="section-title">{section.displayName}</h2>

      {/* 태그 탭 */}
      <div className="tag-tabs">
        {section.tags.map((tag) => (
          <button
            key={tag.tag}
            className={`tag-tab ${activeTag === tag.tag ? 'active' : ''}`}
            onClick={() => setActiveTag(tag.tag)}
          >
            {tag.displayName}
          </button>
        ))}
      </div>

      {/* 공연 목록 */}
      <div className="performance-list">
        {activeTagData?.performances.map((performance) => (
          <PerformanceCard key={performance.id} performance={performance} />
        ))}
      </div>

      {/* 더보기 버튼 */}
      {activeTagData && activeTagData.performances.length > 0 && (
        <Link to={`/home/tags/${activeTag}`} className="more-link">
          더보기 &gt;
        </Link>
      )}
    </section>
  );
};
```

### 5.4 공연 카드 컴포넌트

```tsx
// components/PerformanceCard.tsx
interface PerformanceCardProps {
  performance: HomePerformance;
}

const PerformanceCard = ({ performance }: PerformanceCardProps) => {
  const formatDateRange = (start: string, end: string) => {
    const startDate = new Date(start);
    const endDate = new Date(end);
    return `${startDate.getMonth() + 1}.${startDate.getDate()} ~ ${endDate.getMonth() + 1}.${endDate.getDate()}`;
  };

  return (
    <Link to={`/performances/${performance.id}`} className="performance-card">
      <div className="card-image">
        {performance.mainImageUrl ? (
          <img src={performance.mainImageUrl} alt={performance.title} />
        ) : (
          <div className="placeholder-image">이미지 없음</div>
        )}
      </div>
      <div className="card-info">
        <h3 className="card-title">{performance.title}</h3>
        <p className="card-venue">{performance.venueName || '장소 미정'}</p>
        <p className="card-date">
          {formatDateRange(performance.startDate, performance.endDate)}
        </p>
      </div>
    </Link>
  );
};
```

### 5.5 태그별 더보기 페이지

```tsx
// pages/TagPerformancesPage.tsx
import { useParams } from 'react-router-dom';
import { useTagPerformances } from '@/hooks/useHomeData';

const TagPerformancesPage = () => {
  const { tag } = useParams<{ tag: HomeSectionTag }>();
  const { data, isLoading } = useTagPerformances(tag!);

  if (isLoading) return <LoadingSpinner />;

  return (
    <div className="tag-performances-page">
      <h1>{data?.displayName}</h1>

      <div className="performance-grid">
        {data?.performances.map((performance) => (
          <PerformanceCard key={performance.id} performance={performance} />
        ))}
      </div>

      {data?.performances.length === 0 && (
        <p className="empty-message">등록된 공연이 없습니다.</p>
      )}
    </div>
  );
};
```

---

## 6. 주의사항

### 6.1 노출 조건

- **visible=true인 공연만 노출됩니다.** 관리자가 공연을 숨김 처리하면 홈 화면에서 자동으로 제외됩니다.
- 태그에 등록된 공연이 없으면 해당 태그의 `performances` 배열은 빈 배열(`[]`)로 반환됩니다.

### 6.2 순서

- 섹션은 `displayOrder` 순서대로 정렬되어 반환됩니다.
- 태그는 섹션 내에서 `displayOrder` 순서대로 정렬됩니다.
- 공연은 관리자가 지정한 순서대로 정렬됩니다.

### 6.3 캐싱 권장

- 홈 화면 데이터는 자주 변경되지 않으므로 적절한 캐싱을 권장합니다.
- React Query 사용 시 `staleTime: 5분` 정도로 설정하면 좋습니다.

---

## 7. 에러 처리

### 7.1 HTTP 상태 코드

| 상태 코드 | 설명 | 처리 방법 |
|----------|------|----------|
| 200 | 성공 | 정상 처리 |
| 400 | 잘못된 태그/섹션 코드 | 유효한 코드인지 확인 |
| 500 | 서버 오류 | 재시도 또는 에러 메시지 표시 |

### 7.2 에러 처리 예시

```typescript
const fetchWithErrorHandling = async () => {
  try {
    const response = await fetch('/api/home/sections');

    if (!response.ok) {
      if (response.status === 400) {
        throw new Error('잘못된 요청입니다.');
      }
      throw new Error('서버 오류가 발생했습니다.');
    }

    return await response.json();
  } catch (error) {
    console.error('홈 데이터 조회 실패:', error);
    // 폴백 UI 또는 재시도 로직
    throw error;
  }
};
```

---

## 8. FAQ

### Q1. 공연이 여러 태그에 동시에 노출될 수 있나요?
**A:** 네, 가능합니다. 예를 들어 "뮤지컬 위키드"가 "인기티켓 > 뮤지컬"과 "데이트코스 > 뮤지컬" 모두에 노출될 수 있습니다.

### Q2. 태그 내 공연 순서는 어떻게 결정되나요?
**A:** 관리자가 어드민 페이지에서 직접 순서를 지정합니다. 순서는 `displayOrder` 값으로 관리됩니다.

### Q3. 지역 태그는 어떻게 적용되나요?
**A:** 지역 태그(REGION_*)는 공연장 주소를 기반으로 자동 매핑됩니다. 관리자가 수동으로 지정하지 않습니다.

### Q4. 빈 태그(공연 없음)도 응답에 포함되나요?
**A:** 네, 포함됩니다. `performances` 배열이 빈 배열(`[]`)로 반환됩니다. 프론트엔드에서 빈 태그를 숨길지 여부는 UI 정책에 따라 결정하세요.

### Q5. 전체 섹션 조회 시 데이터가 너무 많지 않나요?
**A:** 현재는 모든 데이터를 한 번에 반환합니다. 성능 이슈가 발생하면 페이징이나 lazy loading 도입을 검토할 예정입니다.

---

## 9. 관리자용 페이지 가이드 (Admin Guide)

어드민(Admin) 페이지 기획 및 개발을 위한 가이드입니다.

### 9.1 홈 전시 관리 (대시보드)
홈 화면의 전체 구조를 파악하고 관리하는 페이지입니다.

- **API**: `GET /api/admin/home/sections/metadata`
- **UI 구조 추천**:
    - **상단 탭**: 섹션별 탭 (인기티켓 / 데이트코스 / 추천 / 지역)
    - **좌측 사이드바**: 선택된 섹션 하위의 태그 목록
    - **메인 영역**: 선택된 태그의 공연 리스트 (Drag & Drop 지원)

### 9.2 공연 상세 - 홈 노출 설정 (개별 설정)
공연 등록/수정 페이지에서 해당 공연을 홈 화면 어디에 노출할지 설정합니다.

- **API**:
    - 조회: `GET /api/admin/performances/{id}/home-tags`
    - 추가: `POST /api/admin/performances/{id}/home-tags`
    - 삭제: `DELETE /api/admin/performances/{id}/home-tags/{tag}`
- **UI 구조 추천**:
    - **섹션명**: "홈 화면 노출 설정"
    - **추가 버튼**: 클릭 시 모달 오픈 -> `HomeSectionTag` Dropdown 선택
    - **태그 칩(Chip)**: 등록된 태그 표시 (삭제 버튼 포함)
    - **주의**: `REGION_`으로 시작하는 태그는 **수동 추가 불가** 처리 (비활성화 또는 목록에서 제외)

### 9.3 순서 변경 UX 가이드 (Drag & Drop)
태그 내 공연 노출 순서를 변경하는 UX입니다.

- **API**: `PATCH /api/admin/home-tags/{tag}/performances/order`
- **프로세스**:
    1.  리스트에서 공연 카드를 드래그하여 순서 변경
    2.  "순서 저장" 버튼 클릭
    3.  변경된 전체 리스트(`[{id:1, order:1}, {id:5, order:2}...]`)를 서버로 전송

### 9.4 주의사항
- **메타 데이터 활용**: `HomeSection`, `HomeSectionTag` 구조는 하드코딩하지 말고 `metadata` API를 통해 동적으로 받아와서 렌더링하세요.
- **지역 태그**: 지역 태그는 공연장 주소 변경 시 자동 갱신되므로, 관리자가 수동으로 수정할 수 없도록 UI를 제어해야 합니다.

---

## 10. 참고 자료

- **API 명세**: Swagger UI (`/swagger-ui/index.html`)
- **설계 문서**: `documents/CCS-145_HOME_CATEGORY_DESIGN.md`
- **관련 소스 코드**:
  - `HomeController.kt`: 사용자용 홈 API
  - `HomeService.kt`: 홈 화면 비즈니스 로직
  - `HomeSection.kt`, `HomeSectionTag.kt`: Enum 정의
  - `AdminHomeTagController.kt`: 관리자용 API