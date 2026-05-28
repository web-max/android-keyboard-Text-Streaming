# Routed Issue

Target: inboxes/emulator_issues.md
Severity: medium
Title: Emulator boot failure may block subsequent tasks

Body:
The headless emulator hung during boot in T000 and verification was bypassed per user request. The instructions in EMULATOR_SETUP.md might need refinement to run reliably in a CI/headless environment. This could block T003, which explicitly requires emulator verification.
