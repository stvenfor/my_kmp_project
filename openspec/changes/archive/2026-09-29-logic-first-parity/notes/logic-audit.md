# Logic gap audit (2026-09-29)

Flutter SoT: `my_ai_project`. KMP: shared feature engines / hosts. Pixel SoT bitmaps noted as **SoT-skin** (not logic-pass).

## Chat + Friend

| Item | Detail |
|------|--------|
| Flutter | `mock_im_chat_store.dart`, `im_chat_repository.dart` (280ms send → 2s peer-read); friend pages |
| KMP | `ImEngine.kt` / `MockImEngine`; `FriendScreen.kt` + `FriendMockData` |
| Level | Chat **strong**; Friend **thin** |
| Gaps | Friend seed ↔ IM peers; accept request → `ensureConversation` |
| Priority | P1 |

## Community

| Item | Detail |
|------|--------|
| Flutter | `mock_post_repository.dart` (35 posts, like, create, comments, search*) |
| KMP | `MockCommunityEngine.kt`; search UI often stub |
| Level | **partial** |
| Gaps | Bind search tabs to `search*`; comments; topic heat |
| Priority | **P0** |

## Classroom

| Item | Detail |
|------|--------|
| Flutter | `classroom_mock_data.dart` (classes, HW, giftCard, dubbing, video) |
| KMP | SoT bitmaps for gift/dubbing/video; thin `ClassroomHwMock` |
| Level | **SoT-skin** |
| Gaps | Port mock models; model-driven claim/HW (replace bitmap-only host) |
| Priority | **P0** |

## Mall / Wallet / Pay / Membership

| Item | Detail |
|------|--------|
| Flutter | membership mock + payment gateway (no fake success); mall/wallet modules |
| KMP | `FlaggedPayGateway` honest; mall/wallet/membership often SoT bodies |
| Level | Pay **strong**; shelves **SoT-skin** |
| Gaps | Catalog/order/balance engines; membership plan selection from mock |
| Priority | P1 |

## Media / AI / Music

| Item | Detail |
|------|--------|
| Flutter | short video / dubbing mocks; AI SSE; music mock URLs |
| KMP | SoT pages + StubMediaPlayer / toast AI |
| Level | **SoT-skin** / thin |
| Gaps | Mock feed/publish; mock SSE chunk stream; bind playable URLs where player exists |
| Priority | P2 |

## commonTest

No domain tests yet for the above (auth/session/uuid/tokens only).
