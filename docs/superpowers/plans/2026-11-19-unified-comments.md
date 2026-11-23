# Unified Comments S1a Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Deliver the additive backend foundation (entities, migration, `CommentStore` contract, real repositories, focused tests) for unified post/video/article comments without changing existing community service code or legacy `Repository` signatures.

**Architecture:** Go workspace multi-module (`model`, `common`, `core`, `app`). New entities live in `model/entity`; a new additive migration creates canonical tables; a new `CommentStore` interface is implemented on the existing `*community.PostgresRepository` in new files. Legacy `community.Repository` and all current callers/stubs remain untouched and compiling. DDL is never applied to shared/live DB; integration tests use an isolated schema.

**Tech Stack:** Go 1.25, Gin, GORM + PostgreSQL, `github.com/google/uuid`, `go -C <module>` workspace commands.

**Approved design:** `docs/superpowers/specs/2026-11-19-unified-comments-design.md`.

---

## Guardrails (whole plan)

- Work in `/Users/qpy/Desktop/github/my_go_study` only for Go source; plan file in KMP main repo only.
- Preserve all pre-existing dirty files; do not modify `service.go`, existing `repository.go` interface, or existing stubs/tests.
- No `panic`/`TODO`/stub returned as finished; no DDL on shared DB; no commit/push.
- Invariants from approved design: `subject_id` only (no redundant `target_type` on comments), `root_id`/`reply_to_id`, no `parent_id`, no `depth`, root-only pin CHECK, required `client_msg_id` for new API, canonical SHA-256 payload hash, `score = 2*like_count + reply_count` for roots only (non-root 0), `root_count` delta = `C(after)-C(before)`.

---

### Task 1: New entities in `model/entity`

**Files:**
- Create `model/entity/comment.go`
- Create `model/entity/article.go`
- Test: `model/entity/comment_entity_test.go`

**Step 1 — write failing test** for score helper and table names:

```go
func TestCommentScore_RootsOnly(t *testing.T) {
    if got := CommentScore(3, 4); got != 10 { t.Fatalf("got %d", got) }
    if got := CommentScore(0, 0); got != 0 { t.Fatalf("got %d", got) }
}
```

**Step 2 — run** `go -C model test ./entity -run TestCommentScore -v`; expect FAIL (undefined).

**Step 3 — implement** entities:
- `WysCommentSubject{ID uuid.UUID, TargetType string, TargetID uuid.UUID, AuthorID *string, CommentCount, RootCount int64, LastCommentAt *time.Time, CreatedAt, UpdatedAt time.Time}`, table `wys_comment_subjects`.
- `WysComment{ID uuid.UUID, SubjectID uuid.UUID, RootID *uuid.UUID, ReplyToID *uuid.UUID, AuthorID string, Content string, LegacyReplyToNickname *string, PayloadHash string, ClientMsgID *string, IsAuthorReply bool, IsPinned bool, Status int16, LikeCount, ReplyCount, Score int64, DeletedAt *time.Time, CreatedAt, UpdatedAt time.Time}`, table `wys_comments`; constants `CommentStatusVisible=0/Hidden=1/Deleted=2`; helper `CommentScore(likes, replies int64) int64 { return 2*likes + replies }`.
- `WysCommentLike{CommentID uuid.UUID, UserID string, CreatedAt time.Time}`, table `wys_comment_likes`.
- `WysCommentModerationLog{ID int64, CommentID uuid.UUID, ActorID string, Action string, Reason *string, CreatedAt time.Time}`, table `wys_comment_moderation_log`; action constants including `mod_delete`.
- `WysArticle{ID uuid.UUID, AuthorID string, Title, Body string, Status int16, Source string, DeletedAt *time.Time, CreatedAt, UpdatedAt time.Time}`, table `wys_articles`; status constants draft/published/hidden.

**Step 4 — run** `go -C model test ./entity -v`; expect PASS.

---

### Task 2: Additive migration (SQL only; never applied here)

