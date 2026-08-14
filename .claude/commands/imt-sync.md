---
description: feature를 master로 최신화(모드 A) 또는 배포 후 master를 열린 test/release/dev에 forward merge(모드 B)
argument-hint: [A|B] (생략 시 현재 브랜치로 판단)
---

v4의 **단방향 최신화**를 수행한다. 인자: `$ARGUMENTS`

> **절대 규칙: 당겨오는 건 master만.** `git merge dev` / `git merge test` / `git merge release-*` 는 전부 금지 —
> 미배포 기능이 섞여 master까지 딸려 들어간다.

모드를 인자로 안 주면 현재 브랜치로 판단한다: feature 위 → 모드 A, master/배포 직후 → 모드 B. 애매하면 물어본다.

---

## 모드 A — feature 최신화 (`git merge master`)

**언제:** master가 움직일 때마다. 특히 **test에서 오래 대기하는 기능**일수록 필수 (오래 살수록 드리프트가 크다).

```bash
git fetch origin --quiet
git checkout <feature>
git merge origin/master
```

1. rerere 확인 (같은 충돌을 dev/test/release/master 4곳에 반복 merge하므로 필수):
   ```bash
   git config --get rerere.enabled || git config --global rerere.enabled true
   ```
2. 충돌이 나면 **여기서(로컬에서) 푼다.** 여기서 한 번 풀면 이후 dev·test·release·master merge가 전부 깨끗해진다.
   - rerere가 자동 재적용한 해결은 **눈으로 검증**한다 (`git diff`). 잘못 푼 기록이면 `git rerere forget <파일>` 후 재해결.
3. push: `git push origin <feature>`
4. **재병합 안내** — feature가 움직였으므로 이미 merge해 둔 목적지에 다시 보내야 한다:
   - test에 있던 기능이면 → test에 재병합 (2번째 merge부터는 새 커밋만 diff에 보인다)
   - release에 있던 기능이면 → 그 release에도 재병합
   - **rebase 금지** (이미 공유된 브랜치) — 항상 merge로 처리한다.

### 가드
- 역방향 merge 시도(`git merge dev/test/release`)는 **거부하고** 이유를 설명한다.
- 대상이 feature가 아니면(master/test/release/dev) 모드 B인지 확인한다.

---

## 모드 B — 배포 후 forward merge (master → 열린 목적지)

**언제:** prod 배포/hotfix 직후. 안 하면 `git diff release-{날짜} master`(4.1절)에 무관한 hotfix가 섞여 보여 판독이 안 된다.

### 1. 대상 파악
```bash
git fetch origin --prune --quiet
git ls-remote --heads origin 'release-*'                       # 열린 release
git branch -r --list 'origin/test' 'origin/dev'
```
각 브랜치가 master에 뒤처졌는지 확인:
```bash
git merge-base --is-ancestor origin/master origin/<branch> && echo "최신" || echo "forward merge 필요"
```

### 2. forward merge (뒤처진 것만)
- **dev** — 휘발성이고 파이프라인 밖이므로 로컬 merge + push 허용:
  ```bash
  git checkout dev && git merge origin/master && git push origin dev
  ```
- **test / release-{날짜}** — 공유 브랜치이므로 **PR이 기본**이다:
  ```bash
  gh pr create --base test --head master --title "chore: forward merge master into test" --body "배포 후 기준선 갱신 (v4 5단계)"
  ```
  단, **충돌이 있으면 PR로 풀지 말고** 로컬에서 풀어 push한 뒤 PR을 merge한다 (rerere는 서버사이드 merge에 안 먹는다).
  사용자가 로컬 직접 merge를 원하면 승인받고 진행한다.

> `master → test/release/dev`는 forward merge라서 허용된다. 반대 방향(`test → 어디로든`, `release → 어디로든`)은 **금지**다.

### 3. feature 브랜치 최신화 안내
살아 있는 feature(특히 test 대기분)에 모드 A를 돌리라고 목록으로 안내한다:
```bash
git for-each-ref --format='%(refname:short)' refs/remotes/origin \
  | grep -E '^origin/[0-9]{4}/' \
  | while read -r b; do
      git merge-base --is-ancestor origin/master "$b" || echo "최신화 필요: $b"
    done
```

### 4. 사후 정리 안내 (실행은 승인 후)
- 사이클이 끝난 `release-{날짜}` 삭제: `git push origin --delete release-{날짜}`
- **prod 반영 완료된** feature만 삭제 — test 대기분은 절대 삭제하지 않는다:
  ```bash
  git merge-base --is-ancestor <feature> origin/master && echo "삭제 가능" || echo "미반영 — 유지"
  ```
- dev/test 리셋 주기 도래 여부 안내 (대기 5개 초과 또는 월 1회 → 7절 리셋+재구성 절차)

## 가드 (공통)
- 삭제·force push는 **항상 사용자 승인 후.** `--force-with-lease` 없이 force push 금지.
- test 리셋이 필요해 보이면 절차만 안내하고 **직접 실행하지 않는다** (대기 기능 재구성 목록은 릴리스 운영자가 작성).
