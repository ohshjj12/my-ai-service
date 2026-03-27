package com.example.aiservice.controller;

import com.example.aiservice.entity.UltrasoundImage;
import com.example.aiservice.repository.UltrasoundImageRepository;
import com.example.aiservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.io.File;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final UltrasoundImageRepository ultrasoundImageRepository;
    private final UserRepository userRepository;

    /**
     * 업로드된 초음파 이미지 파일 다운로드
     * GET /api/images/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"))
                .getId();

        UltrasoundImage image = ultrasoundImageRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("이미지를 찾을 수 없습니다: " + id));

        if (image.getStoredFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        File file = new File(image.getStoredFilePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + image.getOriginalFileName() + "\"")
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .body(resource);
    }
}
