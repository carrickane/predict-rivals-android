# Predict Rivals KMM Client Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **Git note:** the user is committing this work themselves. Every task below ends with a
> "Commit" step written for completeness/reuse of this document — **the executing agent must
> skip that step** (stage nothing, run no `git commit`) and move to the next task with changes
> left in the working tree.

**Goal:** Build the full KMM/Compose-Multiplatform client (auth, tournaments, match curation,
predictions, scoring, standings, live, calendar) against the live Predict Rivals backend, with
Android as the target that gets actually run and verified this pass.

**Architecture:** MVVM in `shared/src/commonMain`. Ktor client (per-platform engine) + Auth
Bearer plugin for token refresh, Koin for DI, Compose Multiplatform Navigation for routing,
`multiplatform-settings` for token persistence, `kotlinx-datetime` for kickoff-deadline logic.
Full rationale in [docs/superpowers/specs/2026-08-17-kmm-client-design.md](../specs/2026-08-17-kmm-client-design.md).

**Tech Stack:** Kotlin 2.4.10, Compose Multiplatform 1.11.1, Ktor 3.2.0, Koin 4.1.0,
kotlinx-serialization-json 1.9.0, kotlinx-datetime 0.7.1, multiplatform-settings 1.3.0,
androidx-navigation-compose (JetBrains multiplatform fork) 2.9.0-beta03.

---

## Verified API reference (copy exactly — do not "clean up" field names)

```
POST /api/auth/email/register  {"email","password","name"}
  -> 201 {"tokens":{"accessToken","refreshToken"},"user":{"id","name","role"}}
POST /api/auth/email/login     {"email","password"}
  -> 200 same shape as register
POST /api/auth/refresh         {"refreshToken"}
  -> 200 {"accessToken","refreshToken"}  (assumed from doc; 401 shape confirmed live: {"error":"Invalid refresh token"})

POST /api/tournaments          {"name","playerLimit"}
  -> 201 {"id","name","ownerUserId","joinCode","playerLimit","playerCount","format","status","createdAt"}
POST /api/tournaments/join     {"joinCode"}
  -> 200 <tournament> | 404 {"error":"..."} | 409 {"error":"Tournament has already started"}
POST /api/tournaments/{id}/start -> 200 <tournament>
GET  /api/tournaments/mine     -> 200 [<tournament>]
GET  /api/tournaments/{id}     -> 200 <tournament>

GET /api/tournaments/{id}/standings   -> 200 [{"rank","userId","name","totalPoints","exactCount"}]
GET /api/tournaments/{id}/top-scorers -> 200 same shape
GET /api/tournaments/{id}/users/{userId}/stats
  -> 200 {"userId","name","totalPoints","exactCount","totalPredictions","scoredPredictions","accuracy"}
GET /api/tournaments/{id}/calendar -> 200 [] until rounds exist (shape of a round unverified)
GET /api/tournaments/{id}/rounds/current -> 404 {"error":"No rounds found for tournament {id}"} until a round exists
GET /api/tournaments/{id}/live -> same 404 shape until a round exists

Any tournament-scoped GET as non-member -> 403 {"error":"..."}
```

UNVERIFIED (guessed from doc conventions — each DTO field below carries a `// UNVERIFIED` comment
in the code; correct on first real use):
- `GET /api/fixtures/candidates` — query params unknown, returns `[]` for everything tried
- `POST /api/tournaments/{id}/matches` — guessed `{"fixtureIds": [f1..f9]}`
- `PATCH /api/tournaments/{id}/matches/{matchId}/score` — guessed `{"homeScore","awayScore","status"}`
- `POST /api/predictions` — guessed `{"matchId","homeScore","awayScore"}`
- `WS /ws/tournaments/{id}/live?token=...` — assumed identical payload to `GET .../live`

---

### Task 1: Dependency wiring

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `shared/build.gradle.kts`

- [ ] **Step 1: Add version catalog entries**

In `gradle/libs.versions.toml`, add to `[versions]`:

```toml
ktor = "3.2.0"
koin = "4.1.0"
kotlinxSerializationJson = "1.9.0"
kotlinxDatetime = "0.7.1"
multiplatformSettings = "1.3.0"
navigationCompose = "2.9.0-beta03"
```

Add to `[libraries]`:

```toml
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-contentNegotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinxJson = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-auth = { module = "io.ktor:ktor-client-auth", version.ref = "ktor" }
ktor-client-websockets = { module = "io.ktor:ktor-client-websockets", version.ref = "ktor" }
ktor-client-logging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-js = { module = "io.ktor:ktor-client-js", version.ref = "ktor" }
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinxDatetime" }
multiplatformSettings-noArg = { module = "com.russhwolf:multiplatform-settings-no-arg", version.ref = "multiplatformSettings" }
multiplatformSettings-test = { module = "com.russhwolf:multiplatform-settings-test", version.ref = "multiplatformSettings" }
androidx-navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "navigationCompose" }
```

Add to `[plugins]`:

```toml
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

- [ ] **Step 2: Wire the plugin and dependencies into `shared/build.gradle.kts`**

Add the plugin alias to the `plugins {}` block:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}
```

Replace the `sourceSets { ... }` block with:

```kotlin
sourceSets {
    androidMain.dependencies {
        implementation(libs.compose.uiToolingPreview)
        implementation(libs.compose.uiTooling)
        implementation(libs.ktor.client.okhttp)
    }
    commonMain.dependencies {
        implementation(libs.compose.runtime)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)
        implementation(libs.compose.ui)
        implementation(libs.compose.components.resources)
        implementation(libs.compose.uiToolingPreview)
        implementation(libs.androidx.lifecycle.viewmodelCompose)
        implementation(libs.androidx.lifecycle.runtimeCompose)
        implementation(libs.androidx.navigation.compose)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.contentNegotiation)
        implementation(libs.ktor.serialization.kotlinxJson)
        implementation(libs.ktor.client.auth)
        implementation(libs.ktor.client.websockets)
        implementation(libs.ktor.client.logging)
        implementation(libs.koin.core)
        implementation(libs.koin.compose)
        implementation(libs.koin.compose.viewmodel)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.datetime)
        implementation(libs.multiplatformSettings.noArg)
    }
    commonTest.dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.ktor.client.mock)
        implementation(libs.multiplatformSettings.test)
    }
    iosMain.dependencies {
        implementation(libs.ktor.client.darwin)
    }
    jsMain.dependencies {
        implementation(libs.wrappers.browser)
        implementation(libs.ktor.client.js)
    }
    wasmJsMain.dependencies {
        implementation(libs.ktor.client.js)
    }
}
```

- [ ] **Step 3: Verify it resolves and compiles**

Run: `./gradlew :shared:compileAndroidMain --console=plain`
Expected: `BUILD SUCCESSFUL`. If any artifact fails to resolve, it's almost always a stale patch
version — search `https://search.maven.org/solrsearch/select?q=g:%22<group>%22+AND+a:%22<artifact>%22&core=gav&rows=5&wt=json`
for the real latest and bump only that one line.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml shared/build.gradle.kts
git commit -m "build: add Ktor, Koin, navigation-compose, datetime, settings dependencies"
```

---

### Task 2: Remove template boilerplate

**Files:**
- Delete: `shared/src/commonMain/kotlin/com/balltown/predictrivals/Greeting.kt`
- Delete: `shared/src/commonMain/kotlin/com/balltown/predictrivals/GreetingUtil.kt`
- Delete: `shared/src/commonMain/kotlin/com/balltown/predictrivals/Platform.kt`
- Delete: `shared/src/androidMain/kotlin/com/balltown/predictrivals/Platform.android.kt`
- Delete: `shared/src/iosMain/kotlin/com/balltown/predictrivals/Platform.ios.kt`
- Delete: `shared/src/jsMain/kotlin/com/balltown/predictrivals/Platform.js.kt`
- Delete: `shared/src/wasmJsMain/kotlin/com/balltown/predictrivals/Platform.wasmJs.kt`
- Delete: `shared/src/androidHostTest/kotlin/com/balltown/predictrivals/SharedLogicAndroidHostTest.kt`
- Delete: `shared/src/iosTest/kotlin/com/balltown/predictrivals/SharedLogicIOSTest.kt`
- Delete: `shared/src/webTest/kotlin/com/balltown/predictrivals/SharedLogicWebTest.kt`
- Delete: `shared/src/commonTest/kotlin/com/balltown/predictrivals/SharedCommonTest.kt`

These are the wizard's placeholder "Greeting"/"Platform name" demo and its throwaway tests —
nothing in the app will reference them once `App.kt` is rewritten in Task 22. Deleting now (before
anything depends on them) avoids a dangling-reference cleanup later.

- [ ] **Step 1: Delete the files listed above**

Run: `rm shared/src/commonMain/kotlin/com/balltown/predictrivals/Greeting.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/GreetingUtil.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/Platform.kt shared/src/androidMain/kotlin/com/balltown/predictrivals/Platform.android.kt shared/src/iosMain/kotlin/com/balltown/predictrivals/Platform.ios.kt shared/src/jsMain/kotlin/com/balltown/predictrivals/Platform.js.kt shared/src/wasmJsMain/kotlin/com/balltown/predictrivals/Platform.wasmJs.kt shared/src/androidHostTest/kotlin/com/balltown/predictrivals/SharedLogicAndroidHostTest.kt shared/src/iosTest/kotlin/com/balltown/predictrivals/SharedLogicIOSTest.kt shared/src/webTest/kotlin/com/balltown/predictrivals/SharedLogicWebTest.kt shared/src/commonTest/kotlin/com/balltown/predictrivals/SharedCommonTest.kt`

Note: `shared/src/commonMain/kotlin/com/balltown/predictrivals/App.kt` still references
`Greeting()` at this point — that's fixed in Task 22, not this task. Expect
`:shared:compileAndroidMain` to fail after this step; that's expected and resolved later, so skip
the usual "verify it compiles" step here.

- [ ] **Step 2: Commit**

```bash
git add -A shared/src
git commit -m "chore: remove wizard placeholder Greeting/Platform demo code"
```

---

### Task 3: TokenStore

**Files:**
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/storage/TokenStore.kt`
- Test: `shared/src/commonTest/kotlin/com/balltown/predictrivals/data/storage/TokenStoreTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.balltown.predictrivals.data.storage

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TokenStoreTest {

    @Test
    fun `load returns null when nothing saved`() {
        val store = TokenStore(MapSettings())
        assertNull(store.load())
    }

    @Test
    fun `save then load round-trips both tokens`() {
        val store = TokenStore(MapSettings())
        store.save(TokenPair(accessToken = "access-1", refreshToken = "refresh-1"))
        assertEquals(TokenPair("access-1", "refresh-1"), store.load())
    }

    @Test
    fun `save again overwrites the previous pair (refresh tokens are single-use)`() {
        val store = TokenStore(MapSettings())
        store.save(TokenPair("access-1", "refresh-1"))
        store.save(TokenPair("access-2", "refresh-2"))
        assertEquals(TokenPair("access-2", "refresh-2"), store.load())
    }

    @Test
    fun `clear removes both tokens`() {
        val store = TokenStore(MapSettings())
        store.save(TokenPair("access-1", "refresh-1"))
        store.clear()
        assertNull(store.load())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.data.storage.TokenStoreTest" --console=plain`
Expected: FAIL — `TokenStore` / `TokenPair` unresolved references.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.balltown.predictrivals.data.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import com.russhwolf.settings.get

data class TokenPair(val accessToken: String, val refreshToken: String)

class TokenStore(private val settings: Settings = Settings()) {

    fun save(tokens: TokenPair) {
        settings[KEY_ACCESS] = tokens.accessToken
        settings[KEY_REFRESH] = tokens.refreshToken
    }

    fun load(): TokenPair? {
        val access: String? = settings[KEY_ACCESS]
        val refresh: String? = settings[KEY_REFRESH]
        if (access == null || refresh == null) return null
        return TokenPair(access, refresh)
    }

    fun clear() {
        settings.remove(KEY_ACCESS)
        settings.remove(KEY_REFRESH)
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.data.storage.TokenStoreTest" --console=plain`
Expected: PASS, 4 tests.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/balltown/predictrivals/data/storage/TokenStore.kt shared/src/commonTest/kotlin/com/balltown/predictrivals/data/storage/TokenStoreTest.kt
git commit -m "feat: add TokenStore for persisting the access/refresh token pair"
```

---

### Task 4: ApiException + HTTP status mapping

**Files:**
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/api/ApiException.kt`
- Test: `shared/src/commonTest/kotlin/com/balltown/predictrivals/data/api/ApiExceptionTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.balltown.predictrivals.data.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ApiExceptionTest {

    @Test
    fun `maps known status codes to their typed exception`() {
        assertIs<ApiException.Unauthorized>(apiExceptionFor(401, """{"error":"nope"}"""))
        assertIs<ApiException.Forbidden>(apiExceptionFor(403, """{"error":"nope"}"""))
        assertIs<ApiException.NotFound>(apiExceptionFor(404, """{"error":"nope"}"""))
        assertIs<ApiException.Conflict>(apiExceptionFor(409, """{"error":"nope"}"""))
        assertIs<ApiException.ServerError>(apiExceptionFor(500, """{"error":"nope"}"""))
        assertIs<ApiException.ServerError>(apiExceptionFor(502, """not json"""))
    }

    @Test
    fun `extracts the backend's error message when the body is the standard error shape`() {
        val ex = apiExceptionFor(404, """{"error":"No tournament found for that join code"}""")
        assertEquals("No tournament found for that join code", ex.message)
    }

