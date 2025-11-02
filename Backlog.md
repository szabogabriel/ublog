# uBlog Project Backlog

**Last Updated:** November 2, 2025

This document tracks all known issues, improvements, and feature requests for the jFileListing/ublog project.

---

## 🐛 Critical Bugs & Issues

### #1 - Filesystem Watchdog Permissions
**Priority:** HIGH  
**Status:** Open  
**Description:** The WatchService in `MarkdownContentProvider` fails with AccessDeniedException in containerized environments
**Tasks:**
- [ ] Make watchdog optional via configuration flag
- [ ] Add fallback mechanism when watchdog can't initialize
- [ ] Log appropriate warnings when watchdog disabled
- [ ] Update documentation about container limitations

**Files:** `src/main/java/info/gabrielszabo/ublog/content/MarkdownContentProvider.java`

---

### #2 - Recursive Error Handling
**Priority:** MEDIUM  
**Status:** Fixed (Needs Testing)  
**Description:** Fixed infinite recursion in `getEntryContentAsHttp()` but needs comprehensive testing
**Tasks:**
- [ ] Add unit tests for error handling scenarios
- [ ] Test with missing Error.md file
- [ ] Test with invalid category paths
- [ ] Verify error page displays correctly

**Files:** `src/main/java/info/gabrielszabo/ublog/content/MarkdownContentProvider.java`

---

### #3 - Thread Safety Issues
**Priority:** HIGH  
**Status:** Open  
**Description:** `MarkdownContentProvider` cache updates aren't fully synchronized. The `categories`, `quickLinks`, and `entries` maps are modified without proper synchronization during cache refresh.
**Tasks:**
- [ ] Add synchronized blocks around cache modification
- [ ] Consider using ConcurrentHashMap for all caches
- [ ] Review all multi-threaded access patterns
- [ ] Add thread safety tests

**Files:** `src/main/java/info/gabrielszabo/ublog/content/MarkdownContentProvider.java`

---

### #4 - Resource Cleanup on Shutdown
**Priority:** HIGH  
**Status:** Open  
**Description:** No proper shutdown hooks for cleaning up resources
**Tasks:**
- [ ] Add shutdown hook to call `ConsoleLogService.shutdown()`
- [ ] Stop watchdog thread gracefully on application exit
- [ ] Ensure database connections close properly
- [ ] Add graceful shutdown for server socket
- [ ] Handle SIGTERM signal in containers

**Files:** 
- `src/main/java/info/gabrielszabo/ublog/App.java`
- `src/main/java/info/gabrielszabo/ublog/log/ConsoleLogService.java`
- `src/main/java/info/gabrielszabo/ublog/content/MarkdownContentProvider.java`

---

## 🔧 Configuration & Setup

### #5 - Configuration Management
**Priority:** MEDIUM  
**Status:** Open  
**Description:** `Config` class has hardcoded defaults and unclear property loading logic
**Tasks:**
- [ ] Create `application.properties` with all available options
- [ ] Document all configuration properties
- [ ] Support environment variables for container deployment
- [ ] Add configuration validation on startup
- [ ] Provide sensible defaults for all settings
- [ ] Add config reload capability (optional)

**Files:** 
- `src/main/java/info/gabrielszabo/ublog/config/Config.java`
- `src/main/resources/application.properties` (to be created)

---

### #6 - Port Configuration
**Priority:** MEDIUM  
**Status:** Open  
**Description:** Server port is hardcoded to 8080
**Tasks:**
- [ ] Make port configurable via `Config` class
- [ ] Support `PORT` environment variable
- [ ] Support `SERVER_PORT` property in config file
- [ ] Update documentation with port configuration examples
- [ ] Validate port number range

**Files:** 
- `src/main/java/info/gabrielszabo/ublog/server/Server.java`
- `src/main/java/info/gabrielszabo/ublog/config/Config.java`

---

### #7 - Missing Configuration Options
**Priority:** LOW  
**Status:** Open  
**Description:** Several configuration options that should be configurable are hardcoded
**Tasks:**
- [ ] Log level configuration (DEBUG, INFO, WARN, ERROR)
- [ ] Database connection pool settings
- [ ] Cache refresh interval for watchdog
- [ ] Template directory location
- [ ] Blog folder location
- [ ] Max file size for markdown files
- [ ] Thread pool size for server
- [ ] Request timeout settings

