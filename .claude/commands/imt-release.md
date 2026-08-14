---
description: 배포 확정분만 release-{날짜}에 조립하고 uat 환경 dispatch까지 안내/실행 (재조립 -v2 포함)
argument-hint: [MMDD] [feature 브랜치...] (생략 시 열린 release / test 대기분에서 후보 제시)
---

배포일이 확정된 기능만 모아 **UAT 조립대**(`release-{날짜}`)를 만들고 uat 환경에 올린다. 인자: `$ARGUMENTS`

`release-{날짜}`는 **사이클 1회용 목적지**다. 어디에도 merge하지 않고, 배포가 끝나면 삭제한다.

## 가드 (먼저 확인)

1. `git fetch origin --quiet`
2. **네이밍 검증** — `release-{MMDD}` 만 허용. 재조립은 `release-{MMDD}-v2`.
   - `release-2.13.0-0819` 같은 **버전+날짜 혼합 명명은 금지**. `release-` prefix는 UAT 조립대 전용이다.
     버전 트랙 묶음이면 `epic/{트랙}` 으로 다른 이름을 쓰도록 안내한다 (부록 A).
3. **fold-in 금지** — 열려 있는 다른 `release-*`를 이번 release에 흡수하지 않는다. 미배포 기능은 **원본 feature 브랜치에서** 다시 merge한다.
4. 조립 대상은 **배포가 확정된 feature만**. test에서 대기 중인 기능은 그대로 남긴다 (대기가 다른 기능의 배포를 막지 않는다).

## 절차

### 1. 대상 확정
- MMDD 없으면 물어본다. 열린 release 후보: `git ls-remote --heads origin 'release-*'`
- 조립 대상 feature를 사용자에게 확인받는다. 후보는 test에 올라간 기능 목록에서 뽑는다:
  ```bash
  git log origin/test --not origin/master --merges --oneline
  ```
  → 여기 있는 것 중 **배포 확정된 것만** 고른다. 확정 여부는 사용자에게 확인한다 (Claude가 판단하지 않는다).

### 2. release 브랜치 생성 (없으면)
**항상 최신 master에서** 생성한다 — 이전 release나 test에서 따면 미배포 기능이 딸려 들어간다.
```bash
git checkout -b release-{MMDD} origin/master
git push -u origin release-{MMDD}
```
이미 있으면 checkout하고, master가 움직였으면 forward merge부터 한다 (`/imt-sync` 모드 B).

### 3. 확정분 merge (Merge Commit)
```bash
git merge --no-ff <feature1>
git merge --no-ff <feature2>   # 여러 개면 반복
git push origin release-{MMDD}
```
- **충돌은 항상 로컬에서** 푼다 (rerere 캐시가 클론-로컬이라 GitHub UI merge엔 안 먹는다).
  여기서 충돌을 푼 사람이 **master 반영(`/imt-deploy`)까지 수행**해야 한다. 담당자가 갈리면 다르게 풀릴 수 있고, 그건 4.1절 diff에서 드러난다.
- 리뷰 강도는 **확인 수준** (조합 구성이 맞는지). 본 리뷰는 이미 test PR에서 했다.
- PR로 올려도 되지만(base=`release-{MMDD}`), 충돌이 있으면 로컬에서 풀어 push한 뒤 PR을 merge한다.

### 4. UAT 내용물 확인 — 사용자에게 보여준다
```bash
git log release-{MMDD} --not master --oneline --merges
git diff --stat master release-{MMDD}
```
"UAT에 올라가는 것 = master + 이 목록" 임을 한 화면에 정리해 보여준다.

### 5. ⏸ uat dispatch (승인 후)
UAT는 **단일 환경**이다. 열린 release가 여러 개라도 **한 번에 하나만 점유**한다. 되돌리기 어려운 동작이므로 **승인 전 실행 금지**:
- 먼저 현재 UAT 점유 상황을 확인하고 알린다: `gh run list --workflow=uat-build-deploy.yml -L 3`
- 다른 release가 검증 중이면 **덮어쓰기 경고**를 하고 진행 여부를 묻는다.
```bash
gh workflow run uat-build-deploy.yml --ref release-{MMDD}
```
실행 후 `gh run list --workflow=uat-build-deploy.yml -L 1`로 트리거 확인 → 링크 안내.

## 재조립 (4.5절 — 배포에서 기능을 제외할 때)

UAT를 A+B로 통과했는데 B를 빼고 A만 내보내면, prod 조합(master+A)은 **UAT에서 검증된 적이 없다.**

- **기본**: `release-{MMDD}-v2`를 master에서 새로 만들어 **A만** merge → uat 재검증 → 배포
- **예외**: A·B가 명백히 독립(도메인·파일 겹침 없음, 기능 간 호출 없음)이면 팀 판단으로 생략 가능 —
  **생략 사유를 배포 기록(PR 본문/라벨)에 남긴다.** Claude가 독립성을 단독 판정하지 말고 근거(겹치는 파일 목록)를 제시하고 사용자에게 확인받는다:
  ```bash
  git diff --name-only master...<featureA> > /tmp/a.txt
  git diff --name-only master...<featureB> > /tmp/b.txt
  comm -12 <(sort /tmp/a.txt) <(sort /tmp/b.txt)   # 겹치는 파일 = 독립성 반증
  ```
- 제외된 B는 release에서 빠져 **test로 복귀**(수정 계속). B 브랜치는 삭제하지 않는다.

## fast path (단건 수시 배포)

기능 1개짜리 긴급 건은 release 조립을 생략할 수 있다:
1. feature를 master 최신으로 유지 (`git merge master`) — feature = master + 이 기능이므로 release와 동일한 트리
2. **uat를 feature 브랜치에서 직접 dispatch**: `gh workflow run uat-build-deploy.yml --ref <feature>`
   (워크플로의 master-ancestor 가드가 낡은 브랜치를 걸러낸다)
3. UAT 통과 → `/imt-deploy`

## 사후

- 배포 완료 후 `release-{MMDD}` **삭제**. 다음 release로 흡수 금지.
- master가 움직이면 열린 release에 forward merge (`/imt-sync` 모드 B) — 안 하면 4.1절 diff가 지저분해진다.
