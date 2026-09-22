# 장소 검색 API 변경 안내

> **배포 상태:** 이 문서는 백엔드 개발 브랜치에 구현된 API 계약을 설명합니다. 운영 Railway 서버에 배포하기 전까지 신규 계약을 운영 앱에 연결하지 마세요.

## 1. 변경 내용

- 카테고리와 키워드 지도 검색은 전체 마커와 `totalCount`를 반환합니다.
- 바텀시트 목록은 별도 `/list` API에서 페이지 단위로 반환합니다.
- 프론트엔드는 카테고리 코드를 변환하지 않고 `전체`, `숙박`, `식당카페` 같은 한글 값을 전달합니다.
- Tour API 장소와 동물병원이 같은 검색 결과에 포함되며 `placeType`으로 구분합니다.
- 비회원과 반려동물을 선택하지 않은 회원도 검색할 수 있으며 마커 색상은 `GREY`입니다.

## 2. 인증과 좌표

- 모든 장소 검색 API는 비회원도 호출할 수 있습니다.
- 로그인 사용자는 `Authorization: Bearer {accessToken}` 헤더를 보냅니다.
- `petId`는 선택 값입니다. 비회원이 `petId`를 보내거나 다른 사용자의 반려동물 ID를 보내면 `403` 오류가 발생합니다.
- `mapX`, `currMapX`, `swLng`, `neLng`는 경도입니다.
- `mapY`, `currMapY`, `swLat`, `neLat`는 위도입니다.
- 프론트엔드가 화면보다 약 150% 넓은 bbox를 계산하고, 백엔드는 전달받은 bbox를 그대로 조회합니다.

공통 성공 응답은 다음 구조입니다.

```json
{
  "success": true,
  "message": "응답 메시지",
  "data": {}
}
```

## 3. 검색용 카테고리

```http
GET /places/search-categories
```

응답 예시:

```json
{
  "success": true,
  "message": "검색용 카테고리 목록을 조회했습니다.",
  "data": [
    { "category": "전체" },
    { "category": "숙박" },
    { "category": "축제" },
    { "category": "동물병원" }
  ]
}
```

응답의 `category` 값을 카테고리 검색 API에 그대로 전달합니다.

| `category` 값 | 내부 조회 코드 |
| --- | --- |
| `전체` | 모든 카테고리 |
| `숙박` | `AC` |
| `축제` | `EV01` |
| `공연` | `EV02` |
| `행사` | `EV03` |
| `체험관광` | `EX` |
| `식당카페` | `FD` |
| `역사관광` | `HS` |
| `레저스포츠` | `LS` |
| `자연관광` | `NA` |
| `쇼핑` | `SH` |
| `문화관광` | `VE` |
| `동물병원` | `TMDHOSP` |

`GET /places/categories`는 활성 장소가 사용하는 원본 분류 트리를 제공하는 별도 API입니다. 검색 화면의 고정 메뉴에는 `/places/search-categories`를 사용하세요.

## 4. 지도 검색 응답

카테고리와 키워드 지도 검색은 `PlaceMapSearchResponse`를 반환합니다.