---

## 📝 Documentation

### #8 - README.md
**Priority:** HIGH  
**Status:** Open  
**Description:** Create comprehensive project documentation
**Tasks:**
- [ ] Write project description and purpose
- [ ] Add installation instructions
- [ ] Document configuration options
- [ ] Explain running locally vs containerized
- [ ] Add API/endpoint documentation
- [ ] Document blog folder structure requirements
- [ ] Add screenshots/examples
- [ ] Include troubleshooting section
- [ ] Add contributing guidelines
- [ ] Include license information

**Files:** `README.md` (to be created)

---

### #9 - Code Documentation
**Priority:** MEDIUM  
**Status:** Open  
**Description:** Add comprehensive Javadoc and inline comments
**Tasks:**
- [ ] Add Javadoc to all public APIs
- [ ] Document thread safety guarantees
- [ ] Explain the caching strategy
- [ ] Document expected file structure
- [ ] Add package-info.java files
- [ ] Document design patterns used
- [ ] Add architecture decision records (ADRs)

---

### #10 - Example Blog Content
**Priority:** LOW  
**Status:** Open  
**Description:** Provide sample markdown files demonstrating best practices
**Tasks:**
- [ ] Create example blog structure in `examples/` directory
- [ ] Show proper file naming conventions
- [ ] Demonstrate category structure
- [ ] Examples of quick links usage
- [ ] Show markdown features (images, links, code blocks)
- [ ] Include template customization examples

---

## 🧪 Testing

### #11 - Unit Tests
**Priority:** HIGH  
**Status:** Open  
**Description:** Currently NO tests exist in the project
**Tasks:**
- [ ] Add JUnit 5 dependency
- [ ] Test `MarkdownContentProvider` - file reading, caching, category extraction
- [ ] Test `MustacheService` - template rendering
- [ ] Test `Config` - property loading and defaults
- [ ] Test `RequestHandler` - routing logic
- [ ] Test `Server` - request parsing
- [ ] Test `CommonmarkRenderingService` - markdown conversion
- [ ] Test `EntryDescriptor` and `Category` domain objects
- [ ] Aim for 80%+ code coverage

**Files:** `src/test/java/**` (to be created)

---

### #12 - Integration Tests
**Priority:** MEDIUM  
**Status:** Open  
**Description:** Add end-to-end integration tests
**Tasks:**
- [ ] Full HTTP request/response cycle tests
- [ ] File system operations tests
- [ ] Database operations tests (if implemented)
- [ ] Concurrent request handling tests
- [ ] Container smoke tests
- [ ] Performance/load tests

---

### #13 - Test Coverage
**Priority:** MEDIUM  
**Status:** Open  
**Description:** Add test coverage reporting
**Tasks:**
- [ ] Add JaCoCo Maven plugin
- [ ] Configure coverage thresholds
- [ ] Generate coverage reports in CI/CD
- [ ] Add coverage badge to README

**Files:** `pom.xml`

---

## 🔒 Security

### #14 - Path Traversal Vulnerability
**Priority:** HIGH  
**Status:** Open  
**Description:** `MarkdownContentProvider` doesn't validate paths, could allow reading files outside blogFolder via `../` in URLs
**Tasks:**
- [ ] Add path sanitization and validation
- [ ] Ensure all file reads are within blogFolder
- [ ] Add security tests for path traversal attempts
- [ ] Log security violations
- [ ] Return 403 Forbidden for invalid paths

**Files:** `src/main/java/info/gabrielszabo/ublog/content/MarkdownContentProvider.java`

---

### #15 - XSS Protection
**Priority:** HIGH  
**Status:** Open  
**Description:** HTML content from markdown is not sanitized
**Tasks:**
- [ ] Add OWASP Java HTML Sanitizer dependency
- [ ] Sanitize HTML after CommonMark conversion
- [ ] Whitelist safe HTML tags and attributes
- [ ] Add security tests for XSS attempts
- [ ] Document safe markdown practices

**Files:** `src/main/java/info/gabrielszabo/ublog/markdown/CommonmarkRenderingService.java`

---

