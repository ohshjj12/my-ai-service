package com.example.aiservice.controller;

import com.example.aiservice.entity.PredictionResult;
import com.example.aiservice.entity.UltrasoundImage;
import com.example.aiservice.service.ShareCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ShareCardService shareCardService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/app")
    public String app() {
        return "app";
    }

    @GetMapping("/timeline")
    public String timeline() {
        return "timeline";
    }

    /**
     * 공개 공유 페이지 - OG 메타태그 포함
     * GET /share/{id}
     */
    @GetMapping("/share/{id}")
    public String sharePage(@PathVariable Long id, Model model) {
        try {
            PredictionResult result = shareCardService.getPublicResult(id);
            UltrasoundImage image = result.getUltrasoundImage();

            boolean isMale = "MALE".equalsIgnoreCase(result.getPredictedGender());
            String genderText = isMale ? "남자아이" : "여자아이";
            String genderEmoji = isMale ? "👦" : "👧";
            int confidencePct = result.getConfidenceScore() != null
                    ? (int) Math.round(result.getConfidenceScore() * 100) : 0;

            model.addAttribute("predictionId", id);
            model.addAttribute("genderText", genderText);
            model.addAttribute("genderEmoji", genderEmoji);
            model.addAttribute("confidencePct", confidencePct);
            model.addAttribute("reportText", result.getReportText());
            model.addAttribute("weekNumber", image != null ? image.getWeekNumber() : null);
            model.addAttribute("analyzedAt", result.getAnalyzedAt());
            model.addAttribute("isMale", isMale);
            model.addAttribute("cardImageUrl", "/share/" + id + "/card.png");

        } catch (Exception e) {
            model.addAttribute("error", "공유 결과를 찾을 수 없습니다.");
        }
        return "share";
    }
}