**Files:**
- Create `migrations/20261120120000_unified_comments.up.sql`
- Create `migrations/20261120120000_unified_comments.down.sql`

**Step 1 — write SQL** with `CREATE TABLE IF NOT EXISTS` for the five canonical tables + `comment_migration_state(stage text)` + `wys_comment_backfill_exceptions(original_id uuid, post_id uuid, user_id varchar(64), content text, reason varchar(255), created_at timestamptz)`. Include CHECK/partial unique/FKs from design: root-only pin, self-reference disallowed, non-root `reply_count=0`, `(subject_id,root_id)` and `(subject_id,reply_to_id)` composite self FKs with `UNIQUE(id,subject_id)`, indexes for latest/hot/replies/author.

**Step 2 — down guard** (no invented CLI flag):

```sql
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM comment_migration_state WHERE stage IN ('C','D','E')) THEN
    RAISE EXCEPTION 'refuse destructive down after shadow/cutover; use canonical-compatible rollback';
  END IF;
END $$;
DROP TABLE IF EXISTS wys_comment_moderation_log;
DROP TABLE IF EXISTS wys_comment_likes;
DROP TABLE IF EXISTS wys_comments;
DROP TABLE IF EXISTS wys_comment_subjects;
DROP TABLE IF EXISTS wys_articles;
DROP TABLE IF EXISTS wys_comment_backfill_exceptions;
DROP TABLE IF EXISTS comment_migration_state;
```

**Step 3 — verify by reading** (no DB execution): `rg "CREATE TABLE|CHECK|RAISE" migrations/20261120120000_unified_comments.*.sql`.

---

### Task 3: Additive `CommentStore` contract and pure helpers

**Files:**
- Create `app/internal/community/comment_store.go`
- Test: `app/internal/community/comment_store_test.go`

**Step 1 — write failing tests** for: canonical hash stability/separators, cursor round-trip, container delta, score helper integration.

**Step 2 — run** `go -C app test ./internal/community/... -run 'TestCanonical|TestCommentCursor|TestContainerDelta' -v`; expect FAIL.

**Step 3 — implement**:
- `type CommentStore interface { EnsureSubject(...); CreateUnifiedComment(...) (*entity.WysComment, error); ListUnifiedRoots(...) (*CommentPage, error); ListUnifiedReplies(...) (*CommentPage, error); LikeUnifiedComment(...) (*CommentLikeResult, error) }`
- Input/result structs: `CommentCreateInput`, `CommentListFilter`, `CommentPage`, `CommentLikeResult`, `CommentConflictError{ID uuid.UUID}`; sentinel `ErrCommentInvalid`, `ErrCommentTargetNotFound`, `ErrCommentConflict`.
- Pure helpers: `CanonicalPayloadHash(input)` using sorted-key canonical JSON via `encoding/json` on a fixed struct (no pipe concatenation); `encodeCommentCursor/decodeCommentCursor` with `base64.RawURLEncoding` of JSON; `containerDelta(before, after bool) int64`.
- Compile assertion `var _ CommentStore = (*PostgresRepository)(nil)` goes in Task 4 file.

**Step 4 — run tests**; expect PASS.

---

### Task 4: Real repository implementation

**Files:**
- Create `app/internal/community/repo_comment_postgres.go`

**Step 1 — implement** all `CommentStore` methods on `*PostgresRepository` (new file only):
- `EnsureSubject`: insert with deterministic ID supplied by caller; `ON CONFLICT (target_type,target_id) DO NOTHING`.
- `CreateUnifiedComment`: validate, canonical hash, transaction with lock order subject → root → reply_to → comment; idempotency re-query; same-subject/same-root checks; insert; update subject counters with `C(after)-C(before)`; update root `reply_count+1` and `score=2*like_count+reply_count`; map unique violation to `CommentConflictError`.
- `ListUnifiedRoots`: pinned separate channel; container predicate; cursor latest/hot; `limit+1` for `HasMore`; totals from subject.
- `ListUnifiedReplies`: root_id + status=0, cursor, `limit+1`.
- `LikeUnifiedComment`: transaction, lock comment, `INSERT ... ON CONFLICT DO NOTHING` / `DELETE RETURNING`, update `like_count` and root `score` only.