### #16 - CSRF Protection
**Priority:** MEDIUM  
**Status:** Open  
**Description:** No CSRF tokens for any operations
**Tasks:**
- [ ] Implement CSRF token generation
- [ ] Validate tokens on state-changing operations
- [ ] Add token to forms/templates
- [ ] Handle token expiration

---

### #17 - Input Validation
**Priority:** MEDIUM  
**Status:** Open  
**Description:** Insufficient input validation throughout the application
**Tasks:**
- [ ] Validate all URL parameters
- [ ] Add file size limits for markdown files
- [ ] Implement rate limiting
- [ ] Validate HTTP headers
- [ ] Add request size limits
- [ ] Sanitize all user inputs

---

### #18 - Security Headers
**Priority:** MEDIUM  
**Status:** Open  
**Description:** Missing important HTTP security headers
**Tasks:**
- [ ] Add X-Content-Type-Options: nosniff
- [ ] Add X-Frame-Options: DENY
- [ ] Add Content-Security-Policy
- [ ] Add X-XSS-Protection: 1; mode=block
- [ ] Add Referrer-Policy
- [ ] Add Strict-Transport-Security (if HTTPS)

**Files:** `src/main/java/info/gabrielszabo/ublog/server/**`

---

## 🚀 Features & Enhancements

### #19 - Error Handling Improvements
**Priority:** MEDIUM  
**Status:** Open  
**Description:** Better error handling and user-friendly error pages
**Tasks:**
- [ ] Create custom error pages for 404, 500, etc.
- [ ] Improve error messages in logs
- [ ] Sanitize stack traces in production
- [ ] Add error monitoring/alerting
- [ ] Return proper HTTP status codes
- [ ] Add user-friendly error descriptions

---

### #20 - Logging Improvements
**Priority:** MEDIUM  
**Status:** Open  
**Description:** Enhance logging capabilities
**Tasks:**
- [ ] Implement log levels in `ConsoleLogService`
- [ ] Add file-based logging option
- [ ] Structured logging (JSON format)
- [ ] Request/response logging middleware
- [ ] Log rotation
- [ ] Configurable log output format
- [ ] Add correlation IDs for request tracing

**Files:** `src/main/java/info/gabrielszabo/ublog/log/**`

---

### #21 - Performance Optimizations
**Priority:** LOW  
**Status:** Open  
**Description:** Various performance improvements
**Tasks:**
- [ ] Add ETag support for HTTP caching
- [ ] Implement gzip compression for responses
- [ ] Add connection pooling
- [ ] Lazy loading for large markdown files
- [ ] Optimize file system scanning
- [ ] Add caching headers
- [ ] Profile and optimize hot paths

---

### #22 - HTTP Feature Completeness
**Priority:** LOW  
**Status:** Open  
**Description:** Support standard HTTP features
**Tasks:**
- [ ] Support HEAD requests
- [ ] Return proper HTTP status codes everywhere
- [ ] Improve Content-Type detection
- [ ] Support Range requests for large files
- [ ] Add OPTIONS method support
- [ ] Implement conditional requests (If-Modified-Since)

---

### #23 - Blog Features
**Priority:** LOW  
**Status:** Open  
**Description:** Additional blog-specific features
**Tasks:**
- [ ] RSS/Atom feed generation
- [ ] Search functionality
- [ ] Tag support (beyond categories)
- [ ] Date-based sorting
- [ ] Pagination for long category lists
- [ ] Related posts suggestions
- [ ] Archive by date
- [ ] Table of contents generation
- [ ] Reading time estimation

---

### #24 - Admin Interface
**Priority:** LOW  
**Status:** Open  
**Description:** Web-based administration panel
**Tasks:**
- [ ] Web-based markdown editor
- [ ] File upload interface
- [ ] Category management
- [ ] Preview before publish
- [ ] Authentication system
- [ ] Draft posts support
- [ ] Bulk operations
- [ ] Analytics dashboard

---

## 🏗️ Code Quality & Architecture

### #25 - Dependency Injection
**Priority:** LOW  
**Status:** Open  
**Description:** Better utilize the jDI framework
**Tasks:**
- [ ] Review current DI usage patterns
- [ ] Refactor manual instantiation to use DI
- [ ] Add lifecycle management
- [ ] Document DI configuration
- [ ] Consider migrating to Spring Boot (major change)

