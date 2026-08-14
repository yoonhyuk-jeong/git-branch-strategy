# IMT Git Branch 전략 v4 (초안)

> **상태: 팀 논의용 초안 (2026-08-05).** v3(Confluence 686096423, 2026-07-14) 확정 이후 팀 운영 요건 재확인에서 나온 변경을 반영했다.
> **Confluence 게시 완료 (2026-08-05):** https://trialinformatics.atlassian.net/wiki/spaces/ImageTrial/pages/716800002 (v3 하위 페이지). 이 파일이 원본 — 수정 시 페이지 동기화 필요.

## 0. 문서 개요

v3 확정 이후 아래 운영 현실을 반영해 **3브랜치 → 4브랜치**로 개정한다.

v3 → v4 핵심 변경:

1. **`test` 브랜치 신설** — 모든 기능은 test(QA) → uat를 반드시 거친다는 팀 요건을 브랜치로 명시화. dev(연동)와 test(QA 게이트)를 분리
2. **test 대기 공식화** — QA가 끝나도 배포일이 정해질 때까지 기능이 test에 머물 수 있다. "test까지만 머무는 기능"과 "uat까지 가는 기능"이 공존하는 현실을 모델에 수용
3. **uat 자동배포 목표 폐기** — release-{날짜}가 동시에 여러 개 열리는 현실(실측: 0807/0814/0819 동시 오픈)에서 push 자동은 UAT 덮어쓰기 위험. **uat는 dispatch 유지**
4. **묶음 배포 방식 정리** — 여러 기능을 함께 내보낼 땐 **조립 PR 1건 = prod 배포 1회**로 통일 (v3의 "묶음 push 1회 = 배포 1회"는 현행 prod 트리거에서 동작하지 않아 바로잡음)
5. **수시배포 공식화** — "월 1회 정기배포" 전제 제거. 배포는 필요할 때마다 release-{날짜}를 만들어 나간다
6. 선별 **제외** 시 재검증 규칙, rerere 운영 전제(담당자 고정) 명시

버전 트랙(FormBuilder 2.x 등) 운영안은 이번에 **정식 도입하지 않는다** — 부록 A에 검토안으로만 남기고, 도입 여부는 팀 논의로 결정한다.

v3의 핵심 원칙은 그대로 유지한다: **하나의 기능 = 하나의 브랜치 = 같은 커밋(SHA)이 모든 목적지에 독립적으로 merge된다.** 사본 브랜치 금지, Squash 폐지, master = prod 진실.

---

## 0.5 개발자 일상 요약 (이것만 알면 됨)

1. **master에서 브랜치 하나 판다.** 복사본(`-dev`, `-test`, `-uat` 등) 절대 안 만든다.
2. **보내고 싶은 곳에 그 브랜치를 merge한다.**
   - FE 연동이 필요하면 → `dev` (선택)
   - QA 받을 준비가 되면 → `test` (여기서 본 리뷰 1회)
   - 배포가 확정되면 → `release-{날짜}` (UAT)
3. **prod에 내보낸다 = 그 feature 브랜치를 master에 merge한다.** 끝.

의무 merge는 **test / release / master 3회**, dev는 연동 필요할 때만. *(dev 선택화는 팀 논의 중 — 체크포인트 B2. 확정 전까지 현행대로 dev도 merge)*
추가 습관 하나: `git merge master`로 내 브랜치를 주기적으로 최신화 (8절).

---

## 1. 왜 v4인가 — v3 확정 이후 확인된 운영 현실

