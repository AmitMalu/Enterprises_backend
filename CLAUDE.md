# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Spring Boot 3.4.2 / Java 17 backend (WAR deployment to Tomcat) for **Smart Credit Solution Enterprises Admin Portal** — a fintech reseller/distribution admin backend. It manages a multi-level hierarchy of resellers (SuperAdmin → Admin → Enterprises → Whitelabel → SuperDistributor → Partner → Distributor → Retailer) and integrates with many third-party vendors for AEPS, DMT, BBPS/recharge, payment gateways, and payouts. Single module, single package root: `com.scs.admin`.

## Common commands

Build tool is Maven via the wrapper (`mvnw`/`mvnw.cmd`) — always use the wrapper, not a system `mvn`.

```
./mvnw clean install -DskipTests   # build the WAR (target/enterprises.war)
./mvnw spring-boot:run             # run locally (port 8081, context path /adminPortal)
./mvnw test                        # run tests
./mvnw test -Dtest=ClassName#methodName   # run a single test
```

There is effectively no test suite beyond the generated `AdminApplicationTests` context-load smoke test — don't assume behavior is covered by tests; verify manually or by reading calling code.

### Deployment

`deploy.bat` → `deploy.ps1` (config in `deploy-config.ps1`) is a real, working deployment script: it fetches `origin/master`, builds in a throwaway git worktree, SCPs the WAR to the UAT/prod server over SSH, and hot-swaps it into a running Tomcat (`/opt/tomcat-10.1.34`) with an automatic backup of the previous WAR. Treat this as a production deployment action — never run it without explicit user instruction, since it stops and restarts the live Tomcat process on the remote server.

## Configuration and profiles

- `application.properties` is the base config; `spring.profiles.active=uat` and `spring.profiles.include=swagger` are the defaults, with `application-uat.properties` / `application-prod.properties` layered on top for DB URLs and environment-specific base URLs.
- Config files contain live credentials and vendor secrets (DB password, JWT secret, SMS/Digilocker/Paysprint keys, AES base secret) committed directly in `src/main/resources/*.properties`. Be careful with these — don't print them unnecessarily and don't add new secrets in plaintext without asking.
- Swagger/OpenAPI UI is enabled at `/swagger-ui.html` and `/v3/api-docs` (springdoc), served under context path `/adminPortal`.

## Architecture

### Hierarchy model
`HierarchyMaster` defines the reseller levels (SA/AD/ET/WL/SD/PT/DT/RT — seeded by `HierarchyMasterDataInitializer` on startup). Two independent representations of the tree coexist and are kept in sync only by application code, not a foreign key: `AccessHierarchyNode`/`AccessHierarchyPath` (a real closure table — `{ancestorId, descendantId, depth}` — used for O(1) ancestor/descendant checks) and a `parentAccount` string field hand-copied onto each of the seven profile entities (`Admin`/`Enterprises`/`WhiteLabels`/`SuperDistributor`/`Partner`/`Distributor`/`Retailer`, no shared base class between them). Each level (except SuperAdmin) has its own wallet-balance + ledger table pair — e.g. `Retailer` → `RetailerPrepaidAmount`/`RetailerPostpaidAmount`/`RetailerGullakAmount` + matching `*TransactionDetails`, mirrored per level (Gullak only exists for Retailer and Distributor) — 32 physical tables total, no shared wallet abstraction. A raw-JDBC layer (`CommonMethods`, `DynamicTransactionRepositoryImpl`, `DynamicWalletRepository`) resolves the right table name per hierarchy level at runtime and writes ledger rows via native SQL, bypassing JPA.

`HierarchyAccessService`/`HierarchyAccessValidator` are the intended subtree-access enforcers (`hasAccessToHierarchy()` walks the closure table, with an unconditional bypass for `AD`), but **they are not consistently called** — e.g. `DashboardController`/`DashboardServiceImpl` never invoke them, so as written any authenticated caller can request another hierarchy's dashboard/wallet data by ID. Don't assume access is scoped just because the service exists; check whether the specific controller path actually calls it.

The documented `HierarchyAggregationStrategy`/`DashboardStrategy` rollup pattern (`RtDashboardStrategy`, `SdDashboardStrategy`, `ADDashboardStrategy`) is **largely dead code** — only one aggregation strategy is ever registered in the `Map`, so looking one up by level throws for anything else, and no controller references the `DashboardStrategy` family at all. The dashboard that's actually live is `strategy/DashboardServiceImpl` (2,400+ lines), which independently switches on hierarchy type and does its own recursive subtree walk via `UserDetailsRepository.findByParentAccount` rather than the closure table built for that purpose. If you're touching dashboard/aggregation code, work in `DashboardServiceImpl`, not the strategy classes.

