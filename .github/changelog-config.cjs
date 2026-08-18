'use strict'

const config = require('conventional-changelog-conventionalcommits')

// refactor/chore/build/ci는 conventionalcommits 프리셋 기본값에서 hidden 처리되어 CHANGELOG.md에 표시되지 않는다.
// 이 프로젝트는 실제 배포 파이프라인/빌드 구성 변경도 릴리즈 노트에 남기기 위해 hidden을 false로 override한다.
// docs/style/test는 애플리케이션 동작에 직접 영향이 없어 숨김을 유지한다.
module.exports = config({
  types: [
    { type: 'feat', section: 'Features' },
    { type: 'fix', section: 'Bug Fixes' },
    { type: 'perf', section: 'Performance Improvements' },
    { type: 'revert', section: 'Reverts' },
    { type: 'refactor', section: 'Code Refactoring', hidden: false },
    { type: 'chore', section: 'Chores', hidden: false },
    { type: 'build', section: 'Build System', hidden: false },
    { type: 'ci', section: 'Continuous Integration', hidden: false },
    { type: 'docs', section: 'Documentation', hidden: true },
    { type: 'style', section: 'Styles', hidden: true },
    { type: 'test', section: 'Tests', hidden: true },
  ],
}).then((preset) => {
  // chore(release): v... [skip ci] 자동 커밋은 태그 이후에 push되어 다음 릴리즈 범위에 포함되므로,
  // Chores를 노출한 이후로는 매 릴리즈마다 지난 릴리즈의 버전관리용 커밋이 반복 노출된다. 이를 막기 위해 별도로 제외한다.
  const originalTransform = preset.writerOpts.transform
  preset.writerOpts.transform = (commit, context) => {
    if (commit.type === 'chore' && commit.scope === 'release') {
      return undefined
    }
    return originalTransform(commit, context)
  }
  return preset
})
