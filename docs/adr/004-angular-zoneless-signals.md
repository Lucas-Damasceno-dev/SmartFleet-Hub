# 004 - Angular Zoneless with Signals

* **Status:** Accepted
* **Date:** 2025-01-17
* **Deciders:** Frontend Team

## Context
The dashboard requires real-time updates (GPS positions every 3-5 seconds) with smooth map rendering. Traditional Zone.js change detection causes performance issues with frequent updates.

## Decision
We will use **Angular Zoneless** mode with **Signals** for state management because:
1. Fine-grained reactivity without Zone.js overhead
2. Predictable change detection only when signals change
3. Better performance for real-time data updates
4. Future-proof (Angular's recommended direction)
5. Simpler mental model for reactive state

## Consequences
* **Positive:** Significant performance improvement for real-time features
* **Positive:** Reduced bundle size (no zone.js)
* **Positive:** More explicit and predictable updates
* **Negative:** Still experimental (API may change)
* **Negative:** Some third-party libraries may require adaptation
