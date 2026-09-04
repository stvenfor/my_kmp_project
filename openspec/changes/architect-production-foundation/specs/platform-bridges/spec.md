## ADDED Requirements

### Requirement: Component-layer bridges
Push, in-app WebView, Pay, and Chat/IM ports SHALL live under a `component` layer (or Gradle modules) that features depend on; implementations use expect/actual without features importing vendor SDKs directly.

#### Scenario: Pay must not stay Unavailable forever
- **WHEN** Spike II Must gate runs
- **THEN** at least one sandbox/real SDK pay path returns Success/Cancel/Failure — not a permanent Unavailable placeholder on the Must channel
