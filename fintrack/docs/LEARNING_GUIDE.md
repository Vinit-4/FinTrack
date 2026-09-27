# FinTrack — learning and interview guide

This is the document to read before an interview. Each module answers the same
five questions: what it does, why it exists, how data flows through it, which
files matter, and what you will be asked about it.

---

## The two flows to memorise

Say these out loud until they are automatic.

**Inside the backend**

```
Controller  →  Service  →  Repository  →  JPA/Hibernate  →  PostgreSQL
```

- **Controller** receives the HTTP request, checks nothing but shape, calls a service, returns a status code.
- **Service** holds the rules: is this allowed, what should be calculated, what should be saved.
- **Repository** is an interface. Spring Data writes the implementation at startup.
- **Hibernate** turns objects into SQL and rows back into objects.
- **PostgreSQL** stores it.

**End to end**

```
React component
  → Axios HTTP request (with the JWT in the Authorization header)
    → Spring controller
      → Service
        → Repository
          → PostgreSQL
        ← rows
      ← entities, converted to DTOs
    ← JSON response
  ← React state updates, the UI re-renders
```

Concretely, adding an expense: `AddTransaction.jsx` calls
`client.post('/transactions', payload)` → `TransactionController.create` →
`TransactionService.create` → `transactionRepository.save` → Hibernate issues
`INSERT INTO transactions ...` → the saved row comes back as a `Transaction`
entity → `TransactionMapper` turns it into a `TransactionResponse` → Spring
serialises that to JSON → React navigates to the transactions list.

---

## Module 1 — Entities and the database

**What it does.** Defines the two tables as Java classes: `User` and `Transaction`.

**Why we need it.** With JPA you describe the shape of your data once, as
objects, and Hibernate handles the SQL. You stop writing `INSERT`/`SELECT` by
hand and stop mapping `ResultSet` columns into objects.

**Data flow.** `@Entity` class → Hibernate reads the annotations at startup →
creates or updates the table → at runtime, objects become rows and rows become objects.

**Files.** `entity/User.java`, `entity/Transaction.java`, `entity/Category.java`,
`entity/TransactionType.java`, `database/schema.sql`.

**Code to understand.**

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "user_id", nullable = false)
private User user;
```

Many transactions, one user. `LAZY` means Hibernate does not fetch the user row
until you actually call `transaction.getUser()`. And:

```java
@Column(nullable = false, precision = 15, scale = 2)
private BigDecimal amount;
```

`BigDecimal`, never `double`. Binary floating point cannot represent `0.1`
exactly, so sums drift. In money code that is a bug you cannot ship.

**Interview questions.**

- *What is ORM / what does Hibernate do?* Maps Java objects to relational rows so you work with objects instead of SQL strings.
- *`@ManyToOne` vs `@OneToMany`?* Which side holds the foreign key. Here `Transaction` owns `user_id`, so `@ManyToOne` is on `Transaction`.
- *Why no `List<Transaction>` inside `User`?* We never need "load a user and all their transactions at once", and it would invite accidentally loading thousands of rows. The repository queries by `user.id` instead.
- *LAZY vs EAGER, and the N+1 problem?* EAGER loads the related row every time. With a list of 100 transactions each eagerly loading its user, you get 1 + 100 queries. LAZY avoids it; when you genuinely need both, use a `JOIN FETCH`.
- *Why `EnumType.STRING` and not `ORDINAL`?* ORDINAL stores the position, so inserting a new enum value in the middle silently corrupts every existing row.
- *`ddl-auto=update` in production?* No. It never drops or renames safely. Use Flyway or Liquibase.

---

## Module 2 — Repository layer

**What it does.** Declares the database operations we need. No implementation is written.

**Why we need it.** Spring Data JPA generates the implementation from the method
name at startup. `findByIdAndUserId(Long id, Long userId)` becomes
`SELECT ... WHERE id = ? AND user_id = ?`.

**Data flow.** Service calls the interface method → Spring's proxy builds the
query → Hibernate runs it → rows come back as entities.

**Files.** `repository/UserRepository.java`, `repository/TransactionRepository.java`,
`util/TransactionSpecifications.java`.

**Code to understand.**

```java
Optional<Transaction> findByIdAndUserId(Long id, Long userId);
```

This single line is the security model of the whole application. Ownership is
enforced by the `WHERE` clause, so it cannot be forgotten.

```java
@Query("SELECT t.category, COALESCE(SUM(t.amount), 0) FROM Transaction t " +
       "WHERE t.user.id = :userId AND t.type = :type " +
       "AND t.transactionDate BETWEEN :start AND :end " +
       "GROUP BY t.category ORDER BY SUM(t.amount) DESC")
