// 프론트(또는 Swagger)로부터 /users/me라는 실제 URL로 요청이 들어오면, 그걸 받아서 UserService에게 일을 시키고, 결과를 다시 정해진 형식으로 포장해서 돌려주는 창구 역할을 하는 파일

package com.tmd.backend.controller;

import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.dto.response.user.UserResponse;
import com.tmd.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RequiredArgsConstructor
@RequestMapping("/users")
// 이 컨트롤러의 모든 API는 "/users"로 시작함
// (예: /users/me → 최종 주소는 /users + /me)

@RestController
// 이 클래스가 REST API를 처리하는 Controller임을 Spring에게 알림

public class UserController {

    private final UserService userService;
    // Service를 주입받음. @RequiredArgsConstructor가 생성자 자동으로 만들어줌

    @GetMapping("/me")
    // GET 방식으로 "/users/me" 요청이 오면 아래 메서드가 실행됨

    public ResponseEntity<SuccessResponseDto<UserResponse>> getMyInfo(
        @AuthenticationPrincipal String email) {
        // @AuthenticationPrincipal 덕분에, JWT 필터가 저장해둔 "로그인한 사람의 email"이 이 email 매개변수에 자동으로 들어옴
        // (우리가 직접 꺼내는 코드를 안 짜도 Spring이 자동으로 넣어줌)

        UserResponse response = userService.getMyInfo(email);
        // Service에게 email을 넘겨서 실제 조회 작업을 시킴
        // Service가 완성해서 돌려준 UserResponse를 response에 저장

        return ResponseEntity.ok(
            SuccessResponseDto.success("내 정보 조회가 완료되었습니다.", response)
        );
        // 200 OK 상태코드와 함께, SuccessResponseDto로 감싼 최종 응답을 리턴
        // (success: true, message: "내 정보 조회가 완료되었습니다.", data: response)
    }


    // 회원탈퇴 withdraw 다루는 메서드 추가
    @DeleteMapping("/me")  // DELETE 방식으로 "/users/me" 요청이 오면 아래 메서드가 실행됨
    public ResponseEntity<SuccessResponseDto<Void>> withdraw(
        @AuthenticationPrincipal String email
        // JWT 토큰에서 꺼낸 로그인한 사람의 email이 자동으로 들어옴
    ) {
        userService.withdraw(email);
        // Service에게 email을 넘겨서 실제로 회원 탈퇴(연관 데이터 삭제 포함) 처리시킴

        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("회원 탈퇴가 완료되었습니다."));
        // 200 OK와 함께, 데이터 없이 성공 메시지만 리턴
    }
}
