# uBlog – Technical Review & Improvement Backlog

**Status:** Proof of Concept  
**Goal:** Maintain a minimalist, filesystem-based Java blogging engine with low memory consumption, optional authentication, and customizable Mustache skins.

## P0 – Security & Correctness

### 1. Fix session ID generation
- [x] Replace timestamp-based session IDs with cryptographically secure random tokens (`SecureRandom`).
- [x] Use at least 128 bits of randomness.
- [x] Prevent session ID collisions.
- [x] Avoid logging session identifiers.

**Affected:** `SessionService.java`

### 2. Enforce session expiration
- [x] Ensure session validity is checked when resolving a session.
- [x] Reject expired sessions before returning user information.
- [x] Remove expired sessions from memory.
- [x] Prevent expired sessions from being refreshed.

**Affected:** `SessionService.java`, `RequestHandlerService.java`

### 3. Correct logout handling
- [x] Fix the condition preventing logout from invalidating sessions.
- [x] Clear the browser's session cookie.
- [x] Ensure logout works even when the session has expired.
- [x] Separate logout processing from template context generation.

**Affected:** `RequestHandlerService.java`

### 4. Fix HTTP request framing
- [x] Read HTTP headers until the complete header terminator (`\r\n\r\n`) is received.
- [x] Read the request body according to `Content-Length`, measured in bytes.
- [x] Handle incomplete requests and unexpected connection termination.
- [x] Introduce request-size and header-size limits.
- [x] Introduce socket read timeouts.
- [x] Reject unsupported transfer encodings safely.
- [x] Handle malformed HTTP requests without terminating worker threads unexpectedly.

**Affected:** `PooledSocketHandler.java`, `HttpRequest.java`

### 5. Fix HTTP character encoding
- [x] Use explicit UTF-8 encoding when writing responses.
- [x] Calculate `Content-Length` based on the actual response bytes.
- [x] Decode incoming request bodies using the appropriate character encoding.
- [x] Ensure special and non-ASCII characters work correctly.

**Affected:** `PooledSocketHandler.java`, `HttpRequest.java`, `HttpResponse.java`

### 6. Secure password handling
- [x] Replace plaintext password comparison with password-hash verification.
- [x] Use Argon2id, bcrypt, or another established password-hashing algorithm.
- [x] Store algorithm parameters and salts alongside password hashes.
- [x] Handle missing or malformed password properties safely.
- [x] Introduce basic login rate limiting.

**Affected:** `LoginService.java`, `UserService.java`, `User.java`

### 7. Secure cookies and cookie parsing
- [x] Properly parse multiple cookies from a single `Cookie` header.
- [x] Add support for `HttpOnly`, `Secure`, `SameSite`, and `Path` attributes in response cookies.
- [x] Validate cookie names and values before serialization.
- [x] Prevent HTTP header injection.
- [x] Handle invalid and missing session cookies safely.
- [x] Avoid logging sensitive cookie values.

**Affected:** `Cookie.java`, `HttpRequest.java`, `HttpResponse.java`

### 8. Protect filesystem paths
- [x] Validate usernames before converting them into filenames.
- [x] Prevent `../` path traversal.
- [x] Normalize and validate requested content paths.
- [x] Restrict content access to the configured content root.
- [x] Prevent access to user property files and other private application files.
- [x] Define how symbolic links should be handled.
- [x] Apply the same restrictions to future file-editing functionality.

**Affected:** `UserService.java`, `MarkdownContentProvider.java`, `RequestHandlerService.java`

### 9. Fix concurrent access to shared state
- [x] Replace the session `HashMap` instances with a thread-safe design.
- [x] Consider using a single map associating session IDs with session objects.
- [x] Replace or remove the `WeakHashMap` user cache.
- [x] Make template caching safe for concurrent requests.
- [x] Ensure session creation, validation, refresh, and invalidation behave correctly under concurrency.

