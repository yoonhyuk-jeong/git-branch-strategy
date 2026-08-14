package com.ti;

/**
 * IMT Git Branch 전략 v4 검증용 샌드박스 앱.
 *
 * <p>실제 로직은 없다. 브랜치/merge/배포 플로우를 돌려보기 위한 최소 진입점이며,
 * 각 feature 브랜치는 {@link FeatureRegistry}에 자기 기능을 한 줄 등록한다.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=== git-branch-strategy sandbox ===");
        System.out.println("env     : " + System.getProperty("app.env", "local"));
        System.out.println("version : " + Version.CURRENT);
        System.out.println();

        // 등록된 기능 목록 출력 — 어느 환경에 무엇이 배포됐는지 눈으로 확인하는 용도
        FeatureRegistry.printAll();
    }
}
