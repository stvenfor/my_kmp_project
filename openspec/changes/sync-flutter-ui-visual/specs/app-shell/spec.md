## ADDED Requirements

### Requirement: Shell chrome visual parity
Splash, privacy dialog, and main tab bar chrome SHALL match Flutter shell visuals for icons, labels, sizes, and selected/unselected assets on each target that ships the shell.

#### Scenario: Tab icons from synced assets
- **WHEN** the user views the main four-tab bar
- **THEN** Home/Chat/Community/Mine icons use synced Flutter-equivalent assets (not improvised glyphs alone)

### Requirement: Immersive chrome remains product-correct
Immersive edge-to-edge behavior SHALL remain in effect while visual assets are updated; interactive controls MUST stay clear of system insets.

#### Scenario: Tab bar after asset swap
- **WHEN** tab icons are replaced with synced assets
- **THEN** the tab bar height and inset padding still match the immersive shell contract