**Affected:** `SessionService.java`, `UserService.java`, `MustacheTemplateService.java`

---

# uBlog – Remaining Issues After Security Refactoring

**Reviewed commit:** `22cd100b70d96a1fbe94ae3087936daccec0446d`

## Objective

Review and fix the following remaining correctness, security, and concurrency concerns.

**Important constraints:**
- Preserve the existing lightweight, filesystem-based architecture.
- Do not introduce a web framework, database, or unnecessary dependencies.
- Preserve compatibility with Java 17+.
- Keep memory allocations low. The application has successfully handled authentication and page rendering with low memory footprint like `-Xmx4m`.
- Avoid major refactoring outside the affected components.
- Do not mark an issue complete without verifying its implementation.

---

## P0 – Fix before public deployment

### 1. HTTP request-size integer overflow

**File:** `HttpRequest.java`

Current code:

```java
if (header.length + contentLength > MAX_REQUEST_BYTES) {
    throw new RequestException(413, "Request too large");
}
```

Integer overflow can bypass this limit when a very large `Content-Length` is supplied.

**Tasks:**
- [x] Replace addition-based checking with overflow-safe bounds validation.
- [x] Reject oversized bodies before allocating the body buffer.
- [x] Reject negative, invalid, and conflicting `Content-Length` values.
- [x] Test maximum integer values and requests just above the configured limit.

Suggested approach:

```java
if (contentLength > MAX_REQUEST_BYTES - header.length) {
    throw new RequestException(413, "Request too large");
}
```

### 2. Eliminate redundant HTTP request parsing

**File:** `HttpRequest.java`

`readFrom(InputStream)` correctly reads the request body as bytes, but then reconstructs a textual HTTP request:

```java
return new HttpRequest(headerSection + body);
```

The existing string constructor subsequently reparses the request and uses string lengths where byte lengths are required.

**Tasks:**
- [x] Parse HTTP headers once.
- [x] Preserve the decoded request body directly.
- [x] Avoid reconstructing the original HTTP request string.
- [x] Remove legacy body-truncation logic based on `String.length()`.
- [x] Ensure charset decoding happens exactly once.
- [x] Test UTF-8 characters and multibyte request bodies.
- [x] Retain header limits, body limits, socket timeouts, and malformed-request handling.

**Acceptance criteria:** UTF-8 form submissions are handled correctly, including non-ASCII text, without body truncation or duplicate parsing.

### 3. Correct login form parsing

**File:** `RequestHandlerService.java`

Login form fields are currently extracted using string splitting:

```java
String[] parts = loginData.split("&");
String[] keyValue = part.split("=");
```

This does not correctly handle standard URL-encoded form data.

**Tasks:**
- [x] Parse `application/x-www-form-urlencoded` correctly.
- [x] Decode percent-encoded characters.
- [x] Handle `+` as a space within form values.
- [x] Preserve `=` and `&` characters when encoded in passwords.
- [x] Handle missing and duplicate fields safely.
- [x] Validate the form content type.
- [x] Require HTTP `POST` for `/loginaction`.


---

## P1 – Architecture & Maintainability

### 10. Separate request, session, and rendering contexts
- [ ] Define clear responsibilities for `SessionContext`.
- [ ] Separate authentication operations from session state.
- [ ] Introduce a generic request context if needed.
- [ ] Keep sensitive authentication information out of rendering contexts.
- [ ] Avoid duplicating authoritative session state.
- [ ] Make rendering data available through a stable template model.

**Affected:** `SessionContext.java`, `RequestHandlerService.java`, `BlogContext.java`

### 11. Decouple page data from Mustache
- [ ] Remove concrete `MustacheBlogContext` casting from `MustacheTemplateService`.
- [ ] Move content lookup and navigation construction out of the Mustache-specific context.
- [ ] Define a generic page model.
- [ ] Keep the template engine responsible primarily for rendering.
- [ ] Define stable template variables for third-party skins.

