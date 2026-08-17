# Predict Rivals — KMM Client Design

**Date:** 2026-08-17
**Scope:** Kotlin Multiplatform + Compose Multiplatform client (Android first, then iOS/Web) for
the backend described in [FEATURES_AND_API_SUMMARY.md](../../../FEATURES_AND_API_SUMMARY.md).
No backend work in this doc — the backend already exists and is deployed at
`https://predict-rivals-backend-production.up.railway.app`.

## 1. Starting point

Repo is a fresh JetBrains KMM/Compose-Multiplatform wizard skeleton (package
`com.balltown.predictrivals`), targets Android + iOS + Web (JS/Wasm), builds successfully as-is
(`./gradlew :androidApp:assembleDebug` verified). Toolchain is bleeding-edge: AGP 9.0.1, Kotlin
2.4.10, Compose Multiplatform 1.11.1, Material3 1.11.0-alpha07, Lifecycle 2.11.0-beta01, minSdk 29.

## 2. API contract — verified vs. guessed

`API.md` and the linked design specs referenced by the summary doc don't exist in this repo. The
summary doc's field names also turned out to be wrong in places. Rather than guess blind, the
live backend was exercised directly (two throwaway test accounts, two throwaway tournaments) to
capture real shapes. Two real bugs surfaced along the way and should be fixed backend-side
independent of this client work:

