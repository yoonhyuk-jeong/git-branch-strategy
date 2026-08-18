package com.ti.feature;

import java.util.List;

/** 결과 데이터 CSV 내보내기 (IMTDEV-4002). */
public final class CsvExporter {

    private CsvExporter() {
    }

    // 통계 담당자가 SAS로 넘기기 전 단계 — 구분자는 쉼표 고정
    public static String toCsv(List<String> rows) {
        return String.join(",", rows);
    }
}
