---
description: 칸반 보드 출력 — 어느 기능이 dev/test/release/master 중 어디까지 갔는지 + 금지 규칙 위반 스캔
argument-hint: (없음)
---

v4의 **"어디까지 merge했나 = 그 기능의 현재 단계"** 를 표로 만들어 보여준다. 읽기 전용 — 아무것도 변경하지 않는다.

## 1. 최신 상태 확보

```bash
git fetch origin --prune --quiet
```

## 2. 칸반 보드 계산

`git branch -r --contains <feature>` 로 그 feature가 merge된 목적지를 뽑는다.

> **셸 주의:** 이 레포 기본 셸은 zsh다. zsh는 `for t in $VAR` 에서 **단어 분리를 하지 않아** 리스트가
> 한 덩어리로 들어온다. 그래서 리스트 순회는 반드시 파이프 + `while IFS= read -r` 로 쓴다.

```bash
git for-each-ref --format='%(refname:short)' refs/remotes/origin \
  | grep -E '^origin/([0-9]{4}/|hotfix/|epic/|feat/)' | sort \
  | while IFS= read -r f; do
      merged=$(git branch -r --contains "$f" 2>/dev/null | sed 's/^[* ] *//' \
               | grep -E '^origin/(dev|test|master|release-)' | sed 's|^origin/||' | tr '\n' ' ')
      stale=""
      # master 대비 뒤처짐 = 최신화 필요 (v4 8절)
      git merge-base --is-ancestor origin/master "$f" 2>/dev/null || stale="  ⚠ master 뒤처짐"
      printf '%s\t%s%s\n' "${f#origin/}" "${merged:-(없음)}" "$stale"
    done
```

출력은 `feature<TAB>merge된 목적지들` 형태다. 이걸 **v4 2절 형식의 칸반 표로 렌더링**해서 보여준다
(열 = dev / test / 열려 있는 release-* / master, 값 = ✓ 또는 —). 각 행에 **단계 해석**을 붙인다:

| 패턴 | 해석 | 다음 액션 |
| --- | --- | --- |
| dev만 O | 개발/연동 중 | 준비되면 `/imt-test` |
| test O, release - | **QA 중 또는 QA통과·배포일 미정(test 대기)** | 배포 확정 시 `/imt-release` |
| test O, release O, master - | UAT 검증 중 = 이번에 나가는 것 | 통과 시 `/imt-deploy` |
| master O | prod 반영 완료 | 브랜치 삭제 가능 |
| master O 인데 test/release에 - | hotfix (test 우회) | 열린 목적지에 forward merge (`/imt-sync` B) |
| ⚠ master 뒤처짐 | 드리프트 누적 중 | `/imt-sync` 모드 A |

## 3. 환경별 내용물

```bash
echo "── UAT 후보 (열린 release마다) ──"
git for-each-ref --format='%(refname:short)' refs/remotes/origin | grep '^origin/release-' \
  | while IFS= read -r r; do
      echo "[$r]"; git log "$r" --not origin/master --merges --oneline
    done
echo "── test 단계 이상 전부 (대기분 포함) ──"
git log origin/test --not origin/master --merges --oneline
```

> `git log`는 결과가 비어도 exit 0이므로 `|| echo "없음"` 이 안 먹는다. 비었는지 판단은
> `[ -z "$(git log ... )" ]` 로 하거나 출력이 없으면 "없음"으로 읽는다.

- **열린 release가 2개 이상이면 알린다** — UAT는 단일 환경이라 dispatch 순서를 통제해야 한다.
- 실제로 어느 환경에 무엇이 떠 있는지는 git이 모른다 (dispatch 시점의 문제). 필요하면 확인:
  ```bash
  gh run list --workflow=uat-build-deploy.yml -L 3
  gh run list --workflow=test-build-deploy.yml -L 3
  ```

## 4. 금지 규칙 위반 스캔

```bash
echo "── ① 타겟별 사본 브랜치 (금지 1) ──"
git for-each-ref --format='%(refname:short)' refs/remotes/origin | grep -E -- '-(dev|test|uat|master|release)$' || echo "없음 ✅"

echo "── ② release 네이밍 위반 (금지: 버전+날짜 혼합) ──"
git for-each-ref --format='%(refname:short)' refs/remotes/origin | grep '^origin/release-' \
  | grep -vE '^origin/release-[0-9]{4}(-v[0-9]+)?$' || echo "없음 ✅"

echo "── ③ dev/test 직접 커밋 = orphan 후보 (금지 7) ──"
for b in origin/dev origin/test; do
  echo "[$b]"; git log "$b" --not origin/master --no-merges --oneline
done
```
- ③에 뜬 커밋은 **어느 feature 브랜치에도 없는지** 확인한다 (있으면 정상적인 feature 경유 커밋):
  ```bash
  git branch -r --contains <sha>
  ```
  origin/dev(또는 test)에만 있으면 **리셋 시 소실되는 orphan** → feature 브랜치로 회수하라고 안내.

## 5. 요약 출력

마지막에 3줄로 정리한다:
- 지금 UAT 점유 중인 release / 이번에 나갈 기능
- test 대기 중인 기능 수 (**5개 초과면 리셋+재구성 주기 도래** — v4 7절)
- 최신화(`/imt-sync` A) 또는 forward merge(`/imt-sync` B)가 필요한 브랜치 목록

## 가드
- 읽기 전용. fetch 외에 어떤 것도 변경하지 않는다.
- 배포 여부/확정 여부를 Claude가 추측하지 않는다 — git이 아는 것(merge 여부)만 사실로 보고하고, 나머지는 사용자에게 확인한다.
