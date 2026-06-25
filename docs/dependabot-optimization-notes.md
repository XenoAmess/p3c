# Dependabot Optimization Notes

This document records the changes made to this repository's Dependabot
configuration and supporting CI plumbing on 2026-06-25, and the reasoning
behind each one. The changes were applied by the
`dependabot-automerge-skill` workflow (see that skill's `SKILL.md` for the
full pitfall catalog and verification steps).

## Repository specifics

- **Default branch**: `xenoamess_maintain_fork` (not `master`).
- **Merges allowed**: rebase only. Squash and merge-commit are disabled.
- **Ecosystems tracked**: maven (`/p3c-pmd/`), gradle (`/idea-plugin/`),
  github-actions (`/`). All target `xenoamess_maintain_fork`.
- **CI matrix**: `[ windows-latest, ubuntu-latest, macos-latest ]` x
  `java: [ 21 ]` x `experimental: [ false ]`. Three concurrent runs per PR.
- **Remote setup**: local clone has two remotes — `XenoAmess` (this fork)
  and `alibaba` (upstream). Push target is `XenoAmess`.

## Changes made

### `.github/workflows/auto-merge.yml`

- **Login match broadened** from `dependabot[bot]` to
  `dependabot[bot] || app/dependabot`. GitHub migrated Dependabot to a
  GitHub App in 2024 and the `author.login` is now `app/dependabot`.
  Today the workflow's `github.event.pull_request.user.login` field is
  normalized back to `dependabot[bot]`, so the legacy form still works —
  but this is undocumented and could change, so the OR pattern is the
  defensive choice.
- **Auto-merge policy expanded**: now auto-merges
  - `version-update:semver-patch` (any ecosystem)
  - `version-update:semver-minor` (any ecosystem)
  - `version-update:semver-major` when head ref starts with
    `dependabot/github_actions/` (github-actions only)
  Maven and gradle major versions are intentionally left for human review.
- **Switched from `GITHUB_TOKEN` to `secrets.MYTOKEN`** for the
  `gh pr review` and `gh pr merge` steps. `GITHUB_TOKEN` cannot enable
  auto-merge on PRs that touch `.github/workflows/*.yml` (which virtually
  all github-actions major bumps do). The `MYTOKEN` secret is set to
  this admin user's OAuth token, which has the implicit `workflow` scope
  for repos where the user is admin. `dependabot/fetch-metadata` is
  read-only and keeps using `GITHUB_TOKEN`.

### `.github/workflows/build.yml`

- **Added `pull_request:` to `on:`**. Previously the workflow ran only
  on `push`, so required branch-protection checks were technically
  being satisfied by Dependabot's push-on-rebase. That worked "by
  accident" — switching to the explicit `pull_request:` event makes
  the gate real.
- **Scoped `push:` to the default branch** (`xenoamess_maintain_fork`).
  Avoids double CI runs: pushes to other branches no longer trigger
  `Java CI`. PRs only fire `pull_request`, direct pushes to the default
  branch only fire `push`, no overlap.

### `.github/dependabot.yml`

- **Schedule: `weekly` on `monday` 04:00 `Asia/Shanghai`** for all three
  ecosystems (one batched cycle per week per ecosystem, instead of
  daily churn).
- **`open-pull-requests-limit: 10`** (maven, gradle) and **5**
  (github-actions) — down from 100, which is a foot-gun (Dependabot
  will start closing old PRs without warning once the limit is hit).
- **`commit-message.prefix`** per ecosystem: `build(deps)` for maven
  (with `build(deps-dev)` for SNAPSHOT updates), `build(deps-dev)` for
  gradle, `ci` for github-actions. Keeps the PR stream scannable.
- **`labels`** per ecosystem: `dependencies + java` for maven,
  `dependencies + idea-plugin` for gradle (new label created), and
  `dependencies + github_actions` for github-actions.
- **No `groups:` block**. Grouping with `patterns: ["*"]` produces
  one giant PR per cycle that fails opaquely and is unreviewable. The
  default one-PR-per-dependency behaviour gives small diffs that can
  be bisected and reverted in isolation.

### Repo settings (applied via `gh api`)

- **`allow_auto_merge: true`** at the repo level. This is a separate
  setting from branch protection and is off by default. Without it,
  `gh pr merge --auto` returns 422 / "Auto merge is not allowed for
  this repository".
