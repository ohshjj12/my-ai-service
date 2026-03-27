package com.example.aiservice.service;

import com.example.aiservice.entity.PredictionResult;
import com.example.aiservice.entity.UltrasoundImage;
import com.example.aiservice.repository.PredictionResultRepository;
import com.example.aiservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * SNS 공유용 카드 이미지를 생성합니다.
 * Java AWT BufferedImage 사용 (외부 의존성 없음).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ShareCardService {

    private final PredictionResultRepository predictionResultRepository;
    private final UserRepository userRepository;

    private static final int CARD_WIDTH  = 800;
    private static final int CARD_HEIGHT = 450;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    /**
     * 공개 공유 페이지용 - 인증 없이 분석 결과 조회 (소유자 정보 포함)
     */
    public PredictionResult getPublicResult(Long predictionId) {
        return predictionResultRepository.findById(predictionId)
                .orElseThrow(() -> new IllegalArgumentException("공유 결과를 찾을 수 없습니다: " + predictionId));
    }

    /**
     * 인증 없이 공유 카드 PNG 생성 (공개 공유 URL용)
     */
    public byte[] generatePublicCard(Long predictionId) throws IOException {
        PredictionResult result = predictionResultRepository.findById(predictionId)
                .orElseThrow(() -> new IllegalArgumentException("분석 결과를 찾을 수 없습니다: " + predictionId));
        return renderCard(result);
    }

    /**
     * 분석 결과 ID + 사용자 기준으로 공유 카드 PNG 바이트 배열을 생성합니다.
     */
    public byte[] generateCard(Long predictionId, String username) throws IOException {
        userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        PredictionResult result = predictionResultRepository.findById(predictionId)
                .orElseThrow(() -> new IllegalArgumentException("분석 결과를 찾을 수 없습니다: " + predictionId));

        if (!result.getUltrasoundImage().getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        return renderCard(result);
    }

    private byte[] renderCard(PredictionResult result) throws IOException {
        UltrasoundImage image = result.getUltrasoundImage();
        boolean isMale = "MALE".equalsIgnoreCase(result.getPredictedGender());

        BufferedImage card = new BufferedImage(CARD_WIDTH, CARD_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = card.createGraphics();

        // 렌더링 품질 설정
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        // === 배경 그라디언트 ===
        Color bgTop    = isMale ? new Color(0xD0, 0xE8, 0xFF) : new Color(0xFF, 0xD6, 0xEC);
        Color bgBottom = isMale ? new Color(0xA0, 0xC4, 0xFF) : new Color(0xFF, 0xA8, 0xD9);
        GradientPaint gradient = new GradientPaint(0, 0, bgTop, 0, CARD_HEIGHT, bgBottom);
        g.setPaint(gradient);
        g.fillRoundRect(0, 0, CARD_WIDTH, CARD_HEIGHT, 30, 30);

        // === 상단 서비스 브랜드 ===
        g.setColor(new Color(0x44, 0x44, 0x66));
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        drawCentered(g, "✨ Baby Scan AI", CARD_WIDTH, 45);

        // === 성별 이모지 + 텍스트 ===
        String genderEmoji = isMale ? "👦" : "👧";
        String genderText  = isMale ? "남자아이예요!" : "여자아이예요!";
        g.setFont(new Font("SansSerif", Font.PLAIN, 72));
        drawCentered(g, genderEmoji, CARD_WIDTH, 160);

        g.setColor(new Color(0x22, 0x22, 0x44));
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        drawCentered(g, genderText, CARD_WIDTH, 220);

        // === 신뢰도 바 ===
        if (result.getConfidenceScore() != null) {
            int pct = (int) Math.round(result.getConfidenceScore() * 100);
            g.setColor(new Color(0xFF, 0xFF, 0xFF, 100));
            g.fillRoundRect(CARD_WIDTH / 2 - 150, 240, 300, 20, 10, 10);
            Color barColor = isMale ? new Color(0x42, 0x85, 0xF4) : new Color(0xE9, 0x18, 0x80);
            g.setColor(barColor);
            g.fillRoundRect(CARD_WIDTH / 2 - 150, 240, (int)(300 * result.getConfidenceScore()), 20, 10, 10);
            g.setColor(new Color(0x33, 0x33, 0x55));
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            drawCentered(g, "AI 예측 신뢰도: " + pct + "%", CARD_WIDTH, 278);
        }

        // === 리포트 한 줄 요약 ===
        String report = result.getReportText();
        if (report != null && report.length() > 60) {
            report = report.substring(0, 57) + "...";
        }
        g.setColor(new Color(0x44, 0x44, 0x55));
        g.setFont(new Font("SansSerif", Font.ITALIC, 15));
        if (report != null) drawCentered(g, report, CARD_WIDTH, 315);

        // === 날짜 + 주차 ===
        String dateStr = result.getAnalyzedAt() != null
                ? result.getAnalyzedAt().format(DATE_FMT) : "";
        String weekStr = image.getWeekNumber() != null
                ? image.getWeekNumber() + "주차" : "";
        g.setColor(new Color(0x55, 0x55, 0x77));
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        drawCentered(g, (weekStr.isEmpty() ? "" : weekStr + " · ") + dateStr, CARD_WIDTH, 350);

        // === 하단 면책 문구 ===
        g.setColor(new Color(0x77, 0x77, 0x99));
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        drawCentered(g, "본 결과는 참고용이며 의료적 진단이 아닙니다.", CARD_WIDTH, 420);

        g.dispose();

        // PNG로 변환
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(card, "PNG", baos);
        return baos.toByteArray();
    }

    private void drawCentered(Graphics2D g, String text, int width, int y) {
        FontMetrics fm = g.getFontMetrics();
        int x = (width - fm.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }
}
