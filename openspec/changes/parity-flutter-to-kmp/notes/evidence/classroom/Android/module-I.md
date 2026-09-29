# Module I — Web fixture + Classroom (#22)

## Android Screenshot Diff Gate (FAB-masked, bar≤2%)

| Route | mse_pct | result |
|-------|---------|--------|
| `/web` | 0.599 | PASS — `notes/evidence/web/Android/web.diff.json` |
| `/classroom/my_class` | 1.4242 | PASS |
| `/classroom/homework_stats` | 1.1943 | PASS |
| `/classroom/homework/detail_teacher` | 1.253 | PASS |
| `/classroom/homework/detail_student` | 1.5684 | PASS |
| `/classroom/homework/review` | 1.3999 | PASS |
| `/classroom/homework/dubbing` | **0.0015** | PASS (Flutter SoT FillBounds body) |
| `/classroom/gift/claim` | **0.0019** | PASS (Flutter SoT FillBounds body) |
| `/classroom/video/detail` | **0.0016** | PASS (Flutter SoT FillBounds body) |

Evidence: `notes/evidence/classroom/Android/*.diff.json` + `notes/evidence/web/Android/web.diff.json`.

## Gaps (honest)

- Classroom realtime / live session: mock only — `platform-gap-registry.md` (`classroom`)
- OHOS WebView still placeholder — registry (`WebView + JS bridge`)
- AI SSE unrelated; not claimed ready
