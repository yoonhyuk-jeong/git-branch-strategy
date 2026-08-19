# IMT Git Branch 전략 v4 — 발표 자료

> **https://yoonhyuk-jeong.github.io/git-branch-strategy/**

브랜치·merge·배포 플로우와 버전/digest 추적을 설명하는 시각 자료다.
이 레포에는 `docs/` 아래 HTML만 있다 — GitHub Pages가 Team 플랜에서 private 레포를
서빙하지 못해 **문서만 public으로 분리**해 둔 것이다.

| 페이지 | 내용 |
| --- | --- |
| [`index.html`](docs/index.html) | 허브 |
| [`v4-flow.html`](docs/v4-flow.html) | **① 규칙** — 전략 시각 설명. 4개 독립 목적지, 조립 PR, 금지 규칙 10개, `/imt-*` 커맨드 |
| [`cycle-0819.html`](docs/cycle-0819.html) | **② 실행 기록** — 0819 사이클. 조직 샌드박스에서 실행 (v1.3.0) |
| [`cycle-0818.html`](docs/cycle-0818.html) | 부록 — 0818 사이클. **이 레포에서 실행** (조직 이관 전) |

## 히스토리를 남겨 둔 이유

부록인 `cycle-0818.html`이 기록한 사이클은 **이 레포에서 실제로 돌린 것**이다.
그 증거는 문서가 아니라 레포에 있다:

- 브랜치 `dev` / `test` — "브랜치 = 칸반"의 실물
- 태그 `v1.0.0` `v1.1.0` `v1.1.1` / `uat-v1.1.0-rc.1` `uat-v1.1.1-rc.1` — 배포 원장
- Actions 실행 기록 — 문서의 `run ↗` 링크가 가리키는 곳

그래서 브랜치·태그·커밋 히스토리는 그대로 둔다.
샌드박스 소스와 워크플로는 조직 레포로 이관했고 여기서는 제거했다
(이 레포에 남아 있던 사본은 이관 이후 갱신되지 않아 실물과 어긋난 상태였다).

## 규칙 원본

이 문서는 시각화일 뿐이고, 단일 진실은 사내 Confluence의
**IMT Git Branch 전략 v4** 문서다. 충돌하면 원본이 우선한다.
