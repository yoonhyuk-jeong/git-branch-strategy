package com.ti;

/**
 * 배포 추적용 버전 상수.
 *
 * <p>v4 12절 "이미지 태그: 버전/SHA 병행" 요건을 샌드박스에서 모방한 것.
 * "UAT에서 검증한 것 = prod에 나간 것"을 증명하는 축이 버전과 SHA이므로,
 * 더미 워크플로도 이 값과 커밋 SHA를 함께 출력한다.
 */
public final class Version {

    /** 배포 시 semantic-release가 올리는 값을 대신하는 상수. */
    public static final String CURRENT = "1.0.0";

    private Version() {
    }
}