**Files:** Multiple files across the project

---

### #26 - Error Propagation
**Priority:** MEDIUM  
**Status:** Open  
**Description:** Many methods silently catch exceptions
**Tasks:**
- [ ] Define proper exception hierarchy
- [ ] Create custom exception types
- [ ] Let exceptions bubble up appropriately
- [ ] Add exception handling at appropriate layers
- [ ] Document exception contracts in APIs

---

### #27 - Magic Strings
**Priority:** LOW  
**Status:** Open  
**Description:** Many hardcoded strings throughout codebase
**Tasks:**
- [ ] Create constants for file extensions (".md")
- [ ] Constants for reserved filenames ("Index", "Error")
- [ ] Constants for HTTP headers
- [ ] Constants for configuration keys
- [ ] Move to centralized constants classes

---

### #28 - Code Duplication
**Priority:** MEDIUM  
**Status:** Open  
**Description:** Repeated code patterns
**Tasks:**
- [ ] Extract common file reading logic
- [ ] Consolidate path construction
- [ ] Create utility classes for common operations
- [ ] Apply DRY principle throughout
- [ ] Refactor duplicated business logic

---

### #29 - Class Responsibilities (SRP)
**Priority:** LOW  
**Status:** Open  
**Description:** Some classes have too many responsibilities
**Tasks:**
- [ ] Split `MarkdownContentProvider` into: `FileReader`, `ContentCache`, `FileWatcher`, `ContentProvider`
- [ ] Review all classes for Single Responsibility Principle
- [ ] Improve separation of concerns
- [ ] Consider introducing service layer

**Files:** `src/main/java/info/gabrielszabo/ublog/content/MarkdownContentProvider.java`

---

### #30 - Null Safety
**Priority:** MEDIUM  
**Status:** Open  
**Description:** Many places return null instead of Optional
**Tasks:**
- [ ] Refactor to use `Optional<T>` where appropriate
- [ ] Add null checks where necessary
- [ ] Consider using annotations (@NonNull, @Nullable)
- [ ] Document null contracts in APIs

---

## 📦 Build & Deployment

### #31 - Maven Configuration Improvements
**Priority:** MEDIUM  
**Status:** Open  
**Description:** Enhance Maven build configuration
**Tasks:**
- [ ] Add version properties for all dependencies
- [ ] Configure maven-enforcer-plugin for dependency conflicts
- [ ] Add maven-release-plugin for versioning
- [ ] Add dependency convergence checking
- [ ] Configure reproducible builds
- [ ] Add build timestamp in manifest

**Files:** `pom.xml`

---

### #32 - Container Improvements
**Priority:** MEDIUM  
**Status:** Open  
**Description:** Optimize container image and runtime
**Tasks:**
- [ ] Multi-stage build to reduce image size
- [ ] Run as non-root user by default
- [ ] Add health check endpoint
- [ ] Proper signal handling (SIGTERM)
- [ ] Optimize layer caching
- [ ] Use distroless or alpine base image
- [ ] Document container deployment

**Files:** `Containerfile`

---

### #33 - CI/CD Pipeline
**Priority:** MEDIUM  
**Status:** Open  
**Description:** Automate build and deployment
**Tasks:**
- [ ] Create GitHub Actions or GitLab CI configuration
- [ ] Automated testing on PR
- [ ] Container image building
- [ ] Automated deployment to staging
- [ ] Release automation
- [ ] Automated dependency updates (Dependabot)

**Files:** `.github/workflows/**` or `.gitlab-ci.yml` (to be created)

---

### #34 - Monitoring & Observability
**Priority:** MEDIUM  
**Status:** Open  
**Description:** Add production monitoring capabilities
**Tasks:**
- [ ] Add health check endpoint (`/health`)
- [ ] Add readiness endpoint (`/ready`)
- [ ] Metrics endpoint in Prometheus format (`/metrics`)
- [ ] Readiness/liveness probes for Kubernetes
- [ ] Application performance monitoring (APM)
- [ ] Distributed tracing support

---

## 🗄️ Database

