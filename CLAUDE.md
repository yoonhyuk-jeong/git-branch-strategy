# CLAUDE.md

## 이 레포의 정체

**IMT Git Branch 전략 v4 검증용 샌드박스.** 제품 코드가 아니다. 브랜치/merge/배포 플로우와
`.claude/` 도구(skill + `/imt-*` 커맨드)를 실제로 돌려보고 부수기 위한 곳이다.

- 규칙 원본(단일 진실): [Confluence — IMT Git Branch 전략 v4](https://trialinformatics.atlassian.net/wiki/spaces/ImageTrial/pages/716800002) (확정 2026-08-14). **로컬 사본을 두지 않는다**
- 검증 시나리오 체크리스트: `TESTING_V4.md`
- 브랜치/merge/배포 작업은 **`imt-git-flow` 스킬 규칙을 따른다** (자동 참고됨)

## 중요한 차이 (실전 레포와 비교)

| 항목 | 이 샌드박스 | 실전(`image-trial-server`) |
| --- | --- | --- |
| CI 워크플로 | **더미** — Docker/배포 없이 build + 버전·SHA 출력만 | 실제 ghcr 푸시 + 배포 |
| `.claude/` 파일 | 실전 레포에서 포팅 (같은 내용 유지) | 원본 |
| 브랜치 파괴 | 자유 (force push·reset 실험 OK) | 금지 |

`.claude/commands/*.md`와 `.claude/skills/imt-git-flow/SKILL.md`는 **실전 레포와 동일하게 유지**한다.
여기서 고친 게 쓸만하면 `image-trial-server`로 역포팅하는 것이 이 샌드박스의 목적이다.
(`/imt-release`·`/imt-sync`·`/imt-status` 3개는 여기서 처음 구현했으므로 아직 역포팅 안 됨.)

## 빌드

```bash
./gradlew build
java -cp build/classes/java/main com.ti.Main
```

주의: 이 레포는 Windows에서 만들어져 `gradlew`가 CRLF였다. LF로 정규화해 뒀으니 되돌리지 말 것.

## 셸 주의 (zsh)

기본 셸이 zsh다. **zsh는 `for x in $VAR` 에서 단어 분리를 하지 않는다** — 브랜치 목록을 변수에 담아
순회하면 한 덩어리로 들어와서 조용히 틀린 결과가 나온다. 리스트 순회는 파이프 + `while IFS= read -r` 로 쓴다.
(`$(...)` 명령 치환은 zsh에서도 분리되므로 그건 괜찮다.)

## 코드 구조 (충돌 실험용으로 의도된 것)

- `FeatureRegistry.register()` — **의도된 충돌 지점.** 모든 feature가 여기에 한 줄 추가 → 동시 진행 시 반드시 충돌 (rerere 검증용)
- `src/main/java/com/ti/feature/` — 파일만 추가하는 독립 기능 (충돌 없음 케이스 검증용)
- `Version.CURRENT` — v4 12절 "버전/SHA 병행 태그" 요건을 재현하기 위한 상수

## 코드 규칙

- 주요 로직에 **한글 주석**
- 커밋 메시지: Conventional Commits
