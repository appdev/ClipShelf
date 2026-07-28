# Final fix report — launch-at-login SMAppService migration

Date: 2026-07-28
Worktree: `/Volumes/extendData/Data/IdeaProjects/ClipDock-launch-at-login-smappservice`
Starting head: `3ea1bd1e7544c076c263885290b8198d8fd8ada2`
Source review: `.superpowers/sdd/2026-07-28-launch-at-login-smappservice-migration/final-review.md`

## Result

All four open final-review findings were addressed in one fix wave without
reading, writing, registering, unregistering, or deleting the production
ClipDock Login Item or its actual legacy plist.

1. Legacy plist presence is no longer treated as consent. The production
   legacy adapter calls `SMAppService.statusForLegacyPlist(at:)`, maps the
   result through the app-owned `LaunchAtLoginServiceStatus` boundary, and
   startup migration proceeds only for `.enabled` legacy authorization.
2. A user disable now attempts modern unregistration and legacy cleanup
   independently. Single and dual failures are retained; the caller's existing
   read-only fallback refresh reports the actual post-attempt active state.
3. The user approval path has a focused regression test: user enable opens
   Login Items exactly once, while startup migration never opens Settings and
   never removes the legacy artifact before modern `.enabled` status.
4. Legacy removal now accepts only a regular file or a symbolic link. A
   directory at the expected plist path produces a controlled localized error
   and is not recursively removed.

Diagnostics now include `legacyAuthorizationStatus` and set
`migrationNeeded=true` only when an installed legacy artifact is authorized as
`.enabled`. Disabled legacy state therefore remains read-only, reports off in
the UI when the modern service is inactive, and cannot silently re-enable the
modern Login Item.

## Red/green evidence

### Legacy authorization and consent

RED command:

```bash
cd macOS && swift test --filter LaunchAtLoginControllerTests
```

Exit `1`. The current implementation ignored the fake legacy authorization
status. Exact runner summary:

```text
Test run with 10 tests failed after 0.001 seconds with 10 issues.
```

The disabled case observed `.migrated` instead of
`.retainedLegacy(.requiresApproval)`, `currentState().isOn == true`, one modern
registration call, and one legacy removal call. The `.notFound` and `.unknown`
legacy cases likewise observed `.migrated`, one registration call, and one
removal call.

GREEN command:

```bash
cd macOS && swift test --filter LaunchAtLoginControllerTests
```

Exit `0` after adding the legacy authorization boundary and startup gate:

```text
Suite LaunchAtLoginControllerTests passed after 0.001 seconds.
Test run with 10 tests passed after 0.001 seconds.
```

### Explicit legacy authorization diagnostics

RED command:

```bash
cd macOS && swift test --filter LaunchAtLoginControllerTests
```

Exit `1` at compile time because the test-required diagnostic field did not
exist. Exact primary compiler errors:

```text
error: value of type 'LaunchAtLoginDiagnostics' has no member 'legacyAuthorizationStatus'
error: extra argument 'legacyAuthorizationStatus' in call
```

GREEN command:

```bash
cd macOS && swift test --filter LaunchAtLoginControllerTests
```

Exit `0` after the diagnostic field and command output were added:

```text
Suite LaunchAtLoginControllerTests passed after 0.001 seconds.
Test run with 10 tests passed after 0.001 seconds.
```

### Independent disable cleanup and partial failure state

RED command:

```bash
cd macOS && swift test --filter LaunchAtLoginControllerTests
```

Exit `1`. Exact runner summary:

```text
Suite LaunchAtLoginControllerTests failed after 0.001 seconds with 4 issues.
Test run with 13 tests failed after 0.001 seconds with 4 issues.
```

When `unregister()` failed, `legacy.removeCallCount` was `0` instead of `1` and
the artifact remained installed. When both operations were configured to fail,
the result contained only `unregister failed`, omitted `cleanup failed`, and
again recorded zero removal attempts.

GREEN command:

```bash
cd macOS && swift test --filter LaunchAtLoginControllerTests
```

Exit `0` after independent attempts and error aggregation:

```text
Suite LaunchAtLoginControllerTests passed after 0.001 seconds.
Test run with 13 tests passed after 0.001 seconds.
```

The three focused failure cases verify:

- unregister failure plus successful legacy removal returns failure, leaves the
  modern service active, removes the legacy artifact, and refreshes to on;
- successful unregister plus cleanup failure returns failure, leaves the legacy
  registration visible and refreshes to on; and
- dual failure attempts both operations, retains both error messages, and
  refreshes to on.

### User approval flow

The approval behavior already existed, so the new regression test was
mutation-checked. After temporarily replacing the user-only
`.requiresApproval` Settings-open call with `break`, the RED command was:

```bash
cd macOS && swift test --filter userEnableRequiringApprovalOpensSettingsWhileStartupNeverDoes
```

Exit `1`, exact issue and summary:

```text
Expectation failed: (userService.openSettingsCallCount → 0) == 1
Suite LaunchAtLoginControllerTests failed after 0.001 seconds with 1 issue.
Test run with 1 test failed after 0.001 seconds with 1 issue.
```

The production call was restored immediately. GREEN command:

```bash
cd macOS && swift test --filter userEnableRequiringApprovalOpensSettingsWhileStartupNeverDoes
```

Exit `0`:

