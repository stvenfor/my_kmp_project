## Purpose

Aligns shared visual tokens and chrome with Flutter `module_common_ui` AppTheme so pixel-level UI parity has a single token source instead of Demo-era colors.

## ADDED Requirements

### Requirement: Color token parity
Shared color tokens SHALL match Flutter AppTheme primary accent, backgrounds, surfaces, separators, and text roles used by product screens (including accent equivalent to Flutter `#007AFF` unless Flutter itself uses a screen-local override).

#### Scenario: Home and tab chrome colors
- **WHEN** Home root and main tab bar render on any target
- **THEN** background, accent, and label colors match Flutter reference screens within pixel tolerance

### Requirement: Typography and spacing scale
Shared text styles and spacing used by common chrome (nav bars, tab labels, list rows) SHALL match Flutter sizes and weights for the corresponding widgets.

#### Scenario: Tab label
- **WHEN** the main tab label is shown
- **THEN** font size and weight match Flutter iOS-style tab label treatment

### Requirement: Screen-local overrides documented
Where Flutter uses a feature-local theme, KMP SHALL reproduce that override on the same screens rather than forcing the global token incorrectly.

#### Scenario: Feature theme
- **WHEN** a Flutter feature applies a local theme color
- **THEN** the KMP screen uses the same local treatment and records it in acceptance notes for that screen
