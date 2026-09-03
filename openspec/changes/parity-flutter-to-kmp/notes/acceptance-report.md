# Acceptance report (draft)

Status: **in progress** — Android evidence updated 2026-09-03 (Web/commerce/friend/live/classroom).

## Passed capabilities
- (none at full 1A×2A yet)

## Partial / evidenced (Android)
- **Shell:** splash / privacy / Home chrome under `notes/evidence/shell/Android/`
- **Auth:** guest→Chat→Login→`200 OK`→resume (`auth/Android/`)
- **Bridges:** deeplink cold-start; scan deny; Web offline fixture「离线网页」(`bridges/Android/`)
- **Commerce:** membership catalog + honest channel-unavailable UX (`commerce/Android/`); pay SDK still stub
- **Friend / Live / Classroom:** mock multi-page graphs evidenced under respective `notes/evidence/*/Android/`
- **5.4 Push:** registry `missing`×3 (explicit incomplete)

## Incomplete capabilities (platforms)
- See `platform-gap-registry.md`
- **3.5 / 5.5 / 6.6 / 8.5 / 9.4:** need iOS + OHOS device runs (and sandbox pay / register unblock where noted)
- **1A Flutter side-by-side** pixel packs still open for most surfaces

## Deferred follow-ups
- Capture Flutter reference screenshots for 1A
- WeChat/Alipay OpenSDK + sandbox Success for 8.5
- OHOS `want.uri` → `acceptDeepLink`; real OHOS WebView
- Vendor push / IM / live realtime SDKs

## Sign-off
| role | name | date | 1A pixel | 2A three-platform |
|---|---|---|---|---|
| engineering |  |  |  |  |
| product |  |  |  |  |