```json
{
  "totalCount": 2,
  "markers": [
    {
      "placeId": 101,
      "mapX": 127.0276,
      "mapY": 37.4979,
      "markerColor": "GREY",
      "placeType": "TOUR"
    },
    {
      "placeId": 205,
      "mapX": 127.0412,
      "mapY": 37.5011,
      "markerColor": "GREY",
      "placeType": "ANIMAL_HOSPITAL"
    }
  ]
}
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `totalCount` | number | 검색 조건에 맞아 반환된 전체 마커 수 |
| `markers` | array | 전체 지도 마커 목록 |
| `markers[].placeType` | string | `TOUR` 또는 `ANIMAL_HOSPITAL` |

지도 검색 응답은 페이지네이션하지 않습니다. `title`, `distance`, `favorite`, `averageRating`은 바텀시트 목록 응답에서 확인합니다.

## 5. 카테고리 지도 마커 조회

```http
GET /places/search/category
```

| 파라미터 | 필수 | 설명 |
| --- | --- | --- |
| `swLat` | O | bbox 남서쪽 위도 |
| `swLng` | O | bbox 남서쪽 경도 |
| `neLat` | O | bbox 북동쪽 위도 |
| `neLng` | O | bbox 북동쪽 경도 |
| `category` | X | 한글 카테고리. 생략하거나 `전체`이면 bbox 안의 모든 장소 |
| `petId` | X | 반려동물 ID |

```http
GET /places/search/category?swLat=37.45&swLng=126.95&neLat=37.55&neLng=127.10&category=식당카페&petId=1
```

전체 검색은 `category=전체`를 보내거나 파라미터를 생략합니다.

## 6. 카테고리 바텀시트 목록 조회

```http
GET /places/search/category/list
```

| 파라미터 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `swLat`, `swLng`, `neLat`, `neLng` | O | - | 지도 검색과 같은 bbox |
| `currMapX` | O | - | 거리 계산 기준 경도 |
| `currMapY` | O | - | 거리 계산 기준 위도 |
| `category` | X | - | 지도 검색과 같은 한글 카테고리 |
| `petId` | X | - | 반려동물 ID |
| `page` | X | `0` | 0부터 시작하는 페이지 번호 |
| `size` | X | `20` | 페이지 크기, 최대 100 |

```http
GET /places/search/category/list?swLat=37.45&swLng=126.95&neLat=37.55&neLng=127.10&currMapX=127.0276&currMapY=37.4979&category=식당카페&page=0&size=20
```

응답의 `data`는 다음 구조입니다.

```json
{
  "content": [
    {
      "placeId": 101,
      "title": "반려견 동반 카페",
      "firstImage": "https://example.com/place.jpg",
      "mapX": 127.0276,
      "mapY": 37.4979,
      "distance": 820,
      "favorite": false,
      "markerColor": "GREY",
      "averageRating": 4.5,
      "placeType": "TOUR"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1000,
  "totalPages": 50,
  "hasNext": true
}
```

목록은 현재 좌표에서 가까운 순으로 정렬하며, 거리가 같으면 `placeId` 오름차순입니다.

## 7. 키워드 검색

지도 마커:

```http
GET /places/search/keyword?keyword=용산공원&petId=1
```

`PlaceMapSearchResponse`를 반환하며 전체 마커가 `markers`에 포함됩니다.

바텀시트 목록:

```http
GET /places/search/keyword/list?keyword=용산공원&petId=1&currMapX=126.9832&currMapY=37.5299&page=0&size=20
```

카테고리 바텀시트와 같은 페이지 응답 구조를 반환합니다.

## 8. 프론트엔드 적용 순서

1. `GET /places/search-categories`로 검색 메뉴를 구성합니다.
2. 선택한 항목의 한글 `category`를 카테고리 지도 API와 목록 API에 전달합니다.
3. 검색 조건이 바뀌면 지도 API로 전체 마커를 교체하고 목록 상태를 비운 뒤 `page=0`을 요청합니다.
4. 스크롤 하단에서 `hasNext=true`이면 다음 페이지를 추가합니다.
5. `category`, `keyword`, `petId`, bbox 또는 현재 좌표가 바뀌면 목록 페이지를 초기화합니다.

## 9. 프론트엔드 체크리스트

- [ ] `categoryCode` 대신 한글 `category` 사용
- [ ] `식당카페` 값을 그대로 전송
- [ ] 지도 응답의 `data.totalCount`, `data.markers` 사용
- [ ] 지도 응답에서 목록 전용 필드를 참조하지 않도록 변경
- [ ] 바텀시트 목록을 `/list` API와 페이지네이션으로 변경
- [ ] `page`를 0부터 시작하고 `hasNext=false`이면 다음 요청 중단
- [ ] `placeType`에 따라 Tour API 장소와 동물병원 표시 처리
- [ ] 비회원 또는 `petId` 미지정 시 `GREY` 마커 확인
- [ ] 운영 배포 전까지 신규 계약을 운영 Base URL에 연결하지 않도록 확인
