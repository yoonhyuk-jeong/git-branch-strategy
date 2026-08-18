package com.ti;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 실행 중인 앱이 "나는 어떤 버전이고 어떤 산출물인가"를 스스로 답하게 하는 클래스.
 *
 * <p><b>핵심 설계: 버전은 굽지 않고 주입한다.</b> 버전 문자열을 빌드 시점에 산출물에 박으면
 * 산출물 바이트가 버전에 종속되어, 같은 코드라도 버전이 다르면 digest 가 달라진다.
 * 그러면 "UAT 에서 검증한 그 산출물이 prod 에 그대로 나갔다"를 digest 로 증명할 수 없다.
 *
 * <p>그래서 우선순위를 이렇게 둔다:
 * <ol>
 *   <li>환경변수 {@code APP_VERSION} — 배포 시 주입되는 실제 배포 버전 (재빌드 불필요)</li>
 *   <li>jar 안의 {@code version.yml} — 빌드 시점 fallback (로컬 실행·주입 누락 대비)</li>
 *   <li>{@code unknown}</li>
 * </ol>
 *
 * <p>{@code APP_DIGEST}/{@code APP_COMMIT} 도 같은 이유로 실행 시 주입된다.
 * 실제 레포에서는 compose 의 {@code environment:} 로 넣고, 여기서는 dummy 배포 스텝이 넣는다.
 */
public final class Version {

    private static final Pattern VERSION_LINE = Pattern.compile("(?m)^version:\\s*(\\S+)");

    /** 배포 버전. 주입값 > jar 내장값 > unknown. */
    public static final String CURRENT = resolveVersion();

    /** 이 산출물의 digest. 배포 파이프라인이 주입한다. */
    public static final String DIGEST = envOr("APP_DIGEST", "unknown");

    /** 빌드된 커밋 SHA. 배포 파이프라인이 주입한다. */
    public static final String COMMIT = envOr("APP_COMMIT", "unknown");

    /** 어느 환경에 떠 있는가 (dev/test/uat/prod). */
    public static final String ENV = envOr("APP_ENV", System.getProperty("app.env", "local"));

    private Version() {
    }

    private static String resolveVersion() {
        // 1순위: 주입된 배포 버전
        String injected = envOr("APP_VERSION", null);
        if (injected != null) {
            return injected;
        }
        // 2순위: jar 안에 들어온 version.yml (빌드 시점 값)
        String baked = readBakedVersion();
        return baked != null ? baked : "unknown";
    }

    private static String readBakedVersion() {
        try (InputStream in = Version.class.getResourceAsStream("/version.yml")) {
            if (in == null) {
                return null;
            }
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Matcher m = VERSION_LINE.matcher(text);
            return m.find() ? m.group(1) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static String envOr(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