| # | 현실 | v3와의 간극 |
| --- | --- | --- |
| 1 | 모든 기능은 test(QA)와 uat를 **반드시** 거쳐야 한다 | v3의 test는 "dev 통합 soup 검증"이라 배포 후보/기능 단위 게이트 증적이 성립하지 않음 |
| 2 | **test까지만 머무는 기능**과 uat까지 가는 기능이 공존한다 | v3에는 배포 대기 개념이 없음. dev soup에 섞여 관리 불가 |
| 3 | 사실상 전부 수시배포 — 8월에만 release 3개(0807/0814/0819) 동시 오픈 | v3의 "월 1회", "release는 가능한 늦게, 하나만" 전제와 어긋남 |
| 4 | 열린 release가 여러 개인 상태에서 release push → uat 자동은 검증 중인 UAT를 덮어씀 | v3 12절 목표 트리거의 위험 |
| 5 | 버전 트랙(v2.11.0 → master 통째 merge #3111, release-2.13.0-0819)이 별도 관행으로 존재 | v3 금지 규칙 4(release→master 통째 금지)와 충돌 예정 |
| 6 | prod 트리거는 `pull_request closed`인데 v3는 "묶음은 push 1회로 배포 1회"를 가정 | 직접 push는 배포 0회, PR N개는 배포 N회 — 메커니즘 불일치 |

v2 → v3의 진단(사본 브랜치, SHA 불일치, dev 오염)과 처방(same-SHA, feature 단위 merge)은 여전히 유효하다. v4는 그 위에 팀 게이트 요건과 실제 배포 주기를 얹는다.

---

## 2. v4 핵심 원칙

> **하나의 기능 = 하나의 브랜치 = 동일한 커밋(SHA)이 dev / test / release / master에 각각 독립 merge된다.**
> **네 브랜치는 승격 파이프라인이 아니라 4개의 독립 목적지다. 어디까지 merge했나 = 그 기능의 현재 단계다.**

1. `master`는 Mainline이자 prod 진실. UAT 통과분만 들어온다.
2. feature는 **항상 master에서 분기**하고, 타겟별 사본을 만들지 않는다.
3. **모든 경로 Merge Commit** — Squash 금지.
4. `dev`, `test`는 휘발성 목적지(리셋됨), `release-{날짜}`는 사이클 1회용, `master`만 영구 기준선.
5. **master 반영은 언제나 feature 단위 merge.** test / release는 어디에도 merge하지 않는다.
6. **기능은 test에서 배포를 기다릴 수 있다.** 배포일이 미정이어도 test에 merge해 QA를 받고, 배포 확정 시점에 release로 나아간다. 대기 중인 기능은 다른 기능의 배포를 막지 않는다.

### 브랜치 = 칸반 보드

```
         dev   test   release-0812   master
featA     ✓     ✓         —            —      QA 중 / QA 통과·배포일 미정 (test 대기)
featB     ✓     ✓         ✓            ✓      이번에 나가는 것
featC     ✓     —         —            —      개발/연동 중
```

단계 확인: `git branch --contains <sha>` / `git log test --not master` (test 단계 이상 전부) / `git log release-0812 --not master` (UAT 후보)

---

## 3. 브랜치 구성 & 환경 매핑

| 브랜치 | 배포 환경 | 성격 | 수명 |
| --- | --- | --- | --- |
| `master` | **prod** (PR merge 자동) | Mainline·prod 진실. UAT 통과분만. 모든 feature의 시작점 | 영구 |
| `dev` | **dev** (push 자동) | FE/BE 연동·개발 확인 놀이터. **배포 파이프라인 밖** (merge 선택) | 휘발 — 정기 리셋 |
| `test` | **test** (dispatch) | **QA 게이트 + 배포 대기실.** QA 중이거나 배포일을 기다리는 기능이 머무는 곳. master + (QA 중 + 배포 미정) | 휘발 — 정기 리셋 + 재구성 |
| `release-{날짜}` | **uat** (dispatch) | 이번 배포 확정분만 모은 UAT 조립대. **어디에도 merge 안 함** | 사이클 1회용 — 생성→삭제 |
| `feature` | — | 기능 단위 개발. master에서 생성, **prod 반영 완료까지 유일한 원본** | prod 반영 후 삭제 |
| `hotfix/{설명}` | — | 운영 긴급 대응. master에서 생성 | 반영 후 삭제 |

**세 검증의 성격:**

- **dev 환경** = 개발 중 연동 확인 (비공식, 게이트 아님)
- **test 환경 (test 브랜치)** = **기능 단위 QA 게이트.** 기능이 test에 merge된 상태에서 QA 사인오프
- **uat 환경 (release-{날짜})** = **배포 후보 조합의 최종 게이트.** prod에 나갈 조합(master + 확정분)을 그대로 검증

> 감사 요건이 "배포 후보 **조합**이 test 환경도 거쳐야 한다"로 해석되는 경우: UAT 전 release-{날짜}를 test 환경에 1회 dispatch(후보 스모크)하고 종료 후 test 브랜치를 재배포한다. 기본 절차는 아니며 필요 시에만.

### 명명 규칙

```
feature : {날짜}/{이니셜}/{JIRA}-{기능명}
          예) 0812/YH/IMTDEV-1980-user-filtering
          (날짜 = 목표 배포일. 앞에 두면 git branch 목록이 날짜별로 묶임. 11절 참조)

release : release-{월일}             예) release-0812
          재구성 시) release-0812-v2

hotfix  : hotfix/{짧은설명}          예) hotfix/excel-sheet-name-length
```

**금지:** `{feature}-dev`, `{feature}-test`, `{feature}-master` 같은 타겟별 사본 명명. `release-{버전}-{날짜}` 같은 혼합 명명(예: release-2.13.0-0819)도 금지 — `release-` prefix는 UAT 조립대 전용이다. 버전 트랙 묶음이라면 다른 이름으로 정리한다 (부록 A 참고).

---

## 4. 머지 전략 & 리뷰 정책

| 경로 | 전략 | 비고 |
| --- | --- | --- |
| `feature → dev` | Merge Commit | 선택 (연동 필요 시). 충돌 시 로컬 직접 merge 허용 |
| `feature → test` | **Merge Commit (PR)** | **본 리뷰 1회는 여기서** |
| `feature → release-{날짜}` | Merge Commit (PR) | 조합 구성 확인 수준 |
| `feature → master` | Merge Commit (PR) | prod에 갈 유일한 경로. 확인 수준 |
| `master → dev / test / release` | Merge Commit (forward merge) | master 이동 시 기준선 갱신 |
| `hotfix → master` | Merge Commit (PR) | hotfix는 본 리뷰를 master PR에서 |

> `test → 어디로든`, `release → 어디로든` merge는 없다. 둘 다 목적지 전용이다.

### 4.1 통째 merge / FF 불허 (v3 유지)

release(또는 test)가 전체 그대로 나가는 것처럼 보여도 master에는 **항상 feature 단위로 merge한다.** 근거는 v3 4.1절과 동일:

1. 수시배포마다 master가 움직여 FF 전제가 거의 성립하지 않음
2. 모드가 둘이면 원칙이 흔들리고 선별 배포 근육이 퇴화함
3. **개별 merge + `git rerere` = 통째와 byte 동일 트리 + 선별성 유지**

**rerere 운영 전제 (v4 신설):** rerere 캐시는 클론-로컬이다(`.git/rr-cache/`, push/pull로 전파 안 됨). GitHub PR 화면의 merge 버튼(서버사이드)에도 적용되지 않는다. 따라서:

- **충돌 해결은 항상 로컬에서** — 로컬에서 풀고 push한 뒤 PR merge
- **release 조립에서 충돌을 푼 사람이 master 반영도 수행** (같은 클론 = 같은 캐시). 담당자가 갈리면 재해결이 필요하고, 다르게 풀리면 아래 diff 검증에서 드러난다

**배포 직전 정합성 확인:**

```shell
git diff release-0812 master
# 비어 있음    → release 전체가 그대로 prod 반영 (통째와 동일함을 증명)
# 제외분만 보임 → 의도적 제외 (실수 아님이 눈으로 확인)
# 그 외가 보임 → 충돌이 다르게 풀렸거나 forward merge 누락 — 원인 확인 후 배포
```

이 판독은 "master 이동 시 release에 forward merge" 규율이 지켜졌을 때 깨끗하게 나온다. diff에 무관한 hotfix가 섞여 보이면 forward merge부터.

### 리뷰 정책

같은 커밋이 네 곳을 흐르므로 리뷰는 한 번이면 된다. **QA 받기 직전이 리뷰 받기 가장 좋은 시점**이므로 본 리뷰를 test PR로 둔다.

| PR | 리뷰 강도 |
| --- | --- |
| feature → dev | 생략 가능 (연동 fast-lane) |
| feature → **test** | **본 리뷰 1회 (Files Changed 탭)** |
| feature → release-{날짜} | 확인 수준 (조합 구성이 맞는지) |
| feature → master | 확인 수준 (이미 리뷰·QA·UAT 통과한 동일 커밋) |
| hotfix → master | 본 리뷰 (test를 거치지 않으므로) |

---

## 5. 개발 및 배포 워크플로우

```
master ─── feat/A 생성 (항상 master에서)
              │
  (선택)     ├──→ dev ──────────→ [dev 환경] 자동 (FE/BE 연동)
              │
   ① merge   ├──→ test ─────────→ [test 환경] dispatch (QA 게이트 — 대기 가능)
              │
   ② merge   ├──→ release-0812 ─→ [uat 환경] dispatch (배포 확정분만)
              │
   ③ merge   └──→ master ───────→ [prod 환경] 자동
```

### 1단계: 개발 & 연동 (feature → dev, 선택)

1. `master`에서 feature 브랜치 생성
2. FE 연동이 필요하면 `dev`에 Merge Commit → dev 환경 자동배포
3. 추가 수정은 같은 feature 브랜치에서 커밋 후 재병합
4. 충돌 시: 로컬에서 dev checkout → feature merge → 해결 → push (dev는 휘발성이라 직접 merge 허용). **feature에 dev를 merge하는 역방향은 금지**

### 2단계: QA (feature → test)

1. QA 받을 준비가 되면 **test에 PR로 Merge Commit** — 여기서 본 리뷰 1회
2. test 환경은 **dispatch로 배포** (소스: test 브랜치) → QA가 원하는 시점에 검증 시작. 검증 중에 다른 기능이 test에 merge돼도 환경이 덮어써지지 않는다
3. QA 지적 수정은 같은 feature 브랜치에서 커밋 → test 재병합 (2번째 merge부터 새 커밋만 diff에 보임)
4. **QA 통과 후 배포일이 미정이면 그대로 test에 남아 대기한다.** 대기 중에도 master가 움직이면 feature를 `git merge master`로 최신화하고 test에 재병합 (8절)

### 3단계: UAT 후보 구성 & 검증 (feature → release-{날짜})

1. 배포일 확정 시 `master`에서 `release-{날짜}` 생성
2. **배포 확정된 feature만** Merge Commit으로 병합 (test 대기분은 그대로 남긴다)
3. uat dispatch로 배포. UAT는 단일 환경 — **한 번에 하나의 release만** 점유 (열린 release가 여러 개여도 배포는 dispatch로 통제)
4. `git log release-0812 --not master --oneline`으로 "UAT에 뭐가 올라가 있나" 확인
5. 버그 발견 시: 같은 feature 브랜치에서 수정 → **test, release에 각각 재병합** (같은 커밋. dev는 선택)

### 4단계: 운영 배포 (feature → master)

- **prod에 갈 것은 언제나 feature 브랜치를 master에 merge.** release는 master로 merge하지 않는다
- **선별**: 배포 확정 feature만 master merge. 나머지는 release에 남기거나(계속 수정) test로 되돌린다(배포 연기)
- **묶음**: 조립 브랜치에 feature merge들을 모아 **master로 PR 1건** (Create a merge commit) → prod 배포 정확히 1회. feature마다 PR을 따로 merge하면 배포가 N회 돈다 (concurrency group이 완충하지만 조립 PR이 기본)
- master merge → prod 자동배포 (**master에 dispatch 안 붙임** — UAT가 진짜 게이트, 검증한 소스 그대로 prod)
- 배포 후 정합성 확인: `git diff release-0812 master` (4.1절)

### 4.5단계: 선별 '제외' 시 재검증 (v4 신설)

UAT를 A+B로 통과했는데 B를 제외하고 A만 내보내면, prod 조합(master+A)은 **UAT에서 검증된 적이 없다** (UAT는 master+A+B를 봤다).

- **기본 규칙: 제외 발생 시 `release-{날짜}-v2`로 재조립(A만) → UAT 재검증 → 배포**
- 예외: A와 B가 명백히 독립(도메인·파일 겹침 없음, 기능 간 호출 없음)이면 팀 판단으로 재검증 생략 가능 — 생략 사유를 배포 기록에 남긴다

### 5단계: 배포 후 정리

`master`를 열려 있는 test / release / dev에 forward merge → 사이클 끝난 release-{날짜} 삭제 → prod 반영된 feature 브랜치 삭제 → (주기 도래 시) dev·test 리셋 (7절).

### fast path: 단건 수시 배포 (v4 신설)

기능 1개짜리 긴급 건은 release 조립을 생략할 수 있다:

1. feature 브랜치를 master 최신 상태로 유지 (`git merge master` — **master가 ancestor인지 확인**)
2. test 환경 QA는 test 브랜치 merge로 (동일), **uat는 feature 브랜치에서 직접 dispatch** (feature = master + 이 기능이므로 release와 동일한 트리)
3. UAT 통과 → feature를 master에 PR merge → prod

hotfix는 여기서 test 단계까지 축약 가능하되(긴급도 판단), 본 리뷰를 master PR에서 수행하고 가능하면 uat 스모크는 거친다.

---

## 6. 시나리오

전제: featA·featB·featC 진행 중.

### 시나리오 1. 단일 배포 (일상)

C 하나만 나감 → test에서 QA 통과 → `release-{날짜}`에 C만 merge → UAT(= master+C) → C를 master merge → prod. release 삭제. (급하면 fast path로 release 생략 가능)

### 시나리오 2. 대기와 추월 (v4 신설 — 가장 흔한 패턴)

A는 QA 통과했지만 배포일 미정(test 대기), B는 배포 확정.

1. B: test QA 통과 → `release-0812`에 **B만** merge → UAT → master → prod
2. A는 test에서 그대로 대기 — B의 배포에 아무 영향 없음
3. B 배포로 master 이동 → **test·dev에 forward merge** + A 브랜치도 `git merge master` 최신화
4. A 배포 확정 시 그 시점의 `release-{날짜}`에 merge → UAT → prod

### 시나리오 3. 묶음 배포

배포일이 같은 A·B를 `release-{날짜}`에 함께 merge → UAT에서 **합쳐진 조합 검증** (test에서 각각 QA 통과했어도 A+B 조합 검증은 여기가 처음).

- 둘 다 통과: 조립 브랜치 → master PR 1건 (배포 1회). `git diff release-{날짜} master` 확인
- B만 문제: **4.5절 적용** — B 제외 후 `release-{날짜}-v2`(A만) 재조립 → UAT 재검증 → A 배포. B는 release에서 빠져 test로 복귀(수정 계속)

### 시나리오 4. C만 prod 선행 (수시배포)

A·B는 다음 release 예정, C는 폼 등의 사유로 먼저.

1. `release-{날짜}` = **C만** → UAT 단독 검증 (dev/test에서 A·B와 섞여 돌았던 것은 master+C 조합의 검증이 아님)
2. C를 master merge → prod. C는 master 기반이라 A·B가 딸려 들어갈 수 없음
3. master 이동 → 열려 있는 release·test·dev forward merge, A·B 브랜치 최신화 후 재검증

**한계 (전략 무관의 본질적 제약):** C가 A·B 코드에 의존하면 단독 배포 불가 — 의존 기능을 먼저/함께 내보낸다.

---

## 7. 브랜치 수명주기 & 리셋

### release-{날짜} — 사이클 1회용 (v3 유지)

생성 → UAT → 배포 → **삭제**. 다음 release로 흡수(fold-in) 금지. 미배포 기능은 원본 브랜치로 새 release에 재병합.

### test — 휘발성 + 배포 대기실 (v4 신설)

test에만 있는 커밋 = ① feature 병합 사본(원본 생존) ② 배포 대기 중인 기능 ③ 충돌 해결·노이즈. 리셋 시 대기 기능을 재구성한다:

```shell
# [리셋 절차 — dev와 동일하되 재구성 목록 선행]

# 0. 재구성 목록: 지금 test에 살아 있는(미배포) 기능 확인
git log origin/test --not origin/master --merges --oneline
#    → 대기 기능 목록 기록

# 1. 안전망
git tag test-archive-$(date +%Y%m%d) origin/test && git push origin test-archive-$(date +%Y%m%d)

# 2. orphan 검출 (test 직접 커밋은 금지지만 확인)
git log origin/test --not origin/master --no-merges --oneline
#    → 어느 feature에도 없는 커밋은 feature로 회수

# 3. 리셋 + 재구성
git checkout test && git reset --hard origin/master
git push origin test --force-with-lease
git merge --no-ff <체류 feature 1> && git merge --no-ff <체류 feature 2> ...
git push origin test
```

**주기:** 배포 사이클 종료 시점 기준으로 판단 — 대기 기능이 적으면 forward merge로 충분, **대기·노이즈가 쌓이면(예: 대기 5개 초과 또는 월 1회) 리셋+재구성.**

### dev — 휘발성 (v3 유지, 부담 완화)

파이프라인 밖이므로 리셋은 편의 문제. 분기 1회~월 1회 수준으로 v3 7절 절차(archive tag → orphan 회수 → reset → 재병합) 그대로.

---

## 8. 충돌 관리 (v3 유지 + rerere 전제 추가)

### 핵심 습관: master를 feature로 정기적으로 당겨온다

```shell
git checkout feat/A
git merge master        # master가 움직일 때마다
```

feature에서 미리 한 번 풀면 이후 dev·test·release·master merge가 전부 깨끗해진다. **test에서 오래 대기하는 기능일수록 이 습관이 중요하다** (오래 살수록 드리프트가 크다).

### 절대 규칙: 당겨오는 건 master만

```
git merge master    ✅ OK
git merge dev       ❌ 금지 — 미배포 기능이 섞여 master까지 딸려 들어감
git merge test      ❌ 금지
git merge release   ❌ 금지
```

### git rerere

```shell
git config --global rerere.enabled true
```

- 한 번 푼 충돌을 기억해 자동 재적용 — 같은 feature를 4곳에 반복 merge하는 이 모델의 필수 장비
- **캐시는 클론-로컬. release 조립 충돌을 푼 사람이 master 반영까지 수행한다** (4.1절)
- 충돌 해결은 항상 로컬에서 (GitHub UI merge에는 rerere가 적용되지 않음)
- 잘못 푼 기록은 `git rerere forget <파일>` 후 재해결

### 근본 처방 (충돌이 만성이면)

feature 잘게 쪼개 자주 merge / inert·flag로 미리 merge / 같은 파일 만지는 사람끼리 merge 순서 조율.

---

## 9. Hotfix / 수시배포

### Hotfix

1. `master`에서 `hotfix/{설명}` 생성 → 수정 → **master PR (본 리뷰 여기서)** → prod
2. 가능하면 uat 스모크(fast path — feature 브랜치에서 dispatch)를 거친다. 긴급도에 따라 팀 판단
3. 사후처리: master를 test / release / dev에 forward merge. **타겟별 cherry-pick 사본 금지**

### 수시배포 (폼 선배포 등)

시나리오 4 또는 fast path. feature 자체가 배포 단위이므로 별도 expedite 브랜치 불필요.

---

## 10. 금지 규칙

1. 타겟별 사본 브랜치(`-dev`, `-test`, `-master`, `-release`) 생성 금지
2. feature 브랜치에 dev / test / release를 merge 금지 (역방향 오염)
3. release-{날짜}를 다음 release로 흡수(fold-in) 금지
4. **test / release를 어디로든 merge 금지** — 둘 다 목적지 전용. master는 항상 feature 단위로만 받는다 (통째 FF 포함 금지, 4.1절)
5. dev / test / release에서 feature 분기 금지 — **항상 master**
6. 이미 공유된 브랜치 rebase 금지 — `git merge master`로 대신
7. dev / test에 직접 커밋 금지 — 모든 코드는 feature 브랜치 경유 (리셋 시 소실 방지)
8. master에 UAT 미통과분 merge 금지 — master는 항상 prod와 같아야 hotfix 기준선이 유효
9. UAT 조합에서 기능을 제외하고 배포할 때 재검증 생략 금지 (독립성 예외는 사유 기록, 4.5절)

---

## 11. 날짜 컨벤션 (v3 유지)

```
{날짜}/{이니셜}/{JIRA}-{기능}     예) 0812/YH/IMTDEV-3660-adjudicator-custom
git branch --list "0812/*"        # 8/12 배포 후보 전부
```

- 날짜 = **목표** (밀리면 `git branch -m`으로 rename, 또는 감수). 배포일 미정 기능(test 대기형)도 잠정 목표일을 넣어 date-first를 유지한다
- 실제 배포 기록의 진실은 master merge 커밋 + 버전 태그 + PR 라벨(`deploy:0812`)
- UAT 실제 내용물의 진실은 `git log release-0812 --not master`

---

## 12. CI/CD 트리거 구성 (목표 상태 — yml 적용은 별도 작업)

**현행 (2026-08-05 실측):** dev push 자동 / test `workflow_dispatch`(브랜치 선택) / uat `workflow_dispatch` / prod `pull_request closed(merged)` 자동. 이미지 태그는 환경명 고정, concurrency 없음.

| 환경 | 목표 트리거 | 현행 대비 |
| --- | --- | --- |
| dev | `dev` push 자동 | 동일 |
| test | `workflow_dispatch` (소스: **test 브랜치**) | 동일 유지 — 배포 소스만 test 브랜치로 고정. QA가 검증 시작 시점을 통제 |
| uat | `workflow_dispatch` (소스: release-{날짜} 또는 fast path feature) | 동일 유지 — **v3의 "release push 자동" 목표 폐기** (다중 release 시 UAT 덮어쓰기 위험) |
| prod | `pull_request closed(merged)` 자동 유지 + **concurrency group** | 🔄 concurrency 추가. 묶음은 조립 PR 1건이 기본 |

필수 (보조 권장에서 승격):

- **이미지 태그: 버전/SHA 병행** (`ti-server:v1.216.0`, `ti-server:sha-8f0ae97`) — "UAT에서 검증한 이미지 = prod 이미지" 증명. 임상 플랫폼 감사 요건
- **prod concurrency group** (`cancel-in-progress: false`) — 연속 merge 시 Blue-Green 중첩 방지 (이전 인스턴스 미종료 이슈와 직결)

보조 권장:

- 배포 공지(Teams): 버전 + 커밋 SHA + 포함 Jira
- uat dispatch에 "master ancestor 확인" 가드 (fast path용: 낡은 feature 브랜치 배포 방지)
- semantic-release: 현행 유지. `chore(release)` 커밋이 forward merge로 test/release/dev에 흐르는 것은 정상

---

## 13. v3 → v4 전환 체크리스트 (1회성)

현재 상태 실측 (2026-08-05): release-0729 삭제 완료 ✓ / release-{날짜} 네이밍 정착 ✓ / test dispatch 전환 완료 ✓(#3106) / **dev 605커밋 divergence 미해소 / 열린 release 3개(0807, 0814, 2.13.0-0819) / 이미지 태그·concurrency 미적용**

1. **`test` 브랜치 신설** — 그 시점 master에서 생성 → 현재 QA 진행·대기 중인 기능들을 재병합(재구성 목록은 릴리스 운영자가 작성). `test-build-deploy.yml`은 현행 dispatch 유지, 배포 소스로 test 브랜치 선택 안내만
2. **dev 1회 리셋** — 605커밋 divergence 해소. orphan 검출 선행 (v3 7절 절차)
3. **열린 release 정리** — 0807/0814는 현행 사이클대로 마감 후 삭제. `release-2.13.0-0819`는 성격 확인 — 버전 트랙 묶음이면 `release-` prefix를 피해 rename 권장, 운영 방식은 부록 A 논의로
4. **GitHub 설정** — test/release/master 대상 PR Merge Commit 허용 확인, `git rerere` 전 팀원 활성화 안내, PR 라벨(`deploy:{날짜}`) 도입
5. **CI/CD yml 변경** (12절) — prod concurrency, 이미지 태그 버전/SHA. 별도 합의 후 적용 (test/uat 트리거는 현행 dispatch 그대로)
6. **팀 공유 및 적용 시작일 지정** — 특히 두 가지 습관 변화 공지: 본 리뷰가 test PR로 이동, 충돌 해결은 항상 로컬(rerere)

---

## 14. 컨벤션 요약

| 상황 | 규칙 |
| --- | --- |
| feature 생성 | 항상 `master`에서 |
| feature 개수 | 기능당 **1개** — 사본 금지 |
| 브랜치 명명 | `{날짜}/{이니셜}/{JIRA}-{기능}` (date-first 고정 — 미정이면 잠정 목표일) |
| 의무 merge | **3회** (test / release-{날짜} / master) + dev는 선택 *(선택화는 팀 논의 중 — B2)* |
| 모든 merge | Merge Commit (Squash 금지) |
| 본 리뷰 | **test PR** (Files Changed 탭) — dev는 fast-lane, release/master는 확인 |
| QA 게이트 | test 브랜치 merge + test 환경 검증. **배포 미정이면 test 대기 가능** |
| 최종 게이트 | release-{날짜} UAT — 배포 확정분만, dispatch 배포 |
| master 반영 | 항상 feature/epic 단위 (통째 merge/FF 금지 — 4.1) |
| 묶음 배포 | 조립 브랜치 → master PR 1건 = 배포 1회 |
| 배포 제외 | release 재조립(-v2) + UAT 재검증이 기본 (독립성 예외는 사유 기록) |
| 배포 정합성 | `git diff release-{날짜} master` — 비면 전체 반영, 제외분만 보이면 정상 |
| 충돌 예방 | `git merge master` 정기 최신화 + rerere (해결은 항상 로컬, 담당자 고정) |
| hotfix/수시 후 | master → test / release / dev forward merge |
| dev / test | 휘발성 — 정기 리셋 (test는 대기 기능 재구성 포함) |
| release-{날짜} | 사이클 1회용 — 어디에도 merge 안 함, 배포 후 삭제 |
| 버전 트랙(대형 묶음) | 정식 규칙 아님 — 부록 A (도입 검토) |
| feature 삭제 | prod 반영 완료 후 |

---

## 부록 A. Epic(버전 트랙) 브랜치 운영안 — 도입 검토용, 정식 규칙 아님

> **배경:** FormBuilder 2.x처럼 여러 하위 작업이 하나의 버전으로 묶여 수 주간 개발되는 트랙이 실제로 존재한다 (v2.11.0 → master 통째 merge #3111, 2.12.0 트랙, release-2.13.0-0819). 이 관행은 금지 규칙 4("master는 feature 단위로만")와 형식상 충돌한다. 도입한다면 아래처럼 "epic 브랜치 = 큰 feature"로 정의해 v4 틀 안으로 흡수할 수 있다. **도입 여부는 팀 논의로 결정하며, 결정 전까지 버전 트랙은 현행 방식을 유지한다.** 단, `release-` prefix와의 이름 혼동만은 피한다 (release-{날짜}는 UAT 조립대 전용 — merge 금지 대상이라 정반대 규칙이 적용되므로).

### 운영안

핵심: **epic 브랜치 = 큰 feature 브랜치.** 본문 규칙이 전부 동일하게 적용된다.

1. `epic/{트랙이름}`을 **master에서 분기** (예: `epic/formbuilder-2.13.0`)
2. 하위 작업은 epic에서 분기해 **epic으로 PR** (하위 작업 리뷰는 여기서)
3. epic 브랜치가 배포 단위: **epic을 dev / test / release / master에 same-SHA로 merge** — feature와 완전히 동일
4. epic도 `git merge master`로 정기 최신화 (오래 살수록 필수)
5. 타겟별 사본(`epic/...-dev` 등) 금지, epic에 dev/test/release를 merge하는 역방향 금지

### 이 정의가 해소하는 것

"v2.11.0 브랜치를 master에 통째 merge"는 epic을 배포 단위 feature로 취급하면 금지 규칙 위반이 아니다 — epic 브랜치 자체가 그 배포의 feature이기 때문이다. 금지 규칙 4가 겨냥하는 것은 release-{날짜}(UAT 조립대)를 master에 붓는 것이지, 배포 단위가 원래 큰 작업이 아니다.

### 미도입 시 유의점

- 버전 트랙 브랜치가 master 통째 merge로 나가는 순간 `git log <트랙> --not master` 추적과 선별 배포가 그 트랙에는 적용되지 않음을 인지하고 운영할 것
- 트랙 브랜치도 최소한 "master에서 분기 + 사본 금지 + 역방향 merge 금지"는 지키는 것을 권장 (same-SHA 원칙 유지)

---

## 부록 B. Claude Code 배포 플로우 도구 (CLI) 사용법 — 실험/도입 검토

> **상태: 초안 (2026-08-13).** CLI Claude Code를 쓰는 팀원이 v4를 쉽게 지키도록 만든 보조 도구. 핵심 3개(`/imt-feature`·`/imt-test`·`/imt-deploy`)만 우선 스캐폴딩했고 미검증이다. v4 자체가 초안이므로 이 도구도 도입 검토 단계.

### 무엇인가

v4 규칙을 Claude Code의 **skill(지식)** 과 **slash command(동작)** 으로 옮긴 것. `image-trial-server` 레포 `.claude/`에 체크인되어 **clone하면 바로 사용**(설치 불필요). 지금은 이 BE 레포 단독이며, 여러 레포로 확장 시 plugin + 사내 marketplace로 승격을 검토한다.

- **skill = 지식(자동)**: 브랜치/merge/배포 얘기가 나오면 Claude가 알아서 v4 규칙을 참고한다.
- **command = 동작(명시 실행)**: 위험한 절차는 `/명령`으로 콕 찍어야 돈다.

### 구성

> `/`로 시작하는 것은 **당신이 직접 입력해 실행하는 명령**입니다. `/`가 없는 `imt-git-flow`는 실행하는 게 아니라, 브랜치·merge·배포 얘기를 하면 **Claude가 자동으로 끌어와 참고하는 v4 규칙 모음**입니다 (아무것도 안 쳐도 배경에서 동작 — 커맨드를 안 써도 대화가 v4를 지키게 만든다).

| 이름 | 역할 |
| --- | --- |
| `imt-git-flow` | v4 규칙 모음 — 브랜치/배포 작업 시 Claude가 자동 참고 (직접 실행하는 게 아님) |
| `/imt-feature` | master에서 feature 분기 + 네이밍 검증 + rerere on |
| `/imt-test` | 현재 feature → test PR(본 리뷰) → test dispatch |
| `/imt-deploy` | 조립 PR → master(1건=배포1회) + 정합성 diff, 최종 merge 승인 후 |
| `/imt-release`·`/imt-sync`·`/imt-status` | 설계 완료, 구현 예정 |

### 사용법

**1) 새 기능 시작 — `/imt-feature <JIRA> <기능명> [MMDD] [이니셜]`**

- master 최신에서 `{MMDD}/{이니셜}/{JIRA}-{기능명}` 브랜치를 만든다.
- 예: `/imt-feature IMTDEV-1980 user-filtering 0812` → `0812/YH/IMTDEV-1980-user-filtering`
- 네이밍이 규칙에 안 맞거나 분기원이 master가 아니면 만들지 않고 교정안을 준다.

**2) QA 올리기 — `/imt-test`** (feature 브랜치에서 실행)

- feature를 `test`로 **PR(본 리뷰 지점)** 생성 → 승인 후 `gh workflow run test-build-deploy.yml --ref test`로 test 환경 dispatch.
- test 브랜치가 없으면 신설 여부를 먼저 물어본다(13절 전환 스텝).

**3) prod 배포 — `/imt-deploy [release-{날짜}]`**

