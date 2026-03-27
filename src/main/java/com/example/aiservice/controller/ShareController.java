package com.example.aiservice.controller;

import com.example.aiservice.service.ShareCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * SNS 공유 카드 API
 */
@RestController
@RequiredArgsConstructor
public class ShareController {

    private final ShareCardService shareCardService;

    /**
     * 인증된 사용자의 공유 카드 이미지(PNG) 다운로드.
     * GET /api/predictions/{id}/share-card
     */
    @GetMapping(value = "/api/predictions/{id}/share-card", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getShareCard(
            @PathVariable Long id,
            Authentication authentication) throws Exception {

        byte[] imageBytes = shareCardService.generateCard(id, authentication.getName());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header("Content-Disposition", "inline; filename=\"baby-card-" + id + ".png\"")
                .body(imageBytes);
    }

    /**
     * 공개 공유 카드 이미지(PNG) - 인증 불필요, OG 이미지용.
     * GET /share/{id}/card.png
     */
    @GetMapping(value = "/share/{id}/card.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getPublicShareCard(@PathVariable Long id) throws Exception {
        byte[] imageBytes = shareCardService.generatePublicCard(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header("Content-Disposition", "inline; filename=\"baby-card-" + id + ".png\"")
                .header("Cache-Control", "public, max-age=3600")
                .body(imageBytes);
    }
}