**Affected:** `MustacheBlogContext.java`, `MustacheTemplateService.java`, `BlogContext.java`

### 12. Improve content-cache consistency
- [ ] Avoid clearing shared caches while HTTP requests may be reading them.
- [ ] Consider building immutable metadata snapshots.
- [ ] Atomically replace the active snapshot after filesystem changes.
- [ ] Handle filesystem watcher overflow events.
- [ ] Ensure newly created subdirectories are registered correctly.
- [ ] Handle deleted or renamed directories.
- [ ] Clearly distinguish metadata caching from content caching.

**Affected:** `MarkdownContentProvider.java`

### 13. Introduce explicit page-resolution results
- [ ] Distinguish successful content resolution from missing content.
- [ ] Return HTTP `404` for nonexistent pages.
- [ ] Return appropriate authentication and authorization responses (`401`, `403`, or redirects).
- [ ] Return HTTP `400` for malformed requests.
- [ ] Keep error-page appearance configurable through templates.
- [ ] Avoid treating every request as HTTP `200`.

**Affected:** `RequestHandlerService.java`, `HttpResponse.java`, `ContentProvider.java`

### 14. Improve URL and filename handling
- [ ] Decode URL paths correctly.
- [ ] Encode generated links correctly.
- [ ] Support filenames containing spaces and Unicode characters.
- [ ] Establish consistent mappings between filenames and public URLs.
- [ ] Reject unsafe or ambiguous paths.
- [ ] Consider how permission suffixes affect public URLs.

**Affected:** `HttpRequest.java`, `MarkdownContentProvider.java`, template context classes

### 15. Review dependency injection and service abstractions
- [ ] Identify interfaces with only one implementation.
- [ ] Retain abstractions that serve actual extensibility requirements.
- [ ] Avoid unnecessary configuration complexity.
- [ ] Keep the processing pipeline understandable.
- [ ] Avoid adding abstractions solely for hypothetical future features.

**Affected:** `Config.java`, service interfaces and implementations

---

## P2 – Performance & Minimalism

### 16. Reduce unnecessary request allocations
- [ ] Avoid rendering `Error.md` during every context initialization.
- [ ] Initialize only the data required for the requested page.
- [ ] Avoid building unused navigation or entry lists.
- [ ] Review temporary object creation during rendering.
- [ ] Measure allocations before introducing optimizations.

**Affected:** `MustacheBlogContext.java`, `RequestHandlerService.java`

### 17. Remove unused database infrastructure
- [x] Remove H2 if no longer required.
- [x] Remove unused database classes and services.
- [x] Remove database-related configuration properties.
- [ ] Remove database setup from the container image.
- [ ] Remove unused database documentation.
- [ ] Confirm that the application runs without database dependencies.

**Affected:** `pom.xml`, `Config.java`, `Containerfile`, persistence classes

### 18. Reduce container image size
- [ ] Replace the full JDK base image with a runtime-only image.
- [ ] Experiment with `jlink`.
- [ ] Determine the minimal required Java modules.
- [ ] Remove unnecessary files and dependencies from the final container.
- [ ] Run the application as a non-root user.
- [ ] Avoid globally writable (`777`) application directories.
- [ ] Measure compressed image size and process RSS.

**Affected:** `Containerfile`, `pom.xml`

### 19. Establish reproducible memory benchmarks
- [ ] Verify operation with an 8 MB heap.
- [ ] Test whether a 4 MB heap is practical.
- [ ] Measure idle and peak heap usage.
- [ ] Measure process RSS.
- [ ] Measure allocations per rendered request.
- [ ] Test with 10, 100, and 1,000 Markdown articles.
- [ ] Test concurrent requests and large articles.
- [ ] Compare rendering with template caching enabled and disabled.
- [ ] Record latency percentiles rather than only average response times.