**Step 2 — compile assertion** `var _ CommentStore = (*PostgresRepository)(nil)` at top.

**Step 3 — run** `go -C app test ./internal/community/... ./internal/delivery/http/router/...`; expect PASS with unchanged legacy tests.

---

### Task 5: Integration tests (build-tagged; real up migration on isolated temp DB)

**Files:**
- Create `app/internal/community/comment_store_integration_test.go` with `//go:build integration`

**Step 1 — self-provisioning harness (no AutoMigrate, no operator DSN):** `TestMain` derives the admin connection from `COMMENT_IT_ADMIN_DSN` → repo-root `.env` `DATABASE_*` → compose defaults (`postgres:postgres@127.0.0.1:5432`), `CREATE DATABASE comment_it_it_<unix>_<rand>` (prefix `comment_it_`, unique suffix), executes the **real** `migrations/20261120120000_unified_comments.up.sql` statement-by-statement (dollar-quote-aware splitter; no multi-statement Exec), runs tests, then `DROP DATABASE ... WITH (FORCE)`. Never touches `comment_it` / `my_go_study` / shared tables; never prints passwords.

**Step 2 — covered cases (all executed, not `-run ^$`):** migration up idempotence (second run), real pin CHECK (`ck_comment_pin_root_visible` nonroot/hidden pin rejected, `uq_comments_pinned` single visible pinned root), same-subject composite FKs (cross-subject root/reply rejected), nonroot score invariant (`ck_comment_score`: root identity, nonroot score≠0 rejected, liked reply legal), real down guard (stage 'D' → `RAISE EXCEPTION`), last-reply container round trip under approved reply-target visibility (hidden/deleted reply target rejected; visible descendant of hidden root replyable; `root_count` 1→0→1, never negative), sequential + concurrent idempotency (same key same payload all succeed with one row/counters+1; same key different payload → one winner + 409s), microsecond-precision pagination (12 roots + 10 replies all inside ONE millisecond with exact tie timestamps, latest/hot/reply traversal, no repeats/skips, hidden high-score root excluded by container predicate, absent next cursor at end), like visibility (reply like keeps score=0; hidden/deleted like rejected; unlike of hidden content allowed), `ListUnifiedReplies` distinguishes nonroot/not-found from empty root.

**Step 3 — run command:** `go -C app test -tags integration ./internal/community -run TestCommentStoreIntegration -v` (local Docker PG `my_go_study_postgres` must be up; nothing else to configure).

---

### Task 6: Verification and handoff record

**Step 1 — run** exactly:
- `go -C model test ./entity`
- `go -C app test ./internal/community/... ./internal/delivery/http/router/...`
- `go -C app build ./cmd/api` (only because wiring file may be read; if unchanged, still acceptable as narrow app module build)

**Step 2 — record** exact outputs and actual Go contracts (method signatures, error vars, table names) in worker_done body for next slice (S2 service/handler/router).

**Step 3 — send worker_done** with real changed files and evidence; no commit/push.