```text
Suite LaunchAtLoginControllerTests passed after 0.001 seconds.
Test run with 1 test passed after 0.001 seconds.
```

### Directory-shaped legacy path

RED command:

```bash
cd macOS && swift test --filter legacyRemovalRejectsDirectoryWithoutDeletingItsContents
```

Exit `1` at compile time because the safe test-only URL seam did not exist:

```text
error: incorrect argument label in call (have 'plistURL:fileManager:', expected 'bundleIdentifier:fileManager:')
error: cannot convert value of type 'URL' to expected argument type 'String'
```

GREEN command:

```bash
cd macOS && swift test --filter legacyRemovalRejectsDirectoryWithoutDeletingItsContents
```

Exit `0` after adding the item-type guard:

```text
Suite LaunchAtLoginControllerTests passed after 0.002 seconds.
Test run with 1 test passed after 0.002 seconds.
```

The test creates only a unique temporary directory, places a sentinel file
inside it, receives the controlled error `The legacy Login Item path is not a
removable file.`, and verifies both the directory and sentinel still exist.

## Final verification

Fresh focused command after all source and test changes:

```bash
cd macOS && swift test --filter 'LaunchAtLoginControllerTests|PreferencesCoordinatorTests|PanelRegressionPlannerTests|launchAtLoginAppleEventDetectorRequiresOpenApplicationLoginItemDescriptor|appRuntimeKeepsPreferencesHiddenForLaunchAtLoginPresentation|appRuntimeKeepsPreferencesHiddenForModernLoginItemWithoutLegacyArgument|appRuntimeShowsPreferencesForExplicitRequestWhenModernLoginItemLaunches|appRuntimeShowsPreferencesWhenApplicationReopens'
```

Exit `0`:

```text
Suite PanelRegressionPlannerTests passed after 0.041 seconds.
Suite PanelRuntimeSeamTests passed after 0.577 seconds.
Suite LaunchAtLoginControllerTests passed after 0.581 seconds.
Suite PreferencesCoordinatorTests passed after 0.594 seconds.
Test run with 46 tests passed after 0.595 seconds.
```

Fresh build and static checks:

```bash
cd macOS && swift build
```

Exit `0`: `Build complete! (1.34s)`.

```bash
cd macOS && find Sources/ClipDock/Resources Sources/ClipboardPanelApp/Resources \
  -name Localizable.strings -print0 | xargs -0 -n1 plutil -lint
```

Exit `0`; all eight localization files reported `OK`.

```bash
cd macOS && git diff --check
```

Exit `0` with no output.

## Files changed

- `macOS/Sources/ClipDock/LaunchAtLoginController.swift`
- `macOS/Sources/ClipDock/AppCommands.swift`
- `macOS/Tests/ClipboardPanelAppTests/LaunchAtLoginControllerTests.swift`
- `macOS/Sources/ClipDock/Resources/{en,ja,ko,zh-Hant}.lproj/Localizable.strings`
- `macOS/Sources/ClipboardPanelApp/Resources/{en,ja,ko,zh-Hant}.lproj/Localizable.strings`
- `.superpowers/sdd/2026-07-28-launch-at-login-smappservice-migration/final-fix-report.md`

No `macOS/scripts` file or ad-hoc release policy was changed. No generated
bridge, Rust source, live Login Item, production legacy plist, installed app,
or release artifact was changed.

## Decisions and self-review

- Reused one app-owned status enum for modern and legacy ServiceManagement
  mappings so unknown future values retain the existing non-mutating
  `@unknown default -> .unknown` behavior.
- Kept plist existence separate from authorization. Existence controls whether
  there is an artifact to inspect; `.enabled` authorization alone permits
  startup migration.
- Preserved the existing migration outcome vocabulary: disabled legacy maps to
  `.retainedLegacy(.requiresApproval)`; inactive/unknown/not-found legacy maps
  to `.retainedLegacy(.modernServiceInactive)`.
- Kept the existing `Result<LaunchAtLoginState, LaunchAtLoginError>` API. On
  failure, Preferences already re-reads `currentState()`, so the partial result
  is based on actual post-attempt service/artifact state rather than a guessed
  state embedded in the error.
- Aggregated independent disable errors in operation order, separated by a
  newline. A single error retains its original localized description verbatim.
- Allowed regular files and symbolic links during cleanup. Removing a symlink
  removes the link itself and does not recursively remove a directory target;
  directory and other item types are rejected before `removeItem(at:)`.
- Added the controlled cleanup error to all four shipped locales in both
  resource roots.
- Mutation review confirms that removing the legacy authorization gate, the
  independent cleanup attempt, the user Settings-open call, or the file-type
  guard is caught by the focused tests.

## Concerns and explicit limitations

- No live ServiceManagement registration, unregistration, BTM state, logout or
  login relaunch, real legacy plist, installed application, or System Settings
  UI was exercised. All mutation behavior uses fakes; the file hardening test
  uses a unique temporary path.
- The full Swift suite and packaging/signing workflow were not rerun in this
  final fix wave because the assigned gate was the focused controller,
  presentation, preferences, runtime, build, and localization checks. The
  branch's previously documented two unrelated schema-15-versus-16 full-suite
  failures therefore remain a known baseline concern rather than fresh
  evidence from this wave.
- As already recorded by the final review, ad-hoc-signing compatibility with
  real `SMAppService` registration across supported macOS versions remains an
  explicit release-validation limitation.
