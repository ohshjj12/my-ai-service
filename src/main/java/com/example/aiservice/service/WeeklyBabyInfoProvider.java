package com.example.aiservice.service;

import com.example.aiservice.dto.WeeklyBabyInfo;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * 임신 주차별 아기 표준 발달 정보를 제공합니다.
 * 출처: WHO / 대한산부인과학회 표준 수치 참고 (±10% 범위 정상)
 */
@Component
public class WeeklyBabyInfoProvider {

    private static final Map<Integer, WeeklyBabyInfo> DATA = Map.ofEntries(
            Map.entry(4, WeeklyBabyInfo.builder().week(4)
                    .avgLengthMm(1.0).avgWeightG(null)
                    .sizeComparison("양귀비 씨앗").developmentDesc("수정란이 착상을 완료하고 세포 분열이 시작됩니다.")
                    .momTip("임신을 처음 알게 되는 시기예요. 엽산 섭취를 시작하세요.").build()),
            Map.entry(5, WeeklyBabyInfo.builder().week(5)
                    .avgLengthMm(2.0).avgWeightG(null)
                    .sizeComparison("참깨").developmentDesc("심장이 뛰기 시작하고 뇌, 척수의 기초가 형성됩니다.")
                    .momTip("입덧이 시작될 수 있어요. 소량씩 자주 드세요.").build()),
            Map.entry(6, WeeklyBabyInfo.builder().week(6)
                    .avgLengthMm(4.0).avgWeightG(null)
                    .sizeComparison("렌틸콩").developmentDesc("팔다리 싹이 생기고 얼굴 윤곽이 잡히기 시작합니다.")
                    .momTip("첫 산부인과 방문으로 심박을 확인해보세요.").build()),
            Map.entry(7, WeeklyBabyInfo.builder().week(7)
                    .avgLengthMm(10.0).avgWeightG(null)
                    .sizeComparison("블루베리").developmentDesc("뇌가 빠르게 자라고 눈, 코, 입이 자리를 잡습니다.")
                    .momTip("피로감이 심할 수 있어요. 충분히 쉬세요.").build()),
            Map.entry(8, WeeklyBabyInfo.builder().week(8)
                    .avgLengthMm(16.0).avgWeightG(1.0)
                    .sizeComparison("딸기").developmentDesc("손가락·발가락이 생기고 근육이 발달해 움직이기 시작합니다.")
                    .momTip("커피는 하루 200mg(아메리카노 1잔) 이하로 줄여주세요.").build()),
            Map.entry(9, WeeklyBabyInfo.builder().week(9)
                    .avgLengthMm(23.0).avgWeightG(2.0)
                    .sizeComparison("포도").developmentDesc("태아기가 시작됩니다. 외부 생식기가 발달하기 시작합니다.")
                    .momTip("속이 쓰릴 수 있어요. 식사 후 바로 눕지 마세요.").build()),
            Map.entry(10, WeeklyBabyInfo.builder().week(10)
                    .avgLengthMm(31.0).avgWeightG(4.0)
                    .sizeComparison("살구").developmentDesc("손발톱이 자라기 시작하고 이빨 싹이 생깁니다.")
                    .momTip("입덧 피크 시기예요. 곧 나아질 거예요!").build()),
            Map.entry(11, WeeklyBabyInfo.builder().week(11)
                    .avgLengthMm(41.0).avgWeightG(7.0)
                    .sizeComparison("라임").developmentDesc("거의 모든 장기가 형성되었습니다. 얼굴이 더욱 선명해집니다.")
                    .momTip("첫 기형아 검사(NT 검사)를 준비할 시기예요.").build()),
            Map.entry(12, WeeklyBabyInfo.builder().week(12)
                    .avgLengthMm(54.0).avgWeightG(14.0)
                    .sizeComparison("자두").developmentDesc("손가락과 발가락이 완전히 분리됩니다. 반사 반응이 생깁니다.")
                    .momTip("1차 기형아 검사 시기! 유산 위험이 크게 줄어드는 시기예요.").build()),
            Map.entry(13, WeeklyBabyInfo.builder().week(13)
                    .avgLengthMm(74.0).avgWeightG(23.0)
                    .sizeComparison("복숭아").developmentDesc("성별이 외부에서 구분 가능해지기 시작합니다. 성문이 발달합니다.")
                    .momTip("안정기 진입! 입덧이 줄어드는 경우가 많아요.").build()),
            Map.entry(14, WeeklyBabyInfo.builder().week(14)
                    .avgLengthMm(87.0).avgWeightG(43.0)
                    .sizeComparison("레몬").developmentDesc("얼굴 근육이 발달해 표정을 짓기 시작합니다.")
                    .momTip("임신선(복부 중앙 선)이 생기기 시작할 수 있어요.").build()),
            Map.entry(15, WeeklyBabyInfo.builder().week(15)
                    .avgLengthMm(100.0).avgWeightG(70.0)
                    .sizeComparison("사과").developmentDesc("청각이 발달해 소리를 들을 수 있습니다. 태동을 느낄 수 있습니다.")
                    .momTip("태교 음악을 들려주기 좋은 시기예요.").build()),
            Map.entry(16, WeeklyBabyInfo.builder().week(16)
                    .avgLengthMm(116.0).avgWeightG(100.0)
                    .sizeComparison("배").developmentDesc("두피에 머리카락이 자라기 시작하고 눈썹이 생깁니다.")
                    .momTip("쿼드 검사(2차 기형아) 시기가 다가옵니다!").build()),
            Map.entry(17, WeeklyBabyInfo.builder().week(17)
                    .avgLengthMm(130.0).avgWeightG(140.0)
                    .sizeComparison("무").developmentDesc("지방이 쌓이기 시작합니다. 피부가 덜 투명해집니다.")
                    .momTip("배가 불러오면서 허리 통증이 생길 수 있어요.").build()),
            Map.entry(18, WeeklyBabyInfo.builder().week(18)
                    .avgLengthMm(142.0).avgWeightG(190.0)
                    .sizeComparison("고구마").developmentDesc("귀가 완전히 자리를 잡아 소리 방향을 가릴 수 있습니다.")
                    .momTip("2차 기형아 검사(16~18주) 준비하세요.").build()),
            Map.entry(19, WeeklyBabyInfo.builder().week(19)
                    .avgLengthMm(152.0).avgWeightG(240.0)
                    .sizeComparison("망고").developmentDesc("태아를 감싸는 특수 코팅(태지)이 생깁니다.")
                    .momTip("태동! 처음 느끼는 경우도 많아요. 희미한 두드림처럼 느껴져요.").build()),
            Map.entry(20, WeeklyBabyInfo.builder().week(20)
                    .avgLengthMm(255.0).avgWeightG(300.0)
                    .sizeComparison("바나나").developmentDesc("이제부터는 머리부터 발끝까지 재는 키를 측정합니다.")
                    .momTip("임신 중간! 정밀 초음파(정밀 기형아 검사) 시기예요.").build()),
            Map.entry(21, WeeklyBabyInfo.builder().week(21)
                    .avgLengthMm(268.0).avgWeightG(360.0)
                    .sizeComparison("당근").developmentDesc("눈꺼풀과 눈썹이 완전히 형성됩니다.")
                    .momTip("임신선이 더 진해질 수 있어요. 출산 후 자연스럽게 사라져요.").build()),
            Map.entry(22, WeeklyBabyInfo.builder().week(22)
                    .avgLengthMm(277.0).avgWeightG(430.0)
                    .sizeComparison("스파게티 한 주먹").developmentDesc("입술이 완전히 형성됩니다. 피부에 솜털이 나 있습니다.")
                    .momTip("임신성 당뇨 검사를 준비할 시기예요 (24~28주).").build()),
            Map.entry(23, WeeklyBabyInfo.builder().week(23)
                    .avgLengthMm(286.0).avgWeightG(500.0)
                    .sizeComparison("큰 망고").developmentDesc("청각이 거의 완성됩니다. 엄마 심장 소리를 들을 수 있습니다.")
                    .momTip("태교 대화가 효과적인 시기예요. 말을 많이 걸어주세요.").build()),
            Map.entry(24, WeeklyBabyInfo.builder().week(24)
                    .avgLengthMm(298.0).avgWeightG(600.0)
                    .sizeComparison("옥수수").developmentDesc("뇌가 빠르게 발달하고 폐 표면활성제가 생성되기 시작합니다.")
                    .momTip("24주 이후 조산해도 생존 가능성이 있어요. 무리하지 마세요.").build()),
            Map.entry(25, WeeklyBabyInfo.builder().week(25)
                    .avgLengthMm(346.0).avgWeightG(660.0)
                    .sizeComparison("루타바가").developmentDesc("손톱이 완전히 형성됩니다. 성별이 초음파로 명확히 보입니다.")
                    .momTip("손발이 붓기 시작할 수 있어요. 누울 때 다리를 높게 두세요.").build()),
            Map.entry(26, WeeklyBabyInfo.builder().week(26)
                    .avgLengthMm(355.0).avgWeightG(760.0)
                    .sizeComparison("상추 한 덩이").developmentDesc("눈을 뜨기 시작합니다. 처음으로 빛을 느낄 수 있습니다.")
                    .momTip("임신성 당뇨 검사(포도당 부하 검사) 시기예요.").build()),
            Map.entry(27, WeeklyBabyInfo.builder().week(27)
                    .avgLengthMm(368.0).avgWeightG(875.0)
                    .sizeComparison("꽃양배추").developmentDesc("뇌에 주름이 생기기 시작합니다. 성장이 가속화됩니다.")
                    .momTip("3분기 직전! 산전 교육 등록을 고려해보세요.").build()),
            Map.entry(28, WeeklyBabyInfo.builder().week(28)
                    .avgLengthMm(380.0).avgWeightG(1000.0)
                    .sizeComparison("가지").developmentDesc("눈이 완전히 열립니다. 드림(REM) 수면이 가능합니다.")
                    .momTip("3분기 시작! 이제부터 2주마다 산전 검사를 받을 수 있어요.").build()),
            Map.entry(29, WeeklyBabyInfo.builder().week(29)
                    .avgLengthMm(388.0).avgWeightG(1150.0)
                    .sizeComparison("호박").developmentDesc("뼈가 완전히 굳어지기 시작합니다. 칼슘 흡수가 많아집니다.")
                    .momTip("칼슘 풍부한 음식(우유, 두부, 치즈)을 충분히 드세요.").build()),
            Map.entry(30, WeeklyBabyInfo.builder().week(30)
                    .avgLengthMm(399.0).avgWeightG(1300.0)
                    .sizeComparison("큰 양배추").developmentDesc("폐가 성숙해집니다. 조산 시 생존율이 크게 높아집니다.")
                    .momTip("태동 횟수를 세어보세요. 2시간 내 10번 이상이 정상이에요.").build()),
            Map.entry(31, WeeklyBabyInfo.builder().week(31)
                    .avgLengthMm(411.0).avgWeightG(1500.0)
                    .sizeComparison("코코넛").developmentDesc("모든 오감이 작동합니다. 빛과 소리에 반응합니다.")
                    .momTip("숨이 찰 수 있어요. 서서히 움직이고 자주 쉬세요.").build()),
            Map.entry(32, WeeklyBabyInfo.builder().week(32)
                    .avgLengthMm(422.0).avgWeightG(1700.0)
                    .sizeComparison("스쿼시").developmentDesc("손발톱이 손가락 끝까지 자랐습니다. 머리카락이 보입니다.")
                    .momTip("출산 가방 준비를 시작해도 좋아요!").build()),
            Map.entry(33, WeeklyBabyInfo.builder().week(33)
                    .avgLengthMm(433.0).avgWeightG(1900.0)
                    .sizeComparison("파인애플").developmentDesc("면역계가 형성됩니다. 뇌가 매우 빠르게 성장합니다.")
                    .momTip("브랙스턴 힉스(가진통)가 시작될 수 있어요.").build()),
            Map.entry(34, WeeklyBabyInfo.builder().week(34)
                    .avgLengthMm(450.0).avgWeightG(2100.0)
                    .sizeComparison("멜론").developmentDesc("지방이 빠르게 쌓이고 피부가 분홍빛을 띱니다.")
                    .momTip("출산 예정 병원과 분만 방법을 확인해두세요.").build()),
            Map.entry(35, WeeklyBabyInfo.builder().week(35)
                    .avgLengthMm(462.0).avgWeightG(2350.0)
                    .sizeComparison("허니듀 멜론").developmentDesc("신장이 완전히 발달합니다. 간이 폐기물 처리를 시작합니다.")
                    .momTip("자주 소변이 마려울 수 있어요.").build()),
            Map.entry(36, WeeklyBabyInfo.builder().week(36)
                    .avgLengthMm(473.0).avgWeightG(2600.0)
                    .sizeComparison("파파야").developmentDesc("아기가 골반 방향으로 자리를 잡기 시작합니다(선진부 고정).")
                    .momTip("숨쉬기가 편해졌다면 아기가 내려온 거예요.").build()),
            Map.entry(37, WeeklyBabyInfo.builder().week(37)
                    .avgLengthMm(485.0).avgWeightG(2850.0)
                    .sizeComparison("겨울 멜론").developmentDesc("'조기 만삭(Early term)' - 폐, 뇌, 간이 거의 완성됩니다.")
                    .momTip("37주 이후 출산은 안전해요. 언제든 병원 갈 준비를 해두세요.").build()),
            Map.entry(38, WeeklyBabyInfo.builder().week(38)
                    .avgLengthMm(496.0).avgWeightG(3050.0)
                    .sizeComparison("수박").developmentDesc("태지가 대부분 사라집니다. 솜털도 거의 없어집니다.")
                    .momTip("이슬이 비치거나 양수가 새면 바로 병원으로 가세요.").build()),
            Map.entry(39, WeeklyBabyInfo.builder().week(39)
                    .avgLengthMm(504.0).avgWeightG(3250.0)
                    .sizeComparison("호박").developmentDesc("뇌가 계속 발달합니다. 출생 후에도 3년간 폭발적으로 성장해요.")
                    .momTip("진통 간격을 재는 앱을 준비해두세요.").build()),
            Map.entry(40, WeeklyBabyInfo.builder().week(40)
                    .avgLengthMm(510.0).avgWeightG(3400.0)
                    .sizeComparison("호박(출산 준비 완료)")
                    .developmentDesc("완전한 만삭! 아기는 언제든 세상에 나올 준비가 됐습니다.")
                    .momTip("드디어 출산일! 긴장하지 마세요, 잘 하실 수 있어요. 💪").build())
    );

    /** 주어진 주차의 아기 정보를 반환합니다. 범위 밖이면 Optional.empty() */
    public Optional<WeeklyBabyInfo> getInfo(int week) {
        return Optional.ofNullable(DATA.get(week));
    }

    /** 범위 밖 주차는 가장 가까운 주차 정보로 반환 (4주 미만은 4주, 40주 초과는 40주) */
    public WeeklyBabyInfo getInfoOrNearest(int week) {
        int clamped = Math.max(4, Math.min(40, week));
        WeeklyBabyInfo info = DATA.get(clamped);
        if (info != null) return info;
        // 근처 주차 탐색
        for (int delta = 1; delta <= 3; delta++) {
            info = DATA.get(clamped + delta);
            if (info != null) return info;
            info = DATA.get(clamped - delta);
            if (info != null) return info;
        }
        return WeeklyBabyInfo.builder().week(week).developmentDesc("정보 없음").build();
    }
}
