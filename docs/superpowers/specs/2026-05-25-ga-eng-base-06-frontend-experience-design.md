# GA-ENG-BASE-06 Frontend Experience Foundation Design

## Context

MedKernel v1.0 GA requires a stable frontend foundation before the engine-facing pages can safely scale. The current frontend already has many page shells and page stubs, but route metadata only covers login and dashboard, menus are maintained separately from route registration, breadcrumbs are not derived from a single source, and page state handling is inconsistent across pages.

This change implements `GA-ENG-BASE-06` as a product-experience foundation and page-by-page cleanup pass. It aligns the existing frontend with the product constitution: 5 visible menu groups plus hidden advanced tools, left-side navigation, one page goal, one primary action, unified state machines, reusable seven-step configuration flow, and complete six-state presentation.

## Goals

- Make route metadata the single source of truth for navigation, breadcrumbs, page titles, menu placement, route registration, and permissions metadata.
- Derive the 5+1 menu from route metadata so menu and route paths cannot drift.
- Add a reusable six-state surface for loading, empty, error, forbidden, partial-success, and ready states.
- Upgrade `PageShell`, `AppLayout`, and shared UI components so customer-facing pages follow the same information architecture.
- Refactor existing pages one by one for consistent product experience, status badges, pagination, primary-action hierarchy, and evidence/degradation messaging.
- Keep the scope frontend-only: no new backend behavior, no fake API completion claims, and no hard-coded single-disease business closure.

## Non-Goals

- Do not implement new `GA-ENG-API-*` backend endpoints.
- Do not replace existing mock/static page data with fake production APIs.
- Do not redesign the brand or build a marketing-style landing page.
- Do not create new first-level menus or expose advanced technical views as customer main paths.

## Architecture

### Route Metadata

`frontend/src/shared/config/routes.ts` becomes the canonical page registry. Each page route includes:

- `path`
- `title`
- `breadcrumb`
- `sectionKey`
- `menuKey`
- `menuLabel`
- `requireAuth`
- `hidden`
- optional `permissions`, `pageType`, and `stateMachine`

`frontend/src/shared/config/menu.ts` derives menu sections from this registry and keeps the constitution ordering locked.

### Layout And Navigation

`AppLayout` reads route metadata for:

- selected menu item
- default open menu section
- header title
- breadcrumbs
- hidden advanced-tool entry

The router still lazy-loads pages, but route definitions use the same metadata paths instead of parallel hand-maintained path lists.

### Six-State UI

Add a shared state component under `frontend/src/shared/ui/` that supports:

- loading
- empty
- error
- forbidden
- partial success
- ready

The component must provide consistent Chinese copy, retry/action slots, traceId display for errors, and non-sensitive forbidden messaging.

### Page Cleanup

Refactor all existing pages by type:

- Configuration pages: use `StepFlow`, `StatusBadge`, one primary action, and evidence/rollback language.
- List pages: use consistent filters, table pagination, empty/error/loading states, and status badges.
- Dashboard pages: focus on risk, progress, value, actions, and drill-down intent.
- Advanced tools: keep technical details visible only inside advanced pages, with clear warning/degradation state.
- Auth and not-found pages: keep them simple, branded, and action-oriented.

## Testing Strategy

- Route/menu unit tests:
  - visible sections equal 5
  - hidden advanced section exists
  - every menu path has route metadata
  - every authenticated page has title and breadcrumb
  - no duplicate paths
- Six-state unit tests:
  - each state renders expected text and action affordance
  - error state can show traceId
  - ready state renders children
- Component tests:
  - `PageShell` renders title, description, breadcrumb-aware content area, primary action, extras.
  - `AppLayout` can render menu labels from metadata.
- Page smoke tests:
  - representative pages from each section render without crashing.
- Final verification:
  - `npm run lint`
  - `npm run format:check`
  - `npm run typecheck`
  - `npm test`
  - `npm run build`
  - browser verification on the local app for core routes.

## Acceptance Criteria

- `docs/backlog.md` marks `GA-ENG-BASE-06` as done only after implementation and verification pass.
- All current frontend pages are reachable through metadata-backed routes.
- The customer-facing side menu remains exactly 5 visible groups; advanced tools stay hidden/secondary.
- Header title and breadcrumbs come from route metadata, not raw path strings.
- Existing pages consistently use shared shell/state/status/step components.
- No new customer-facing JSON, DSL, trace, or model prompt exposure outside advanced tools.
- Verification evidence is collected before commit, push, and PR.
