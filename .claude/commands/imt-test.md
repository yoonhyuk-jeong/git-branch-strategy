---
description: 현재 feature를 test 브랜치로 PR(본 리뷰)한 뒤 test 환경 dispatch까지 안내/실행
argument-hint: (없음 — 현재 feature 브랜치에서 실행)
---

현재 feature 브랜치를 **test**로 보내 QA 게이트를 태운다. v4에서 **본 리뷰는 test PR에서 1회** 한다.

## 가드 (먼저 확인)

1. `git fetch origin --quiet`
2. **origin/test 존재 확인**: `git ls-remote --heads origin test`
   - 없으면 **중단**하고 안내한다: "test 브랜치가 없습니다. v4 13절 전환 스텝입니다. `origin/master`에서 test를 신설할까요? (팀 합의된 전환 시점인지 확인 후)" → 사용자 승인 시에만
     `git branch test origin/master && git push -u origin test`.
3. 현재 브랜치가 feature인지 확인(master/test/release/dev면 중단). 사본 명명 아니어야 한다.
4. feature가 master에서 뒤처졌으면 최신화를 권한다: `git merge master`(=`/imt-sync` 모드 A). 충돌은 로컬에서 rerere로 해결.

## 절차 — feature → test PR (본 리뷰)

1. 상태 파악(병렬): `git status`, `git diff test...HEAD`, `git log test..HEAD --oneline`, `git remote -v`.
2. push 안 된 커밋 있으면 `git push -u origin HEAD`.
3. PR 생성 (**base=test**, Merge Commit 유지):
   ```bash
   gh pr create --base test --title "<영어 제목 ≤70자>" --body "$(cat <<'EOF'
   ## Summary
   - 핵심 변경 (1~3줄)

   ## Changes
   - 주요 변경 파일/로직

   ## Test plan (QA)
   - [ ] QA 확인 항목

   🤖 Generated with [Claude Code](https://claude.com/claude-code)
   EOF
   )"
   ```
   - 제목 영어·70자 이내, 본문 한국어. PR URL 출력.
   - 이 PR이 **본 리뷰 지점**임을 사용자에게 알린다(Files Changed 탭 리뷰).

## test 환경 dispatch

PR이 merge된 뒤(또는 사용자가 지금 QA 배포를 원하면), test 환경은 **test 브랜치 소스로 dispatch**한다. 되돌리기 어려운 배포 동작이므로 **실행 전 승인**을 받는다:

```bash
gh workflow run test-build-deploy.yml --ref test
```

승인받고 실행했으면 `gh run list --workflow=test-build-deploy.yml -L 1`로 트리거 확인 후 링크 안내.

## 가드

- **Squash 금지** — Merge Commit만. `test`를 feature로 되받는 역방향 merge 금지.
- QA 지적 수정은 같은 feature 브랜치에서 커밋 → test에 재PR/재병합(새 커밋만 diff).
- QA 통과해도 배포일 미정이면 test에 그대로 대기(정상). 배포 확정 시 `/imt-release`.
