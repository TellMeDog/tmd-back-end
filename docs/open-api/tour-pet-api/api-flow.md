```mermaid
flowchart LR
subgraph 코드조회["1. 코드 조회 (필터용 사전 준비)"]
AC[areaCode2<br/>지역코드]
CC[categoryCode2<br/>cat1→cat2→cat3]
end

subgraph 목록조회["2. 목록 조회"]
ABL[areaBasedList2<br/>지역기반]
LBL[locationBasedList2<br/>위치기반]
SK[searchKeyword2<br/>키워드검색]
end

subgraph 상세조회["3. 상세 조회 (contentId 필요)"]
DC[detailCommon2<br/>공통정보]
DI[detailIntro2<br/>소개정보]
DIMG[detailImage2<br/>이미지정보]
DPT[detailPetTour2<br/>반려동물 동반정보]
end

AC -->|areaCode| ABL
CC -->|cat1/cat2/cat3| ABL
CC -->|cat1/cat2/cat3| LBL
CC -->|cat1/cat2/cat3| SK

ABL -->|contentId| DC
LBL -->|contentId| DC
SK -->|contentId| DC
ABL -->|contentId| DI
ABL -->|contentId| DIMG
ABL -->|contentId| DPT
```

## 오퍼레이션 설명

| 단계 | API | 역할 |
|---|---|---|
| 1. 코드 조회 | `areaCode2` | 지역 필터용 코드 조회 |
| 1. 코드 조회 | `categoryCode2` | 카테고리 필터용 코드 조회 (cat1→cat2→cat3 계층 조회) |
| 2. 목록 조회 | `areaBasedList2` | 지역 기준 목록 조회 |
| 2. 목록 조회 | `locationBasedList2` | 좌표(위경도) 기준 목록 조회 |
| 2. 목록 조회 | `searchKeyword2` | 키워드 기반 검색 |
| 3. 상세 조회 | `detailCommon2` | 이름/주소/개요/위치 |
| 3. 상세 조회 | `detailIntro2` | 운영시간/휴무일/요금 |
| 3. 상세 조회 | `detailImage2` | 이미지 리스트 |
| 3. 상세 조회 | `detailPetTour2` | 반려동물 동반 가능 정보 (핵심 기능) |

## 주의사항
- `categoryCode2`, `areaCode2`는 목록 조회 API에 넘길 **필터 파라미터를 알아내기 위한 사전 조회**이지, 실제 서비스 화면에서 매번 호출하는 게 아님 (한 번 조회해서 코드표로 저장해두고 재사용)
- `detailPetTour2`는 반드시 목록 조회로 얻은 `contentId`가 있어야 호출 가능
