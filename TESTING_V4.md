# v4 전략 검증 시나리오 (샌드박스)

이 레포는 **IMT Git Branch 전략 v4를 실제로 돌려보기 위한 샌드박스**다. 운영 코드가 없으므로
브랜치를 망가뜨려도 되고, 더미 워크플로가 실제 배포 대신 build + 태그 출력만 한다.

- 규칙 원본(단일 진실): [Confluence — IMT Git Branch 전략 v4](https://trialinformatics.atlassian.net/wiki/spaces/ImageTrial/pages/716800002/IMT+Git+Branch+v4) — 확정 2026-08-14. **로컬 사본은 두지 않는다**

## 환경 구성 (완료된 것)

| 항목 | 상태 |
| --- | --- |
| mainline | `master` (main에서 rename, GitHub default도 master) |
| 목적지 브랜치 | `dev`, `test` (master에서 신설) |
| GitHub merge 설정 | Merge commit **허용** / Squash·Rebase **비활성** (금지 규칙 8 강제) |
| auto-delete branch on merge | **off** — 켜면 test PR merge 때 feature가 삭제돼 same-SHA 모델이 깨진다 |
| `rerere` | 전역 활성 |
| CI 워크플로 | dev push 자동 / test dispatch / uat dispatch / prod PR-closed + concurrency (전부 더미) |
| Claude 도구 | skill `imt-git-flow` + 커맨드 6개 (`.claude/`, 체크인됨) |

## 충돌 실험 장치

- **`FeatureRegistry.register()`** — 모든 feature가 여기에 한 줄 추가 → 두 기능이 동시에 진행되면 **반드시 충돌**.
  rerere가 같은 충돌을 test/release/master 3~4곳에 자동 재적용하는지 검증하는 지점.
- **`src/main/java/com/ti/feature/*.java`** — 파일만 추가하면 충돌 없음. "명백히 독립" 케이스(4.5절 예외) 검증용.

---

## 시나리오 체크리스트

### S0. 기본 왕복 (1개 기능이 4곳을 흐른다)
- [ ] `/imt-feature IMTDEV-0001 hello-feature 0814` → `0814/YH/IMTDEV-0001-hello-feature` 생성, 분기원이 `origin/master`인지 확인
- [ ] 커밋 후 `/imt-test` → **test PR 생성**(본 리뷰 지점), merge 후 `test-build-deploy.yml` dispatch
- [ ] `/imt-release 0814` → `release-0814` 조립 → uat dispatch. **uat 워크플로의 master-ancestor 가드가 통과**하는지 확인
- [ ] `/imt-deploy release-0814` → master PR 1건 → **prod 워크플로가 정확히 1회** 돌고, "부모 2개 = merge commit" 로그가 찍히는지 확인
- [ ] **같은 SHA인지 검증**: `git log <feature> -1 --format=%H` 가 dev/test/release/master 전부에 있는지 `git branch -r --contains <sha>`

### S1. 시나리오 2 — 대기와 추월 (가장 흔한 패턴)
- [ ] featA, featB 둘 다 test에 merge (둘 다 `FeatureRegistry`를 건드려 **충돌을 유발**)
- [ ] featB만 `release-{날짜}`에 merge → UAT → master → prod. **featA는 test에 그대로 대기**
- [ ] `/imt-status` 로 A=`test O / release -`, B=`전부 O` 로 보이는지 확인
- [ ] `/imt-sync` 모드 B로 master를 test/dev에 forward merge + A 브랜치 최신화(모드 A)
- [ ] **A가 test에 대기하는 동안 B 배포가 막히지 않았는지** — 이게 v4가 v3에서 고치려던 핵심

### S2. 시나리오 3 — 묶음 배포 (조립 PR 1건 = 배포 1회)
- [ ] A·B를 같은 `release-{날짜}`에 merge → UAT에서 **조합** 검증
- [ ] 조립 브랜치(`assemble/{날짜}`)에 A·B를 merge → **master PR 1건**
- [ ] prod 워크플로 실행 횟수가 **정확히 1회**인지: `gh run list --workflow=prod-build-deploy.yml`
- [ ] 대조 실험: feature별 PR 2건을 각각 merge하면 **배포가 2회** 도는지 확인 (concurrency가 줄만 세우고 횟수는 줄이지 않음)
- [ ] `git diff release-{날짜} master` → **비어 있어야** 정상

### S3. 4.5절 — 배포에서 제외 (재검증 규칙)
- [ ] A+B로 UAT 통과 후 B를 제외하고 A만 배포 시도 → `/imt-deploy`가 **재검증 경고로 중단**하는지
- [ ] `release-{날짜}-v2`(A만) 재조립 → uat 재검증 → 배포
- [ ] `git diff release-{날짜} master` 가 **제외분(B)만** 보이는지 (= 의도적 제외임을 눈으로 확인)

### S4. 충돌 & rerere
- [ ] A·B가 `FeatureRegistry` 같은 줄을 건드리게 만든 뒤, A를 test에 merge → 충돌 → **로컬에서 해결**
- [ ] 같은 A를 release에, 그다음 master에 merge → **rerere가 같은 해결을 자동 재적용**하는지 (`Resolved ... using previous resolution`)
- [ ] `.git/rr-cache/` 가 생겼는지 확인 → **push/pull로 전파되지 않음**을 확인 (그래서 담당자 고정 규칙이 있다)
- [ ] GitHub UI에서 충돌 PR을 merge 시도 → rerere가 **안 먹는 것**을 확인

### S5. 금지 규칙이 실제로 막히는지
- [ ] `test` → feature 역방향 merge 요청 → Claude가 **거부**하는지 (금지 2)
- [ ] `release-{날짜}`를 master에 통째 merge 요청 → **거부**하는지 (금지 4)
- [ ] `dev`에서 feature 분기 요청 → **거부**하고 master 분기로 교정하는지 (금지 5)
- [ ] `0814/YH/...-dev` 같은 사본 명명 요청 → **거부**하는지 (금지 1)
- [ ] `release-2.13.0-0819` 같은 혼합 명명 요청 → **거부**하고 `epic/` 안내하는지
- [ ] GitHub PR 화면에 **Squash 버튼이 없는지** (설정으로 강제됨 — 도구가 아니라 이게 최후 방어선)

### S6. hotfix (test 우회)
- [ ] `hotfix/{설명}`을 master에서 생성 → master PR(**본 리뷰 여기서**) → prod
- [ ] 사후 `/imt-sync` B로 master를 test/release/dev에 forward merge
- [ ] forward merge를 **일부러 빼먹고** `git diff release-{날짜} master` 를 보면 **무관한 hotfix가 섞여 보이는지** (4.1절 판독 실패 상황 재현)

### S7. 7절 — test 리셋 + 재구성
- [ ] 대기 기능을 3~4개 쌓은 뒤 리셋 절차 실행: archive tag → orphan 검출 → `reset --hard origin/master` → 대기분 재병합
- [ ] 리셋 후 대기 기능들의 **SHA가 그대로인지** (원본 feature에서 재병합했으므로 같아야 한다)
- [ ] `test`에 직접 커밋을 하나 만든 뒤 리셋 → **그 커밋이 소실되는 것**을 확인 (금지 7의 근거)

---

## 검증 시 자주 쓰는 명령

```bash
# 이 기능이 어디까지 갔나
git branch -r --contains <sha>
git merge-base --is-ancestor <feature> origin/test && echo "test에 있음"

# 각 목적지에 master 대비 무엇이 들어있나
git log origin/test --not origin/master --merges --oneline
git log release-0814 --not origin/master --merges --oneline

# 배포 정합성 (4.1절)
git diff release-0814 master     # 비었음=전체반영 / 제외분만=의도적 / 그외=충돌 다르게 풀림·forward merge 누락

# 워크플로 실행 이력 (배포 횟수 검증)
gh run list --workflow=prod-build-deploy.yml -L 10
```

## 기록할 것

시나리오를 돌리거나 문서를 다시 읽다가 나온 **v4 문서가 틀렸거나 애매한 지점**을 여기 남긴다
(문서가 초안이므로 이게 이 샌드박스의 산출물이다). Confluence 개정 시 근거로 쓴다.

| # | 출처 | 관찰 | 결정 (2026-08-14) |
| --- | --- | --- | --- |
| F1 | 4.1절 · 5단계 · 시나리오 3 | 조립 브랜치와 `release-{날짜}`가 내용이 같은 중복 브랜치 | **조립 브랜치 유지** — 배포가 1회만 돌아야 하므로 PR 1건용 브랜치가 필요. 중복은 수용 |
| F2 | 3절 명명 규칙 | 조립 브랜치 이름이 문서에 정의된 적 없음 (`assemble/`는 커맨드 파일에만) | **3절에 `assemble/{월일}` 신설** (F1 유지에 따라 필수가 됨) |
| F3 | 금지 규칙 7 · 7절 | 직접 커밋 금지·orphan 검출에서 `release-{날짜}`가 빠짐 | **금지 규칙 7에 `release-{날짜}` 추가** + 4.1절 배포 전 체크에 orphan 검출 1줄 추가 |
| F4 | 4.1절 근거 ①②③ | "통째 merge 금지"의 근거 3개가 결론을 다 못 받침 | **4.1절 근거 재작성** — ① orphan 격리 ② 기본값 안전성. FF 논거는 FF에만 한정, rerere는 "장치"로 위치 변경 |

> **반영 완료 (2026-08-14):** 위 4건 + 초안 보류 2건(B2 dev merge 선택 / 부록 A epic 미도입)이
> [Confluence v4 확정본](https://trialinformatics.atlassian.net/wiki/spaces/ImageTrial/pages/716800002)(version 6)과
> `docs/v4-flow.html`에 반영됐다. **v4는 더 이상 초안이 아니다.**
> 아래 상세는 결정 근거로 보존한다 — F1의 "조립 브랜치 폐지" 제안은 **채택되지 않았다**(배포 1회 보장이 우선).

---

### F1. 조립 브랜치 = `release-{날짜}` 내용 중복 (2026-08-14)

**관찰.** 5단계 묶음 배포와 시나리오 3은 조립 브랜치를 새로 만들어 feature들을 다시 merge하라고 한다.
그런데 그 내용물은 `release-{날짜}`와 같다(둘 다 `master + 배포 확정 feature들`). 결과적으로 **트리가 동일한
브랜치 두 개가 정반대 규칙**을 단다 — `release-`는 merge 금지(금지 규칙 4), 조립 브랜치는 merge가 존재 이유.

**실측.** 임시 레포에서 충돌하는 feature 2개를 만들고 두 경로를 비교했다:

- 경로 1 — `release-0814`를 master에 통째 merge → 트리 `364543296d97698d4adbc583a6e1c363daec82bc`
- 경로 2 — master에서 새로 조립해 featA·featB 재merge → 트리 `364543296d97698d4adbc583a6e1c363daec82bc`

**트리 SHA 동일.** 충돌이 있어도 rerere가 `Resolved 'reg.txt' using previous resolution`으로 해결을
재적용한다. merge 커밋 SHA만 다르고 내용물은 byte 단위로 같다. 즉 4.1절 근거 ③("개별 merge + rerere =
통째와 byte 동일 트리")은 **두 경로가 동등하다는 것을 문서가 스스로 인정한 문장**이다.

**제안.** 조립 브랜치를 없애고, 전제조건 3개를 만족할 때 `release-{날짜}`를 그대로 master에 PR한다.
셋 다 기계적으로 검증 가능하므로 `/imt-deploy`가 자동 판정하고, 하나라도 실패하면 현행 조립 경로로 폴백한다.

```bash
git log release-0814 --not master --no-merges --oneline   # 비어야 함 — orphan 없음 (F3)
git merge-base --is-ancestor master release-0814          # 참이어야 함 — forward merge 완료
# 배포 집합 == UAT 검증 집합 (제외 없음) ← 사용자 확인. 제외면 4.5절 -v2 재조립
```

이때 4.1절 정합성 diff는 tautology가 되지만, 재조립을 안 하므로 그 diff가 잡으려던 위험(충돌을 다르게
푸는 것) 자체가 사라진다. **재조립은 위험을 만들고 diff로 잡는 구조**다.

> **유보.** 채택 여부는 F3(release 직접 커밋)이 실제로 얼마나 나는지에 달렸다. 자주 나는 팀이면 현행
> 규칙이 그 사고를 막는 값을 하고, 안 나는 팀이면 조립 브랜치는 순수 오버헤드다. **이 빈도가 손익분기점이다.**

### F2. 조립 브랜치 명명 규칙 누락 (2026-08-14)

**관찰.** 원본 문서는 "조립 브랜치"를 4곳(15행·206행·254행·436행)에서 부르지만 **이름을 준 적이 없다.**
3절 명명 규칙에는 `feature` / `release-{월일}` / `hotfix/{설명}` 만 있다. `assemble/{날짜}`라는 이름은
`.claude/commands/imt-deploy.md`에만 존재한다 — 커맨드 작성자가 구현하려고 붙인 이름이다.

문서만 읽은 팀원은 이 브랜치를 뭐라고 불러야 할지 알 수 없다. `release-` prefix 오용은 금지 규칙으로
막아뒀는데(3절) 조립 브랜치 명명에는 아무 규칙이 없다.

**제안.** F1을 채택하면 브랜치 자체가 없어져 해소된다. 채택하지 않으면 3절에 한 줄 추가:
`assemble : assemble/{월일}   예) assemble/0814  (배포 직후 삭제)`

### F3. 금지 규칙 7이 `release-{날짜}`를 빠뜨렸다 (2026-08-14)

**관찰.** 금지 규칙 7은 "**dev / test**에 직접 커밋 금지"다 — `release-{날짜}`가 없다.
7절 리셋 절차의 orphan 검출도 test용만 있다. release는 배포 후 삭제되므로, 거기 직접 커밋한 것은
조용히 사라지거나(merge 안 함) 조용히 prod에 반영된다(통째 merge).

**실측.** UAT 지적을 feature가 아니라 `release-0814`에 직접 커밋한 뒤 두 경로를 비교했다:

```
통째 merge한 master:            feature 단위 merge한 master:
    baseline                        baseline
    featA                           featA
    featB                           featB
    UAT 급수정(release에 직접)     ← 아무 신호 없이 prod 반영
```

feature 단위로 가면 그 커밋이 남겨지고 **두 곳에서 드러난다**:
`git diff release-0814 master` → `-UAT 급수정(release에 직접)` /
`git log release-0814 --not master --no-merges` → `<sha> fix: UAT 지적 급수정`

이것이 금지 규칙 4(통째 merge 금지)의 **실질적인 근거**다 — 4.1절이 대는 근거 3개보다 이게 강하다.

**제안.**
1. 금지 규칙 7을 "dev / test / **release-{날짜}**에 직접 커밋 금지"로 확장
2. 배포 전 체크(4.1절)에 orphan 검출 1줄 추가 — 위 `--no-merges` 명령
3. 금지 규칙 4의 근거로 이 실측을 인용

### F4. 4.1절 근거 3개가 결론을 다 못 받친다 (2026-08-14)

| 근거 | 검토 |
| --- | --- |
| ① 수시배포로 master가 움직여 **FF 전제가 안 성립** | **merge에는 해당 없음.** fast-forward만 막힌다. forward merge 후 non-FF merge는 정상 동작한다. 그런데 금지 규칙 4는 FF뿐 아니라 merge까지 금지한다 — 논거 범위가 결론보다 좁다 |
| ② 모드가 둘이면 **선별 배포 근육**이 퇴화 | 절반만 유효. **4.5절이 "제외 시 재조립 + UAT 재검증"을 의무화**했으므로 주 경로에서는 선별할 일이 없다. 선별이 실제로 필요한 건 "명백히 독립 + 사유 기록" 예외 경로뿐이다 |
| ③ 개별 merge + rerere = **통째와 byte 동일 트리** | 사실이지만 방향이 반대로 작용한다(F1 실측). 동등함을 인정하는 문장이고, 재조립은 "충돌을 다르게 풀 위험"을 새로 만들어 4.1절 diff를 필요하게 만든다. 통째 merge엔 그 위험이 없다 |

**제안.** 금지 규칙 4의 근거를 다시 쓴다. 남는 진짜 근거는 두 개다:
1. **orphan 격리** — F3 실측
2. **기본값 안전성** — 통째 merge가 표준이면 기본 동작이 "release에 있는 것 전부 나간다"가 된다.
   B가 UAT에서 떨어졌는데 4.5절을 잊고 merge하면 떨어진 B가 prod로 간다. feature 단위면 기본 동작이
   "명시한 것만 나간다"라서 의도치 않은 배포에 별도 행동이 필요하다. (기술적 정확성이 아니라 사람 실수 확률 논거)

①은 FF에만 한정해 적고, ③은 근거가 아니라 "통째와 결과가 같음을 보장하는 장치"로 위치를 바꾼다.

---

### F1·F3 재현 스크립트

임시 레포에서 돈다(이 레포 브랜치는 건드리지 않는다). `rerere.enabled true` 전제.
스크립트 전체를 그대로 붙여 실행해 검증했다 — **트리 SHA는 내용 기반이라 매번 재현되고**(위 `3645432…` 동일),
커밋 SHA는 타임스탬프에 의존하므로 실행마다 달라진다.

```bash
rm -rf /tmp/v4-exp && mkdir -p /tmp/v4-exp && cd /tmp/v4-exp
git init -q -b master && git config user.name t && git config user.email t@t
git config rerere.enabled true
printf 'baseline\n' > reg.txt && git add -A && git commit -qm base

git checkout -q -b featA master && printf 'baseline\nfeatA\n' > reg.txt && git commit -qam featA
git checkout -q -b featB master && printf 'baseline\nfeatB\n' > reg.txt && git commit -qam featB

# release-0814 조립 (featA + featB — 같은 줄을 건드려 충돌)
git checkout -q -b release-0814 master
git merge --no-ff -q featA -m 'merge featA'
git merge --no-ff featB -m 'merge featB' >/dev/null 2>&1 || true
printf 'baseline\nfeatA\nfeatB\n' > reg.txt && git add -A && git commit -qm 'merge featB (충돌 해결)'

# 경로 1: release 통째 merge   /   경로 2: master에서 재조립 (rerere 재적용)
git checkout -q -b path1 master && git merge --no-ff -q release-0814 -m 'deploy (통째)'
git checkout -q -b path2 master
git merge --no-ff -q featA -m 'merge featA'
git merge --no-ff featB -m 'merge featB' >/dev/null 2>&1; git add -A; git commit -qm 'merge featB'

[ "$(git rev-parse path1^{tree})" = "$(git rev-parse path2^{tree})" ] \
  && echo "F1 ✅ 트리 동일" || echo "F1 ❌ 트리 다름"

# F3: release에 직접 커밋한 뒤 두 경로 비교
git checkout -q release-0814
printf 'baseline\nfeatA\nfeatB\nUAT 급수정(release에 직접)\n' > reg.txt
git commit -qam 'fix: UAT 지적 급수정'
git checkout -q -b path1b master && git merge --no-ff -q release-0814 -m 'deploy (통째)'
echo "--- 통째 merge 결과 ---";      git show path1b:reg.txt
echo "--- feature 단위 결과 ---";    git show path2:reg.txt
git log release-0814 --not path2 --no-merges --oneline   # orphan 노출
```
