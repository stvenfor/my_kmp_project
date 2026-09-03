# OHOS / media gaps

- UI uses `StubMediaPlayer` / `MusicSession` on **all** targets (no real decode/render yet).
- `avplayer` cinterop remains in `:composeApp` ohosMain for future wiring; not called by feature UI.
- Android/iOS real engines (Media3 / AVPlayer) are deferred behind `MediaPlayerController`.
- `publishDebugBinariesToHarmonyApp` succeeds with current shell + stubs.