### Multi-vendor integration pattern
Every external-vendor domain (AEPS, DMT, Recharge/BBPS, Payment Gateway, Payout) follows the same three-layer shape — when adding a new vendor, follow the existing pattern rather than inventing a new one:
1. **Vendor-specific impl** in `service/impl/` (e.g. `FingPayAepsServiceImpl`, `PaysprintDmtServiceImpl`, `RazorPayPGServiceImpl`, `RunPaisaPayoutServiceImplVO`) implementing a shared interface (`AepsVendorService`, DMT/Payout equivalents) or, for payment gateways, `PaymentGatewayInitiator` in `paymentgateway/`.
2. **Factory** resolves the configured vendor by name/code at runtime: `AepsVendorFactory`, `DmtVendorFactory`, `RechargeVendorFactory`, `PayoutVendorFactory` (in `util/`), and `PaymentGatewayInitiatorFactory` (in `paymentgateway/`). These are populated by Spring autowiring `List<Interface>` and keying by vendor code — first-word/substring fallback matching is intentional (DB-configured vendor names don't always match bean names exactly).
3. **Orchestrator** (`orchestrator/AepsOrchestrator`, `DmtOrchestrator`, `PGOrchestrator`, `PayoutOrchestrator`, `RechargeOrchestrator`) holds the business logic that's vendor-agnostic — scheme/limit validation, `VendorRouting` lookup (which vendor is active for a given sub-product), calling the resolved vendor via the factory, and persisting the resulting transaction mapping. Controllers *should* call orchestrators, not vendor impls directly — but this is not universal (see below).

**Pattern drift — nearly every domain has a parallel legacy path that bypasses the factory/orchestrator:** `InstantpayAepsTransactionServiceImpl` is a fully self-contained duplicate of AEPS balance/withdrawal/mini-statement that resolves vendor routing itself instead of going through `AepsVendorFactory`/`AepsOrchestrator` (only InstantPay *onboarding* is factory-routed); `RemitterServiceImpl`/`DmtBeneficiaryServiceImpl` are older Paysprint-hardcoded DMT implementations running alongside `DmtOrchestrator`; BBPS bill payment (`BBPSServiceImpl`) is hardcoded to an "A2Z"-style flow that doesn't go through `RechargeVendorFactory` at all. Also several vendors wired into a factory are non-functional stubs returning `null`/empty (`AirtelAepsServiceImpl`, `A2ZRechargeServiceImpl` — not even `@Service`-annotated, `FingpayDmtServiceImpl`) — check an impl is actually implemented before assuming a vendor path works end-to-end.

Vendor callbacks (async webhooks from PGs/payout providers) land in `callbacks/` (`PGCallbackController`, `PayoutCallbackController`, per-vendor async processors). `CallbackParserFactory` → per-vendor `*CallbackParser` (e.g. `CashfreeCallbackParser`, `RazorpayCallbackParser`) exists but in practice only backs the RunPaisa path — most callback controllers dispatch straight into that vendor's own `*Service`/`*ServiceImpl` instead. Callback URLs are whitelisted as unauthenticated in both `SecurityConfig` and `AESFilter` (see below) since external vendors can't present an app token — **`/api/recharge/callback` and `/api/payout/unisus-callback` are currently missing from both whitelists** despite being real vendor webhooks, which would reject those deliveries as unauthenticated; check this before assuming a new/existing callback route is reachable.

Status-check cron jobs live in `cronJob/` (`PgCheckStatusScheduler`, `PayoutCheckStatusScheduler`, `RechargeStatusScheduler`, `BillAvenueStatusScheduler`, etc.) and poll orchestrators/services for transactions stuck in `PENDING`. Coverage is uneven: there's no reconciliation cron at all for AEPS or DMT, `PgCheckStatusScheduler` fetches vendor status but discards it without persisting, and payout reconciliation only covers vendors implementing the `VendorStatusChecker` marker interface (Razorpay and Shadval payouts don't).

### Authentication — not standard Spring Security
Despite `spring-boot-starter-security` and a JJWT dependency being present, request auth is **not** handled by Spring Security's filter chain (`SecurityConfig.securityFilterChain` permits `anyRequest()`) or by `JwtUtil` (confirmed dead — zero references outside its own file). Instead, `AESFilter` is the real gatekeeper: it expects `Authorization: Bearer <hierarchyCode>|<saltBase64>|<ivBase64>|<encryptedToken>`, derives a PBKDF2 key from `app.security.base-secret` + the request's salt, AES-decrypts the token, and checks only an embedded expiry timestamp (no signature/HMAC over the payload). Both `SecurityConfig.PATHS_VIEWABLE_WITHOUT_AUTHENTICATION` and `AESFilter.PATHS_VIEWABLE_WITHOUT_AUTHENTICATION` maintain their own whitelist of public paths (login/OTP endpoints, vendor callbacks, swagger, websockets) — **keep these two lists in sync** when adding a new public endpoint, since either one alone can block/allow a route. `TokenUtil.extractHierarchyId()` (used across ~35 files for authorization logic downstream of the filter) just string-splits the header on `|` without re-decrypting — it trusts that `AESFilter` already validated the token.

