## Purpose

Defines cross-platform remote image loading for Compose UI: a shared public wrapper, placeholder/error behavior, and OHOS arm64 parity with Android/iOS for network images.

## ADDED Requirements

### Requirement: Shared network image wrapper for feature UI
Feature and shell screens that display remote images SHALL load them only through the shared network-image composable in the common UI layer. Call sites MUST NOT import platform image-loader Compose APIs directly.

#### Scenario: Feature uses shared wrapper
- **WHEN** a Home, Community, Friend, or native-shell screen renders a remote avatar, cover, or attachment URL
- **THEN** it invokes the shared network-image API (not a platform-specific image composable)

### Requirement: Placeholder and failure states
When the URL is null/blank, or loading fails, the system SHALL show the caller-supplied local placeholder drawable (or an equivalent local fallback) instead of leaving an empty/broken surface.

#### Scenario: Blank URL
- **WHEN** the shared network-image API receives a null or blank URL
- **THEN** the UI shows the provided placeholder drawable

#### Scenario: Load failure
- **WHEN** a remote image request fails
- **THEN** the UI shows the provided placeholder (or error) drawable

### Requirement: Cleartext URL hardening for ATS-like targets
The system SHALL upgrade `http://` image URLs to `https://` before requesting, so iOS App Transport Security and similar policies can load demo/mock image hosts.

#### Scenario: HTTP upgrade
- **WHEN** the URL starts with `http://`
- **THEN** the request uses the `https://` equivalent

### Requirement: OHOS arm64 loads remote images
On OHOS arm64 device builds, the system SHALL resolve and link a CPF-adapted image-loading artifact line that publishes `ohosArm64` variants, and SHALL display remote images through the shared wrapper (not placeholder-only).

#### Scenario: ohosArm64 dependency resolution
- **WHEN** `:composeApp` is compiled for `ohosArm64` with the adopted catalog versions
- **THEN** Gradle resolves image-loader modules that include an `ohosArm64` variant and the module links successfully

#### Scenario: Remote cover visible on OHOS arm64
- **WHEN** a user opens a screen that passes a valid HTTPS image URL on an OHOS arm64 build
- **THEN** the remote image is displayed (subject to network availability), not only the local placeholder

### Requirement: Local resource images remain resource-based
Bundled Compose resources (icons, splash, SoT static bodies, tab icons) SHALL continue to use Compose resource painters. The network-image wrapper MUST NOT be required for non-URL assets.

#### Scenario: Local icon unchanged
- **WHEN** a screen draws a bundled drawable such as a nav back icon or splash logo
- **THEN** it uses Compose resource painting, not the network-image wrapper

### Requirement: Unsupported OHOS x64 fallback
If the adopted image-loader line does not publish a usable `ohosX64` variant, `ohosX64` builds SHALL keep compiling by falling back to placeholder-only rendering for network images.

#### Scenario: ohosX64 still builds
- **WHEN** `:composeApp` is compiled for `ohosX64` without an image-loader `ohosX64` artifact
- **THEN** the build succeeds and network-image call sites show the local placeholder