### 20. Optimize content and template processing
- [ ] Evaluate repeated Markdown parsing and rendering costs.
- [ ] Consider optional caching of rendered Markdown.
- [ ] Invalidate cached content when files change.
- [ ] Avoid unnecessary intermediate strings.
- [ ] Preserve the low-memory design rather than introducing unconditional content caching.

---

## P3 – Usability, Testing & Documentation

### 21. Ensure deterministic content ordering
- [ ] Sort categories consistently.
- [ ] Sort article listings consistently.
- [ ] Sort quick links consistently.
- [ ] Consider metadata-defined ordering later.

**Affected:** `MarkdownContentProvider.java`

### 22. Introduce automated tests
- [ ] HTTP request parsing tests.
- [ ] Cookie parsing and serialization tests.
- [ ] Session creation, expiration, and invalidation tests.
- [ ] Authentication tests.
- [ ] Concurrent-session tests.
- [ ] Filesystem path validation tests.
- [ ] Filesystem watcher and cache tests.
- [ ] Template rendering tests.
- [ ] HTTP error-response tests.
- [ ] Low-memory integration tests.

### 23. Clean up repository and configuration
- [ ] Remove stale configuration properties.
- [ ] Review sample user files for exposed credentials.
- [ ] Ensure test credentials are never reused in real deployments.
- [ ] Update the README to reflect authentication and sessions.
- [ ] Document supported environment variables.
- [ ] Document the content directory structure.
- [ ] Document application-data directory permissions.
- [ ] Document known limitations of the custom HTTP implementation.
- [ ] Add a minimal deployment example.

---

## Planned Feature Development

These are future capabilities rather than defects identified in the current implementation.

### 24. Filesystem-based user management
- [ ] Support `.admins` and `.users` directories, or equivalent role directories.
- [ ] Define user properties and metadata.
- [ ] Reject duplicate usernames across roles.
- [ ] Support account activation and deactivation.
- [ ] Ensure user changes invalidate cached authorization data.
- [ ] Add optional public user profiles.

### 25. File-based article metadata
- [ ] Load optional properties files matching Markdown filenames.
- [ ] Support title, author, date, tags, and visibility metadata.
- [ ] Define public, user-only, and administrator-only visibility.
- [ ] Enforce visibility at content resolution, not only navigation generation.
- [ ] Preserve sensible defaults when metadata is missing.

### 26. Customizable skins
- [ ] Define a stable template-data contract.
- [ ] Support separate directories for different skins.
- [ ] Allow skin selection through configuration.
- [ ] Support skin-specific CSS, images, and JavaScript.
- [ ] Document available template variables.
- [ ] Keep skins independent of internal Java implementations.
- [ ] Ensure user-supplied content is safely rendered as HTML.

### 27. Browser-based Markdown editor
- [ ] Create a separate administration template.
- [ ] Integrate a lightweight JavaScript Markdown editor.
- [ ] Implement authenticated content read/write endpoints.
- [ ] Restrict editing privileges.
- [ ] Protect state-changing requests against CSRF.
- [ ] Validate filenames and paths.
- [ ] Use safe or atomic file replacement.
- [ ] Handle concurrent edits.
- [ ] Support article creation, editing, renaming, and deletion.

---

## Guiding Principles

1. **Filesystem first:** Content and user data remain accessible without uBlog.
2. **Minimal dependencies:** Prefer standard Java functionality where reasonable.
3. **Low memory footprint:** Measure actual resource usage and avoid unnecessary allocations.
4. **Simple deployment:** Ideally one JAR, a content directory, and a skin directory.
5. **Clear boundaries:** HTTP, authentication, content resolution, and rendering remain separate concerns.
6. **Optional complexity:** Basic public blogging should not require authentication or an editor.
7. **Security without framework bloat:** Use established cryptographic primitives and correct HTTP behavior.
8. **Incremental development:** Preserve a working application while improving individual components.

**Target:** A secure, customizable, filesystem-native blogging engine that remains understandable, lightweight, and easy to deploy.