There are two live, independently public routes that both mint a fully-trusted session token: the intended flow `POST /auth/login` → OTP via `POST /auth/verify-otp`, and a second, OTP-less shortcut `POST /api/aes/generate-token` that does the same credential check and issues an equally-valid token. Login also still accepts a legacy plaintext-equality password fallback alongside the BCrypt check. Keep this in mind if asked to "harden login" — OTP alone isn't the actual gate.

### Layering conventions
`controller/` → `service/` (interface) → `service/impl/` (implementation) → `repositories/` (Spring Data JPA, `entities/`). `dto/`, `request/`, `response/` separate API payload shapes from entities; `mapper/` and `ModelMapper` (bean, defined in `AdminApplication`) handle conversions. `spec/` holds `JpaSpecification`-based dynamic query builders for filterable list/report endpoints (`GenericSpecification` + per-entity specs). Reporting/export endpoints (`service/reports/`, Apache POI/OpenCSV) generate Excel/CSV downloads for transactions, wallets, and logs, some triggered by `cronJob/` schedulers (`HourlyReportScheduler`, `TransactionReportScheduler`, `WalletHistoryReportScheduler`).

### Other integrations
WebSocket support (`WebSocketConfig`/`WebSocketAuthConfig`/`service/impl/WebSocketService`) for live notifications — note `WebSocketAuthConfig` only labels the STOMP session from the cleartext token prefix, it never decrypts/validates it, and a missing `Authorization` header is silently allowed through. Email (`EmailService`) and SMS (`commons/ShivSmsService`, plus a hardcoded-credential `SMSGatewayAPI`/Gupshup fallback) for OTP/notifications. DigiLocker, Surepass, and various KYC/eKYC vendor integrations under `service/impl/*Ekyc*`. AOP logging aspects (`commons/GlobalApiLoggingAspect`, `VendorServiceLoggingAspect` with `@LoggableApi`/`@LoggableVendor` annotations) provide cross-cutting request/vendor-call logging into `VendorLogs`. Note `AuditEntityListener`'s `createdBy`/`editedBy` stamping always resolves to `"SYSTEM"`, since Spring Security's `SecurityContext` (which it reads from) is never actually populated under this app's AES-based auth — don't rely on those columns for per-user attribution.

## Known issues (verified by code read, not yet fixed)

These are correctness/financial/security gaps found during a full architecture review — worth knowing before touching related code, and worth fixing deliberately rather than accidentally papering over:

- **Payments**: Cashfree webhook signature verification in `PGCallbackController` is present but commented out (logs "verified" unconditionally). `PgCheckStatusScheduler`'s reconciliation fetches vendor status but discards it without persisting.
- **Payouts**: `RazorpayPayoutServiceImpl.sendPayout()` falls through to `return null` on its success path, so successful Razorpay payouts are reported as failures upstream. No code path automatically reverses a wallet pre-debit when a payout fails.
- **Wallets/concurrency**: no pessimistic locking, `SELECT...FOR UPDATE`, or version column anywhere in wallet debit/credit code (`CommonMethods` and friends) — reads-then-writes race under concurrent requests, and `FundingServiceImpl`'s multi-threaded bulk-upload path makes this more likely.
- **Authorization**: dashboard/wallet-balance endpoints don't call `HierarchyAccessService` — any authenticated caller can query another hierarchy's data by ID.
- **DMT limits**: the per-vendor ₹25k/month cap on `DmtRemitter` is trivially bypassed to ~₹75k by rotating across the three routed vendors.
- **Fund movement**: `MoveToBankServiceImpl`'s AES-credential vendor branch never debits the wallet; `FundRequestServiceImpl`'s approval method is entirely commented out (inert — `PrefundRequestServiceImpl` is the flow actually in use).
- **Exceptions**: `ResourceNotFoundException`'s `@ResponseStatus(404)` is dead — `GlobalExceptionHandler`'s broader `RuntimeException` handler intercepts first, so it's actually served as 400.
- **Config**: RunPaisa payout `client_id`/`client_secret` in `application.properties` are literal placeholder text (`"not yet"`); BillAvenue credentials are hardcoded in source and its status-check call points at a staging host.
- **CORS**: three independent, drifted allowlists (`SecurityConfig`, `CorsFilterConfig`, `WebConfig`) — a new white-label frontend domain needs adding in up to three places.

A full architecture dossier (data model, complete API surface, per-domain transaction flows, and the full risk register) was produced during this review but not persisted to the repo — regenerate on request if deeper detail is needed than the summary above.