- 배포 확정분을 조립해 **master로 PR 1건**(= 배포 1회) 생성.
- `git diff release-{날짜} master`로 정합성을 판독하고, 일부 제외면 4.5 재검증을 경고.
- **최종 merge는 절대 자동으로 하지 않는다** — PR 링크·포함 목록·diff 판독 결과를 보여주고 사용자가 승인해야 merge.

### 안전 원칙

- 위험 동작(배포/dispatch/최종 merge)은 **항상 사용자 승인 후** 실행.
- shared 브랜치 반영은 직접 push가 아니라 **PR**.
- **이 도구는 Claude를 통할 때만 유효하다** — 터미널 직접 `git push`나 GitHub UI Squash 버튼은 못 막는다. **진짜 강제는 GitHub 설정**(Squash/Rebase merge 비활성, branch protection, required PR). 이 도구는 "올바른 길을 쉬운 길로" 만드는 생산성 계층이다.

### 상태 / 남은 일

- 완료: `test` 브랜치 신설(13절 1번), 핵심 3개 커맨드 + skill 스캐폴딩.
- 예정: `/imt-release`·`/imt-sync`·`/imt-status` 구현, 실사용 검증, (다중 레포 시) plugin 패키징.

---

참고 문서:

- [IMT Git Branch 전략 v3](https://trialinformatics.atlassian.net/wiki/spaces/ImageTrial/pages/686096423) (2026-07-14) — 3브랜치 모델, v2→v3 진단
- [IMT Git Branch 전략 v2](https://trialinformatics.atlassian.net/wiki/spaces/ImageTrial/pages/580157473) (2026-04-30)
- `claude-help/workflow/GIT_WORKFLOW_RULES.md` — Release ID/manifest 체계 제안 (이미지 태그·Teams 공지로 경량 채택)

작성일: 2026-08-05 (v3 원문 검토 + 2026-08-05 repo 실측 + 팀 요건 논의 기반)
