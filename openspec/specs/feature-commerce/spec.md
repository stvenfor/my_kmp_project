# Feature Commerce Specification

## Purpose

Defines payment and membership commerce flows with platform SDK adapters so checkout behavior is consistent while native pay channels remain pluggable.

## Requirements

### Requirement: Membership and pay entry
The system SHALL provide membership/pay entry screens that present purchasable options and initiate checkout.

#### Scenario: Open membership
- **WHEN** the user opens membership from a documented entry
- **THEN** plan options are shown and a purchase action is available when configured

### Requirement: Pluggable pay gateway
The system SHALL invoke payments through a platform pay gateway interface supporting WeChat and Alipay channels when enabled on that target.

#### Scenario: Channel unavailable
- **WHEN** a pay channel is not configured on the current target
- **THEN** the UI shows a clear unavailable/error state instead of crashing

### Requirement: Result surfacing
The system SHALL surface payment success, cancel, and failure outcomes to the user after the native SDK returns.

#### Scenario: User cancels pay
- **WHEN** the user cancels in the native pay sheet
- **THEN** the app returns to the pay/membership screen with a cancel outcome
