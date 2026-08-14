---
description: IMT Git Branch v4 배포 플로우 — feature 브랜치 생성/QA(test)/UAT(release)/prod(master) merge, forward merge, 금지 규칙. 이 레포에서 브랜치 생성·merge·PR·배포 관련 작업을 할 때 항상 이 규칙을 따른다.
---

# IMT Git Branch v4 플로우

이 레포의 브랜치/배포 규칙은 **v4**를 따른다. 브랜치 생성, merge, PR, 배포, 충돌 처리 관련 작업을 할 때 아래 규칙을 지키고, 반복 절차는 전용 커맨드(`/imt-*`)를 안내한다.

> **규칙 원본(단일 진실):** [`claude-help/workflow/GIT_BRANCH_STRATEGY_V4_DRAFT.md`](../../claude-help/workflow/GIT_BRANCH_STRATEGY_V4_DRAFT.md)
> 여기 요약과 원본이 충돌하면 원본이 우선. 세부/시나리오는 원본을 읽어라.

## 핵심 원칙

> **하나의 기능 = 하나의 브랜치 = 동일한 커밋(SHA)이 dev / test / release / master에 각각 독립 merge된다.**
> 네 브랜치는 승격 파이프라인이 아니라 4개의 독립 목적지다. "어디까지 merge했나 = 그 기능의 현재 단계."

- `master` = prod 진실. **UAT 통과분만** 들어온다. 모든 feature의 분기점.
- `dev` = 연동 놀이터(push 자동, 파이프라인 밖, merge 선택).
- `test` = **QA 게이트 + 배포 대기실**(dispatch). QA 통과해도 배포일 미정이면 여기 머문다.
- `release-{날짜}` = 이번 배포 확정분만 모은 UAT 조립대(dispatch). **어디에도 merge 안 함.** 배포 후 삭제.
- 모든 merge는 **Merge Commit** — Squash 금지.

## 브랜치 = 칸반

```
         dev   test   release-0812   master
featA     ✓     ✓         —            —      QA통과·배포일 미정 (test 대기)
featB     ✓     ✓         ✓            ✓      이번에 나가는 것
featC     ✓     —         —            —      개발/연동 중
```

단계 확인: `git branch --contains <sha>` / `git log test --not master` / `git log release-0812 --not master`

## 개발자 일상 3단계

1. **master에서 브랜치 하나 판다.** 복사본(`-dev`/`-test`/`-uat`) 절대 금지. → `/imt-feature`
2. **보내고 싶은 곳에 그 브랜치를 merge한다.**
   - 연동 필요 → `dev` (선택)
   - QA 준비 → `test` (여기서 **본 리뷰 1회**) → `/imt-test`
   - 배포 확정 → `release-{날짜}` (UAT) → `/imt-release`
3. **prod로 = 그 feature를 master에 merge.** → `/imt-deploy`

추가 습관: `git merge master`로 내 브랜치 정기 최신화 → `/imt-sync`

## 커맨드 지도

| 커맨드 | 언제 |
| --- | --- |
| `/imt-feature` | master에서 새 feature 분기 (네이밍 검증 + rerere on) |
| `/imt-test` | feature → test PR(본 리뷰) → test 환경 dispatch |
| `/imt-release` | release-{날짜} 조립 → uat dispatch |
| `/imt-deploy` | 조립 PR → master (1건=1배포) + 정합성 diff, **최종 merge는 승인 후** |
| `/imt-sync` | feature 최신화 / master forward merge(PR) |
| `/imt-status` | 칸반: 어느 기능이 어느 단계인지 |

## 금지 규칙 (절대)

1. 타겟별 사본 브랜치(`-dev`/`-test`/`-master`/`-release`) 생성 금지.
2. feature에 dev/test/release를 merge 금지(역방향 오염). **당겨오는 건 `git merge master`만.**
3. `test` / `release`를 **어디로든 merge 금지** — 둘 다 목적지 전용. master는 **항상 feature 단위**로만 받는다(통째/FF 포함 금지).
4. dev/test/release에서 feature 분기 금지 — **항상 master**.
5. 이미 공유된 브랜치 rebase 금지 — `git merge master`로 대신.
6. dev/test에 직접 커밋 금지 — 모든 코드는 feature 경유.
7. master에 UAT 미통과분 merge 금지.
8. **모든 merge는 Merge Commit — Squash/Rebase merge 금지.**

## 네이밍

```
feature : {날짜}/{이니셜}/{JIRA}-{기능명}   예) 0812/YH/IMTDEV-1980-user-filtering
release : release-{월일}                     예) release-0812   (재조립: release-0812-v2)
hotfix  : hotfix/{짧은설명}
```
- 날짜 = 목표 배포일(MMDD). date-first라 `git branch --list "0812/*"`로 배포 후보가 묶인다.
- **혼합 네이밍 금지**: `release-2.13.0-0819` 같은 버전+날짜 혼합 금지. `release-` prefix는 UAT 조립대 전용.

## 충돌

- `git config --global rerere.enabled true` (같은 feature를 4곳에 반복 merge하므로 필수).
- rerere 캐시는 **클론-로컬** → GitHub UI merge엔 안 먹는다. **충돌 해결은 항상 로컬에서** 풀고 push한 뒤 PR merge. release 조립에서 충돌 푼 사람이 master 반영까지 수행.

## 강제의 한계 (중요)

이 스킬/커맨드는 **Claude를 통할 때만** 유효하다 — 터미널 직접 `git push`나 GitHub UI Squash 버튼은 못 막는다. **진짜 강제는 GitHub 설정**(Squash/Rebase merge 비활성, master/test branch protection, required PR)이다. 이 플로우 도구는 "올바른 길을 가장 쉬운 길로" 만드는 생산성 계층이지 최후 방어선이 아니다.
