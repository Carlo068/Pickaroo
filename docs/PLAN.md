# Pickaroo App: Complete Plan

> Android frontend (Kotlin, Jetpack Compose, MVVM). Backend: **PickarooAPI**, developed separately.
> **Current phase: frontend only, on mock data.** Every screen works against mock data that has the same shape as the PickarooAPI contract. Connecting the real API later means replacing one data source, not rewriting screens.

---

## 1. Product summary

Pickaroo lets small shops offer **pickup** and **delivery** without building their own infrastructure.

| Role | What they do in the app |
|---|---|
| **Customer** | Chooses a shop, browses the catalog, fills a cart, and checks out with **pickup** (picks a time slot) or **delivery** (gives an address). Tracks the order and gets a notification when it's ready. |
| **Shop staff** | Sees incoming orders, prepares them, and marks them *ready*. Updates stock. |
| **Shop owner** | Everything staff can do, plus managing products, pickup slots and staff, and seeing sales. |
| **Delivery driver** | Sees available delivery orders, accepts one, picks it up, delivers it, and shares their location. |

One account can hold several roles (for example, an owner who also shops as a customer). Each role is assigned per shop where that applies.

---

## 2. Backend as the app sees it

The app only ever talks to PickarooAPI over HTTP. Here is how each backend store affects the frontend.

| Store | Holds | What it means for the app |
|---|---|---|
| **MongoDB** (single source of truth) | Users, shops, catalog (categories, groups, products, items), orders, slot configuration | Every read model the app shows comes from here. The mock JSON copies these documents. |
| **Neo4J** | Roles and permissions graph: `(User)-[:HAS_ROLE {shopId}]->(Role)-[:GRANTS]->(Permission)`, `(User)-[:OWNS\|WORKS_AT]->(Shop)`, `(Driver)-[:DELIVERS]->(Order)` | Login and `/me` return the roles and the **permission list**. The app decides what to show from **permissions**, not from role names. |
| **Redis** | JWT sessions (refresh tokens, revocation list) and caches (catalog, shop hours, slot availability) | Short-lived access token plus a rotating refresh token. Logout revokes the refresh token. The app must handle `401`, refresh once, then send the user back to login. |
| **ScyllaDB** *(assumed use, to confirm with the API team)* | High-write time series: order status events, driver location pings, notification feed | The order timeline, the live driver position and the notifications list are read from "events" endpoints and polled. |

---

## 3. Frontend architecture

### 3.1 Layers (per feature, extending the current `ui/<feature>/{model,data,network,viewmodel,view}` pattern)

```
view (Compose) ─▶ viewmodel (StateFlow<UiState>) ─▶ repository (interface)
                                                   ├─ Mock<Feature>Repository   ← this phase
                                                   └─ Remote<Feature>Repository ← when PickarooAPI is ready
```

- **DTOs** (`network/`) match the API JSON exactly (snake_case through `@SerializedName`).
- **Domain models** (`model/`) are what screens use. Mappers turn DTOs into domain models.
- **Repositories** (`data/`) are interfaces. ViewModels receive them through their constructor, the same way `CartViewModel(storage)` already does, so they can be tested with fakes.
- **`AppContainer`** (`common/di/AppContainer.kt`) creates every repository once. A single switch, `BuildConfig.USE_MOCK_API`, picks Mock or Remote. This needs `buildFeatures.buildConfig = true`. There's no Hilt, which keeps the project simple for the class.
- **UI state rule:** every screen state carries `isLoading`, `error` and `data`. A shared `ErrorState` / `LoadingState` composable lives in `common/ui/`.

### 3.2 Mock data strategy (same approach as today's product gist)

| Kind | Source | Notes |
|---|---|---|
| **Read-only seed data** | JSON files in the repo under `docs/mock/`, each also published as a **GitHub gist** and read through Retrofit (like `GistService`) | The repo copy is the editable source. The gist is what the app downloads. A raw URL pinned to a commit never changes; a `raw/<file>` URL without the commit always returns the latest version but is cached for about 5 minutes. **Use pinned URLs** so builds are reproducible. Debug builds also package the repo files as assets (`assets.srcDir("../docs/mock")`), so the app can read them offline. |
| **Writes** (register, orders, status changes, stock edits, slot bookings, driver actions) | `MockPickarooApi`, an in-app class that implements the same Retrofit interfaces | It starts from the seed data and saves changes as JSON in `AppPreferences`, like `CartStorage` does. It simulates latency (300–600 ms), and can optionally inject errors to test error states. It returns `HttpException` 401/403/404/409 exactly as the real API would. |

The mock enforces permissions (it returns 403), so permission bugs show up before the real backend exists.

