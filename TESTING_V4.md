# v4 전략 검증 시나리오 (샌드박스)

이 레포는 **IMT Git Branch 전략 v4를 실제로 돌려보기 위한 샌드박스**다. 운영 코드가 없으므로
브랜치를 망가뜨려도 되고, 더미 워크플로가 실제 배포 대신 build + 태그 출력만 한다.

- 규칙 원본: [`claude-help/workflow/GIT_BRANCH_STRATEGY_V4_DRAFT.md`](claude-help/workflow/GIT_BRANCH_STRATEGY_V4_DRAFT.md)
- Confluence: [IMT Git Branch 전략 v4](https://trialinformatics.atlassian.net/wiki/spaces/ImageTrial/pages/716800002/IMT+Git+Branch+v4)

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

각 시나리오를 돌린 뒤 **v4 문서가 틀렸거나 애매한 지점**을 여기 남긴다 (문서가 초안이므로 이게 이 샌드박스의 산출물):

| 시나리오 | 관찰 | v4 문서 수정 제안 |
| --- | --- | --- |
| | | |
