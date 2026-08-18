package com.ti.feature;

/** Visit 목록 응답에서 null 이 그대로 나가던 문제 방어 (IMTDEV-4003). */
public final class VisitNullGuard {

    private VisitNullGuard() {
    }

    // 미업로드 상태를 null 로 내보내면 클라이언트가 터진다 — 빈 문자열로 정규화
    public static String normalize(String raw) {
        return raw == null ? "" : raw;
    }
}