    @Test
    fun `falls back to a generic message when the body isn't the standard error shape`() {
        val ex = apiExceptionFor(500, "not json")
        assertEquals("Something went wrong. Please try again.", ex.message)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.data.api.ApiExceptionTest" --console=plain`
Expected: FAIL — `apiExceptionFor` / `ApiException` unresolved.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.balltown.predictrivals.data.api

import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ErrorBody(val error: String)

private val errorBodyJson = Json { ignoreUnknownKeys = true }

sealed class ApiException(override val message: String) : Exception(message) {
    class Unauthorized(message: String) : ApiException(message)
    class Forbidden(message: String) : ApiException(message)
    class NotFound(message: String) : ApiException(message)
    class Conflict(message: String) : ApiException(message)
    class ServerError(message: String) : ApiException(message)
    class NetworkError(message: String) : ApiException(message)
}

private const val GENERIC_MESSAGE = "Something went wrong. Please try again."

fun apiExceptionFor(statusCode: Int, rawBody: String): ApiException {
    val message = runCatching { errorBodyJson.decodeFromString<ErrorBody>(rawBody).error }
        .getOrElse { GENERIC_MESSAGE }
    return when (statusCode) {
        401 -> ApiException.Unauthorized(message)
        403 -> ApiException.Forbidden(message)
        404 -> ApiException.NotFound(message)
        409 -> ApiException.Conflict(message)
        else -> ApiException.ServerError(message)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.data.api.ApiExceptionTest" --console=plain`
Expected: PASS, 3 tests.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/balltown/predictrivals/data/api/ApiException.kt shared/src/commonTest/kotlin/com/balltown/predictrivals/data/api/ApiExceptionTest.kt
git commit -m "feat: add typed ApiException mapped from HTTP status + backend error body"
```

---

### Task 5: Auth DTOs, unauthenticated AuthApi, and ApiClient (Auth Bearer + refresh)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/dto/AuthDto.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/api/AuthApi.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/api/ApiClient.kt`
- Test: `shared/src/commonTest/kotlin/com/balltown/predictrivals/data/dto/AuthDtoTest.kt`
- Test: `shared/src/commonTest/kotlin/com/balltown/predictrivals/data/api/ApiClientTest.kt`

`AuthApi` is a plain, unauthenticated Ktor client used only for register/login/refresh (these
calls happen before there's a token, or *are* the refresh itself, so they must not go through the
Auth plugin). `ApiClient` is the authenticated client every other repository uses; its Auth
plugin's `refreshTokens` block calls `AuthApi.refresh(...)` directly, so there's no circular
dependency between "the client that refreshes" and "a repository that would otherwise own
refreshing."

- [ ] **Step 1: Write the failing DTO test**

```kotlin
package com.balltown.predictrivals.data.dto

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses a real register-or-login response`() {
        val body = """
            {"tokens":{"accessToken":"acc-123","refreshToken":"ref-123"},"user":{"id":1,"name":"ProbeTest","role":"player"}}
        """.trimIndent()
        val parsed = json.decodeFromString<AuthResponseDto>(body)
        assertEquals("acc-123", parsed.tokens.accessToken)
        assertEquals("ref-123", parsed.tokens.refreshToken)
        assertEquals(1, parsed.user.id)
        assertEquals("ProbeTest", parsed.user.name)
        assertEquals("player", parsed.user.role)
    }

    @Test
    fun `serializes a register request with the real field name (name, not displayName)`() {
        val request = RegisterRequestDto(email = "a@b.com", password = "pw", name = "A")
        assertEquals("""{"email":"a@b.com","password":"pw","name":"A"}""", json.encodeToString(request))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.data.dto.AuthDtoTest" --console=plain`
Expected: FAIL — DTO classes unresolved.

- [ ] **Step 3: Write the DTOs**

```kotlin
package com.balltown.predictrivals.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDto(val email: String, val password: String, val name: String)

@Serializable
data class LoginRequestDto(val email: String, val password: String)

@Serializable
data class RefreshRequestDto(val refreshToken: String)

@Serializable
data class TokenPairDto(val accessToken: String, val refreshToken: String)

@Serializable
data class UserDto(val id: Int, val name: String, val role: String)

@Serializable
data class AuthResponseDto(val tokens: TokenPairDto, val user: UserDto)

@Serializable
data class RefreshResponseDto(val accessToken: String, val refreshToken: String) // UNVERIFIED shape, 401 path confirmed live only
```

- [ ] **Step 4: Run DTO test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.data.dto.AuthDtoTest" --console=plain`
Expected: PASS, 2 tests.

- [ ] **Step 5: Write the failing ApiClient test**

```kotlin
package com.balltown.predictrivals.data.api

import com.balltown.predictrivals.data.storage.TokenPair
import com.balltown.predictrivals.data.storage.TokenStore
import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiClientTest {

    @Test
    fun `attaches the stored access token as a bearer header`() = runTest {
        val tokenStore = TokenStore(MapSettings()).apply { save(TokenPair("valid-access", "valid-refresh")) }
        var seenAuthHeader: String? = null
        val engine = MockEngine { request ->
            seenAuthHeader = request.headers[HttpHeaders.Authorization]
            respond("""{"id":1,"name":"x","ownerUserId":1,"joinCode":"AB","playerLimit":2,"playerCount":1,"format":"solo_points","status":"open","createdAt":"now"}""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val authApi = AuthApi(engine = engine, baseUrl = BASE_URL)
        val client = buildApiClient(engine = engine, baseUrl = BASE_URL, tokenStore = tokenStore, authApi = authApi)

        client.get("$BASE_URL/api/tournaments/1")

        assertEquals("Bearer valid-access", seenAuthHeader)
    }

    @Test
    fun `refreshes once on 401 then retries the original request with the new token`() = runTest {
        val tokenStore = TokenStore(MapSettings()).apply { save(TokenPair("expired-access", "old-refresh")) }
        var call = 0
        val engine = MockEngine { request ->
            call++
            when {
                request.url.encodedPath == "/api/auth/refresh" ->
                    respond("""{"accessToken":"new-access","refreshToken":"new-refresh"}""",
                        HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                call == 1 -> respond("""{"error":"Invalid access token"}""", HttpStatusCode.Unauthorized,
                    headersOf(HttpHeaders.ContentType, "application/json"))
                else -> respond("""{"id":1,"name":"x","ownerUserId":1,"joinCode":"AB","playerLimit":2,"playerCount":1,"format":"solo_points","status":"open","createdAt":"now"}""",
                    HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val authApi = AuthApi(engine = engine, baseUrl = BASE_URL)
        val client = buildApiClient(engine = engine, baseUrl = BASE_URL, tokenStore = tokenStore, authApi = authApi)

        client.get("$BASE_URL/api/tournaments/1")

        assertEquals(TokenPair("new-access", "new-refresh"), tokenStore.load())
    }

    private companion object {
        const val BASE_URL = "https://predict-rivals-backend-production.up.railway.app"
    }
}
```

- [ ] **Step 6: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.data.api.ApiClientTest" --console=plain`
Expected: FAIL — `AuthApi` / `buildApiClient` unresolved.

- [ ] **Step 7: Write AuthApi**

```kotlin
package com.balltown.predictrivals.data.api

import com.balltown.predictrivals.data.dto.AuthResponseDto
import com.balltown.predictrivals.data.dto.LoginRequestDto
import com.balltown.predictrivals.data.dto.RefreshRequestDto
import com.balltown.predictrivals.data.dto.RefreshResponseDto
import com.balltown.predictrivals.data.dto.RegisterRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Unauthenticated client for the three auth endpoints that must never go through the Auth plugin. */
class AuthApi(engine: HttpClientEngine, private val baseUrl: String) {

    private val client = HttpClient(engine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    suspend fun register(email: String, password: String, name: String): AuthResponseDto =
        postForAuth("$baseUrl/api/auth/email/register", RegisterRequestDto(email, password, name))

    suspend fun login(email: String, password: String): AuthResponseDto =
        postForAuth("$baseUrl/api/auth/email/login", LoginRequestDto(email, password))

    suspend fun refresh(refreshToken: String): RefreshResponseDto {
        val response = client.post("$baseUrl/api/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequestDto(refreshToken))
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) throw apiExceptionFor(response.status.value, body)
        return Json { ignoreUnknownKeys = true }.decodeFromString(body)
    }

    private suspend inline fun <reified TRequest> postForAuth(url: String, request: TRequest): AuthResponseDto {
        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) throw apiExceptionFor(response.status.value, body)
        return Json { ignoreUnknownKeys = true }.decodeFromString(body)
    }
}
```

- [ ] **Step 8: Write ApiClient**

```kotlin
package com.balltown.predictrivals.data.api

import com.balltown.predictrivals.data.storage.TokenPair
import com.balltown.predictrivals.data.storage.TokenStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

const val API_BASE_URL = "https://predict-rivals-backend-production.up.railway.app"

fun buildApiClient(
    engine: HttpClientEngine,
    baseUrl: String,
    tokenStore: TokenStore,
    authApi: AuthApi,
): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(Logging) { level = LogLevel.INFO }
    install(WebSockets)
    install(Auth) {
        bearer {
            loadTokens {
                tokenStore.load()?.let { BearerTokens(it.accessToken, it.refreshToken) }
            }
            refreshTokens {
                val current = tokenStore.load() ?: return@refreshTokens null
                val refreshed = runCatching { authApi.refresh(current.refreshToken) }.getOrNull() ?: return@refreshTokens null
                tokenStore.save(TokenPair(refreshed.accessToken, refreshed.refreshToken))
                BearerTokens(refreshed.accessToken, refreshed.refreshToken)
            }
        }
    }
    HttpResponseValidator {
        validateResponse { response ->
            if (!response.status.isSuccess()) {
                throw apiExceptionFor(response.status.value, response.bodyAsText())
            }
        }
    }
}.also { /* baseUrl kept as a parameter for symmetry with AuthApi; callers pass full URLs built from API_BASE_URL */ }
```

- [ ] **Step 9: Run test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.data.api.ApiClientTest" --console=plain`
Expected: PASS, 2 tests.

- [ ] **Step 10: Commit**

```bash
git add shared/src/commonMain/kotlin/com/balltown/predictrivals/data/dto/AuthDto.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/data/api/AuthApi.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/data/api/ApiClient.kt shared/src/commonTest/kotlin/com/balltown/predictrivals/data/dto/AuthDtoTest.kt shared/src/commonTest/kotlin/com/balltown/predictrivals/data/api/ApiClientTest.kt
git commit -m "feat: add auth DTOs, unauthenticated AuthApi, and the Bearer-refreshing ApiClient"
```

---

### Task 6: AuthRepository

**Files:**
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/domain/model/User.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/repository/AuthRepository.kt`
- Test: `shared/src/commonTest/kotlin/com/balltown/predictrivals/data/repository/AuthRepositoryTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.AuthApi
import com.balltown.predictrivals.data.storage.TokenPair
import com.balltown.predictrivals.data.storage.TokenStore
import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthRepositoryTest {

    @Test
    fun `register saves tokens and returns the user`() = runTest {
        val tokenStore = TokenStore(MapSettings())
        val engine = MockEngine {
            respond("""{"tokens":{"accessToken":"acc","refreshToken":"ref"},"user":{"id":1,"name":"A","role":"player"}}""",
                HttpStatusCode.Created, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val repository = AuthRepository(AuthApi(engine, "https://x"), tokenStore)

        val user = repository.register(email = "a@b.com", password = "pw", name = "A")

        assertEquals("A", user.name)
        assertEquals(TokenPair("acc", "ref"), tokenStore.load())
    }

    @Test
    fun `logout clears the stored tokens`() = runTest {
        val tokenStore = TokenStore(MapSettings()).apply { save(TokenPair("acc", "ref")) }
        val repository = AuthRepository(AuthApi(MockEngine { error("not called") }, "https://x"), tokenStore)

        repository.logout()

        assertNull(tokenStore.load())
    }

    @Test
    fun `isLoggedIn reflects whether a token pair is stored`() = runTest {
        val tokenStore = TokenStore(MapSettings())
        val repository = AuthRepository(AuthApi(MockEngine { error("not called") }, "https://x"), tokenStore)

        assertEquals(false, repository.isLoggedIn())
        tokenStore.save(TokenPair("acc", "ref"))
        assertEquals(true, repository.isLoggedIn())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.data.repository.AuthRepositoryTest" --console=plain`
Expected: FAIL — `AuthRepository` unresolved.

- [ ] **Step 3: Write the domain model**

```kotlin
package com.balltown.predictrivals.domain.model

data class User(val id: Int, val name: String, val role: String)
```

- [ ] **Step 4: Write AuthRepository**

```kotlin
package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.AuthApi
import com.balltown.predictrivals.data.storage.TokenPair
import com.balltown.predictrivals.data.storage.TokenStore
import com.balltown.predictrivals.domain.model.User

class AuthRepository(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
) {
    suspend fun register(email: String, password: String, name: String): User {
        val response = authApi.register(email, password, name)
        tokenStore.save(TokenPair(response.tokens.accessToken, response.tokens.refreshToken))
        return User(response.user.id, response.user.name, response.user.role)
    }

    suspend fun login(email: String, password: String): User {
        val response = authApi.login(email, password)
        tokenStore.save(TokenPair(response.tokens.accessToken, response.tokens.refreshToken))
        return User(response.user.id, response.user.name, response.user.role)
    }

    fun logout() = tokenStore.clear()

    fun isLoggedIn(): Boolean = tokenStore.load() != null
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.data.repository.AuthRepositoryTest" --console=plain`
Expected: PASS, 3 tests.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/balltown/predictrivals/domain/model/User.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/data/repository/AuthRepository.kt shared/src/commonTest/kotlin/com/balltown/predictrivals/data/repository/AuthRepositoryTest.kt
git commit -m "feat: add AuthRepository (register/login/logout/isLoggedIn)"
```

---

### Task 7: Tournament DTOs + domain model + TournamentRepository

**Files:**
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/dto/TournamentDto.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/domain/model/Tournament.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/repository/TournamentRepository.kt`
- Test: `shared/src/commonTest/kotlin/com/balltown/predictrivals/data/repository/TournamentRepositoryTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.ApiException
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private const val TOURNAMENT_JSON =
    """{"id":1,"name":"Probe Cup","ownerUserId":1,"joinCode":"9VFVPN","playerLimit":10,"playerCount":1,"format":"solo_points","status":"open","createdAt":"2026-08-17T08:54:01.979038Z"}"""

class TournamentRepositoryTest {

    @Test
    fun `create posts name and playerLimit and maps the response`() = runTest {
        val engine = MockEngine { respond(TOURNAMENT_JSON, HttpStatusCode.Created, headersOf(HttpHeaders.ContentType, "application/json")) }
        val repository = TournamentRepository(testApiClient(engine))

        val tournament = repository.create(name = "Probe Cup", playerLimit = 10)

        assertEquals("9VFVPN", tournament.joinCode)
        assertEquals("open", tournament.status)
    }

    @Test
    fun `join surfaces the backend's conflict message when already started`() = runTest {
        val engine = MockEngine { respond("""{"error":"Tournament has already started"}""", HttpStatusCode.Conflict, headersOf(HttpHeaders.ContentType, "application/json")) }
        val repository = TournamentRepository(testApiClient(engine))

        val error = assertFailsWith<ApiException.Conflict> { repository.join(joinCode = "9VFVPN") }
        assertEquals("Tournament has already started", error.message)
    }

    @Test
    fun `mine maps a list of tournaments`() = runTest {
        val engine = MockEngine { respond("[$TOURNAMENT_JSON]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }
        val repository = TournamentRepository(testApiClient(engine))

        val tournaments = repository.mine()

        assertEquals(1, tournaments.size)
        assertEquals("Probe Cup", tournaments.first().name)
    }
}
```

- [ ] **Step 2: Add the shared MockEngine test helper used above and by every later repository test**

**Files:**
- Create: `shared/src/commonTest/kotlin/com/balltown/predictrivals/data/repository/TestApiClient.kt`

```kotlin
package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.AuthApi
import com.balltown.predictrivals.data.api.buildApiClient
import com.balltown.predictrivals.data.storage.TokenPair
import com.balltown.predictrivals.data.storage.TokenStore
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine

const val TEST_BASE_URL = "https://predict-rivals-backend-production.up.railway.app"

fun testApiClient(engine: MockEngine): HttpClient {
    val tokenStore = TokenStore(MapSettings()).apply { save(TokenPair("test-access", "test-refresh")) }
    return buildApiClient(engine, TEST_BASE_URL, tokenStore, AuthApi(engine, TEST_BASE_URL))
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.data.repository.TournamentRepositoryTest" --console=plain`
Expected: FAIL — `TournamentRepository` unresolved.

- [ ] **Step 4: Write the DTO and domain model**

```kotlin
package com.balltown.predictrivals.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class TournamentDto(
    val id: Int,
    val name: String,
    val ownerUserId: Int,
    val joinCode: String,
    val playerLimit: Int,
    val playerCount: Int,
    val format: String,
    val status: String,
    val createdAt: String,
)

@Serializable
data class CreateTournamentRequestDto(val name: String, val playerLimit: Int)

@Serializable
data class JoinTournamentRequestDto(val joinCode: String)
```

```kotlin
package com.balltown.predictrivals.domain.model

data class Tournament(
    val id: Int,
    val name: String,
    val ownerUserId: Int,
    val joinCode: String,
    val playerLimit: Int,
    val playerCount: Int,
    val format: String,
    val status: String,
    val createdAt: String,
) {
    val isOpen get() = status == "open"
    val isActive get() = status == "active"
    fun isOwnedBy(userId: Int) = ownerUserId == userId
}
```

- [ ] **Step 5: Write TournamentRepository**

```kotlin
package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.API_BASE_URL
import com.balltown.predictrivals.data.dto.CreateTournamentRequestDto
import com.balltown.predictrivals.data.dto.JoinTournamentRequestDto
import com.balltown.predictrivals.data.dto.TournamentDto
import com.balltown.predictrivals.domain.model.Tournament
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class TournamentRepository(private val client: HttpClient) {

    suspend fun create(name: String, playerLimit: Int): Tournament =
        client.post("$API_BASE_URL/api/tournaments") {
            contentType(ContentType.Application.Json)
            setBody(CreateTournamentRequestDto(name, playerLimit))
        }.body<TournamentDto>().toDomain()

    suspend fun join(joinCode: String): Tournament =
        client.post("$API_BASE_URL/api/tournaments/join") {
            contentType(ContentType.Application.Json)
            setBody(JoinTournamentRequestDto(joinCode))
        }.body<TournamentDto>().toDomain()

    suspend fun start(tournamentId: Int): Tournament =
        client.post("$API_BASE_URL/api/tournaments/$tournamentId/start").body<TournamentDto>().toDomain()

    suspend fun mine(): List<Tournament> =
        client.get("$API_BASE_URL/api/tournaments/mine").body<List<TournamentDto>>().map { it.toDomain() }

    suspend fun get(tournamentId: Int): Tournament =
        client.get("$API_BASE_URL/api/tournaments/$tournamentId").body<TournamentDto>().toDomain()

    private fun TournamentDto.toDomain() = Tournament(id, name, ownerUserId, joinCode, playerLimit, playerCount, format, status, createdAt)
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.data.repository.TournamentRepositoryTest" --console=plain`
Expected: PASS, 3 tests.

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/com/balltown/predictrivals/data/dto/TournamentDto.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/domain/model/Tournament.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/data/repository/TournamentRepository.kt shared/src/commonTest/kotlin/com/balltown/predictrivals/data/repository/TournamentRepositoryTest.kt shared/src/commonTest/kotlin/com/balltown/predictrivals/data/repository/TestApiClient.kt
git commit -m "feat: add Tournament DTO/domain model and TournamentRepository (create/join/start/mine/get)"
```

---

### Task 8: Standings, top-scorers, and user-stats

**Files:**
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/dto/StandingsDto.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/domain/model/Standing.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/repository/StandingsRepository.kt`
- Test: `shared/src/commonTest/kotlin/com/balltown/predictrivals/data/repository/StandingsRepositoryTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.balltown.predictrivals.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StandingsRepositoryTest {

    @Test
    fun `standings maps rank, user, and points`() = runTest {
        val engine = MockEngine {
            respond("""[{"rank":1,"userId":1,"name":"ProbeTest","totalPoints":0,"exactCount":0}]""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val repository = StandingsRepository(testApiClient(engine))

        val standings = repository.standings(tournamentId = 1)

        assertEquals(1, standings.single().rank)
        assertEquals("ProbeTest", standings.single().name)
    }

    @Test
    fun `userStats maps accuracy and prediction counts`() = runTest {
        val engine = MockEngine {
            respond("""{"userId":1,"name":"ProbeTest","totalPoints":0,"exactCount":0,"totalPredictions":0,"scoredPredictions":0,"accuracy":0.0}""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val repository = StandingsRepository(testApiClient(engine))

        val stats = repository.userStats(tournamentId = 1, userId = 1)

        assertEquals(0.0, stats.accuracy)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.data.repository.StandingsRepositoryTest" --console=plain`
Expected: FAIL — `StandingsRepository` unresolved.

- [ ] **Step 3: Write the DTO and domain model**

```kotlin
package com.balltown.predictrivals.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class StandingDto(val rank: Int, val userId: Int, val name: String, val totalPoints: Int, val exactCount: Int)

@Serializable
data class UserStatsDto(
    val userId: Int,
    val name: String,
    val totalPoints: Int,
    val exactCount: Int,
    val totalPredictions: Int,
    val scoredPredictions: Int,
    val accuracy: Double,
)
```

```kotlin
package com.balltown.predictrivals.domain.model

data class Standing(val rank: Int, val userId: Int, val name: String, val totalPoints: Int, val exactCount: Int)

data class UserStats(
    val userId: Int,
    val name: String,
    val totalPoints: Int,
    val exactCount: Int,
    val totalPredictions: Int,
    val scoredPredictions: Int,
    val accuracy: Double,
)
```

- [ ] **Step 4: Write StandingsRepository**

```kotlin
package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.API_BASE_URL
import com.balltown.predictrivals.data.dto.StandingDto
import com.balltown.predictrivals.data.dto.UserStatsDto
import com.balltown.predictrivals.domain.model.Standing
import com.balltown.predictrivals.domain.model.UserStats
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class StandingsRepository(private val client: HttpClient) {

    suspend fun standings(tournamentId: Int): List<Standing> =
        client.get("$API_BASE_URL/api/tournaments/$tournamentId/standings").body<List<StandingDto>>().map { it.toDomain() }

    suspend fun topScorers(tournamentId: Int): List<Standing> =
        client.get("$API_BASE_URL/api/tournaments/$tournamentId/top-scorers").body<List<StandingDto>>().map { it.toDomain() }

    suspend fun userStats(tournamentId: Int, userId: Int): UserStats =
        client.get("$API_BASE_URL/api/tournaments/$tournamentId/users/$userId/stats").body<UserStatsDto>().let {
            UserStats(it.userId, it.name, it.totalPoints, it.exactCount, it.totalPredictions, it.scoredPredictions, it.accuracy)
        }

    private fun StandingDto.toDomain() = Standing(rank, userId, name, totalPoints, exactCount)
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.data.repository.StandingsRepositoryTest" --console=plain`
Expected: PASS, 2 tests.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/balltown/predictrivals/data/dto/StandingsDto.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/domain/model/Standing.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/data/repository/StandingsRepository.kt shared/src/commonTest/kotlin/com/balltown/predictrivals/data/repository/StandingsRepositoryTest.kt
git commit -m "feat: add Standings/top-scorers/user-stats DTOs, models, and StandingsRepository"
```

---

### Task 9: Calendar + current round

**Files:**
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/dto/RoundDto.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/domain/model/Round.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/repository/CalendarRepository.kt`
- Test: `shared/src/commonTest/kotlin/com/balltown/predictrivals/data/repository/CalendarRepositoryTest.kt`

Round/match shape is unverified (never had a real round to observe), so this DTO is built from the
doc's description ("round + its matches", each match presumably having the fixture's teams, a
kickoff time, and a score) with `// UNVERIFIED` markers. `currentRound` must treat the confirmed
live 404 (`"No rounds found for tournament {id}"`) as "no round yet," not as an error to surface.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.balltown.predictrivals.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalendarRepositoryTest {

    @Test
    fun `calendar maps an empty list when no rounds exist yet`() = runTest {
        val engine = MockEngine { respond("[]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }
        val repository = CalendarRepository(testApiClient(engine))

        assertTrue(repository.calendar(tournamentId = 1).isEmpty())
    }

    @Test
    fun `currentRound returns null instead of throwing on the confirmed 404`() = runTest {
        val engine = MockEngine {
            respond("""{"error":"No rounds found for tournament 1"}""", HttpStatusCode.NotFound, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val repository = CalendarRepository(testApiClient(engine))

        assertNull(repository.currentRound(tournamentId = 1))
    }

    @Test
    fun `currentRound maps a real round when one exists`() = runTest {
        val engine = MockEngine {
            respond(
                """{"roundNumber":1,"matches":[{"id":10,"homeTeam":"A","awayTeam":"B","kickoffAt":"2026-08-20T18:00:00Z","homeScore":null,"awayScore":null,"status":"scheduled"}]}""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val repository = CalendarRepository(testApiClient(engine))

        val round = repository.currentRound(tournamentId = 1)

        assertEquals(1, round?.roundNumber)
        assertEquals(1, round?.matches?.size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.data.repository.CalendarRepositoryTest" --console=plain`
Expected: FAIL — `CalendarRepository` unresolved.

- [ ] **Step 3: Write the DTO and domain model**

```kotlin
package com.balltown.predictrivals.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class MatchDto( // UNVERIFIED: never observed a real round; shape inferred from the doc
    val id: Int,
    val homeTeam: String,
    val awayTeam: String,
    val kickoffAt: String,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val status: String,
)

@Serializable
data class RoundDto(val roundNumber: Int, val matches: List<MatchDto>) // UNVERIFIED
```

```kotlin
package com.balltown.predictrivals.domain.model

data class Match(
    val id: Int,
    val homeTeam: String,
    val awayTeam: String,
    val kickoffAt: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val status: String,
)

data class Round(val roundNumber: Int, val matches: List<Match>)
```

- [ ] **Step 4: Write CalendarRepository**

```kotlin
package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.API_BASE_URL
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.dto.MatchDto
import com.balltown.predictrivals.data.dto.RoundDto
import com.balltown.predictrivals.domain.model.Match
import com.balltown.predictrivals.domain.model.Round
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class CalendarRepository(private val client: HttpClient) {

    suspend fun calendar(tournamentId: Int): List<Round> =
        client.get("$API_BASE_URL/api/tournaments/$tournamentId/calendar").body<List<RoundDto>>().map { it.toDomain() }

    suspend fun currentRound(tournamentId: Int): Round? =
        try {
            client.get("$API_BASE_URL/api/tournaments/$tournamentId/rounds/current").body<RoundDto>().toDomain()
        } catch (e: ApiException.NotFound) {
            null
        }

    private fun RoundDto.toDomain() = Round(roundNumber, matches.map { it.toDomain() })
    private fun MatchDto.toDomain() = Match(id, homeTeam, awayTeam, kickoffAt, homeScore, awayScore, status)
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.data.repository.CalendarRepositoryTest" --console=plain`
Expected: PASS, 3 tests.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/balltown/predictrivals/data/dto/RoundDto.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/domain/model/Round.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/data/repository/CalendarRepository.kt shared/src/commonTest/kotlin/com/balltown/predictrivals/data/repository/CalendarRepositoryTest.kt
git commit -m "feat: add Round/Match DTOs and CalendarRepository (calendar + current round)"
```

---

### Task 10: Fixture search (unverified) + curation (round creation, score override)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/dto/FixtureDto.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/domain/model/Fixture.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/repository/FixtureRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/repository/CurationRepository.kt`
- Test: `shared/src/commonTest/kotlin/com/balltown/predictrivals/data/repository/FixtureRepositoryTest.kt`
- Test: `shared/src/commonTest/kotlin/com/balltown/predictrivals/data/repository/CurationRepositoryTest.kt`

`GET /api/fixtures/candidates` returned `[]` for every query param tried live, so its request
shape is a placeholder query builder (`from`/`to`/`search`) that will need correcting once real
params are known — the response DTO shape is also unverified since it was never populated.

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.balltown.predictrivals.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class FixtureRepositoryTest {

    @Test
    fun `candidates maps an empty result (the only response observed live)`() = runTest {
        val engine = MockEngine { respond("[]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }
        val repository = FixtureRepository(testApiClient(engine))

        assertTrue(repository.candidates(from = "2026-08-20", to = "2026-08-27", search = null).isEmpty())
    }

    @Test
    fun `candidates maps a hypothetical populated result`() = runTest {
        val engine = MockEngine {
            respond("""[{"id":501,"homeTeam":"Arsenal","awayTeam":"Chelsea","kickoffAt":"2026-08-22T15:00:00Z","competition":"Premier League"}]""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val repository = FixtureRepository(testApiClient(engine))

        val candidates = repository.candidates(from = "2026-08-20", to = "2026-08-27", search = null)

        assertTrue(candidates.single().homeTeam == "Arsenal")
    }
}
```

```kotlin
package com.balltown.predictrivals.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CurationRepositoryTest {

    @Test
    fun `createRound posts the 9 fixture ids and maps the round`() = runTest {
        var capturedBody: String? = null
        val engine = MockEngine { request ->
            capturedBody = (request.body as io.ktor.client.request.forms.TextContent).text
            respond(
                """{"roundNumber":1,"matches":[{"id":1,"homeTeam":"A","awayTeam":"B","kickoffAt":"2026-08-20T18:00:00Z","homeScore":null,"awayScore":null,"status":"scheduled"}]}""",
                HttpStatusCode.Created, headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = CurationRepository(testApiClient(engine))

        val round = repository.createRound(tournamentId = 1, fixtureIds = (1..9).toList())

        assertEquals(1, round.roundNumber)
        assertEquals("""{"fixtureIds":[1,2,3,4,5,6,7,8,9]}""", capturedBody)
    }

    @Test
    fun `overrideScore patches home and away score`() = runTest {
        val engine = MockEngine {
            respond(
                """{"id":1,"homeTeam":"A","awayTeam":"B","kickoffAt":"2026-08-20T18:00:00Z","homeScore":2,"awayScore":1,"status":"finished"}""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = CurationRepository(testApiClient(engine))

        val match = repository.overrideScore(tournamentId = 1, matchId = 1, homeScore = 2, awayScore = 1, status = "finished")

        assertEquals(2, match.homeScore)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.data.repository.FixtureRepositoryTest" --tests "com.balltown.predictrivals.data.repository.CurationRepositoryTest" --console=plain`
Expected: FAIL — `FixtureRepository` / `CurationRepository` unresolved.

- [ ] **Step 3: Write the DTOs and domain model**

```kotlin
package com.balltown.predictrivals.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class FixtureCandidateDto( // UNVERIFIED: endpoint returned [] for every param tried live
    val id: Int,
    val homeTeam: String,
    val awayTeam: String,
    val kickoffAt: String,
    val competition: String,
)

@Serializable
data class CreateRoundRequestDto(val fixtureIds: List<Int>) // UNVERIFIED

@Serializable
data class ScoreOverrideRequestDto(val homeScore: Int, val awayScore: Int, val status: String) // UNVERIFIED
```

```kotlin
package com.balltown.predictrivals.domain.model

data class Fixture(val id: Int, val homeTeam: String, val awayTeam: String, val kickoffAt: String, val competition: String)
```

- [ ] **Step 4: Write FixtureRepository**

```kotlin
package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.API_BASE_URL
import com.balltown.predictrivals.data.dto.FixtureCandidateDto
import com.balltown.predictrivals.domain.model.Fixture
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class FixtureRepository(private val client: HttpClient) {

    // UNVERIFIED: from/to/search param names are a guess — endpoint returned [] for every
    // combination tried against the live backend. Correct these once real params are known.
    suspend fun candidates(from: String, to: String, search: String?): List<Fixture> =
        client.get("$API_BASE_URL/api/fixtures/candidates") {
            parameter("from", from)
            parameter("to", to)
            search?.let { parameter("search", it) }
        }.body<List<FixtureCandidateDto>>().map { Fixture(it.id, it.homeTeam, it.awayTeam, it.kickoffAt, it.competition) }
}
```

- [ ] **Step 5: Write CurationRepository**

```kotlin
package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.API_BASE_URL
import com.balltown.predictrivals.data.dto.CreateRoundRequestDto
import com.balltown.predictrivals.data.dto.MatchDto
import com.balltown.predictrivals.data.dto.RoundDto
import com.balltown.predictrivals.data.dto.ScoreOverrideRequestDto
import com.balltown.predictrivals.domain.model.Match
import com.balltown.predictrivals.domain.model.Round
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class CurationRepository(private val client: HttpClient) {

    suspend fun createRound(tournamentId: Int, fixtureIds: List<Int>): Round {
        require(fixtureIds.size == 9) { "A round must be created from exactly 9 fixtures, got ${fixtureIds.size}" }
        val dto = client.post("$API_BASE_URL/api/tournaments/$tournamentId/matches") {
            contentType(ContentType.Application.Json)
            setBody(CreateRoundRequestDto(fixtureIds))
        }.body<RoundDto>()
        return Round(dto.roundNumber, dto.matches.map { it.toDomain() })
    }

    suspend fun overrideScore(tournamentId: Int, matchId: Int, homeScore: Int, awayScore: Int, status: String): Match =
        client.patch("$API_BASE_URL/api/tournaments/$tournamentId/matches/$matchId/score") {
            contentType(ContentType.Application.Json)
            setBody(ScoreOverrideRequestDto(homeScore, awayScore, status))
        }.body<MatchDto>().toDomain()

    private fun MatchDto.toDomain() = Match(id, homeTeam, awayTeam, kickoffAt, homeScore, awayScore, status)
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.data.repository.FixtureRepositoryTest" --tests "com.balltown.predictrivals.data.repository.CurationRepositoryTest" --console=plain`
Expected: PASS, 4 tests.

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/com/balltown/predictrivals/data/dto/FixtureDto.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/domain/model/Fixture.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/data/repository/FixtureRepository.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/data/repository/CurationRepository.kt shared/src/commonTest/kotlin/com/balltown/predictrivals/data/repository/FixtureRepositoryTest.kt shared/src/commonTest/kotlin/com/balltown/predictrivals/data/repository/CurationRepositoryTest.kt
git commit -m "feat: add fixture search and curation repositories (unverified DTOs, flagged inline)"
```

---

### Task 11: Predictions + scoring + deadline gating

**Files:**
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/dto/PredictionDto.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/domain/model/Prediction.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/repository/PredictionRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/domain/scoring/ScoreCalculator.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/domain/scoring/PredictionDeadline.kt`
- Test: `shared/src/commonTest/kotlin/com/balltown/predictrivals/data/repository/PredictionRepositoryTest.kt`
- Test: `shared/src/commonTest/kotlin/com/balltown/predictrivals/domain/scoring/ScoreCalculatorTest.kt`
- Test: `shared/src/commonTest/kotlin/com/balltown/predictrivals/domain/scoring/PredictionDeadlineTest.kt`

- [ ] **Step 1: Write the failing ScoreCalculator test (pure logic, no I/O)**

```kotlin
package com.balltown.predictrivals.domain.scoring

import kotlin.test.Test
import kotlin.test.assertEquals

class ScoreCalculatorTest {

    @Test
    fun `exact score match scores 3`() {
        assertEquals(3, pointsFor(predictedHome = 2, predictedAway = 1, actualHome = 2, actualAway = 1))
    }

    @Test
    fun `correct result and correct goal difference (but not exact score) scores 2`() {
        assertEquals(2, pointsFor(predictedHome = 3, predictedAway = 1, actualHome = 2, actualAway = 0))
    }

    @Test
    fun `correct result only (wrong goal difference) scores 1`() {
        assertEquals(1, pointsFor(predictedHome = 3, predictedAway = 1, actualHome = 1, actualAway = 0))
    }

    @Test
    fun `correctly guessed draw scores 1 even with the wrong exact scoreline`() {
        assertEquals(1, pointsFor(predictedHome = 1, predictedAway = 1, actualHome = 2, actualAway = 2))
    }

    @Test
    fun `wrong result scores 0`() {
        assertEquals(0, pointsFor(predictedHome = 2, predictedAway = 0, actualHome = 0, actualAway = 1))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.domain.scoring.ScoreCalculatorTest" --console=plain`
Expected: FAIL — `pointsFor` unresolved.

- [ ] **Step 3: Write ScoreCalculator**

```kotlin
package com.balltown.predictrivals.domain.scoring

fun pointsFor(predictedHome: Int, predictedAway: Int, actualHome: Int, actualAway: Int): Int {
    if (predictedHome == actualHome && predictedAway == actualAway) return 3

    val predictedResult = result(predictedHome, predictedAway)
    val actualResult = result(actualHome, actualAway)
    if (predictedResult != actualResult) return 0

    if (predictedResult == MatchResult.DRAW) return 1 // correctly guessed draw, wrong exact scoreline

    val predictedDifference = predictedHome - predictedAway
    val actualDifference = actualHome - actualAway
    return if (predictedDifference == actualDifference) 2 else 1
}

private enum class MatchResult { HOME_WIN, AWAY_WIN, DRAW }

private fun result(home: Int, away: Int): MatchResult = when {
    home > away -> MatchResult.HOME_WIN
    home < away -> MatchResult.AWAY_WIN
    else -> MatchResult.DRAW
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.domain.scoring.ScoreCalculatorTest" --console=plain`
Expected: PASS, 5 tests.

- [ ] **Step 5: Write the failing PredictionDeadline test**

```kotlin
package com.balltown.predictrivals.domain.scoring

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PredictionDeadlineTest {

    @Test
    fun `editable before kickoff`() {
        val kickoff = Instant.parse("2026-08-20T18:00:00Z")
        val now = Instant.parse("2026-08-20T17:59:59Z")
        assertTrue(isPredictionEditable(kickoffAt = kickoff, now = now))
    }

    @Test
    fun `locked at or after kickoff`() {
        val kickoff = Instant.parse("2026-08-20T18:00:00Z")
        assertFalse(isPredictionEditable(kickoffAt = kickoff, now = kickoff))
        assertFalse(isPredictionEditable(kickoffAt = kickoff, now = Instant.parse("2026-08-20T18:00:01Z")))
    }
}
```

- [ ] **Step 6: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.domain.scoring.PredictionDeadlineTest" --console=plain`
Expected: FAIL — `isPredictionEditable` unresolved.

- [ ] **Step 7: Write PredictionDeadline**

```kotlin
package com.balltown.predictrivals.domain.scoring

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

fun isPredictionEditable(kickoffAt: Instant, now: Instant = Clock.System.now()): Boolean = now < kickoffAt
```

- [ ] **Step 8: Run test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.domain.scoring.PredictionDeadlineTest" --console=plain`
Expected: PASS, 2 tests.

- [ ] **Step 9: Write the failing PredictionRepository test**

```kotlin
package com.balltown.predictrivals.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PredictionRepositoryTest {

    @Test
    fun `submit posts matchId and both scores`() = runTest {
        val engine = MockEngine {
            respond("""{"matchId":10,"predictedHomeScore":2,"predictedAwayScore":1}""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val repository = PredictionRepository(testApiClient(engine))

        val prediction = repository.submit(matchId = 10, homeScore = 2, awayScore = 1)

        assertEquals(10, prediction.matchId)
        assertEquals(2, prediction.predictedHomeScore)
    }
}
```

- [ ] **Step 10: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.data.repository.PredictionRepositoryTest" --console=plain`
Expected: FAIL — `PredictionRepository` unresolved.

- [ ] **Step 11: Write the Prediction DTO, domain model, and repository**

```kotlin
package com.balltown.predictrivals.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class SubmitPredictionRequestDto(val matchId: Int, val homeScore: Int, val awayScore: Int) // UNVERIFIED

@Serializable
data class PredictionDto(val matchId: Int, val predictedHomeScore: Int, val predictedAwayScore: Int) // UNVERIFIED
```

```kotlin
package com.balltown.predictrivals.domain.model

data class Prediction(val matchId: Int, val predictedHomeScore: Int, val predictedAwayScore: Int)
```

```kotlin
package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.API_BASE_URL
import com.balltown.predictrivals.data.dto.PredictionDto
import com.balltown.predictrivals.data.dto.SubmitPredictionRequestDto
import com.balltown.predictrivals.domain.model.Prediction
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class PredictionRepository(private val client: HttpClient) {

    /** Same endpoint handles both first submission and edits, per the doc. */
    suspend fun submit(matchId: Int, homeScore: Int, awayScore: Int): Prediction =
        client.post("$API_BASE_URL/api/predictions") {
            contentType(ContentType.Application.Json)
            setBody(SubmitPredictionRequestDto(matchId, homeScore, awayScore))
        }.body<PredictionDto>().let { Prediction(it.matchId, it.predictedHomeScore, it.predictedAwayScore) }
}
```

- [ ] **Step 12: Run test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.data.repository.PredictionRepositoryTest" --console=plain`
Expected: PASS, 1 test.

- [ ] **Step 13: Commit**

```bash
git add shared/src/commonMain/kotlin/com/balltown/predictrivals/data/dto/PredictionDto.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/domain/model/Prediction.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/data/repository/PredictionRepository.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/domain/scoring/ shared/src/commonTest/kotlin/com/balltown/predictrivals/data/repository/PredictionRepositoryTest.kt shared/src/commonTest/kotlin/com/balltown/predictrivals/domain/scoring/
git commit -m "feat: add predictions repository, scoring calculator, and deadline gating"
```

---

### Task 12: Live (REST snapshot + WebSocket with reconnect/fallback)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/dto/LiveDto.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/domain/model/LiveSnapshot.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/data/repository/LiveRepository.kt`
- Test: `shared/src/commonTest/kotlin/com/balltown/predictrivals/data/repository/LiveRepositoryTest.kt`

The WS payload is assumed identical to the REST snapshot (doc: "same payload, pushed"), so both
paths decode into the same `LiveSnapshotDto`. `LiveRepository.observe` exposes a single
`Flow<LiveSnapshot>` that tries the socket first and falls back to 10s REST polling if the socket
never connects — callers don't need to know which transport is active.

- [ ] **Step 1: Write the failing test (REST fallback path only — a real WS integration is not unit-testable with MockEngine and is verified manually in Task 24)**

```kotlin
package com.balltown.predictrivals.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val LIVE_JSON =
    """{"matches":[{"id":1,"homeTeam":"A","awayTeam":"B","kickoffAt":"2026-08-20T18:00:00Z","homeScore":1,"awayScore":0,"status":"live"}],"standings":[{"rank":1,"userId":1,"name":"P","totalPoints":3,"exactCount":1}]}"""

class LiveRepositoryTest {

    @Test
    fun `snapshot fetches and maps matches and standings`() = runTest {
        val engine = MockEngine { respond(LIVE_JSON, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }
        val repository = LiveRepository(testApiClient(engine))

        val snapshot = repository.snapshot(tournamentId = 1)

        assertEquals(1, snapshot.matches.size)
        assertEquals(1, snapshot.standings.single().rank)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.data.repository.LiveRepositoryTest" --console=plain`
Expected: FAIL — `LiveRepository` unresolved.

- [ ] **Step 3: Write the DTO and domain model**

```kotlin
package com.balltown.predictrivals.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LiveSnapshotDto(val matches: List<MatchDto>, val standings: List<StandingDto>) // UNVERIFIED payload shape
```

```kotlin
package com.balltown.predictrivals.domain.model

data class LiveSnapshot(val matches: List<Match>, val standings: List<Standing>)
```

- [ ] **Step 4: Write LiveRepository**

```kotlin
package com.balltown.predictrivals.data.repository

import com.balltown.predictrivals.data.api.API_BASE_URL
import com.balltown.predictrivals.data.dto.LiveSnapshotDto
import com.balltown.predictrivals.data.dto.MatchDto
import com.balltown.predictrivals.data.dto.StandingDto
import com.balltown.predictrivals.domain.model.LiveSnapshot
import com.balltown.predictrivals.domain.model.Match
import com.balltown.predictrivals.domain.model.Standing
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

private val liveJson = Json { ignoreUnknownKeys = true }

class LiveRepository(private val client: HttpClient) {

    suspend fun snapshot(tournamentId: Int): LiveSnapshot =
        client.get("$API_BASE_URL/api/tournaments/$tournamentId/live").body<LiveSnapshotDto>().toDomain()

    /**
     * Pushes a [LiveSnapshot] on every WebSocket frame. If the socket can't be established after
     * [maxReconnectAttempts] tries (exponential backoff), falls back to polling [snapshot] every
     * [pollIntervalMillis] instead of giving up.
     */
    fun observe(
        tournamentId: Int,
        accessToken: String,
        maxReconnectAttempts: Int = 3,
        pollIntervalMillis: Long = 10_000,
    ): Flow<LiveSnapshot> = flow {
        var attempt = 0
        var socketSucceededOnce = false
        while (attempt < maxReconnectAttempts && !socketSucceededOnce) {
            try {
                client.webSocket(
                    method = HttpMethod.Get,
                    host = API_BASE_URL.removePrefix("https://").removePrefix("http://"),
                    path = "/ws/tournaments/$tournamentId/live",
                    request = { url.parameters.append("token", accessToken) },
                ) {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            socketSucceededOnce = true
                            emit(liveJson.decodeFromString<LiveSnapshotDto>(frame.readText()).toDomain())
                        }
                    }
                }
            } catch (e: Exception) {
                attempt++
                delay(backoffMillis(attempt))
            }
        }
        while (!socketSucceededOnce) {
            emit(snapshot(tournamentId))
            delay(pollIntervalMillis)
        }
    }.catch { emit(snapshot(tournamentId)) }

    private fun backoffMillis(attempt: Int): Long = 500L * (1 shl attempt)

    private fun LiveSnapshotDto.toDomain() = LiveSnapshot(
        matches.map { Match(it.id, it.homeTeam, it.awayTeam, it.kickoffAt, it.homeScore, it.awayScore, it.status) },
        standings.map { Standing(it.rank, it.userId, it.name, it.totalPoints, it.exactCount) },
    )
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests "com.balltown.predictrivals.data.repository.LiveRepositoryTest" --console=plain`
Expected: PASS, 1 test.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/balltown/predictrivals/data/dto/LiveDto.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/domain/model/LiveSnapshot.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/data/repository/LiveRepository.kt shared/src/commonTest/kotlin/com/balltown/predictrivals/data/repository/LiveRepositoryTest.kt
git commit -m "feat: add LiveRepository (REST snapshot + WS with reconnect/polling fallback)"
```

---

### Task 13: Koin DI wiring

**Files:**
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/di/AppModule.kt`
- Create: `shared/src/androidMain/kotlin/com/balltown/predictrivals/di/PlatformEngine.android.kt`
- Create: `shared/src/iosMain/kotlin/com/balltown/predictrivals/di/PlatformEngine.ios.kt`
- Create: `shared/src/jsMain/kotlin/com/balltown/predictrivals/di/PlatformEngine.js.kt`
- Create: `shared/src/wasmJsMain/kotlin/com/balltown/predictrivals/di/PlatformEngine.wasmJs.kt`

An `expect fun platformHttpClientEngine(): HttpClientEngine` picks the right Ktor engine per
target (OkHttp/Darwin/Js), so `AppModule` stays 100% in `commonMain`.

- [ ] **Step 1: Write the expect declaration and the module**

```kotlin
package com.balltown.predictrivals.di

import com.balltown.predictrivals.data.api.API_BASE_URL
import com.balltown.predictrivals.data.api.AuthApi
import com.balltown.predictrivals.data.api.buildApiClient
import com.balltown.predictrivals.data.repository.AuthRepository
import com.balltown.predictrivals.data.repository.CalendarRepository
import com.balltown.predictrivals.data.repository.CurationRepository
import com.balltown.predictrivals.data.repository.FixtureRepository
import com.balltown.predictrivals.data.repository.LiveRepository
import com.balltown.predictrivals.data.repository.PredictionRepository
import com.balltown.predictrivals.data.repository.StandingsRepository
import com.balltown.predictrivals.data.repository.TournamentRepository
import com.balltown.predictrivals.data.storage.TokenStore
import io.ktor.client.engine.HttpClientEngine
import org.koin.dsl.module

expect fun platformHttpClientEngine(): HttpClientEngine

val appModule = module {
    single { TokenStore() }
    single { AuthApi(platformHttpClientEngine(), API_BASE_URL) }
    single { buildApiClient(platformHttpClientEngine(), API_BASE_URL, get(), get()) }
    single { AuthRepository(get(), get()) }
    single { TournamentRepository(get()) }
    single { StandingsRepository(get()) }
    single { CalendarRepository(get()) }
    single { FixtureRepository(get()) }
    single { CurationRepository(get()) }
    single { PredictionRepository(get()) }
    single { LiveRepository(get()) }
}
```

- [ ] **Step 2: Write the per-platform engine actuals**

`shared/src/androidMain/kotlin/com/balltown/predictrivals/di/PlatformEngine.android.kt`:

```kotlin
package com.balltown.predictrivals.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual fun platformHttpClientEngine(): HttpClientEngine = OkHttp.create()
```

`shared/src/iosMain/kotlin/com/balltown/predictrivals/di/PlatformEngine.ios.kt`:

```kotlin
package com.balltown.predictrivals.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun platformHttpClientEngine(): HttpClientEngine = Darwin.create()
```

`shared/src/jsMain/kotlin/com/balltown/predictrivals/di/PlatformEngine.js.kt`:

```kotlin
package com.balltown.predictrivals.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js

actual fun platformHttpClientEngine(): HttpClientEngine = Js.create()
```

`shared/src/wasmJsMain/kotlin/com/balltown/predictrivals/di/PlatformEngine.wasmJs.kt`:

```kotlin
package com.balltown.predictrivals.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js

actual fun platformHttpClientEngine(): HttpClientEngine = Js.create()
```

- [ ] **Step 3: Verify the Android target compiles**

Run: `./gradlew :shared:compileAndroidMain --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/balltown/predictrivals/di shared/src/androidMain/kotlin/com/balltown/predictrivals/di shared/src/iosMain/kotlin/com/balltown/predictrivals/di shared/src/jsMain/kotlin/com/balltown/predictrivals/di shared/src/wasmJsMain/kotlin/com/balltown/predictrivals/di
git commit -m "feat: wire Koin DI module with per-platform Ktor engine selection"
```

---

### Task 14: Theme and navigation routes

**Files:**
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/theme/Theme.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/navigation/Routes.kt`

- [ ] **Step 1: Write the theme**

```kotlin
package com.balltown.predictrivals.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun PredictRivalsTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colorScheme, content = content)
}
```

- [ ] **Step 2: Write the routes**

```kotlin
package com.balltown.predictrivals.ui.navigation

sealed class Routes(val route: String) {
    data object Login : Routes("login")
    data object Register : Routes("register")
    data object Home : Routes("home")
    data object CreateTournament : Routes("tournament/create")
    data object JoinTournament : Routes("tournament/join")
    data object TournamentDetail : Routes("tournament/{tournamentId}") {
        fun of(tournamentId: Int) = "tournament/$tournamentId"
        const val ARG_TOURNAMENT_ID = "tournamentId"
    }
    data object Predictions : Routes("tournament/{tournamentId}/predictions") {
        fun of(tournamentId: Int) = "tournament/$tournamentId/predictions"
    }
    data object Standings : Routes("tournament/{tournamentId}/standings") {
        fun of(tournamentId: Int) = "tournament/$tournamentId/standings"
    }
    data object Calendar : Routes("tournament/{tournamentId}/calendar") {
        fun of(tournamentId: Int) = "tournament/$tournamentId/calendar"
    }
    data object Curate : Routes("tournament/{tournamentId}/curate") {
        fun of(tournamentId: Int) = "tournament/$tournamentId/curate"
    }
    data object Live : Routes("tournament/{tournamentId}/live") {
        fun of(tournamentId: Int) = "tournament/$tournamentId/live"
    }
    data object Profile : Routes("tournament/{tournamentId}/profile") {
        fun of(tournamentId: Int) = "tournament/$tournamentId/profile"
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :shared:compileAndroidMain --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/theme shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/navigation/Routes.kt
git commit -m "feat: add Material3 theme and the navigation route table"
```

---

### Task 15: Auth screens (Login, Register)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/auth/AuthViewModel.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/auth/LoginScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/auth/RegisterScreen.kt`

No unit tests for Composables in this pass (per the design doc — UI is verified by running the
app). `AuthViewModel` covers both screens since they share the same success path (store tokens,
navigate to Home) and error surface.

- [ ] **Step 1: Write AuthViewModel**

```kotlin
package com.balltown.predictrivals.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun login(email: String, password: String) = runAuthCall { authRepository.login(email, password) }

    fun register(email: String, password: String, name: String) = runAuthCall { authRepository.register(email, password, name) }

    private fun runAuthCall(call: suspend () -> Unit) {
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                call()
                AuthUiState.Success
            } catch (e: ApiException) {
                AuthUiState.Error(e.message)
            }
        }
    }
}
```

- [ ] **Step 2: Write LoginScreen**

```kotlin
package com.balltown.predictrivals.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(onLoggedIn: () -> Unit, onNavigateToRegister: () -> Unit, viewModel: AuthViewModel = koinViewModel()) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()

    if (state is AuthUiState.Success) onLoggedIn()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Predict Rivals")
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { viewModel.login(email, password) }, modifier = Modifier.fillMaxWidth(), enabled = state !is AuthUiState.Loading) {
            Text("Log in")
        }
        TextButton(onClick = onNavigateToRegister) { Text("Need an account? Register") }
        if (state is AuthUiState.Loading) CircularProgressIndicator()
        if (state is AuthUiState.Error) Text((state as AuthUiState.Error).message)
    }
}
```

- [ ] **Step 3: Write RegisterScreen**

```kotlin
package com.balltown.predictrivals.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RegisterScreen(onRegistered: () -> Unit, onNavigateToLogin: () -> Unit, viewModel: AuthViewModel = koinViewModel()) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()

    if (state is AuthUiState.Success) onRegistered()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Create your account")
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { viewModel.register(email, password, name) }, modifier = Modifier.fillMaxWidth(), enabled = state !is AuthUiState.Loading) {
            Text("Register")
        }
        TextButton(onClick = onNavigateToLogin) { Text("Already have an account? Log in") }
        if (state is AuthUiState.Loading) CircularProgressIndicator()
        if (state is AuthUiState.Error) Text((state as AuthUiState.Error).message)
    }
}
```

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew :shared:compileAndroidMain --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/auth
git commit -m "feat: add Login and Register screens"
```

---

### Task 16: Home, Create Tournament, Join Tournament screens

**Files:**
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/home/HomeViewModel.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/home/HomeScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/home/CreateTournamentScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/home/JoinTournamentScreen.kt`

- [ ] **Step 1: Write HomeViewModel**

```kotlin
package com.balltown.predictrivals.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.TournamentRepository
import com.balltown.predictrivals.domain.model.Tournament
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Loaded(val tournaments: List<Tournament>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(private val tournamentRepository: TournamentRepository) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = try {
                HomeUiState.Loaded(tournamentRepository.mine())
            } catch (e: ApiException) {
                HomeUiState.Error(e.message)
            }
        }
    }
}
```

- [ ] **Step 2: Write HomeScreen**

```kotlin
package com.balltown.predictrivals.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.balltown.predictrivals.domain.model.Tournament
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    onOpenTournament: (Tournament) -> Unit,
    onCreateTournament: () -> Unit,
    onJoinTournament: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCreateTournament) { Text("Create") }
            Button(onClick = onJoinTournament) { Text("Join") }
        }
        when (val current = state) {
            is HomeUiState.Loading -> CircularProgressIndicator()
            is HomeUiState.Error -> Text(current.message)
            is HomeUiState.Loaded -> LazyColumn {
                items(current.tournaments) { tournament ->
                    ListItem(
                        headlineContent = { Text(tournament.name) },
                        supportingContent = { Text("${tournament.playerCount}/${tournament.playerLimit} players · ${tournament.status}") },
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}
```

Note: `ListItem` above needs a click handler to actually call `onOpenTournament` — add
`Modifier.clickable { onOpenTournament(tournament) }` from `androidx.compose.foundation.clickable`
to the `ListItem`'s modifier in this same step (it's part of this file, not a follow-up).

- [ ] **Step 3: Write CreateTournamentScreen**

```kotlin
package com.balltown.predictrivals.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.TournamentRepository
import com.balltown.predictrivals.domain.model.Tournament
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

sealed class CreateTournamentUiState {
    data object Idle : CreateTournamentUiState()
    data object Loading : CreateTournamentUiState()
    data class Created(val tournament: Tournament) : CreateTournamentUiState()
    data class Error(val message: String) : CreateTournamentUiState()
}

class CreateTournamentViewModel(private val tournamentRepository: TournamentRepository) : ViewModel() {
    private val _state = MutableStateFlow<CreateTournamentUiState>(CreateTournamentUiState.Idle)
    val state: StateFlow<CreateTournamentUiState> = _state.asStateFlow()

    fun create(name: String, playerLimit: Int) {
        _state.value = CreateTournamentUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                CreateTournamentUiState.Created(tournamentRepository.create(name, playerLimit))
            } catch (e: ApiException) {
                CreateTournamentUiState.Error(e.message)
            }
        }
    }
}

@Composable
fun CreateTournamentScreen(onCreated: (Tournament) -> Unit, viewModel: CreateTournamentViewModel = koinViewModel()) {
    var name by remember { mutableStateOf("") }
    var playerLimit by remember { mutableStateOf("10") }
    val state by viewModel.state.collectAsState()

    (state as? CreateTournamentUiState.Created)?.let { onCreated(it.tournament) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("New tournament")
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = playerLimit,
            onValueChange = { playerLimit = it.filter(Char::isDigit) },
            label = { Text("Player limit (2-50)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { playerLimit.toIntOrNull()?.let { viewModel.create(name, it) } },
            enabled = state !is CreateTournamentUiState.Loading,
        ) { Text("Create") }
        if (state is CreateTournamentUiState.Loading) CircularProgressIndicator()
        if (state is CreateTournamentUiState.Error) Text((state as CreateTournamentUiState.Error).message)
    }
}
```

- [ ] **Step 4: Write JoinTournamentScreen**

```kotlin
package com.balltown.predictrivals.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.TournamentRepository
import com.balltown.predictrivals.domain.model.Tournament
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

sealed class JoinTournamentUiState {
    data object Idle : JoinTournamentUiState()
    data object Loading : JoinTournamentUiState()
    data class Joined(val tournament: Tournament) : JoinTournamentUiState()
    data class Error(val message: String) : JoinTournamentUiState()
}

class JoinTournamentViewModel(private val tournamentRepository: TournamentRepository) : ViewModel() {
    private val _state = MutableStateFlow<JoinTournamentUiState>(JoinTournamentUiState.Idle)
    val state: StateFlow<JoinTournamentUiState> = _state.asStateFlow()

    fun join(joinCode: String) {
        _state.value = JoinTournamentUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                JoinTournamentUiState.Joined(tournamentRepository.join(joinCode))
            } catch (e: ApiException) {
                JoinTournamentUiState.Error(e.message)
            }
        }
    }
}

@Composable
fun JoinTournamentScreen(onJoined: (Tournament) -> Unit, viewModel: JoinTournamentViewModel = koinViewModel()) {
    var joinCode by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()

    (state as? JoinTournamentUiState.Joined)?.let { onJoined(it.tournament) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Join a tournament")
        OutlinedTextField(value = joinCode, onValueChange = { joinCode = it.uppercase() }, label = { Text("Join code") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { viewModel.join(joinCode) }, enabled = state !is JoinTournamentUiState.Loading) { Text("Join") }
        if (state is JoinTournamentUiState.Loading) CircularProgressIndicator()
        if (state is JoinTournamentUiState.Error) Text((state as JoinTournamentUiState.Error).message)
    }
}
```

- [ ] **Step 5: Verify it compiles**

Run: `./gradlew :shared:compileAndroidMain --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/home
git commit -m "feat: add Home, Create Tournament, and Join Tournament screens"
```

---

### Task 17: Tournament Detail shell (membership/owner gating + tabs)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/TournamentDetailViewModel.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/TournamentDetailScreen.kt`

Per the doc's client-side rules: any 403 on a tournament-scoped call means "not a member" — this
screen loads the tournament via the no-membership-required `GET /tournaments/{id}` first, and only
then decides whether to show member content or a "join to continue" prompt, rather than waiting
for a 403 from a different endpoint.

- [ ] **Step 1: Write TournamentDetailViewModel**

```kotlin
package com.balltown.predictrivals.ui.tournament

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.TournamentRepository
import com.balltown.predictrivals.domain.model.Tournament
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class TournamentDetailUiState {
    data object Loading : TournamentDetailUiState()
    data class Loaded(val tournament: Tournament, val currentUserId: Int) : TournamentDetailUiState()
    data class Error(val message: String) : TournamentDetailUiState()
}

class TournamentDetailViewModel(
    private val tournamentRepository: TournamentRepository,
    private val currentUserId: Int,
) : ViewModel() {
    private val _state = MutableStateFlow<TournamentDetailUiState>(TournamentDetailUiState.Loading)
    val state: StateFlow<TournamentDetailUiState> = _state.asStateFlow()

    fun load(tournamentId: Int) {
        viewModelScope.launch {
            _state.value = try {
                TournamentDetailUiState.Loaded(tournamentRepository.get(tournamentId), currentUserId)
            } catch (e: ApiException) {
                TournamentDetailUiState.Error(e.message)
            }
        }
    }

    fun startEarly(tournamentId: Int) {
        viewModelScope.launch {
            _state.value = try {
                TournamentDetailUiState.Loaded(tournamentRepository.start(tournamentId), currentUserId)
            } catch (e: ApiException) {
                TournamentDetailUiState.Error(e.message)
            }
        }
    }
}
```

`currentUserId` is a constructor parameter rather than pulled from a global — Task 21 wires it
from the value returned at login/register (stored alongside the token pair; see Task 21 Step 1
for where that's persisted).

- [ ] **Step 2: Write TournamentDetailScreen**

```kotlin
package com.balltown.predictrivals.ui.tournament

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.balltown.predictrivals.domain.model.Tournament
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TournamentDetailScreen(
    tournamentId: Int,
    onOpenPredictions: (Tournament) -> Unit,
    onOpenStandings: (Tournament) -> Unit,
    onOpenCalendar: (Tournament) -> Unit,
    onOpenCurate: (Tournament) -> Unit,
    onOpenLive: (Tournament) -> Unit,
    onOpenProfile: (Tournament) -> Unit,
    viewModel: TournamentDetailViewModel = koinViewModel(),
) {
    LaunchedEffect(tournamentId) { viewModel.load(tournamentId) }
    val state by viewModel.state.collectAsState()

    when (val current = state) {
        is TournamentDetailUiState.Loading -> CircularProgressIndicator()
        is TournamentDetailUiState.Error -> Text(current.message)
        is TournamentDetailUiState.Loaded -> {
            val tournament = current.tournament
            val isOwner = tournament.isOwnedBy(current.currentUserId)
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(tournament.name)
                Text("${tournament.playerCount}/${tournament.playerLimit} players · ${tournament.status}")
                Button(onClick = { onOpenPredictions(tournament) }) { Text("Predictions") }
                Button(onClick = { onOpenStandings(tournament) }) { Text("Standings") }
                Button(onClick = { onOpenCalendar(tournament) }) { Text("Calendar") }
                Button(onClick = { onOpenLive(tournament) }) { Text("Live") }
                Button(onClick = { onOpenProfile(tournament) }) { Text("Profile") }
                if (isOwner && tournament.isOpen) {
                    Button(onClick = { viewModel.startEarly(tournament.id) }) { Text("Start early") }
                }
                if (isOwner && tournament.isActive) {
                    Button(onClick = { onOpenCurate(tournament) }) { Text("Curate matches") }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :shared:compileAndroidMain --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/TournamentDetailViewModel.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/TournamentDetailScreen.kt
git commit -m "feat: add Tournament Detail shell with owner-gated actions"
```

---

### Task 18: Standings and Calendar screens

**Files:**
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/standings/StandingsViewModel.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/standings/StandingsScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/calendar/CalendarViewModel.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/calendar/CalendarScreen.kt`

- [ ] **Step 1: Write StandingsViewModel and StandingsScreen**

```kotlin
package com.balltown.predictrivals.ui.tournament.standings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.StandingsRepository
import com.balltown.predictrivals.domain.model.Standing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class StandingsUiState {
    data object Loading : StandingsUiState()
    data class Loaded(val standings: List<Standing>, val showingTopScorers: Boolean) : StandingsUiState()
    data class Error(val message: String) : StandingsUiState()
}

class StandingsViewModel(private val standingsRepository: StandingsRepository) : ViewModel() {
    private val _state = MutableStateFlow<StandingsUiState>(StandingsUiState.Loading)
    val state: StateFlow<StandingsUiState> = _state.asStateFlow()

    fun load(tournamentId: Int, topScorers: Boolean = false) {
        viewModelScope.launch {
            _state.value = try {
                val standings = if (topScorers) standingsRepository.topScorers(tournamentId) else standingsRepository.standings(tournamentId)
                StandingsUiState.Loaded(standings, topScorers)
            } catch (e: ApiException) {
                StandingsUiState.Error(e.message)
            }
        }
    }
}
```

```kotlin
package com.balltown.predictrivals.ui.tournament.standings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StandingsScreen(tournamentId: Int, viewModel: StandingsViewModel = koinViewModel()) {
    LaunchedEffect(tournamentId) { viewModel.load(tournamentId) }
    val state by viewModel.state.collectAsState()

    when (val current = state) {
        is StandingsUiState.Loading -> CircularProgressIndicator()
        is StandingsUiState.Error -> Text(current.message)
        is StandingsUiState.Loaded -> Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row {
                Text("Top scorers")
                Switch(checked = current.showingTopScorers, onCheckedChange = { viewModel.load(tournamentId, topScorers = it) })
            }
            LazyColumn {
                items(current.standings) { standing ->
                    ListItem(
                        headlineContent = { Text("#${standing.rank} ${standing.name}") },
                        supportingContent = { Text("${standing.totalPoints} pts · ${standing.exactCount} exact") },
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}
```

Add the missing `Row` import (`androidx.compose.foundation.layout.Row`) alongside the others in
this same step.

- [ ] **Step 2: Write CalendarViewModel and CalendarScreen**

```kotlin
package com.balltown.predictrivals.ui.tournament.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.CalendarRepository
import com.balltown.predictrivals.domain.model.Round
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CalendarUiState {
    data object Loading : CalendarUiState()
    data class Loaded(val rounds: List<Round>) : CalendarUiState()
    data class Error(val message: String) : CalendarUiState()
}

class CalendarViewModel(private val calendarRepository: CalendarRepository) : ViewModel() {
    private val _state = MutableStateFlow<CalendarUiState>(CalendarUiState.Loading)
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    fun load(tournamentId: Int) {
        viewModelScope.launch {
            _state.value = try {
                CalendarUiState.Loaded(calendarRepository.calendar(tournamentId))
            } catch (e: ApiException) {
                CalendarUiState.Error(e.message)
            }
        }
    }
}
```

```kotlin
package com.balltown.predictrivals.ui.tournament.calendar

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CalendarScreen(tournamentId: Int, viewModel: CalendarViewModel = koinViewModel()) {
    LaunchedEffect(tournamentId) { viewModel.load(tournamentId) }
    val state by viewModel.state.collectAsState()

    when (val current = state) {
        is CalendarUiState.Loading -> CircularProgressIndicator()
        is CalendarUiState.Error -> Text(current.message)
        is CalendarUiState.Loaded -> if (current.rounds.isEmpty()) {
            Text("No rounds yet.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                current.rounds.forEach { round ->
                    items(round.matches) { match ->
                        ListItem(
                            headlineContent = { Text("${match.homeTeam} vs ${match.awayTeam}") },
                            supportingContent = { Text("Round ${round.roundNumber} · ${match.status}") },
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :shared:compileAndroidMain --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/standings shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/calendar
git commit -m "feat: add Standings (with top-scorers toggle) and Calendar screens"
```

---

### Task 19: Predictions screen (kickoff-deadline gating)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/predictions/PredictionsViewModel.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/predictions/PredictionsScreen.kt`

- [ ] **Step 1: Write PredictionsViewModel**

```kotlin
package com.balltown.predictrivals.ui.tournament.predictions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.CalendarRepository
import com.balltown.predictrivals.data.repository.PredictionRepository
import com.balltown.predictrivals.domain.model.Round
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PredictionsUiState {
    data object Loading : PredictionsUiState()
    data class Loaded(val round: Round?) : PredictionsUiState()
    data class Error(val message: String) : PredictionsUiState()
}

class PredictionsViewModel(
    private val calendarRepository: CalendarRepository,
    private val predictionRepository: PredictionRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<PredictionsUiState>(PredictionsUiState.Loading)
    val state: StateFlow<PredictionsUiState> = _state.asStateFlow()

    fun load(tournamentId: Int) {
        viewModelScope.launch {
            _state.value = try {
                PredictionsUiState.Loaded(calendarRepository.currentRound(tournamentId))
            } catch (e: ApiException) {
                PredictionsUiState.Error(e.message)
            }
        }
    }

    fun submit(matchId: Int, homeScore: Int, awayScore: Int) {
        viewModelScope.launch {
            try {
                predictionRepository.submit(matchId, homeScore, awayScore)
            } catch (e: ApiException) {
                _state.value = PredictionsUiState.Error(e.message)
            }
        }
    }
}
```

- [ ] **Step 2: Write PredictionsScreen**

```kotlin
package com.balltown.predictrivals.ui.tournament.predictions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.balltown.predictrivals.domain.model.Match
import com.balltown.predictrivals.domain.scoring.isPredictionEditable
import kotlinx.datetime.Instant
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PredictionsScreen(tournamentId: Int, viewModel: PredictionsViewModel = koinViewModel()) {
    LaunchedEffect(tournamentId) { viewModel.load(tournamentId) }
    val state by viewModel.state.collectAsState()

    when (val current = state) {
        is PredictionsUiState.Loading -> CircularProgressIndicator()
        is PredictionsUiState.Error -> Text(current.message)
        is PredictionsUiState.Loaded -> if (current.round == null) {
            Text("No round has been set up yet.")
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                current.round.matches.forEach { match ->
                    MatchPredictionRow(match, onSubmit = { home, away -> viewModel.submit(match.id, home, away) })
                }
            }
        }
    }
}

@Composable
private fun MatchPredictionRow(match: Match, onSubmit: (Int, Int) -> Unit) {
    var home by remember { mutableStateOf("") }
    var away by remember { mutableStateOf("") }
    val editable = isPredictionEditable(kickoffAt = Instant.parse(match.kickoffAt))

    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("${match.homeTeam} vs ${match.awayTeam}", modifier = Modifier.padding(end = 8.dp))
        OutlinedTextField(
            value = home,
            onValueChange = { home = it.filter(Char::isDigit) },
            enabled = editable,
            modifier = Modifier.padding(end = 4.dp),
        )
        OutlinedTextField(value = away, onValueChange = { away = it.filter(Char::isDigit) }, enabled = editable)
        Button(
            onClick = { home.toIntOrNull()?.let { h -> away.toIntOrNull()?.let { a -> onSubmit(h, a) } } },
            enabled = editable,
        ) { Text("Save") }
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :shared:compileAndroidMain --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/predictions
git commit -m "feat: add Predictions screen with kickoff-deadline gating"
```

---

### Task 20: Curate screen (owner-only fixture search, round creation, score override) and Live screen

**Files:**
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/curate/CurateViewModel.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/curate/CurateScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/live/LiveViewModel.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/live/LiveScreen.kt`

- [ ] **Step 1: Write CurateViewModel**

```kotlin
package com.balltown.predictrivals.ui.tournament.curate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.CurationRepository
import com.balltown.predictrivals.data.repository.FixtureRepository
import com.balltown.predictrivals.domain.model.Fixture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CurateUiState {
    data object Idle : CurateUiState()
    data object Loading : CurateUiState()
    data class CandidatesLoaded(val candidates: List<Fixture>, val selected: Set<Int>) : CurateUiState()
    data object RoundCreated : CurateUiState()
    data class Error(val message: String) : CurateUiState()
}

class CurateViewModel(
    private val fixtureRepository: FixtureRepository,
    private val curationRepository: CurationRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<CurateUiState>(CurateUiState.Idle)
    val state: StateFlow<CurateUiState> = _state.asStateFlow()

    fun search(from: String, to: String) {
        _state.value = CurateUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                CurateUiState.CandidatesLoaded(fixtureRepository.candidates(from, to, search = null), selected = emptySet())
            } catch (e: ApiException) {
                CurateUiState.Error(e.message)
            }
        }
    }

    fun toggleSelected(fixtureId: Int) {
        val current = _state.value as? CurateUiState.CandidatesLoaded ?: return
        val newSelection = if (fixtureId in current.selected) current.selected - fixtureId else current.selected + fixtureId
        _state.value = current.copy(selected = newSelection)
    }

    fun createRound(tournamentId: Int) {
        val current = _state.value as? CurateUiState.CandidatesLoaded ?: return
        if (current.selected.size != 9) {
            _state.value = CurateUiState.Error("Pick exactly 9 fixtures (currently ${current.selected.size}).")
            return
        }
        viewModelScope.launch {
            _state.value = try {
                curationRepository.createRound(tournamentId, current.selected.toList())
                CurateUiState.RoundCreated
            } catch (e: ApiException) {
                CurateUiState.Error(e.message)
            }
        }
    }
}
```

- [ ] **Step 2: Write CurateScreen**

```kotlin
package com.balltown.predictrivals.ui.tournament.curate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CurateScreen(tournamentId: Int, from: String, to: String, onRoundCreated: () -> Unit, viewModel: CurateViewModel = koinViewModel()) {
    LaunchedEffect(tournamentId) { viewModel.search(from, to) }
    val state by viewModel.state.collectAsState()

    if (state is CurateUiState.RoundCreated) onRoundCreated()

    when (val current = state) {
        is CurateUiState.Idle, is CurateUiState.Loading -> CircularProgressIndicator()
        is CurateUiState.Error -> Text(current.message)
        is CurateUiState.RoundCreated -> Text("Round created.")
        is CurateUiState.CandidatesLoaded -> Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Pick exactly 9 fixtures (${current.selected.size}/9)")
            if (current.candidates.isEmpty()) {
                Text("No fixtures available for this date range yet.")
            }
            LazyColumn {
                items(current.candidates) { fixture ->
                    Row(modifier = Modifier.clickable { viewModel.toggleSelected(fixture.id) }.padding(vertical = 4.dp)) {
                        Checkbox(checked = fixture.id in current.selected, onCheckedChange = { viewModel.toggleSelected(fixture.id) })
                        Text("${fixture.homeTeam} vs ${fixture.awayTeam} (${fixture.competition})")
                    }
                }
            }
            Button(onClick = { viewModel.createRound(tournamentId) }, enabled = current.selected.size == 9) {
                Text("Create round")
            }
        }
    }
}
```

- [ ] **Step 3: Write LiveViewModel**

```kotlin
package com.balltown.predictrivals.ui.tournament.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.repository.LiveRepository
import com.balltown.predictrivals.data.storage.TokenStore
import com.balltown.predictrivals.domain.model.LiveSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LiveViewModel(
    private val liveRepository: LiveRepository,
    private val tokenStore: TokenStore,
) : ViewModel() {
    private val _snapshot = MutableStateFlow<LiveSnapshot?>(null)
    val snapshot: StateFlow<LiveSnapshot?> = _snapshot.asStateFlow()

    fun observe(tournamentId: Int) {
        val accessToken = tokenStore.load()?.accessToken ?: return
        viewModelScope.launch {
            liveRepository.observe(tournamentId, accessToken).collect { _snapshot.value = it }
        }
    }
}
```

- [ ] **Step 4: Write LiveScreen**

```kotlin
package com.balltown.predictrivals.ui.tournament.live

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LiveScreen(tournamentId: Int, viewModel: LiveViewModel = koinViewModel()) {
    LaunchedEffect(tournamentId) { viewModel.observe(tournamentId) }
    val snapshot by viewModel.snapshot.collectAsState()

    val current = snapshot
    if (current == null) {
        CircularProgressIndicator()
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            current.matches.forEach { match ->
                Text("${match.homeTeam} ${match.homeScore ?: "-"} : ${match.awayScore ?: "-"} ${match.awayTeam} (${match.status})")
            }
            Text("Standings")
            current.standings.forEach { standing -> Text("#${standing.rank} ${standing.name}: ${standing.totalPoints}") }
        }
    }
}
```

- [ ] **Step 5: Verify it compiles**

Run: `./gradlew :shared:compileAndroidMain --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/curate shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/tournament/live
git commit -m "feat: add owner-only Curate screen and Live screen (WS/REST)"
```

---

### Task 21: Profile screen, app entry point wiring, and Koin start

**Files:**
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/profile/ProfileViewModel.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/profile/ProfileScreen.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/navigation/NavGraph.kt`
- Modify: `shared/src/commonMain/kotlin/com/balltown/predictrivals/App.kt`
- Modify: `androidApp/src/main/kotlin/com/balltown/predictrivals/MainActivity.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/di/SessionModule.kt`
- Create: `shared/src/commonMain/kotlin/com/balltown/predictrivals/di/ViewModelModule.kt`
- Modify: `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/auth/AuthViewModel.kt`

This is where `currentUserId` (needed by `TournamentDetailViewModel` per Task 17) gets a real
source: a tiny in-memory `SessionStore` set on successful login/register and read by DI.

- [ ] **Step 1: Write SessionStore and register it with Koin**

```kotlin
package com.balltown.predictrivals.di

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.dsl.module

/** Holds the signed-in user's id for the lifetime of the process; cleared on logout. */
class SessionStore {
    private val _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId: StateFlow<Int?> = _currentUserId.asStateFlow()
    fun set(userId: Int) { _currentUserId.value = userId }
    fun clear() { _currentUserId.value = null }
}

val sessionModule = module {
    single { SessionStore() }
}
```

- [ ] **Step 2: Have AuthViewModel populate SessionStore on success**

Modify `shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/auth/AuthViewModel.kt`: add a
`sessionStore: SessionStore` constructor parameter, and change `AuthRepository.login`/`register`
return type usage so the resulting `User.id` is passed to `sessionStore.set(...)` before setting
`AuthUiState.Success`. Concretely:

```kotlin
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val sessionStore: com.balltown.predictrivals.di.SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun login(email: String, password: String) = runAuthCall { authRepository.login(email, password) }

    fun register(email: String, password: String, name: String) = runAuthCall { authRepository.register(email, password, name) }

    private fun runAuthCall(call: suspend () -> com.balltown.predictrivals.domain.model.User) {
        _state.value = AuthUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                val user = call()
                sessionStore.set(user.id)
                AuthUiState.Success
            } catch (e: ApiException) {
                AuthUiState.Error(e.message)
            }
        }
    }
}
```

- [ ] **Step 3: Create ViewModelModule.kt — nothing registered any ViewModel with Koin until now**

Every screen in Tasks 15-20 calls `koinViewModel()`, but no task so far has registered a single
`viewModel { ... }` definition — `appModule` (Task 13) only holds repositories/API plumbing. Without
this module, every one of those `koinViewModel()` calls throws `NoDefinitionFoundException` at
runtime. Create `shared/src/commonMain/kotlin/com/balltown/predictrivals/di/ViewModelModule.kt`:

```kotlin
package com.balltown.predictrivals.di

import com.balltown.predictrivals.ui.auth.AuthViewModel
import com.balltown.predictrivals.ui.home.CreateTournamentViewModel
import com.balltown.predictrivals.ui.home.HomeViewModel
import com.balltown.predictrivals.ui.home.JoinTournamentViewModel
import com.balltown.predictrivals.ui.profile.ProfileViewModel
import com.balltown.predictrivals.ui.tournament.TournamentDetailViewModel
import com.balltown.predictrivals.ui.tournament.calendar.CalendarViewModel
import com.balltown.predictrivals.ui.tournament.curate.CurateViewModel
import com.balltown.predictrivals.ui.tournament.live.LiveViewModel
import com.balltown.predictrivals.ui.tournament.predictions.PredictionsViewModel
import com.balltown.predictrivals.ui.tournament.standings.StandingsViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { AuthViewModel(get(), get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { CreateTournamentViewModel(get()) }
    viewModel { JoinTournamentViewModel(get()) }
    // currentUserId comes from SessionStore, not a nav argument — TournamentDetailViewModel's
    // constructor has no use for tournamentId, so this needs no parametersOf() at the call site.
    viewModel { TournamentDetailViewModel(get(), get<SessionStore>().currentUserId.value ?: -1) }
    viewModel { StandingsViewModel(get()) }
    viewModel { CalendarViewModel(get()) }
    viewModel { PredictionsViewModel(get(), get()) }
    viewModel { CurateViewModel(get(), get()) }
    viewModel { LiveViewModel(get(), get()) }
    viewModel { ProfileViewModel(get(), get(), get()) }
}
```

Every `viewModel { get(), get() }` call's argument count must match that ViewModel's constructor
exactly — cross-check against the class as written in its own task before moving on:
`AuthViewModel(authRepository, sessionStore)`, `HomeViewModel(tournamentRepository)`,
`CreateTournamentViewModel(tournamentRepository)`, `JoinTournamentViewModel(tournamentRepository)`,
`TournamentDetailViewModel(tournamentRepository, currentUserId: Int)`,
`StandingsViewModel(standingsRepository)`, `CalendarViewModel(calendarRepository)`,
`PredictionsViewModel(calendarRepository, predictionRepository)`,
`CurateViewModel(fixtureRepository, curationRepository)`,
`LiveViewModel(liveRepository, tokenStore)`,
`ProfileViewModel(standingsRepository, authRepository, sessionStore)`.

No screen needs a Koin runtime parameter (`parametersOf`) — every screen that needs a
`tournamentId` gets it as a plain function argument from `NavGraph.kt` and passes it to
`viewModel.load(tournamentId)` from a `LaunchedEffect`, exactly as already written in Tasks 17-20.
`TournamentDetailScreen.kt` from Task 17 needs no changes here.

- [ ] **Step 4: Write ProfileViewModel and ProfileScreen**

```kotlin
package com.balltown.predictrivals.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balltown.predictrivals.data.api.ApiException
import com.balltown.predictrivals.data.repository.AuthRepository
import com.balltown.predictrivals.data.repository.StandingsRepository
import com.balltown.predictrivals.di.SessionStore
import com.balltown.predictrivals.domain.model.UserStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Loaded(val stats: UserStats) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(
    private val standingsRepository: StandingsRepository,
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {
    private val _state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun load(tournamentId: Int) {
        val userId = sessionStore.currentUserId.value ?: return
        viewModelScope.launch {
            _state.value = try {
                ProfileUiState.Loaded(standingsRepository.userStats(tournamentId, userId))
            } catch (e: ApiException) {
                ProfileUiState.Error(e.message)
            }
        }
    }

    fun logout() {
        authRepository.logout()
        sessionStore.clear()
    }
}
```

```kotlin
package com.balltown.predictrivals.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(tournamentId: Int, onLoggedOut: () -> Unit, viewModel: ProfileViewModel = koinViewModel()) {
    LaunchedEffect(tournamentId) { viewModel.load(tournamentId) }
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when (val current = state) {
            is ProfileUiState.Loading -> CircularProgressIndicator()
            is ProfileUiState.Error -> Text(current.message)
            is ProfileUiState.Loaded -> {
                Text("${current.stats.totalPoints} points · ${current.stats.exactCount} exact")
                Text("${current.stats.scoredPredictions}/${current.stats.totalPredictions} predictions scored")
                Text("Accuracy: ${current.stats.accuracy}")
            }
        }
        Button(onClick = { viewModel.logout(); onLoggedOut() }) { Text("Log out") }
    }
}
```

- [ ] **Step 5: Write NavGraph**

```kotlin
package com.balltown.predictrivals.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.balltown.predictrivals.ui.auth.LoginScreen
import com.balltown.predictrivals.ui.auth.RegisterScreen
import com.balltown.predictrivals.ui.home.CreateTournamentScreen
import com.balltown.predictrivals.ui.home.HomeScreen
import com.balltown.predictrivals.ui.home.JoinTournamentScreen
import com.balltown.predictrivals.ui.tournament.TournamentDetailScreen
import com.balltown.predictrivals.ui.tournament.calendar.CalendarScreen
import com.balltown.predictrivals.ui.tournament.curate.CurateScreen
import com.balltown.predictrivals.ui.tournament.live.LiveScreen
import com.balltown.predictrivals.ui.tournament.predictions.PredictionsScreen
import com.balltown.predictrivals.ui.tournament.standings.StandingsScreen
import com.balltown.predictrivals.ui.profile.ProfileScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.Login.route) {
        composable(Routes.Login.route) {
            LoginScreen(
                onLoggedIn = { navController.navigate(Routes.Home.route) { popUpTo(Routes.Login.route) { inclusive = true } } },
                onNavigateToRegister = { navController.navigate(Routes.Register.route) },
            )
        }
        composable(Routes.Register.route) {
            RegisterScreen(
                onRegistered = { navController.navigate(Routes.Home.route) { popUpTo(Routes.Login.route) { inclusive = true } } },
                onNavigateToLogin = { navController.popBackStack() },
            )
        }
        composable(Routes.Home.route) {
            HomeScreen(
                onOpenTournament = { navController.navigate(Routes.TournamentDetail.of(it.id)) },
                onCreateTournament = { navController.navigate(Routes.CreateTournament.route) },
                onJoinTournament = { navController.navigate(Routes.JoinTournament.route) },
            )
        }
        composable(Routes.CreateTournament.route) {
            CreateTournamentScreen(onCreated = { navController.navigate(Routes.TournamentDetail.of(it.id)) { popUpTo(Routes.Home.route) } })
        }
        composable(Routes.JoinTournament.route) {
            JoinTournamentScreen(onJoined = { navController.navigate(Routes.TournamentDetail.of(it.id)) { popUpTo(Routes.Home.route) } })
        }
        composable(Routes.TournamentDetail.route, arguments = listOf(navArgument(Routes.TournamentDetail.ARG_TOURNAMENT_ID) { type = NavType.IntType })) { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getInt(Routes.TournamentDetail.ARG_TOURNAMENT_ID) ?: return@composable
            TournamentDetailScreen(
                tournamentId = tournamentId,
                onOpenPredictions = { navController.navigate(Routes.Predictions.of(it.id)) },
                onOpenStandings = { navController.navigate(Routes.Standings.of(it.id)) },
                onOpenCalendar = { navController.navigate(Routes.Calendar.of(it.id)) },
                onOpenCurate = { navController.navigate(Routes.Curate.of(it.id)) },
                onOpenLive = { navController.navigate(Routes.Live.of(it.id)) },
                onOpenProfile = { navController.navigate(Routes.Profile.of(it.id)) },
            )
        }
        composable(Routes.Predictions.route) { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getString(Routes.TournamentDetail.ARG_TOURNAMENT_ID)?.toIntOrNull() ?: return@composable
            PredictionsScreen(tournamentId)
        }
        composable(Routes.Standings.route) { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getString(Routes.TournamentDetail.ARG_TOURNAMENT_ID)?.toIntOrNull() ?: return@composable
            StandingsScreen(tournamentId)
        }
        composable(Routes.Calendar.route) { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getString(Routes.TournamentDetail.ARG_TOURNAMENT_ID)?.toIntOrNull() ?: return@composable
            CalendarScreen(tournamentId)
        }
        composable(Routes.Curate.route) { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getString(Routes.TournamentDetail.ARG_TOURNAMENT_ID)?.toIntOrNull() ?: return@composable
            CurateScreen(tournamentId, from = "2026-08-17", to = "2026-09-17", onRoundCreated = { navController.popBackStack() })
        }
        composable(Routes.Live.route) { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getString(Routes.TournamentDetail.ARG_TOURNAMENT_ID)?.toIntOrNull() ?: return@composable
            LiveScreen(tournamentId)
        }
        composable(Routes.Profile.route) { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getString(Routes.TournamentDetail.ARG_TOURNAMENT_ID)?.toIntOrNull() ?: return@composable
            ProfileScreen(tournamentId, onLoggedOut = { navController.navigate(Routes.Login.route) { popUpTo(0) } })
        }
    }
}
```

- [ ] **Step 6: Rewrite App.kt**

```kotlin
package com.balltown.predictrivals

import androidx.compose.runtime.Composable
import com.balltown.predictrivals.di.appModule
import com.balltown.predictrivals.di.sessionModule
import com.balltown.predictrivals.di.viewModelModule
import com.balltown.predictrivals.ui.navigation.NavGraph
import com.balltown.predictrivals.ui.theme.PredictRivalsTheme
import org.koin.compose.KoinApplication

@Composable
fun App() {
    KoinApplication(application = { modules(appModule, sessionModule, viewModelModule) }) {
        PredictRivalsTheme {
            NavGraph()
        }
    }
}
```

- [ ] **Step 7: Simplify MainActivity**

```kotlin
package com.balltown.predictrivals

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}
```

- [ ] **Step 8: Verify the full Android app builds**

Run: `./gradlew :androidApp:assembleDebug --console=plain`
Expected: `BUILD SUCCESSFUL`. If any `koinViewModel()` call throws `NoDefinitionFoundException` at
runtime (not a compile error — this surfaces when you actually navigate to that screen), check
that the corresponding `viewModel { ... }` line was added to `viewModelModule` in Step 3 and that
`viewModelModule` is passed to `modules(...)` in Step 6 — that's the one place this whole wiring
can silently go missing.

- [ ] **Step 9: Commit**

```bash
git add shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/profile shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/navigation/NavGraph.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/App.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/di/SessionModule.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/di/ViewModelModule.kt shared/src/commonMain/kotlin/com/balltown/predictrivals/ui/auth/AuthViewModel.kt androidApp/src/main/kotlin/com/balltown/predictrivals/MainActivity.kt
git commit -m "feat: wire NavGraph, SessionStore, and Koin start into the app entry point"
```

---

### Task 22: Manual end-to-end verification on Android

**Files:** none (verification only)

- [ ] **Step 1: Install and launch on an emulator/device**

Run: `./gradlew :androidApp:installDebug --console=plain`

Launch the app (via the emulator or `adb shell am start -n com.balltown.predictrivals/.MainActivity`).

- [ ] **Step 2: Walk the golden path**

1. Register a new account (real email format, e.g. `you+test1@example.com`) — expect landing on Home with an empty tournament list.
2. Create a tournament (name + player limit) — expect navigation to its Detail screen showing `open` status and a join code.
3. Log out, register a second account, log back into the first — confirm both accounts persist independently (tests TokenStore + Koin singleton scoping).
4. From the second account, join the first tournament by its code — expect success and the tournament appearing in that account's Home list.
5. As the owner, tap "Start early" — expect status to flip to `active` and the "Curate matches" button to appear.
6. Open Curate — expect it to load without crashing and show "No fixtures available" (the known fixtures-search gap), rather than erroring.
7. Open Standings, Calendar, Live, Profile for the tournament — expect each to load without crashing; Calendar/Predictions should show "no round yet" states; Live should settle into REST polling within a few seconds (watch Logcat for the WS connect/backoff/fallback sequence).
8. Force-quit and relaunch the app — expect it to land back on Home already logged in (TokenStore persisted across process death), not back on Login.

- [ ] **Step 3: Record any deviation as a follow-up, not a blocker**

Anything that 500s or errors unexpectedly here is either the known backend generic-500 behavior
(§2 of the design doc) or a genuine client bug — check Logcat for which, and only fix the latter
in this pass. Fixture search, round creation, predictions, and the WS payload staying unverified
end-to-end is expected and already flagged; this step confirms the *rest* of the app — auth,
tournament lifecycle, membership gating, standings, stats, calendar states, live fallback,
navigation, and session persistence — actually works against the live backend.

- [ ] **Step 4: Commit**

Nothing to commit for this task — it's manual verification. If Step 3 surfaces a client bug, fix
it as a new, separately-committed task appended to this plan rather than silently patching in
place.
