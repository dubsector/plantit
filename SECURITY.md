# Security Policy

## Supported Versions

Only the latest release is supported. Pre-release builds are provided as-is with no security guarantees.

## Reporting a Vulnerability

**Please do not open a public GitHub issue for security vulnerabilities.**

Report vulnerabilities privately using [GitHub Security Advisories](https://github.com/dubsector/plantit/security/advisories/new). This keeps the details confidential until a fix is available.

### What to include

- A description of the vulnerability and its potential impact
- Steps to reproduce or proof-of-concept
- The version(s) affected
- Any suggested fixes if you have them

### Response

You can expect an acknowledgement within **72 hours** and a resolution or status update within **7 days**. If a fix requires significant changes, a timeline will be communicated in the advisory.

## Scope

This plugin runs on a Paper 1.21.4 server. Areas of particular interest:

- Command permission bypass
- WorldGuard region bypass
- Economy/inventory manipulation
- Denial-of-service via game state manipulation
- Plugin messaging channel input handling
