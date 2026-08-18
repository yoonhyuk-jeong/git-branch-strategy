package com.ti.feature;

/** 감사 로그 기록 기능 (IMTDEV-4001). */
public final class AuditLog {

    private AuditLog() {
    }

    // 누가 언제 무엇을 했는지 남긴다 — 임상시험 데이터는 변경 이력이 규제 요건이다
    public static void record(String actor, String action) {
        System.out.printf("[audit] %s -> %s%n", actor, action);
    }
}