### #35 - Database Usage
**Priority:** LOW  
**Status:** Open  
**Description:** H2 dependency included but not used anywhere
**Tasks:**
- [ ] Decide if database features are needed
- [ ] If yes: implement user authentication
- [ ] If yes: implement comments system
- [ ] If yes: implement analytics/statistics
- [ ] If no: remove H2 dependency to reduce JAR size
- [ ] Document database design decisions

**Files:** `pom.xml`, various source files

---

### #36 - Database Migration
**Priority:** LOW  
**Status:** Open  
**Description:** If implementing database features, need migration tool
**Tasks:**
- [ ] Add Flyway or Liquibase dependency
- [ ] Create initial schema migration
- [ ] Version control for schema changes
- [ ] Document migration process
- [ ] Add migration tests

---

## 📐 Code Style & Consistency

### #37 - Code Formatting
**Priority:** LOW  
**Status:** Open  
**Description:** Ensure consistent code style
**Tasks:**
- [ ] Add `.editorconfig` file
- [ ] Configure Checkstyle or Spotless plugin
- [ ] Define formatting rules
- [ ] Consistent indentation (tabs vs spaces)
- [ ] Set up pre-commit hooks
- [ ] Document code style guidelines

---

### #38 - Naming Conventions
**Priority:** LOW  
**Status:** Open  
**Description:** Some naming inconsistencies exist
**Tasks:**
- [ ] Review all method names (e.g., `getError()` returns descriptor not error)
- [ ] Standardize variable naming
- [ ] Review package naming
- [ ] Update to follow Java naming conventions
- [ ] Document naming standards

---

### #39 - Project Structure
**Priority:** LOW  
**Status:** Open  
**Description:** Consider better module organization
**Tasks:**
- [ ] Evaluate separating into Maven modules: core, web, content
- [ ] Improve package organization
- [ ] Group related functionality
- [ ] Consider hexagonal/clean architecture
- [ ] Document architecture decisions

---

## 🔄 Versioning & Compatibility

### #40 - Versioning Strategy
**Priority:** LOW  
**Status:** Open  
**Description:** Define and implement versioning policy
**Tasks:**
- [ ] Move from 1.0-SNAPSHOT to proper releases
- [ ] Define semantic versioning policy
- [ ] Document breaking changes process
- [ ] Create CHANGELOG.md
- [ ] Tag releases in Git
- [ ] Automated version bumping

---

## 📊 Priority Summary

### **HIGH Priority** (Security & Stability)
- #3 - Thread Safety Issues
- #4 - Resource Cleanup on Shutdown
- #8 - README.md Documentation
- #11 - Unit Tests
- #14 - Path Traversal Vulnerability
- #15 - XSS Protection

### **MEDIUM Priority** (Usability & Quality)
- #1 - Filesystem Watchdog Permissions
- #2 - Recursive Error Handling (Needs Testing)
- #5 - Configuration Management
- #6 - Port Configuration
- #9 - Code Documentation
- #12 - Integration Tests
- #13 - Test Coverage
- #16 - CSRF Protection
- #17 - Input Validation
- #18 - Security Headers
- #19 - Error Handling Improvements
- #20 - Logging Improvements
- #26 - Error Propagation
- #28 - Code Duplication
- #30 - Null Safety
- #31 - Maven Configuration Improvements
- #32 - Container Improvements
- #33 - CI/CD Pipeline
- #34 - Monitoring & Observability

### **LOW Priority** (Nice to Have)
- #7 - Missing Configuration Options
- #10 - Example Blog Content
- #21 - Performance Optimizations
- #22 - HTTP Feature Completeness
- #23 - Blog Features
- #24 - Admin Interface
- #25 - Dependency Injection
- #27 - Magic Strings
- #29 - Class Responsibilities (SRP)
- #35 - Database Usage
- #36 - Database Migration
- #37 - Code Formatting
- #38 - Naming Conventions
- #39 - Project Structure
- #40 - Versioning Strategy

---

## 📝 Notes

- This backlog is a living document and should be updated as work progresses
- Items can be promoted/demoted in priority based on project needs
- New items should be added with unique IDs
- Completed items should be marked with status "Done" and completion date
- Consider creating GitHub/GitLab issues from these items for better tracking

---

**Document Version:** 1.0  
**Total Items:** 40