### 3.3 Mock auth (mirrors Redis + JWT)

- `docs/mock/users.json` holds mock users with **mock-only** passwords, display data and roles. One user per role, plus one owner who is also a customer.
- `POST auth/login` returns `{access, refresh, user, roles, permissions}`.
  - `access` is a JWT-shaped token: `base64url(header).base64url(payload).mock`. The payload holds `sub`, `roles`, `exp` (now + 15 min) and `iat`.
  - The app **only decodes** the payload and never verifies it, which is exactly how it will treat the real signed JWT.
- `POST auth/refresh` rotates the refresh token. `POST auth/logout` adds it to a revoked set, imitating the Redis revocation list.
- An **OkHttp `Authenticator`** catches 401, refreshes once, retries the request, and sends the user to login if that fails. It's tested against the mock by forcing an expired `exp`.
- **Registration now works** through the mock: it creates a new user with the `customer` role. This replaces the register tab that does nothing today.
- The class API (`androidbasics-auth-api`) is removed from the auth flow.

### 3.4 Navigation by role

```
AppNavigation
├─ onboarding ─▶ login/register
├─ customer  (tabs: Inicio · Ofertas · Pedidos · Productos · Usuario) + Cart, Checkout, OrderDetail, Tracking
├─ shop      (tabs: Pedidos · Catálogo · Horarios* · Equipo* · Ventas*)   *owner-only, hidden without permission
└─ driver    (Disponibles · Entrega activa · Historial)
```

- The start screen after login depends on the user's roles. Users with more than one role get a **"Cambiar a modo tienda / repartidor / cliente"** switch in Usuario.
- Every guarded screen checks `session.can(Permission.X)` before navigating, and the mock also returns 403. Both are tested.

---

## 4. Roles and permissions

### 4.1 Permissions (strings returned by the API, from Neo4J)

`catalog:read` · `cart:use` · `order:create` · `order:read:own` · `order:cancel:own` · `shop:orders:read` · `order:status:update` · `product:stock:update` · `product:manage` · `slots:manage` · `staff:manage` · `shop:stats:read` · `delivery:read:available` · `delivery:accept` · `delivery:update` · `location:write`

### 4.2 Matrix

| Permission | Customer | Staff | Owner | Driver |
|---|:-:|:-:|:-:|:-:|
| catalog:read | ✅ | ✅ | ✅ | ✅ |
| cart:use, order:create, order:read:own, order:cancel:own | ✅ | | | |
| shop:orders:read, order:status:update, product:stock:update | | ✅ (own shop) | ✅ (own shop) | |
| product:manage, slots:manage, staff:manage, shop:stats:read | | | ✅ (own shop) | |
| delivery:read:available, delivery:accept, delivery:update, location:write | | | | ✅ |

### 4.3 Order status machine (shared by all roles)

| From → To | Who | Fulfilment |
|---|---|---|
| — → `PLACED` | Customer | both |
| `PLACED` → `ACCEPTED` | Staff / Owner | both |
| `PLACED` → `CANCELLED` | Customer (only before `ACCEPTED`), Staff / Owner | both |
| `ACCEPTED` → `PREPARING` → `READY` | Staff / Owner | both |
| `READY` → `PICKED_UP` | Staff / Owner (customer shows a pickup code) | pickup |
| `READY` → `DRIVER_ASSIGNED` | Driver accepts | delivery |
| `DRIVER_ASSIGNED` → `OUT_FOR_DELIVERY` → `DELIVERED` | Driver | delivery |

Each transition is recorded as an event (ScyllaDB in the real backend, a list on the order in the mock). The customer's timeline is built from these events.

---

## 5. API contract the mock implements (`/api/v1`)

| Area | Endpoints |
|---|---|
| Auth | `POST auth/register` · `POST auth/login` · `POST auth/refresh` · `POST auth/logout` · `GET me` |
| Shops | `GET shops?lat=&lng=` · `GET shops/{shopId}` · `GET shops/{shopId}/catalog` · `GET shops/{shopId}/slots?date=` |
| Customer orders | `POST orders` · `GET orders` (mine) · `GET orders/{id}` · `GET orders/{id}/events` · `POST orders/{id}/cancel` |
| Shop operations | `GET shops/{shopId}/orders?status=` · `POST orders/{id}/status` · `PATCH shops/{shopId}/products/{id}` · `POST shops/{shopId}/products` · `PUT shops/{shopId}/slot-config` · `GET/POST/DELETE shops/{shopId}/staff` · `GET shops/{shopId}/stats?range=` |
| Delivery | `GET driver/orders/available` · `POST driver/orders/{id}/accept\|pickup\|deliver` · `POST driver/location` · `GET orders/{id}/driver-location` |
| Notifications | `GET notifications?since=` |

