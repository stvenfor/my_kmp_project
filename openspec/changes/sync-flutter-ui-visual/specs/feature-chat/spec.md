## ADDED Requirements

### Requirement: Chat list and detail visual parity
Chat conversation list and detail chrome (avatars, row layout, composer, bubbles where Flutter defines them) SHALL match Flutter chat module visuals for the mock/content state under test.

#### Scenario: List cells match Flutter structure
- **WHEN** an authenticated user opens the Chat list
- **THEN** cell layout (avatar, title, preview, time) matches Flutter structure and spacing within the pixel SOP