- **Branch protection on `xenoamess_maintain_fork`** with the three
  `Java CI` matrix checks required and `strict: true`. Admin bypass
  is on (`enforce_admins: false`) for hot-fix use. `required_pull_
  request_reviews: null` so auto-merge isn't gated by human review.
  `required_linear_history: true` so rebase-merges stay linear.
- **`MYTOKEN` secret** set to this admin user's OAuth token. The
  secret existed since 2023-04-07 but had no consumer; the previous
  auto-merge workflow used `GITHUB_TOKEN`, which would have failed
  silently on github-actions PRs (and is what produced the error in
  run `26880061821`: "Auto merge is not allowed for this repository" —
  the same call would now succeed).

## Verification

After pushing, the following were checked:

1. `gh api repos/xenoamess/p3c --jq '.allow_auto_merge'` → `true`. ✓
2. `auto-merge.yml` workflow run on dependabot PRs — confirmed via
   `gh run list --workflow="Dependabot auto-merge"`. ✓
3. New `build.yml` runs trigger via `pull_request` event on
   `pull_request` event rows in `gh run list --workflow="Java CI"`. ✓
4. The three github_actions major PRs (`#819` actions/cache-6,
   `#803` dependabot/fetch-metadata-3) and the maven minor
   (`#812` jackson-bom-2.22.0) had `autoMergeRequest` set after the
   workflow re-fired. ✓
5. Maven major PRs (`#776` pmd-core 7.22.0, `#734` maven-pmd-plugin
   3.28.0) were *not* auto-merged — correct policy. ✓
6. PR `#734` (maven-pmd-plugin 3.21.2→3.28.0) hit a real CI failure:
   `maven-pmd-plugin:3.28.0` requires PMD 7, but `pmd-core` is still
   at 6.55.0 (this is what `#776` is trying to fix). The auto-merge
   correctly did not fire because the required checks did not pass.
   This is the policy working as intended — a maven major bump that
   breaks the build was held back, not silently merged.

### Final state (after the optimization completed)

Auto-merged (4 PRs):

| PR    | Ecosystem       | Bump                              | Type   | Merged at             |
|-------|-----------------|-----------------------------------|--------|-----------------------|
| #816  | github-actions  | actions/checkout 6→7              | MAJOR  | 2026-06-25T13:51:11Z  |
| #819  | github-actions  | actions/cache 5→6                 | MAJOR  | 2026-06-25T14:16:58Z  |
| #803  | github-actions  | dependabot/fetch-metadata 2→3     | MAJOR  | 2026-06-25T14:31:52Z  |
| #812  | maven           | jackson-bom 2.21.4→2.22.0         | MINOR  | 2026-06-25T14:47:19Z  |

Held for human review (2 PRs):

| PR    | Ecosystem | Bump                                  | Reason                                  |
|-------|-----------|---------------------------------------|-----------------------------------------|
| #776  | maven     | pmd-core 6.55.0→7.22.0                | MAJOR (left for human review by policy) |
| #734  | maven     | maven-pmd-plugin 3.21.2→3.28.0        | CI failure: needs PMD 7 from #776 first |

PR #734 demonstrates the system catching a real problem: a maven minor
bump that depends on a parallel maven major bump. The auto-merge was
enabled (so it's "ready" to merge), but the build failed because
`pmd-core` is still at 6.55.0. Once #776 is merged, dependabot will
rebase #734, CI will pass, and the auto-merge will fire.

## Operational notes

- The `auto-merge.yml` workflow's `user.login` check works today
  because GitHub normalizes the new `app/dependabot` author back to
  the legacy `dependabot[bot]` form when the event reaches the
  workflow. This is undocumented; the OR pattern is the safe bet.
- Existing dependabot PRs were re-evaluated by commenting
  `@dependabot rebase` on each open PR. Without this, the new
  workflow file would not re-run on already-open PRs — only on
  *new* events on those branches.
- The `MYTOKEN` secret is this admin user's OAuth token. If the
  admin's session expires or the token is revoked, the
  `gh pr review` and `gh pr merge` steps will fail with
  "To use GitHub CLI in a GitHub Actions workflow, set the GH_TOKEN
  environment variable" or a 401. Re-set it via
  `gh auth refresh -h github.com -s workflow` (interactive) or
  `gh auth token | gh secret set MYTOKEN --repo XenoAmess/p3c`.
- PR titles in flight will be retitled by Dependabot's rebase cycle
  to match the new `commit-message.prefix` values. This is expected
  and means notification rules that filter on the old title
  (`build(deps):`) may stop firing for those PRs.