- `POST /api/auth/email/register` 500s (`{"error":"Internal server error"}`) instead of 400ing
  when `name` is missing — the field itself is also undocumented (summary doc doesn't name it).
- More generally: malformed JSON and missing-required-field requests return a generic 500
  instead of 400 across at least `/api/auth/email/register` and `/api/tournaments` — the client
  will treat 500 defensively (generic "something went wrong, retry" UI) rather than assuming
  it always means a real outage, but this is worth tightening backend-side.

### Verified live (safe to build DTOs directly from)

```
POST /api/auth/email/register  {"email","password","name"}
  -> 201 {"tokens":{"accessToken","refreshToken"},"user":{"id","name","role"}}
POST /api/auth/email/login     {"email","password"}
  -> 200 same shape as register
POST /api/auth/refresh         (not directly captured, but 401 shape confirmed: {"error":"Invalid refresh token"})

POST /api/tournaments          {"name","playerLimit"}   <- NOT "playerCap" as the summary doc's field-naming implied
  -> 201 {"id","name","ownerUserId","joinCode","playerLimit","playerCount","format","status","createdAt"}
POST /api/tournaments/join     {"joinCode"}
  -> 200 <tournament>  |  404 {"error":"No tournament found for that join code"}
                        |  409 {"error":"Tournament has already started"}
POST /api/tournaments/{id}/start -> 200 <tournament> (status becomes "active")
GET  /api/tournaments/mine     -> 200 [<tournament>]
GET  /api/tournaments/{id}     -> 200 <tournament>  (no membership required)

GET /api/tournaments/{id}/standings   -> 200 [{"rank","userId","name","totalPoints","exactCount"}]
GET /api/tournaments/{id}/top-scorers -> 200 same shape as standings
GET /api/tournaments/{id}/users/{userId}/stats
  -> 200 {"userId","name","totalPoints","exactCount","totalPredictions","scoredPredictions","accuracy"}
GET /api/tournaments/{id}/calendar -> 200 [] (empty until rounds exist)
GET /api/tournaments/{id}/rounds/current -> 404 {"error":"No rounds found for tournament {id}"} until a round exists
GET /api/tournaments/{id}/live           -> same 404 shape until a round exists

Membership gating confirmed: any tournament-scoped GET returns
403 {"error":"Join the tournament to view its standings"} (message varies per endpoint) for a
non-member.
```

### Unverified — build from doc conventions, confirm on first real use

Could not fully exercise these because `GET /api/fixtures/candidates` returns `[]` for every
query-param combination tried (from/to, dateFrom/dateTo, date, search) — no real fixture IDs were
obtainable, which blocks testing anything downstream of fixture selection:

- `GET /api/fixtures/candidates` — query param names unknown. Client ships a search/date filter
  UI; params will need correcting once real ones are known.
- `POST /api/tournaments/{id}/matches` — guessed body `{"fixtureIds": [f1..f9]}`.
- `PATCH /api/tournaments/{id}/matches/{matchId}/score` — guessed body
  `{"homeScore": n, "awayScore": n, "status": "..."}`.
- `POST /api/predictions` — guessed body `{"matchId": n, "homeScore": n, "awayScore": n}`.
- `WS /ws/tournaments/{id}/live?token=...` — payload assumed to match the `GET .../live` REST
  snapshot shape (doc states they're the same payload), but never observed directly since no
  round ever existed on the probe tournaments.
- Google/Facebook auth bodies — not implemented this pass (see §4).

Each guessed DTO gets a `// UNVERIFIED:` comment at the field level so a future correction is a
quick find, not an archaeology project.

## 3. Architecture

- **Networking**: Ktor Client (OkHttp engine on Android, Darwin on iOS, JS engine on Web) +
  `kotlinx.serialization` for JSON (matches the `Content-Type: application/json` contract).
  Ktor's `Auth` plugin (Bearer provider) handles attaching the access token and automatic
  refresh-on-401; every successful refresh persists the *new* refresh token immediately, since
  the backend's refresh tokens are single-use.
- **DI**: Koin, multiplatform module wiring shared across all targets; platform `MainActivity` /
  iOS entry point / web entry point just start Koin and host the root Compose screen.
- **Navigation**: Compose Multiplatform Navigation (`org.jetbrains.androidx.navigation`), one nav
  graph in `commonMain` shared by all three targets.
- **Local storage**: `multiplatform-settings`, unencrypted, for the access/refresh token pair and
  a "last viewed tournament id" convenience value. Accepted v1 simplification — this is a
  predictions game, not financial data; revisit with platform-native secure storage
  (EncryptedSharedPreferences / Keychain) later if desired.
- **Dates**: `kotlinx-datetime` for kickoff-deadline comparisons and display formatting.
- **Live transport**: Ktor WebSockets plugin, exponential-backoff reconnect (capped), falling
  back to polling `GET .../live` every 10s if the socket can't establish after retries.
- **Pattern**: MVVM. `ViewModel` (androidx.lifecycle, multiplatform, already a dependency of the
  skeleton) exposes `StateFlow<UiState>` per screen; repositories wrap the Ktor client and are the
  only thing that knows about DTOs vs. domain models. Everything above the platform entry point
  lives in `commonMain` — Android/iOS/Web apps are thin shells.

### Module layout (all new code inside `shared/src/commonMain`, platform-specific only where
the compiler forces it — e.g. the Ktor engine dependency, and the WS engine per platform)

```
shared/src/commonMain/kotlin/com/balltown/predictrivals/
  data/
    api/          Ktor client setup, auth plugin config, typed ApiException mapping
    dto/          request/response DTOs (verified + UNVERIFIED-flagged)
    repository/   AuthRepository, TournamentRepository, PredictionRepository,
                  StandingsRepository, LiveRepository, CalendarRepository
    storage/      TokenStore (multiplatform-settings wrapper)
  domain/
    model/        UI-facing domain models (mapped from DTOs)
    scoring/      pure scoring-point calculation + kickoff-deadline gating (unit-testable, no I/O)
  ui/
    auth/         Login, Register screens + ViewModel
    home/         My Tournaments list, Create, Join
    tournament/   Detail shell (tabs) + Predictions/Standings/Live/Calendar/Curate sub-screens
    profile/      Stats + logout
    navigation/   NavGraph, routes
    theme/        Material3 default theme (dynamic color where supported)
  di/             Koin modules
```

## 4. Feature scope for this pass

- Auth: email/password register + login + refresh. **Google/Facebook sign-in deferred** — no
  platform credentials (OAuth client ID, SHA fingerprints, Facebook app ID) provisioned yet. The
  `AuthRepository` interface leaves room to add them without reshaping callers.
- Everything else in the summary doc's feature list is in scope for this single pass: tournament
  create/join/start/list/detail, match curation (owner), predictions (submit/edit with
  kickoff-deadline gating), standings/top-scorers/stats, live (WS + REST fallback), calendar.
- Client-side rules enforced per the doc's "rules a client must handle itself": gate round/match
  creation UI on tournament `status == active`; disable prediction inputs once each match's
  `kickoffAt` passes rather than relying on a server error; treat any 403 on a tournament-scoped
  call as "not a member" and route to Join; always persist the rotated refresh token.

## 5. Error handling

Typed sealed `ApiException` (Unauthorized/Forbidden/NotFound/Conflict/Validation/ServerError/
NetworkError) mapped from HTTP status + backend's `{"error": "..."}` body. UI shows the backend's
message where it's user-meaningful (e.g. join-code errors), and a generic retry affordance for
`ServerError`/`NetworkError` — since this backend is observed to 500 on cases a well-behaved API
would 400 on, a 500 is not necessarily catastrophic and retry is a reasonable first response.

## 6. Testing

`commonTest` unit tests, no live-backend dependency:
- Scoring-point calculation (exact=3, result+GD=2, result-only/correct-draw=1, wrong=0) against
  a table of score pairs.
- Kickoff-deadline gating (prediction editable before `kickoffAt`, locked after).
- DTO parsing against the real JSON samples captured in §2 (verified shapes) plus the guessed
  shapes for the unverified endpoints, so a future correction is a one-place fix.
- Token-refresh rotation logic (old refresh token discarded, new one persisted).

Real end-to-end verification is running the Android app against the live Railway backend:
register → create tournament → join (second account) → start → attempt curate/predict (expected
to surface the fixtures-search gap) → standings → stats → logout/login. iOS and Web builds are
compiled but not functionally verified this pass, per your stated test order (Android first).

## 7. Known gaps going into implementation

1. `GET /api/fixtures/candidates` params are unknown — curation flow will be UI-complete but its
   actual query needs correcting once real params or fixture data are available.
2. Round/match creation, score override, predictions, and the WS live payload are built from
   guessed shapes — first real round created against this backend will likely need a DTO fix
   pass, not a redesign.
3. Backend 500s on missing-required-field and malformed-JSON requests instead of 400ing — flagged
   for backend-side fix, client is defensive but this masks real client bugs during dev (a typo
   in a field name looks identical to a server outage).
4. Google/Facebook sign-in not implemented — deferred until credentials exist.
