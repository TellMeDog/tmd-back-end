# 이미지 업로드 흐름

이미지는 애플리케이션 서버를 거치지 않고 presigned PUT URL로 R2에 직접 업로드한다.

1. `POST /images/presigned-url`로 업로드를 시작한다.
2. 응답의 `presignedUrl`에 `requiredHeaders`를 그대로 포함해 파일을 PUT한다.
3. PUT 성공 후 `POST /images/uploads/{uploadId}/complete`를 호출한다.
4. 완료된 `uploadId`를 리뷰 또는 반려견 등록 요청의 `imageUploadId`로 전달한다.

업로드 시작 요청 예시는 다음과 같다.

```json
{
  "usage": "REVIEW",
  "filename": "visit.jpg",
  "contentType": "image/jpeg",
  "fileSize": 123456
}
```

수정 요청에서 `imageUploadId`를 전달하면 이미지를 교체하고, `removeImage`를 `true`로
전달하면 기존 이미지를 제거한다. 두 필드를 모두 생략하면 기존 이미지를 유지한다.

연결되지 않은 업로드는 기본 24시간 후 스케줄 작업에서 삭제된다. 추가 안전장치로 R2
버킷에 prefix `tmp/`, 만료 기간 1일인 Object Lifecycle Rule을 설정한다. 이 규칙은
애플리케이션 외부의 버킷 설정이므로 Cloudflare Dashboard 또는 배포 인프라에서 한 번
적용해야 한다.