> Verification caveat (environment, not this plan): `go -C app build ./cmd/api` currently fails on a pre-existing dependency conflict — `google.golang.org/grpc v1.71.0` × resolved `golang.org/x/net v0.54.0` (`undefined: http2.TrailerPrefix` in grpc's `handler_server.go`). go.mod files are untouched by this plan; fixing it requires a coordinated grpc/x/net bump, which is explicitly out of scope here. Slice verification uses the narrow package tests above.

---

## Remaining slices (whole approved work; nothing silently omitted)

Dependency chain: **S1a (this plan, Tasks 1–6) → S1b → S2 → {S3, S4, reconciliation}**. Three-slot rule (≤3 agents) and "no dispatch before dependency accepted" apply to every row below.

### S1b — State ops / pin / backfill / article repository (Slot A; depends on S1a accepted)

- **State ops** on `CommentStore` (new file `app/internal/community/repo_comment_state.go`): `hide/unhide/delete/mod_delete/pin/unpin` with subject→root→target lock order, `root_count` = `C(after)-C(before)` deltas and `comment_count`/`reply_count`/`score` increments exactly per design §4.5 table; author delete is terminal tombstone, admin `unhide` only hidden→visible, hidden→deleted visible delta 0; every action writes `wys_comment_moderation_log` (audit distinguishes `delete` vs `mod_delete`); pin only visible root (DB CHECK + partial unique as backstop); tombstone content never returned.
- **Backfill** (one-shot + idempotent): subjects for **all** `wys_posts` / `wys_short_videos` / `wys_articles` (incl. soft-deleted; orphan targets keep `author_id=NULL`), legacy comments copied **all** rows incl. `deleted_at → status=2`, `legacy_reply_to_nickname` preserved, `client_msg_id=NULL`, canonical payload hash; rows violating new CHECKs go to `wys_comment_backfill_exceptions` untouched (zero truncation, zero unattributed loss; normal+exception count must equal legacy total); counters recomputed per §4.4/§4.5; `wys_post_likes` NOT mapped to comment likes.
- **Article repository + lab seed**: `app/internal/article/{repository.go,repo_postgres.go,EnsureSeed}` — minimal read repo + `ExistsVisible` (public subset predicate) + `seedIfLab` 3 deterministic `source='lab_seed'` rows.
- **Tests**: state-op integration tests covering the full §4.5 delta table (including unhide-with-descendants root_count unchanged), pin flows, moderation audit rows, backfill idempotence + exception ledger + row-count reconciliation.

**S1b addendum — backfill APPLY-safety remediation (coordinator dispatch 2; implemented 2026-11-20):**

- Backfill reconciliation is no longer tautological: `LegacyTotal` is an independent SQL count (not enumeration sum), `Migrated`/`Excepted` are destination JOIN counts, dry-run conserves a four-bucket identity (`migrated + excepted + predicted-copy + predicted-exception == legacy total`) so missed enumeration (e.g. UUID-zero) or double-claimed rows fail `OK`.
- APPLY synchronization: single transaction sets `SET LOCAL lock_timeout = '10s'` (bounded waits; 55P03 → rollback + retry; ctx cancellation works during lock waits via pgx), takes advisory lock `pg_advisory_xact_lock(1465263938 /* WYCB */)` (serializes backfill + cutover stage changes), then acquires the maintenance fence in fixed order: legacy `SHARE` → `wys_comment_subjects EXCLUSIVE` → `wys_comments EXCLUSIVE`. EXCLUSIVE conflicts with ROW SHARE (`SELECT FOR UPDATE` table lock), so canonical writers queue at the gate **before** obtaining subject row locks; plain SELECTs (ACCESS SHARE) unaffected. A one-statement three-table SHARE fence was rejected with a proven 40P01 deadlock (writer holds subject row FOR UPDATE → backfill takes all three SHAREs → backfill waits row lock + writer INSERT waits comments SHARE); SHARE ROW EXCLUSIVE still permits ROW SHARE and is equally unusable. Availability cost documented; **continuous mirroring NOT implemented** — catch-up = rerun the idempotent backfill. Runbook: `docs/unified-comments-backfill-runbook.md` (my_go_study).
- Destination verification JOINs on source UUID: subject association + immutable `content/author/created_at/legacy_reply_to_nickname` compared with `IS DISTINCT FROM`; `status`/`deleted_at` deliberately excluded so legitimate canonical state changes survive reruns. UUID-ownership collisions → apply refuses with an actionable report (sample IDs + counts) and full rollback; uncovered source rows also refuse.
- UUID-zero fix: keyset first page has no lower bound (`WHERE id > uuid.Nil` would silently drop the minimum-UUID row).
- Dry-run is a `REPEATABLE READ READ ONLY` snapshot: zero writes, no table locks, predictions reported as predictions.
- Exception ledger upgraded: unshipped migration adds `raw jsonb NOT NULL`; quarantine rows preserve every legacy field (incl. `reply_to_nickname`, `created_at`, `deleted_at`), keyed by `original_id`, never overwritten on rerun; source table never rewritten.
- Real-PG tests added: independent reconciliation vs externally-run SQL, collision refusal + full rollback, uuid.Nil row, exception raw preservation across tampered source + rerun, fenced legacy write (blocks then completes), apply waiting out an uncommitted legacy write and including it in the same run, writer-first interleaving with the real apply (writer FOR UPDATE subject row → apply blocks at the EXCLUSIVE gate → writer INSERT proceeds → commit releases apply; zero 40P01, exact counts — this test reproduced the old SHARE-fence deadlock, verified by temporarily reverting), gate-first manual fence queuing a real canonical writer before its subject row lock, stage C/D/E guard.
- Article lab seed keeps its double env gate; S2 wiring MUST additionally gate on `config.IsLabAppEnv(cfg.AppEnv)` (seedIfLab pattern) — documented in `app/internal/article/seed.go`.

### S2 — Service / DTO / Handler / Router / Optional auth (Slot B; depends on S1b accepted)

- `service.go` switches to `CommentStore` (additive; legacy signatures untouched), DTO assembly per design §7 (`content_state`, `reply_to_author`, capability bits incl. `can_reply` per replied-comment visibility, server-derived identity/URLs).
- Routes: `GET/POST /api/v1/comments`, `GET /api/v1/comments/:id/replies`, `POST/DELETE /api/v1/comments/:id/like`, `DELETE /api/v1/comments/:id`, `POST/DELETE /api/v1/comments/:id/pin`, `PATCH /api/v1/comments/:id/moderation`, `GET /api/v1/articles/:id`; 409 conflict body carries `conflict_with`; unique nested envelope `cursor:{next,has_more}` everywhere.
- `OptionalSessionAuth` middleware reusing the injected `sessionAuth`/`accountGate` chain (anon ok; session-headers-without-Authorization → 401; no mock identity); writes stay strict.
- `main.go` wiring, `model/entity/access.go` permission constants; legacy alias `/api/v1/community/posts/:id/comments` internally remapped (cutover-stage flags per design §9.3, canonical_read/write switches).

### S3 — KMP contract layer (Slot A; depends on S2 accepted) — **DELIVERED 2026-11-23**

Actual delivery is **additive new files** (legacy `CommentModel` / `CommunityPostRepository` / post feed untouched, so `HttpCommunityPostRepositoryTest.kt` stays as-is):

- `feature/community/src/commonMain/.../http/UnifiedCommentModels.kt` — DTOs + enums
- `feature/community/src/commonMain/.../http/UnifiedCommentRepository.kt` — port + typed errors
- `feature/community/src/commonMain/.../http/HttpUnifiedCommentRepository.kt` — real `/api/v1/comments` transport
- `feature/community/src/commonTest/.../http/HttpUnifiedCommentRepositoryTest.kt` — 24 recording-client contract tests

Frozen public surface for S4:

```kotlin
enum class UnifiedCommentTargetType(wireValue) { Post("post"), Video("video"), Article("article") }
enum class UnifiedCommentSort(wireValue) { Latest("latest"), Hot("hot") }
enum class UnifiedCommentModerationAction(wireValue) { Hide("hide"), Unhide("unhide") }
enum class UnifiedCommentContentState { Visible, Hidden, Deleted, Unknown }

data class UnifiedCommentAuthor(userIdRaw, nicknameRaw, avatarRaw, isAuthorRaw) // + userId/nickname/avatar/isAuthor
data class UnifiedComment(
  idRaw, subjectIdRaw, targetTypeRaw, targetIdRaw, rootIdRaw, replyToIdRaw,
  authorRaw, contentRaw, contentStateRaw, legacyReplyToNicknameRaw, replyToAuthorRaw,
  isPinnedRaw, isAuthorReplyRaw, likeCountRaw: Long?, replyCountRaw: Long?, isLikedRaw,
  canReplyRaw, canLikeRaw, canDeleteRaw, canPinRaw, canModerateRaw,
  createdAtRaw, updatedAtRaw,
) // + content (visible-only, fail closed), contentState, author, rootId?, replyToId?,
  //   can* (== true), likeCount/replyCount (Long), createdAtMillis(): Long? / updatedAtMillis(): Long?
data class UnifiedCommentCursor(nextRaw, hasMoreRaw)              // next?, hasMore
data class UnifiedCommentPage(listRaw, pinnedRaw, cursorRaw, totalVisibleRaw: Long?, totalRootsRaw: Long?)
data class UnifiedCommentLike(idRaw, likeCountRaw: Long?, isLikedRaw)
data class UnifiedCommentPin(idRaw, isPinnedRaw)
data class UnifiedCommentStatus(idRaw, statusRaw)

interface UnifiedCommentRepository {
  suspend fun fetchRoots(targetType, targetId, sort = Latest, limit = 20, cursor: String? = null): UnifiedCommentPage
  suspend fun fetchReplies(rootCommentId, limit = 20, cursor: String? = null): UnifiedCommentPage
  suspend fun createComment(targetType, targetId, content, clientMsgId, replyToId: String? = null): UnifiedComment
  suspend fun setLiked(commentId, liked): UnifiedCommentLike
  suspend fun deleteComment(commentId): UnifiedCommentStatus
  suspend fun setPinned(commentId, pinned): UnifiedCommentPin
  suspend fun moderate(commentId, action, reason: String? = null): UnifiedCommentStatus
}

sealed class UnifiedCommentError:
  Unauthorized | Forbidden | NotFound | NotEnabled | Conflict(conflictWith: String?) |
  Invalid | Unavailable(cause) | Failure(serverCode: Int?, cause)
```

Contract locks: `client_msg_id` is caller-owned (never generated/rewritten); 201/200 both decode; 409 keeps `conflict_with`; totals/counts are backend `Long`; cursor token goes into the query map verbatim (transport escapes); missing capability bits / unknown `content_state` fail closed; invalid or missing timestamps stay `null`; failed writes throw, never return fake data.

Verification (main repo `/Users/qpy/Desktop/github/my_kmp_project`, working tree):

- `./gradlew :feature:community:testDebugUnitTest --offline` → BUILD SUCCESSFUL; 61 tests, 0 failures (new 24 + legacy 37: CommunityPostModelDecodeTest 10 / HttpCommunityPostRepositoryTest 20 / MockCommunityEngineTest 7).
- `./gradlew :composeApp:compileDebugKotlinAndroid --offline` → BUILD SUCCESSFUL.
- `-PandroidOnly=true` is currently unusable on this dirty tree: `feature/media/build.gradle.kts:23` references `iosMain`, which that flag skips → configuration fails (pre-existing, not S3).

### S4 — Android UI (Slot B; depends on S3 accepted) — **DELIVERED / DEFECT-REWORKED 2026-11-23**

Actual entry is an **assembly-level overlay** in the Jetpack Android shell (`NativeAndroidMain`), not a route screen: opening comments never unmounts the feed / short-video / secondary page, and closing it restores the previous navigation + bottom-bar state untouched.

**Files (all on main tree `/Users/qpy/Desktop/github/my_kmp_project`)**

- `core/router/.../CommentRoutes.kt` (+ `CommentRoutesTest.kt`): target-aware parse/build. `postId` legacy → post; `targetType|target_type` + `targetId|target_id` explicit; explicit wins over legacy; missing / blank / unknown (`short_video`) → `null` (never defaults to post).
- `feature/community/.../UnifiedCommentTarget.kt`: `Valid(type,id)` / `Invalid`; wire type → `UnifiedCommentTargetType`, unknown → `Invalid`.
- `feature/community/.../UnifiedCommentTime.kt`: relative time only from a parsed server timestamp; null stays null (no "刚刚" fabrication).
- `feature/community/.../UnifiedCommentController.kt`: Compose-state machine over the frozen S3 `UnifiedCommentRepository`.
- `feature/community/.../UnifiedCommentText.kt` (+ test): shared Unicode code-point counter (Go rune) for submit validation / composer counter / send enabled.
- `feature/community/.../UnifiedCommentSheet.kt`: `ModalBottomSheet` (white rounded, drag handle + close), `contentWindowInsets = 0` + `navigationBars ∪ ime` (no doubled nav padding); pull-to-refresh, near-end auto load-more with explicit retry footer, reply-draft resume inputs.
- `feature/community/build.gradle.kts`: one additive edge `implementation(projects.core.base)` for `AppUuid` (no new UUID dependency).
- `component/.../CrossFeatureScreenPort.kt`, `feature/content/.../ContentRouteHost.kt`, `composeApp/.../app/AppCrossFeatureScreens.kt`: optional `onOpenVideoComments` seam (default null; `feature/media` still has zero dependency on `feature/community`).
- `feature/media/.../VideoRouteHost.kt` (+ `VideoRouteQueryTest.kt`): dispatch strips query before matching, so `/video/short/play?videoId=<server uuid>` hits the play page; the page fetches `ShortVideoItemModel` via `HttpShortVideoRepository.get(id)` (real URL/title/counts) and toggles like/unlike through the server; blank/missing ID or no server item → explicit unavailable state, never demo substitution.
- `composeApp/.../NativeAndroidMain.kt`: `commentTarget` overlay state; sign-in snapshots `{target, draft, reply}` plus the overlay to return to, so login reopens the same target/draft without losing the underlying page; sheet rendered above feed/Now Playing.
- `composeApp/.../NativeJetpackTabs.kt`: `JetpackCommunityRoot(onOpenComments)` — post card comment button passes the server `post.id`.

**Routes / targets**

- Feed post → `target=post:<post.id>`; `/community/comment?postId=<id>` (legacy) → post; `/community/comment?targetType=video&targetId=<id>` (also snake_case) → video/article; `/video/short/play?videoId=<id>` 评 button → video.
- Malformed / missing / unsupported target (incl. shared links) opens the sheet in an explicit "无法打开评论" state; never silently becomes post.

**State invariants (controller)**

- Totals (`total_visible`, `total_roots`) come **only** from the server page envelope; no local increment on create / delete / reply, so a timeout-then-replay success cannot double count. Refresh failure keeps the last authoritative totals.
- Pinned is a separate channel: each root page reconciles `pinned` against the page's own value; replacement/unpin restarts from page 1 (cursor splice untrusted). Duplicate IDs replace the DTO **in place** with the newest payload for roots and replies.
- Root requests use a monotonic generation; refresh clears `loadingMore` explicitly (an invalidated page's `finally` cannot own the busy flag). Reply requests use a **global monotonic `replyEpoch`**, so a dropped-and-recreated thread never matches an old in-flight response (no ABA). `CancellationException` is rethrown.
- Refresh invalidates retained reply threads (items/cursor/counts cleared), immediately reloads expanded threads and the thread containing the selected reply target, and lazily reloads collapsed threads on expand. A target that became missing/hidden/deleted/`!can_reply` is marked `replyToStale`; submit is blocked with an explicit message instead of silently becoming a root comment. Confirmed delete/hide applies locally before refresh, so it survives a failed read.
- Tombstone roots (`hidden` / `deleted` / unknown `content_state`) render placeholders and are containers only — replyability follows the server `can_reply` on **visible descendants** (`unified_comment_service.go:202` shape).
- Actions are capability-gated (`can_reply/like/delete/pin/moderate`); like/pin/delete/moderate refresh authoritative server state; failures keep content and show a typed safe Chinese message; 401 → sign-in affordance, 503 `NotEnabled` → distinct unopened state.
- `client_msg_id` is held by one immutable pending submission: same payload retry reuses the key; edits / reply-target changes (including edit-away-and-back) invalidate it. A 409 conflict records the rejected payload; resubmitting the same content+reply target is blocked even after editing away and back — only different content or a different reply target proceeds. Success clears the draft only if the captured operation is still current.
- Length limit is 2000 **Unicode code points** (Go rune) everywhere; UTF-16 code units are never used. HTTP contract rejects non-2xx even if mislabeled `code=0`, and rejects empty-ID write fragments as malformed instead of faking a state transition.
- The sheet supports real pull-to-refresh (`PullToRefreshBox` + `LazyColumn` state) with visible refresh state, near-end auto load-more guarded against errors/in-flight duplicates, and an explicit retry footer (no infinite auto retries). Action row wraps at narrow widths; long nickname/time ellipsize.
- The bottom sheet's back / drag-dismiss only clears `commentTarget`; the underlying composition, route stacks and `bottomBarVisible` are not mutated.

**Verification (main repo, working tree, offline; rework pass 2026-11-23)**

- `./gradlew :core:router:testDebugUnitTest --offline` → BUILD SUCCESSFUL (11 tests).
- `./gradlew :feature:community:testDebugUnitTest --offline` → BUILD SUCCESSFUL, 92 tests / 0 failures (controller 26 incl. stale-loadMore, reply ABA drop/recreate, refresh-reload, delete/hide-under-failed-refresh, replay totals, conflict retry, pin replace/unpin, duplicate DTO replace; repository 26 incl. non-2xx-with-code-0 and empty-ID write fragments; text code points 1).
- `./gradlew :feature:media:testDebugUnitTest --offline` → BUILD SUCCESSFUL, 21 tests (4 new: query-bearing dispatch, query-only ID parse, blank/missing ID → null, server identity/`video_url` binding).
- `./gradlew :composeApp:compileDebugKotlinAndroid --offline` → BUILD SUCCESSFUL.
- `./gradlew :composeApp:assembleDebug --offline` → BUILD SUCCESSFUL.
- `git diff --check` clean.
- `-PandroidOnly=true` still fails at configuration: `feature/media/build.gradle.kts:23` `getByName("iosMain")` does not exist when that flag drops iOS targets (pre-existing, unrelated to S4; not modified).

**Rollout limitations (explicit)**

- Runtime data path is `HttpUnifiedCommentRepository` only — no mock / seed / fabricated identity, media, counts or time. This slice does **not** claim backend rollout state D; activation/cutover flags are separate work.
- `/video/short/play?videoId=<server uuid>` is real (server item/URL/title/counts, server like/unlike). The short-video **listing** entry (`VideoMock.shorts` → `ShortPlay` without query) remains a SoT screenshot demo with no server IDs, so tapping a tile lands on the honest "缺少服务端视频 ID" state — the feed-to-play entry is still partial, not a real video feed.
- The old full-screen `CommunityCommentScreen` remains reachable from non-Android/common hosts for compatibility; only the Android shell is wired (no iOS/Harmony/device verification in this slice).


### Reconciliation / hardening (Slot C; depends on S2; parallel to S4, files disjoint from S3/S4)

- `app/cmd/comment-reconcile/*` (counts vs predicates per design §4.4 对账), `comment_compat_test.go` alias guardrails, `router_anon_read_test.go`/`domain_routes_test.go` updates, migration runbook + `docs/.../acceptance-records/*`.
- Optional-auth negative tests (session headers without Authorization → 401) and anon/success regressions kept green.
