package com.ti;

import java.util.ArrayList;
import java.util.List;

/**
 * 이 샌드박스에 등록된 기능 목록.
 *
 * <p><b>의도된 충돌 지점(conflict hotspot).</b> 모든 feature 브랜치가 {@link #register()}에
 * 자기 한 줄을 추가하도록 해서, 두 feature가 동시에 진행되면 반드시 충돌이 난다.
 * v4 8절의 {@code git merge master} 습관과 {@code git rerere}(같은 충돌을 4곳에 반복 merge)를
 * 실제로 검증하기 위한 장치다.
 *
 * <p>충돌 없는 독립 기능을 테스트하려면 {@code com.ti.feature} 패키지에 파일만 추가하고
 * 여기는 건드리지 않으면 된다 (v4 6절 시나리오 4의 "명백히 독립" 케이스).
 */
public final class FeatureRegistry {

    private FeatureRegistry() {
    }

    private static List<String> register() {
        List<String> features = new ArrayList<>();
        features.add("baseline");
        // ↓ feature 브랜치는 이 아래에 한 줄 추가한다 (여기가 충돌 지점)
        features.add("audit-log");
        features.add("export-csv");
        return features;
    }

    public static void printAll() {
        System.out.println("registered features:");
        for (String feature : register()) {
            System.out.println("  - " + feature);
        }
    }
}
