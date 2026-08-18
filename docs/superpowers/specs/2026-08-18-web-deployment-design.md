# Predict Rivals — Web Deployment Design

**Date:** 2026-08-18
**Scope:** Get the existing `webApp` Compose Multiplatform module reachable at
`https://www.predictrivals.com/`. No new screens, features, or Kotlin logic — the app
(login, tournaments, predictions, calendar, live, standings, profile) already exists in
`shared/` and already talks to the production backend
(`https://predict-rivals-backend-production.up.railway.app`, see `ApiClient.kt`). This doc
covers only how that existing build gets compiled for the browser and hosted.

## 1. Decisions made

- **UI:** reuse the existing Compose Multiplatform screens as-is (no separate React/JS UI).
  `webApp/src/webMain/kotlin/.../main.kt` already calls `ComposeViewport { App() }`.
- **Repo layout:** the website's Gradle module stays inside `predict-rivals-android`, next to
  `androidApp` and `iosApp` — this is the normal way a KMM project works and needs no submodule
  or artifact-publishing to reach `:shared`.
- **`predict-rivals-frontend`:** left empty/untouched. Not used by this deployment.
- **Hosting:** GitHub Pages, driven by a GitHub Actions workflow in `predict-rivals-android`.
  No second repo, no external host account.
- **Domain:** custom domain `predictrivals.com` / `www.predictrivals.com` (already registered on
  Namecheap), canonical host `www.predictrivals.com`, apex redirects to `www`.

## 2. Architecture

One new workflow, `.github/workflows/deploy-web.yml`, triggered on push to `master` (and
manually via `workflow_dispatch`):

1. Checkout, set up JDK (Temurin 21).
2. `./gradlew :webApp:wasmJsBrowserDistribution` — builds the production web bundle
   (HTML/JS/Wasm). Expected output directory: `webApp/build/dist/wasmJs/productionExecutable`
   (standard Kotlin/Wasm Gradle plugin convention) — **not yet verified by an actual build**,
   see Risks below.
3. Upload that directory as a Pages artifact (`actions/upload-pages-artifact`), deploy with
   `actions/deploy-pages`.

A static `CNAME` file containing `www.predictrivals.com` is added to
`webApp/src/webMain/resources/`, so it's copied into every distribution build automatically.
This is required because Actions-based Pages deploys replace the published output on every run —
a CNAME set only through the repo Settings UI would need to survive that, so it needs to travel
with the artifact instead.

Because the site will be served at the domain root (not a GitHub-Pages project subpath), the
existing relative asset paths in `index.html` (`styles.css`, `webApp.js`) need no base-href
changes.

## 3. Manual steps (outside this session / outside my access)

These aren't things I can do — no `git` access this session (per project rules), and no access
to your GitHub account settings or Namecheap dashboard:

1. **Commit & push** the workflow file (and CNAME resource) to `master`.
2. **GitHub → repo Settings → Pages:** set Source to "GitHub Actions", set custom domain to
   `www.predictrivals.com`, later check "Enforce HTTPS" once GitHub finishes issuing the
   certificate.
3. **Namecheap → Advanced DNS** for `predictrivals.com`:
   - Remove Namecheap's default parking-page records on `@` and `www`.
   - Add four A records, host `@`, values `185.199.108.153`, `185.199.109.153`,
     `185.199.110.153`, `185.199.111.153`.
   - Add one CNAME record, host `www`, value `carrickane.github.io.`.
4. Wait for DNS propagation + GitHub's certificate issuance (minutes to ~24h).

## 4. Out of scope

- Any change to `shared/` business logic or UI.
- `predict-rivals-frontend` repo.
- A public marketing/landing page distinct from the app's own Login screen (the site opens
  straight into the app, same as today).
- JS (non-Wasm) fallback target — Wasm-only for now.

## 5. Risks / unverified items

- **Gradle task/output path** (`wasmJsBrowserDistribution` → `build/dist/wasmJs/productionExecutable`)
  is the documented convention but hasn't been confirmed against this project, since compiling
  is deferred until explicitly allowed. If the path is wrong, the workflow's upload step will
  fail with a clear "directory not found" error and the path can be corrected then.
- **First real verification happens on GitHub's servers**, not locally — the first push is what
  actually proves the wasmJs build succeeds end-to-end.

## 6. Testing/verification plan

Deferred: this session doesn't run `git` or `./gradlew` (project rules). Verification is the
first Actions run after the user pushes, observed via the Actions log and the live URL.
