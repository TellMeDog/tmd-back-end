package com.tmd.backend.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    //400
    LOGIN_FAIL(HttpStatus.BAD_REQUEST, "이메일 혹은 비밀번호가 일치하지 않습니다."),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "잘못된 인증번호입니다."),
    EXPIRED_TOKEN(HttpStatus.BAD_REQUEST, "만료된 인증번호입니다."),
    INVALID_PROVIDER(HttpStatus.BAD_REQUEST, "잘못된 PROVIDER입니다."),
    PASSWORD_CONFIRM_FAIL(HttpStatus.BAD_REQUEST, "'비밀번호'와 '비밀번호 확인'이 일치하지 않습니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "올바르지 않은 입력입니다."),
    INVALID_SEARCH_REQUEST(HttpStatus.BAD_REQUEST, "keyword와 category는 동시에 사용할 수 없습니다."),
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "지원하지 않는 카테고리입니다."),
    INVALID_REVIEW_REQUEST(HttpStatus.BAD_REQUEST, "안내된 조건과 달랐어요를 선택한 경우에만 상세 사유를 입력할 수 있습니다."),
    INVALID_MAP_BOUNDS(HttpStatus.BAD_REQUEST, "범위를 초과한 반경입니다."),
    INVALID_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "허용되지 않는 이미지 형식입니다. (jpg, jpeg, png, gif, webp)"),
    INVALID_IMAGE_SIZE(HttpStatus.BAD_REQUEST, "이미지 크기가 올바르지 않거나 허용 범위를 초과했습니다."),
    UNKNOWN_REGION(HttpStatus.BAD_REQUEST, "등록되지 않은 지역입니다."),

    //401
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요한 서비스입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "다시 로그인 해주세요."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 요청입니다."),
    //403
    NOT_VERIFIED_EMAIL(HttpStatus.FORBIDDEN, "이메일 인증이 필요합니다."),
    NOT_OWNER_OF_DOG(HttpStatus.FORBIDDEN, "등록된 반려견이 아닙니다."),
    /*
    - 즐겨찾기 삭제 시 본인 소유 아닌 경우
    - Pet 수정 시 본인 소유 아닌 경우
    - 리뷰 삭제/조회 시 본인 작성 아닌 경우
    - GET /places 요청 시 petId가 본인 소유 아닌 경우
     */
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    //404
    FAVORITE_NOT_FOUND(HttpStatus.NOT_FOUND, "즐겨찾기하지 않은 장소입니다."),
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 반려견입니다."),
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 장소입니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 리뷰입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."), //UserService 구현 시 이메일을 못 찾을 상황 처리를 위해 추가한 코드
    //409
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입한 이메일입니다."),
    ALREADY_FAVORITE(HttpStatus.CONFLICT, "이미 즐겨찾기한 장소입니다."),
    //500
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
