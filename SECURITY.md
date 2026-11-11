# Security Policy

## Supported versions

The project is under development; only the latest state of `main` is supported.

## Reporting a vulnerability

Please do **not** open a public issue for security problems.
Use GitHub's private vulnerability reporting (Security tab → "Report a vulnerability")
once enabled on the repository.

## Scope and responsible use

ResilienceLab includes load-testing and fault-injection capabilities. These must only be
used against systems you own or are explicitly authorized to test. Never run
experiments against third-party systems without written permission.

## Secrets

No credentials are stored in this repository. Configuration is provided through
environment variables (see `.env.example`).
