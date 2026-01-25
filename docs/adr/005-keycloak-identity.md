# 005 - Keycloak for Identity Management

* **Status:** Accepted
* **Date:** 2025-01-17
* **Deciders:** Security Team

## Context
The system requires:
- Centralized authentication for web dashboard, mobile app, and APIs
- Role-based access control (Admin, Manager, Driver)
- SSO capabilities
- OAuth2/OIDC compliance for industry standards

## Decision
We will use **Keycloak** as our Identity Provider instead of Auth0, Okta, or custom implementation because:
1. Open source with no per-user licensing costs
2. Full OAuth2/OIDC implementation
3. Built-in user management UI
4. Flexible identity federation
5. Self-hosted for data sovereignty

## Consequences
* **Positive:** Zero licensing costs at any scale
* **Positive:** Full control over user data
* **Positive:** Extensive customization options
* **Negative:** Requires operational expertise to maintain
* **Negative:** No SLA guarantees (self-managed)
