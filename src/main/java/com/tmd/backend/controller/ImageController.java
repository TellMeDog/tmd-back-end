package com.tmd.backend.controller;

import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.dto.response.file.FileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/images")
public class ImageController {

    @PostMapping
    public ResponseEntity<SuccessResponseDto<FileResponse>> registerImage(@RequestParam("image") MultipartFile image){
        //TODO: file을 R2에 저장한 후 URL 반환
        log.info("이미지 업로드 요청 : fileName: {}, size: {}", image.getOriginalFilename(), image.getSize());
        FileResponse response = FileResponse.builder()
            .imageUrl("https://placehold.co/400x400")
            .build();
        return ResponseEntity.ok(SuccessResponseDto.success("이미지가 업로드되었습니다.", response));
    }
}
