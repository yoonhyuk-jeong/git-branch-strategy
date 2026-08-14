---
description: UAT 통과분을 master에 조립 PR(1건=배포1회)로 반영. 정합성 diff 검증 후 최종 merge는 사용자 승인 필요
argument-hint: [release-{날짜}] (생략 시 열린 release 자동 탐색)
---

UAT 통과분을 **master에 merge = prod 배포**한다. v4에서 prod 배포는 오직 feature(또는 조립 브랜치)를 master에 merge하는 것. **release는 master로 merge하지 않는다.**

인자: `$ARGUMENTS` (배포 대상 `release-{날짜}`; 없으면 열린 release를 찾아 확인)

## 절차

### 1. 배포 대상 확인
- `git fetch origin --quiet`
- 대상 release 확정(인자 없으면 `git ls-remote --heads origin 'release-*'`로 후보 제시).
- UAT에 올라간 내용: `git log <release-{날짜}> --not master --oneline` → 포함 feature 목록을 사용자에게 보여준다.

### 2. 선별 '제외' 재검증 경고 (4.5절)
- 이번에 master로 낼 feature가 UAT 조합의 **부분집합**이면(= 일부 제외) 경고한다:
  > "UAT는 master+A+B를 검증했는데 A만 배포하면 prod 조합(master+A)은 UAT 미검증입니다.
  >  기본 규칙: `release-{날짜}-v2`로 A만 재조립 → UAT 재검증 후 배포.
  >  A·B가 명백히 독립이면 사유를 남기고 생략 가능."
- 재조립이 필요하면 여기서 중단하고 `/imt-release` 재조립을 안내.

### 3. 조립 PR 구성 (1건 = 배포 1회)
- prod 트리거는 `pull_request closed(merged)` → **PR 1건 = 배포 1회.** feature별 개별 PR은 배포 N회가 되므로 금지.
- 조립 브랜치를 만들어 배포 확정 feature들을 Merge Commit으로 모은다(충돌은 로컬 rerere로 해결):
  ```bash
  git checkout -b assemble/{날짜} origin/master
  git merge --no-ff <feature1>
  git merge --no-ff <feature2>   # 여러 개면 반복
  git push -u origin assemble/{날짜}
  ```
  (단건이면 feature 브랜치 자체를 PR base=master로 올려도 됨 — fast path)
- master로 **PR 1건** 생성(**Create a merge commit**):
  ```bash
  gh pr create --base master --title "deploy {날짜}: <요약>" --body "<포함 Jira/feature 목록>"
  ```

### 4. 배포 전 정합성 판독 (4.1절) — 필수
```bash
git diff <release-{날짜}> master   # (조립 반영 상태 기준으로 판독)
```
- **비어 있음** → release 전체가 그대로 prod 반영 (안전, 통째와 동일 증명)
- **제외분만 보임** → 의도적 제외 (2단계에서 확인된 것)
- **그 외가 보임** → 충돌이 다르게 풀렸거나 forward merge 누락 → **여기서 중단.** 원인부터 확인(먼저 `/imt-sync`).

### 5. ⏸ 최종 승인 (자동 merge 금지)
아래를 한 화면에 요약해 보여주고 **사용자 승인을 명시적으로 받는다. 승인 전 절대 merge하지 않는다:**
- 생성된 master PR 링크
- 포함 feature/Jira 목록
- 4단계 diff 판독 결과(비었음 / 제외분만 / 중단사유)

"이대로 prod 배포할까요?"에 사용자가 승인해야 다음으로 간다. 4단계가 "그 외" 상태면 승인 단계로 넘어가지 않는다.

### 6. 배포 & 사후
- (승인 시) master PR을 merge → prod 자동배포(**master에 dispatch 없음** — UAT가 진짜 게이트, 검증한 소스 그대로).
- 배포 후: `/imt-sync`로 master를 열린 test/release/dev에 forward merge, 끝난 release 삭제, 반영된 feature 삭제 안내.

## 가드
- **Squash 금지**, master는 feature/조립 단위로만(통째 merge/FF 금지, release→master 금지).
- 여러 기능 = 조립 PR 1건. 개별 PR 난사 금지.
- UAT 미통과분 master 반영 금지.
