---
description: v4 feature 브랜치를 master에서 신설한다 (네이밍 검증 + rerere on)
argument-hint: <JIRA> <기능명> [MMDD 목표배포일] [이니셜]
---

IMT Git Branch v4 규칙에 따라 새 feature 브랜치를 만든다. 인자: `$ARGUMENTS`

## 규칙 (imt-git-flow 스킬 준수)

- feature는 **항상 최신 master에서** 분기한다. dev/test/release에서 분기 금지.
- 네이밍: `{MMDD}/{이니셜}/{JIRA}-{기능명-kebab}`  예) `0812/YH/IMTDEV-1980-user-filtering`
  - MMDD = 목표 배포일. 미정이면 잠정 목표일을 넣어 date-first를 유지.
  - 이니셜은 대문자(예: YH). 타겟별 사본 명명(`-dev`/`-test` 등) 절대 금지.
- Squash 안 쓰고 Merge Commit 원칙이므로 `rerere`를 켜 둔다.

## 절차

1. 인자 파싱: JIRA 키, 기능명, 선택적 MMDD, 선택적 이니셜.
   - 기능명은 kebab-case로 정규화(공백→`-`, 소문자).
   - MMDD 없으면 사용자에게 목표 배포일을 물어 잠정값이라도 받는다.
   - 이니셜 없으면 `git config user.name` 등에서 추정하되 애매하면 물어본다.
2. 최신 master 확보: `git fetch origin --quiet`
3. 네이밍 조립 후 사용자에게 최종 브랜치명을 한 줄로 보여준다.
4. **origin/master 기준으로 생성**(현재 체크아웃/작업트리가 섞여 들어가지 않게):
   ```bash
   git checkout -b {MMDD}/{이니셜}/{JIRA}-{기능명} origin/master
   ```
5. rerere 미설정이면 켠다:
   ```bash
   git config --global rerere.enabled true   # 이미 true면 생략
   ```
6. 결과 안내: 생성된 브랜치명, 다음 단계(`/imt-test`로 QA, `git merge master`로 최신화).

## 가드

- 현재 브랜치가 master가 아니어도 **반드시 origin/master에서** 분기한다(현재 브랜치에서 분기 금지).
- 네이밍이 규칙에 안 맞으면 만들지 말고 교정안을 제시한다.
