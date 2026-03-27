package com.example.aiservice.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp");

    // 매직바이트 시그니처
    private static final byte[] SIG_JPEG    = {(byte)0xFF, (byte)0xD8, (byte)0xFF};
    private static final byte[] SIG_PNG     = {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] SIG_GIF87   = {0x47, 0x49, 0x46, 0x38, 0x37, 0x61};
    private static final byte[] SIG_GIF89   = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61};
    private static final byte[] SIG_BMP     = {0x42, 0x4D};
    // WebP: bytes 0-3 = RIFF, bytes 8-11 = WEBP
    private static final byte[] SIG_RIFF    = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] SIG_WEBP    = {0x57, 0x45, 0x42, 0x50};

    @PostConstruct
    public void init() {
        Path dir = Paths.get(uploadPath);
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
                log.info("업로드 디렉토리 생성: {}", dir.toAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException("업로드 디렉토리 생성 실패: " + dir, e);
            }
        }
    }

    /**
     * 이미지 바이트 배열을 로컬 디스크에 저장하고 저장 경로를 반환합니다.
     */
    public String store(byte[] bytes, String originalFilename) throws IOException {
        String clean = StringUtils.cleanPath(
                Optional.ofNullable(originalFilename).orElse("unknown.bin"));
        String extension = getExtension(clean);

        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                    "허용되지 않는 파일 형식입니다: " + extension +
                    ". 허용 형식: " + ALLOWED_EXTENSIONS);
        }

        // 매직바이트 검증 — 확장자 위조 방지
        validateMagicBytes(bytes, extension.toLowerCase());

        String storedFilename = UUID.randomUUID() + "." + extension;
        Path targetPath = Paths.get(uploadPath).resolve(storedFilename);
        Files.write(targetPath, bytes);
        log.debug("파일 저장 완료: {}", targetPath.toAbsolutePath());
        return targetPath.toAbsolutePath().toString();
    }

    private void validateMagicBytes(byte[] bytes, String ext) {
        if (bytes == null || bytes.length < 12) return; // 너무 짧으면 스킵
        boolean valid = switch (ext) {
            case "jpg", "jpeg" -> startsWith(bytes, SIG_JPEG);
            case "png"         -> startsWith(bytes, SIG_PNG);
            case "gif"         -> startsWith(bytes, SIG_GIF87) || startsWith(bytes, SIG_GIF89);
            case "bmp"         -> startsWith(bytes, SIG_BMP);
            case "webp"        -> startsWith(bytes, SIG_RIFF) &&
                                  Arrays.equals(Arrays.copyOfRange(bytes, 8, 12), SIG_WEBP);
            default            -> true; // 알 수 없는 형식은 확장자 검사에서 이미 걸러짐
        };
        if (!valid) {
            throw new IllegalArgumentException(
                    "파일 시그니처가 확장자(" + ext + ")와 일치하지 않습니다. 변조된 파일일 수 있습니다.");
        }
    }

    private boolean startsWith(byte[] data, byte[] sig) {
        if (data.length < sig.length) return false;
        for (int i = 0; i < sig.length; i++) {
            if (data[i] != sig[i]) return false;
        }
        return true;
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot > 0) ? filename.substring(dot + 1) : "";
    }
}