List<Object[]> sumGroupedByCategory(...);
```

JPQL, not SQL: it queries the *entity* `Transaction`, not the table. The database
does the aggregation, which is what databases are for.

**Interview questions.**

- *How does Spring Data implement an interface?* A dynamic proxy created at startup; the method name is parsed into a query.
- *Derived query vs `@Query`?* Derived for simple lookups; `@Query` when the name would become unreadable or you need aggregation.
- *JPQL vs native SQL?* JPQL is over entities and stays portable; native SQL is database-specific but can use vendor features.
- *Why `COALESCE(SUM(...), 0)`?* `SUM` of zero rows returns `NULL`, which would make the dashboard show blank instead of ₹0.
- *Why `Optional`?* It forces the caller to handle "not found" instead of dereferencing `null`.

---

## Module 3 — Service layer

**What it does.** All business logic: validation beyond annotations, ownership
checks, calculations, mapping entities to DTOs.

**Why we need it.** Controllers would otherwise become huge and untestable, and
logic would be duplicated when a second caller appears. Services are also where
`@Transactional` belongs.

**Data flow.** Controller passes the current user's id and a request DTO → service
loads or builds entities → repository persists → service maps to a response DTO.

**Files.** `service/AuthService.java`, `service/TransactionService.java`,
`service/DashboardService.java`, `service/AnalyticsService.java`,
`util/TransactionMapper.java`.

**Code to understand.**

```java
private Transaction findOwnedTransaction(Long userId, Long transactionId) {
    return transactionRepository.findByIdAndUserId(transactionId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
}
```

Every read, update and delete goes through this one method. Another user's id
produces exactly the same 404 as a non-existent id, so the API never leaks the
fact that the row exists.

**Interview questions.**

- *Why not put this in the controller?* Separation of concerns; a controller is about HTTP, a service is about the domain. Services are also unit-testable without spinning up a web server.
- *What does `@Transactional` do?* Wraps the method in a database transaction: everything commits together or rolls back together. `readOnly = true` lets the driver and Hibernate skip dirty-checking.
- *Why 404 and not 403 for someone else's transaction?* 403 admits the resource exists. 404 tells an attacker nothing.
- *Where are the dashboard totals calculated?* In PostgreSQL, via `SUM`/`GROUP BY`. Sending every transaction to the browser to add up would not survive real data.

---

## Module 4 — DTOs and validation

**What it does.** Defines the exact JSON the API accepts and returns.

**Why we need it.** Returning a `Transaction` entity would serialise its `User`,
including the password hash. DTOs also let the API shape stay stable when the
database changes.

**Data flow.** JSON → Jackson builds the request DTO → `@Valid` runs the
constraints → service → response DTO → Jackson writes JSON.

**Files.** everything in `dto/`, plus the `@Valid` annotations in the controllers.

**Code to understand.**

```java
@NotNull(message = "Amount is required")
@DecimalMin(value = "0.01", message = "Amount must be greater than zero")
private BigDecimal amount;
```

The "amount greater than zero" rule lives on the server. The React form checks it
too, but only for a faster message — client-side validation is a convenience, not
a control, because anyone can call the API directly with curl.

**Interview questions.**

- *Why DTOs instead of entities?* Security (no password hash leaking), stability, and the ability to expose a different shape than you store.
- *How does `@Valid` fail?* It throws `MethodArgumentNotValidException`, which `GlobalExceptionHandler` turns into a 400 with a `fieldErrors` map.
- *Is client-side validation enough?* No. Never trust the client.

---

## Module 5 — Security: BCrypt, Spring Security and JWT

**What it does.** Registers and authenticates users, and blocks every request that
does not carry a valid token.

**Why we need it.** Financial data. Without this layer anyone could read anyone's
ledger by changing an id in the URL.

**Data flow, registration.** `RegisterRequest` → check the email is free →
`passwordEncoder.encode(password)` → save the hash → sign a JWT → return it.

**Data flow, every later request.** `JwtAuthenticationFilter` reads the
`Authorization` header → `JwtService` verifies the signature and expiry →
`CustomUserDetailsService` loads the user → a `UserPrincipal` is placed in the
`SecurityContext` → the controller receives it through `@AuthenticationPrincipal`.

**Files.** `security/SecurityConfig.java`, `security/JwtService.java`,
`security/JwtAuthenticationFilter.java`, `security/UserPrincipal.java`,
`security/CustomUserDetailsService.java`, `security/JwtAuthenticationEntryPoint.java`,
`service/AuthService.java`.

**Code to understand.**

```java
.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

Stateless: the server keeps nothing between requests, so the token has to prove
everything each time. Our filter runs before Spring's form-login filter.

```java
public ResponseEntity<TransactionResponse> create(
        @AuthenticationPrincipal UserPrincipal currentUser, ...)
```

The user id comes from the verified token, never from the request body. A client
cannot claim to be someone else.

**Interview questions.**

- *What is BCrypt, and why not SHA-256?* BCrypt is a deliberately slow, salted password hashing function. SHA-256 is fast, which is exactly what an attacker wants when brute-forcing.
- *What is the salt for?* It makes two identical passwords hash differently, defeating precomputed rainbow tables. BCrypt stores the salt inside the hash string.
- *Is hashing encryption?* No. Hashing is one-way; there is no key and nothing to decrypt. We never need the original password — we hash the attempt and compare.
- *What are the three parts of a JWT?* Header (algorithm), payload (claims: subject, userId, issued-at, expiry), signature. Base64-encoded and dot-separated.
- *Is the payload encrypted?* No, only signed. Anyone can read it; nobody can change it without the secret. Never put a secret in a JWT payload.
- *Session vs JWT?* Sessions store state on the server and scale by sharing that store. JWTs store state in the token: nothing to look up, but you cannot revoke one before it expires.
- *How do you log out then?* The client deletes the token. To revoke server-side you need short-lived access tokens plus refresh tokens, or a blacklist.
- *Why is CSRF disabled?* CSRF exploits credentials the browser sends automatically, i.e. cookies. Our token is attached by JavaScript to an explicit header, so there is nothing to forge.
- *Why is `/api/auth/**` public?* You cannot present a token before you have one.
- *What happens with an expired token?* `JwtService.isTokenValid` returns false, no authentication is set, `JwtAuthenticationEntryPoint` returns a JSON 401, and the axios interceptor sends the user to the login page.
- *Where is the secret?* An environment variable. The app refuses to start without it, so it cannot be committed by accident.

---

## Module 6 — Filtering with Specifications

**What it does.** Builds the `WHERE` clause of `GET /transactions` at runtime from
whichever query parameters were sent.

**Why we need it.** Seven optional filters would otherwise mean writing a separate
repository method for every combination.

**Data flow.** Query string → `TransactionFilter` DTO (Spring binds it via
`@ModelAttribute`) → `TransactionSpecifications.forUserWithFilters` builds a list
of predicates → `repository.findAll(spec, sort)` → Hibernate assembles one SQL query.

**Files.** `util/TransactionSpecifications.java`, `dto/TransactionFilter.java`,
`service/TransactionService.search`.

**Code to understand.**

```java
predicates.add(builder.equal(root.get("user").get("id"), userId));
if (filter.getType() != null) {
    predicates.add(builder.equal(root.get("type"), filter.getType()));
}
```

The user predicate is added unconditionally and first. Every other predicate is
optional. That ordering is not cosmetic — it is why this endpoint cannot leak.

**Interview questions.**

- *What is the Criteria API?* A type-safe, programmatic way to build queries as objects instead of strings.
- *Why not one `@Query` with `:param IS NULL OR ...`?* It works, but the database cannot use indexes well on those conditions, and it becomes unreadable with seven filters.
- *How do you add a filter?* Add the field to `TransactionFilter` and one `if` block to the specification. Nothing else changes.
- *What would you add for scale?* `Pageable`, for `page`, `size` and a total count.

---

## Module 7 — Exception handling

**What it does.** Converts every exception into the same JSON error shape.

**Why we need it.** Without it, Spring returns default error pages and stack
traces, which leak implementation details and are unusable for the frontend.

**Data flow.** Service throws → the exception propagates past the controller →
`@RestControllerAdvice` catches it by type → builds an `ErrorResponse` with the
right status.

**Files.** `exception/GlobalExceptionHandler.java`, the three custom exceptions,
`dto/ErrorResponse.java`.

**Code to understand.**

```java
@ExceptionHandler(BadCredentialsException.class)
public ResponseEntity<ErrorResponse> handleBadCredentials(...) {
    return build(HttpStatus.UNAUTHORIZED, "Invalid email or password", request);
}
```

Deliberately vague. "No account with that email" would let someone enumerate
which addresses are registered.

**Interview questions.**

- *`@ControllerAdvice` vs `@RestControllerAdvice`?* The Rest version adds `@ResponseBody`, so handlers return JSON.
- *Checked vs unchecked exceptions here?* All custom exceptions extend `RuntimeException`, so services do not need `throws` clauses and Spring can roll transactions back by default.
- *Why does the generic `Exception` handler not return the message?* Internal messages can leak schema details. They go to the log; the client gets a generic sentence.

---

## Module 8 — Dashboard and analytics

**What it does.** Turns raw transactions into the numbers the cards and charts show.

**Why we need it.** Calculations belong on the server: the browser should not
download a year of transactions to draw a pie chart, and the logic stays testable.

**Data flow.** Controller → service → several aggregate queries →
assembled into a response DTO → JSON → Recharts.

**Files.** `service/DashboardService.java`, `service/AnalyticsService.java`,
`controller/DashboardController.java`, `controller/AnalyticsController.java`.

**Code to understand.**

```java
YearMonth currentMonth = YearMonth.now();
LocalDate monthStart = currentMonth.atDay(1);
LocalDate monthEnd   = currentMonth.atEndOfMonth();
```

`YearMonth` handles month lengths and leap years so you never hardcode 30 or 31.

In `getMonthlyTrend`, one range query is issued and the months are grouped in
Java. That keeps the JPQL portable, and for a personal ledger it is a few hundred
rows at most. With millions of rows you would push the grouping into SQL with
`date_trunc`.

**Interview questions.**

- *Why aggregate in SQL and not Java?* The database is optimised for it and sends back a handful of numbers instead of every row.
- *When would you cache this?* If the dashboard were hit constantly and data changed rarely. Here it is one user's own data, so it is not worth the invalidation complexity.
- *How would you handle a different currency per user?* Store a currency code on the user (or per transaction) and never mix currencies in a `SUM`.

---

## Module 9 — React frontend

**What it does.** The interface: auth pages, dashboard, transaction list and form, analytics.

**Why we need it.** It is the only part a non-technical user sees. It holds no
business logic — every figure it displays is computed by the backend.

**Data flow.** A component's `useEffect` runs on mount → axios sends the request
with the token attached by the interceptor → the response goes into `useState` →
React re-renders.

**Files.** `api/client.js`, `context/AuthContext.jsx`, `components/ProtectedRoute.jsx`,
`pages/Dashboard.jsx`, `pages/Transactions.jsx`, `pages/AddTransaction.jsx`.

**Code to understand.**

```javascript
client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
```

One place attaches the token, so no page can forget.

```javascript
const [summaryRes, categoriesRes, monthlyRes] = await Promise.all([...])
```

Three independent requests fired together instead of one after another.

**Interview questions.**

- *What is `useEffect` for?* Side effects after render — fetching, subscriptions, timers. The dependency array controls when it re-runs.
- *Why Context instead of passing props?* The logged-in user is needed by unrelated components at different depths; Context avoids threading props through every level.
- *Why does the cleanup flag (`active`) exist?* To avoid setting state after the component unmounted, if the response arrives late.
- *Is `localStorage` the right place for a JWT?* It is simple and survives refresh, but it is readable by any JavaScript on the page, so an XSS bug exposes the token. An httpOnly cookie is safer against XSS but needs CSRF protection. Know the trade-off — it is a common follow-up question.
- *What is a controlled component?* One whose value comes from React state, so state is the single source of truth.

---

## Module 10 — CORS

**What it does.** Lets the browser at `localhost:5173` call the API at `localhost:8080`.

**Why we need it.** Browsers block cross-origin requests unless the server allows
them. Different port means different origin.

**Files.** `security/SecurityConfig.corsConfigurationSource()`.

**Interview questions.**

- *What is a preflight request?* For non-simple requests the browser first sends `OPTIONS` to ask what is allowed. That is why `OPTIONS` is permitted in the security config.
- *Is CORS a security feature?* It protects the *browser's* user; it does nothing against curl or Postman. Real protection is authentication and authorisation.
- *What changes in production?* `CORS_ALLOWED_ORIGINS` becomes the deployed frontend domain, never `*` with credentials.

---

## 13-day plan

Build in this order; each day ends with something that works.

| Day | Goal | Done when |
|---|---|---|
| 1 | Architecture, Spring Initializr project, PostgreSQL database, Vite React app | `mvn spring-boot:run` starts and connects to Postgres |
| 2 | `User` and `Transaction` entities, enums, relationship, indexes | Hibernate creates both tables on startup |
| 3 | Repositories and a first service; read the generated SQL in the console | You can save and read a transaction from a test |
| 4 | `AuthService` register and login, BCrypt, DTOs and validation | `POST /api/auth/register` returns 201 with a hashed password in the DB |
| 5 | Spring Security config, `JwtService`, the filter, `UserPrincipal` | A protected endpoint returns 401 without a token and 200 with one |
| 6 | Transaction CRUD endpoints, `findByIdAndUserId`, mapper | All five endpoints work in Postman |
| 7 | `TransactionFilter`, Specifications, validation messages | Filtering by category and month returns the right rows |
| 8 | `DashboardService` and `/dashboard/summary` | The six figures match what you can count by hand |
| 9 | `AnalyticsService`: categories, monthly summary, trend | The three analytics endpoints return sensible JSON |
| 10 | React: routing, `AuthContext`, axios interceptors, Login and Register | You can register in the browser and land on an empty dashboard |
| 11 | Dashboard cards, transaction table and form, Recharts | The full user flow works end to end |
| 12 | Tests, Swagger, `GlobalExceptionHandler`, bug fixing | `mvn test` is green and Swagger UI authorises |
| 13 | README, screenshots, `.env.example`, `.gitignore`, push to GitHub | A fresh clone runs by following your own README |

The ML module comes after day 13, if at all.

---

## Before the interview

Be ready to answer these three about *your* project, not about Spring in general:

1. **"Walk me through what happens when a user adds an expense."** Use the end-to-end flow at the top of this document. Name real classes.
2. **"How do you stop user A from reading user B's transactions?"** `findByIdAndUserId`, the user id taken from the verified JWT and never from the request, and 404 rather than 403 so nothing is leaked. Mention the test that proves it.
3. **"What would you change if this had a million users?"** Pagination on the transaction list, aggregation pushed fully into SQL with `date_trunc`, Flyway migrations, refresh tokens, a read replica for analytics, and monitoring. Showing you know what the project *is not* built for is a strong answer.

Two more worth preparing:

- **"What was the hardest part?"** Pick something real — the JWT filter chain, or why the dashboard sums had to move out of the frontend.
- **"What would you do differently?"** Also pick something real: for example, storing the token in an httpOnly cookie, or writing the schema as Flyway migrations from day one.