**Errors** follow one shape, `{ "error": { "code": "SLOT_FULL", "message": "..." } }`, with HTTP 400/401/403/404/409. The app maps `code` to Spanish messages in one place.

> **Agree this table with the PickarooAPI team early.** If their real routes or field names differ, change the DTOs and the mock in one pass. The screens stay untouched.

---

## 6. Mock data files (`docs/mock/`)

| File | Contents | Built from |
|---|---|---|
| `catalog.json` | v3 = `catalogo-v2.json` + `shop_id` on every product and group. `shop_1` keeps the v2 catalog; `shop_2` (bakery) and `shop_3` (greengrocer, pickup-only, with an unavailable product) get small catalogs | `docs/catalogo-v2.json` |
| `shops.json` | 3 shops: `id, name, address, lat, lng, timezone, logo_url, hours[], pickup: {slot_minutes, capacity_per_slot, lead_minutes}, delivery: {enabled, radius_km, fee}` | new |
| `users.json` | 5 users: customer, staff (shop_1), owner (shop_1, also a customer), owner (shop_2), driver. Fields: `id, username, password_mock, name, email, roles: [{role, shop_id?}]` | new |
| `roles.json` | role → permissions (section 4.2), mirroring Neo4J | new |
| `orders.seed.json` | About 10 past orders in different statuses, so dashboards, sales stats and history aren't empty | new |

Rules: no real personal data and no real passwords. Prices stay as they are in the JSON, but the app converts them to **integer cents** right after reading, so totals never drift by a cent.

---

## 7. Roadmap (epics → tasks)

Each task becomes **one staged change set and one commit** (see CLAUDE.md). Every task ships with its tests.

### E0. Foundation: data layer and mock API *(blocks everything)*
1. `AppContainer`, `BuildConfig.USE_MOCK_API`, and the repository interface pattern.
2. `docs/mock/` files (section 6), loaded from the repo in debug builds (publishing them as gists is optional).
3. `MockPickarooApi` core: seed loading, persisted write store, latency, error injection, 403/404/409 helpers.
4. Money as cents (`Money` value class) across catalog and cart. This also fixes the review finding left open on the cart.
5. Shared `LoadingState` / `ErrorState` / empty-state composables and the error-code → message mapper.

*Tests:* unit tests for the seed parser (including the old v2 file), the write store, money math and the error mapper.

### E1. Mock auth and roles *(replaces the class auth API)*
1. Auth DTOs and `MockAuthService` (login, register, refresh, logout, `me`), with JWT-shaped tokens.
2. `SessionRepository`: tokens, roles, permissions, `can(permission)`, and the active role mode.
3. OkHttp auth interceptor plus `Authenticator` (refresh on 401, otherwise logout).
4. Working register tab (customer role), with validation messages.
5. Role-based navigation graphs and the role-mode switch in Usuario.

*Tests:* token decode, expiry and refresh paths, a permission matrix test for every role, navigation tests that check each role lands on its own home, and 403 on guarded mock endpoints.

