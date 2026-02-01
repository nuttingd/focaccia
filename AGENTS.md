# AGENTS.md

This document provides guidelines for agentic coding agents operating in this repository.

## Build/Lint/Test Commands

### Build Commands
- `./gradlew build` - Build the entire project
- `./gradlew assembleDebug` - Build debug APK
- `./gradlew assembleRelease` - Build release APK

### Lint Commands
- `./gradlew lint` - Run static analysis
- `./gradlew lintDebug` - Run debug lint checks
- `./gradlew lintRelease` - Run release lint checks

### Test Commands
- `./gradlew test` - Run unit tests
- `./gradlew connectedAndroidTest` - Run instrumentation tests
- `./gradlew test --tests "*FocacciaE2ETest*" -i` - Run specific E2E test class with info output
- `./gradlew connectedAndroidTest --tests "*FocacciaE2ETest*" -i` - Run specific E2E tests with info output

## Code Style Guidelines

### Imports
- Group imports in order: Android imports, Kotlin imports, Java imports, Third-party imports
- Use fully qualified names for imports when necessary to avoid conflicts
- Prefer `import` statements over fully qualified class names where possible

### Formatting
- Follow Kotlin coding conventions
- Use 4 spaces for indentation (no tabs)
- No trailing whitespace
- Single blank line between top-level definitions
- No space before colon in function declarations
- Space around binary operators

### Types
- Prefer `val` over `var` when possible
- Use explicit type declarations only when necessary
- Use `data class` for simple data structures
- Use sealed classes for related types with a fixed set of subtypes

### Naming Conventions
- Use PascalCase for class names (e.g., `MainActivity`, `AppBlockerAccessibilityService`)
- Use camelCase for variables and functions (e.g., `blockedAppsRepository`, `isAppBlocked`)
- Use UPPER_SNAKE_CASE for constants (e.g., `BLOCKED_PACKAGE`, `UNBLOCKED_PACKAGE`)
- Use descriptive names that clearly indicate the purpose
- Avoid abbreviations unless they are standard (e.g., `btn` for button, but not `calc` for calculator)

### Error Handling
- Use Kotlin's built-in error handling (`try/catch`, `when` expressions)
- Prefer `sealed class` or `enum class` for representing error states
- Handle Android lifecycle events properly to prevent memory leaks
- Log errors appropriately using Android logging framework

### Architecture
- Follow MVVM (Model-View-ViewModel) pattern
- Use Repository pattern for data management
- Implement proper separation of concerns
- Maintain clean interfaces between components

## Cursor/Copilot Rules

No specific cursor or copilot rules found in this repository.

## Git Safety Protocol

### Committing Changes
- NEVER update the git config
- NEVER run destructive/irreversible git commands (like push --force, hard reset, etc) unless explicitly requested
- NEVER skip hooks (--no-verify, --no-gpg-sign, etc) unless explicitly requested
- Avoid git commit --amend. ONLY use --amend when ALL conditions are met:
  - User explicitly requested amend
  - HEAD commit was created by you in this conversation
  - Commit has NOT been pushed to remote
- CRITICAL: If commit FAILED or was REJECTED by hook, NEVER amend - fix the issue and create a NEW commit
- CRITICAL: If you already pushed to remote, NEVER amend unless user explicitly requests it (requires force push)

### Creating Pull Requests
Use the gh command via Bash for ALL GitHub-related tasks including working with issues, pull requests, checks, and releases. If given a Github URL use the gh command to get the information needed.

## Additional Guidelines

### Code References
When referencing specific functions or pieces of code include the pattern `file_path:line_number` to allow users to easily navigate to the source code location.

### Security Best Practices
- NEVER introduce code that exposes or logs secrets and keys
- NEVER commit secrets or keys to the repository
- Always follow security best practices when handling user data

### Documentation
- NEVER proactively create documentation files (*.md) or README files unless explicitly requested by the User
- Only add comments to code if explicitly asked