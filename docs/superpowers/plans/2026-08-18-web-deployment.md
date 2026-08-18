# Web Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the existing `webApp` Compose Multiplatform module reachable at
`https://www.predictrivals.com/` by adding a GitHub Actions workflow that builds it and deploys
it to GitHub Pages, plus a bundled `CNAME` file for the custom domain.

**Architecture:** Two new files only — a static `CNAME` resource (copied into every web build
automatically since it lives under `webApp/src/webMain/resources/`) and a GitHub Actions workflow
(`.github/workflows/deploy-web.yml`) that runs `./gradlew :webApp:wasmJsBrowserDistribution` and
publishes the output to GitHub Pages. No Kotlin/Compose code changes.

**Tech Stack:** GitHub Actions (`actions/checkout`, `actions/setup-java` w/ Temurin 21,
`actions/upload-pages-artifact`, `actions/deploy-pages`), Kotlin/Wasm Gradle plugin (already
configured in `webApp/build.gradle.kts`).

**Reference:** [2026-08-18-web-deployment-design.md](../specs/2026-08-18-web-deployment-design.md)

**Hard constraints for this session (from project CLAUDE.md):** no `git` commands, no
`./gradlew`/compiling. Every task below is written so the agent only creates/edits files — it
never runs a build or a git command. Steps that require those (committing, pushing, enabling
GitHub Pages, editing Namecheap DNS) are explicitly called out as **manual, user-performed**
steps, not agent steps.

---

### Task 1: Add the CNAME file for the custom domain

**Files:**
- Create: `webApp/src/webMain/resources/CNAME`

- [ ] **Step 1: Create the file with exactly this content (no leading/trailing whitespace beyond the single trailing newline)**

```
www.predictrivals.com
```

- [ ] **Step 2: Confirm it sits next to the other web resources**

It must be in the same directory as `index.html` and `styles.css`
(`webApp/src/webMain/resources/`) so the Kotlin/Wasm Gradle plugin copies it into the
distribution output alongside them. No command to run — this is a file-location check, done by
listing the directory:

```
ls webApp/src/webMain/resources/
```

Expected: `CNAME`, `index.html`, `styles.css` all present.

- [ ] **Step 3: No commit yet** — commit happens once, at the end of Task 3, after both files
exist. (Skip ahead to Task 2.)

---

### Task 2: Add the GitHub Actions deploy workflow

**Files:**
- Create: `.github/workflows/deploy-web.yml`

- [ ] **Step 1: Create the workflow file with this exact content**

```yaml
name: Deploy web app to GitHub Pages

on:
  push:
    branches: [master]
  workflow_dispatch:

permissions:
  contents: read
  pages: write
  id-token: write

concurrency:
  group: pages
  cancel-in-progress: true

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: gradle

      - name: Make gradlew executable
        run: chmod +x ./gradlew

      - name: Build web bundle
        run: ./gradlew :webApp:wasmJsBrowserDistribution

      - name: Upload Pages artifact
        uses: actions/upload-pages-artifact@v3
        with:
          path: webApp/build/dist/wasmJs/productionExecutable

  deploy:
    needs: build
    runs-on: ubuntu-latest
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    steps:
      - name: Deploy to GitHub Pages
        id: deployment
        uses: actions/deploy-pages@v4
```

- [ ] **Step 2: Sanity-check the file is valid YAML by eye**

No agent-run command here (compiling/running is out of scope this session) — just re-read the
file to confirm indentation is consistent (2 spaces) and every `- name:` step is nested correctly
under its job's `steps:` list, matching the block above exactly.

- [ ] **Step 3: Note the one unverified detail**

`webApp/build/dist/wasmJs/productionExecutable` is the standard Kotlin/Wasm Gradle plugin output
path for the `wasmJsBrowserDistribution` task, but it has not been confirmed by an actual build
in this project (compiling is deferred). If the first Actions run fails at the "Upload Pages
artifact" step with a "no files were found" / path-not-found error, that's the fix needed —
check the actual build log a few steps earlier for the real output directory Gradle used, and
correct the `path:` value in this file to match.

---

### Task 3: Manual steps (you perform these — not the agent)

These require `git` access, your GitHub account's repo settings, and your Namecheap dashboard —
none of which this session touches.

- [ ] **Step 1: Review the two new files**

```
webApp/src/webMain/resources/CNAME
.github/workflows/deploy-web.yml
```

- [ ] **Step 2: Commit and push**

```bash
git add webApp/src/webMain/resources/CNAME .github/workflows/deploy-web.yml
git commit -m "Add GitHub Pages deploy workflow for webApp"
git push origin master
```

- [ ] **Step 3: Watch the first Actions run**

On GitHub: repo → **Actions** tab → the "Deploy web app to GitHub Pages" run that started from
your push. If it fails, read the failing step's log — the most likely failure point is the
artifact path noted in Task 2 Step 3.

- [ ] **Step 4: Enable GitHub Pages**

Repo → **Settings → Pages**:
- Source: **GitHub Actions**
- Custom domain: `www.predictrivals.com`
- Leave "Enforce HTTPS" unchecked until DNS resolves (Step 6), then come back and check it.

- [ ] **Step 5: Configure DNS at Namecheap**

Namecheap dashboard → Domain List → `predictrivals.com` → **Manage → Advanced DNS**.

First, delete Namecheap's default parking-page records if present (usually a `CNAME` or `URL
Redirect Record` on host `@` or `www` pointing at Namecheap's parking service).

Then add:

| Type | Host | Value |
|---|---|---|
| A Record | `@` | `185.199.108.153` |
| A Record | `@` | `185.199.109.153` |
| A Record | `@` | `185.199.110.153` |
| A Record | `@` | `185.199.111.153` |
| CNAME Record | `www` | `devleksandr.github.io.` |

- [ ] **Step 6: Wait for propagation and verify**

DNS + certificate issuance can take minutes to ~24 hours. Verify with:

```bash
dig +short www.predictrivals.com
dig +short predictrivals.com
```

Expected: the `www` lookup returns `devleksandr.github.io`'s IPs (resolved through the CNAME);
the apex lookup returns the four `185.199.10x.153` addresses. Once both resolve and
`https://www.predictrivals.com/` loads the app in a browser, go back to Settings → Pages and
check "Enforce HTTPS".

---

## Self-review

**Spec coverage:** design doc's decisions (reuse Compose UI / module stays in
predict-rivals-android / frontend repo untouched / GitHub Pages via Actions / custom domain with
apex redirect) are all either already true of the codebase (no task needed) or covered by Task
1–3. The design's "Manual steps" section maps 1:1 onto Task 3.

**Placeholders:** none — every file's exact content is given in full, DNS records are concrete
values, commit message is concrete.

**Type/name consistency:** module name `:webApp`, task name `wasmJsBrowserDistribution`, and
output path `webApp/build/dist/wasmJs/productionExecutable` are used identically in the design
doc, Task 2, and Task 3 troubleshooting note.