### E2. Shops and catalog
1. Shop selection (`GET shops`), wired to the onboarding "Elige tu tienda" message, with the selected shop saved.
2. Catalog v3 repository: categories, groups (filtered by `starts_at`/`ends_at`, ordered by `priority`/`position`), products and items.
3. Home built from real groups and categories, replacing the hardcoded `HomePortfolio` list in `HomeView.kt`.
4. Product detail screen, including portfolio bundles that list their items.
5. **Ofertas** screen from discounted products and active groups (it's an empty stub today).
6. Search across products and items; show stock and availability (out of stock disables "+").

*Tests:* group schedule filtering, discount price math, availability gating, and UI tests for product detail and offers.

### E3. Cart follow-ups *(PIC-1 already done)*
1. Cart is tied to one shop. Adding from another shop asks "¿Vaciar carrito?".
2. Apply discounts and show savings; check stock when quantities change.
3. Cart key comes from the session user id (no more empty-username key).

### E4. Checkout and customer orders
1. Checkout screen (replaces the static Order tab): cart summary, Pickup / Delivery choice, and payment shown as "Pago en tienda / contra entrega" (online payment is out of scope).
2. **Pickup slot picker:** next 3 days, slots generated from `shops.json`, capacity shown, full slots disabled (`409 SLOT_FULL`).
3. Delivery address entry with a radius and fee check.
4. Place order (`POST orders`), then the confirmation screen with the **pickup code**.
5. Pedidos: order history and order detail, including cancel while `PLACED`.

*Tests:* slot generation and capacity, the 409 path, cancel rules, and UI tests for checkout in both modes.

### E5. Tracking and notifications
1. Status timeline from `orders/{id}/events`, polled every 5 s while visible (2 s while out for delivery).
2. Live driver map (osmdroid, see PIC-10) for delivery orders.
3. Local notifications ("¡Tu pedido está listo!", "Tu repartidor va en camino"). These come from a polling `WorkManager` job in this phase; real push (FCM) comes later. Asks for `POST_NOTIFICATIONS` on Android 13+.
4. Notifications list in Usuario.

### E6. Shop dashboard (staff and owner)
1. Incoming orders queue grouped by status, with accept / preparing / ready / picked up actions (staff and owner).
2. Pickup verification: enter or scan the customer's pickup code.
3. Stock and availability quick edit (staff); full product create and edit (owner).
4. Pickup slot configuration: hours, slot length, capacity (owner).
5. Staff management: invite or remove by username (owner).
6. Sales summary: today, this week, top products, using seed plus new orders (owner).

*Tests:* every action checked against the permission matrix (staff gets 403 on owner actions), and status-machine transition tests.

### E7. Delivery driver *(reuses PIC-2's design)*
1. Available orders list and accept, with a 409 when another driver accepts first.
2. Active delivery: map, "Recogí el pedido", "Entregado".
3. Location sharing: simulated route in mock mode. Real GPS (`ACCESS_FINE_LOCATION`) comes behind a flag later.
4. Delivery history and earnings.

### E8. Profile
1. Profile from `GET me`: name, email, roles. Replaces the hardcoded "María González".
2. Saved addresses, used by checkout.
3. Shopping lists stored in the mock, replacing the hardcoded "Compra Semanal", with "Agregar todo al carrito".

### E9. Backend switch-over *(when PickarooAPI is reachable)*
1. A `Remote*Repository` for each feature, plus the real base URL in `BuildConfig`.
2. Contract tests: parse recorded real API responses with the same DTOs the mock uses.
3. Turn `USE_MOCK_API` off for release builds and keep the mock for debug and demos.

**Dependency order:** E0 → E1 → E2 → E3 → E4 → (E5, E6, E7 in parallel) → E8. E9 starts once the API is up.

---

## 8. Testing strategy

| Layer | Tool | What it covers |
|---|---|---|
| Models, mappers, money, status machine, permission checks | JUnit (JVM) | Pure logic; fast; runs on every task |
| Repositories and `MockPickarooApi` | JUnit with an in-memory store | Contract behavior, including 401/403/404/409 |
| ViewModels | JUnit with fake repositories (and `kotlinx-coroutines-test` once ViewModels launch coroutines) | State changes, loading and error states |
| Screens | Compose UI tests (`createComposeRule`) on the emulator | Rendering and interaction through `testTag`s |
| Navigation and roles | Compose UI tests on the real navigation graphs | Each role lands on its own home, and guarded screens are unreachable without permission |

Workflow for every task:
1. Implement the task.
2. The `test-runner` agent runs unit tests, and emulator tests when they're affected.
3. Stage that task's files only, and you commit.
4. `code-reviewer` reviews each epic before you merge.
5. `jira-updater` moves the Jira issues.

Test libraries are pinned to versions that support Android 17 (Espresso 3.7.0, ext-junit 1.3.0).

---

## 9. Relationship to existing Jira work

- **PIC-1 Cart:** done. Its follow-ups are E3, and E0.4 (money in cents).
- **PIC-2 Delivery & driver accounts:** its design carries over into E5 and E7.
  - PIC-7 and PIC-8 are absorbed into E0.3 (one `MockPickarooApi` instead of a separate delivery mock).
  - **PIC-9's role source changes**: roles now come from mock auth (E1), not from a delivery-backend role table.
  - PIC-10 to PIC-13 map directly to E5.2, E4, E5.1 and E7.
- Create the new epics E0–E9 in Jira when you're ready, and re-link or close the PIC-2 tasks they replace.

## 10. Assumptions and open questions

1. **ScyllaDB usage** (events, locations, notifications) is assumed. Confirm with the API team.
2. **Online payment** is out of scope; orders are paid at pickup or on delivery.
3. **One shop per cart.**
4. **Push notifications** use local notifications plus polling until the API has a push channel (FCM).
5. **The API contract in section 5 is the frontend's proposal.** Align routes and field names with PickarooAPI before E9.
6. **Gist hosting:** if editing gists becomes painful, the same JSON can be bundled in `app/src/main/assets/mock/` and read offline. The repository interface hides the difference.
