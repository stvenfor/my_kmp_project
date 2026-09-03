## Purpose

Delivers membership/pay UI and real WeChat/Alipay channels on all three targets, matching Flutter pay module behavior.

## MODIFIED Requirements

### Requirement: Membership and pay entry
The system SHALL provide membership/pay entry screens that present purchasable options and initiate checkout with UI matching Flutter pay/membership pages at pixel level.

#### Scenario: Open membership
- **WHEN** the user opens membership from a documented entry
- **THEN** plan options are shown and a purchase action is available when channels are configured

### Requirement: Pluggable pay gateway
The system SHALL invoke payments through a platform pay gateway supporting WeChat and Alipay channels when Flutter supports them, with real SDK adapters on Android, iOS, and HarmonyOS.

#### Scenario: Channel unavailable
- **WHEN** a pay channel is not configured on the current target
- **THEN** the UI shows a clear unavailable/error state instead of crashing, and the target remains incomplete in the gap registry until configured

### Requirement: Result surfacing
The system SHALL surface payment success, cancel, and failure outcomes to the user after the native SDK returns.

#### Scenario: User cancels pay
- **WHEN** the user cancels in the native pay sheet
- **THEN** the app returns to the pay/membership screen with a cancel outcome

## ADDED Requirements

### Requirement: Flags must not hide incomplete SDKs
Feature flags MAY gate rollout but MUST NOT mark commerce complete while any required target still lacks a real channel adapter.

#### Scenario: Flag-off stub
- **WHEN** pay flags are off and only stub gateways exist
- **THEN** commerce acceptance remains failed and incomplete targets are enumerated
