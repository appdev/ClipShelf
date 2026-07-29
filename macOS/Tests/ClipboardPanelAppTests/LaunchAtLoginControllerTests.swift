import Foundation
import ServiceManagement
import Testing
@testable import ClipDock

@MainActor
private final class FakeLaunchAtLoginService: LaunchAtLoginServicing {
    var status: LaunchAtLoginServiceStatus
    var statusAfterRegister: LaunchAtLoginServiceStatus
    var registerError: Error?
    var unregisterError: Error?
    private(set) var registerCallCount = 0
    private(set) var unregisterCallCount = 0
    private(set) var openSettingsCallCount = 0

    init(
        status: LaunchAtLoginServiceStatus,
        statusAfterRegister: LaunchAtLoginServiceStatus = .enabled
    ) {
        self.status = status
        self.statusAfterRegister = statusAfterRegister
    }

    func register() throws {
        registerCallCount += 1
        if let registerError { throw registerError }
        status = statusAfterRegister
    }

    func unregister() throws {
        unregisterCallCount += 1
        if let unregisterError { throw unregisterError }
        status = .notRegistered
    }

    func openSystemSettingsLoginItems() {
        openSettingsCallCount += 1
    }
}

@MainActor
private final class FakeLegacyLaunchAtLoginArtifact: LegacyLaunchAtLoginArtifactHandling {
    var isInstalled: Bool
    var authorizationStatus: LaunchAtLoginServiceStatus
    var removalError: Error?
    private(set) var removeCallCount = 0

    init(
        isInstalled: Bool,
        authorizationStatus: LaunchAtLoginServiceStatus = .enabled
    ) {
        self.isInstalled = isInstalled
        self.authorizationStatus = authorizationStatus
    }

    func remove() throws {
        removeCallCount += 1
        if let removalError { throw removalError }
        isInstalled = false
    }
}

private struct TestLaunchAtLoginError: LocalizedError {
    let errorDescription: String?
}

struct LaunchAtLoginControllerTests {
    @Test @MainActor
    func serviceManagementStatusesMapIntoTheAppBoundary() {
        #expect(SystemLaunchAtLoginService.mapStatus(.enabled) == .enabled)
        #expect(SystemLaunchAtLoginService.mapStatus(.notRegistered) == .notRegistered)
        #expect(SystemLaunchAtLoginService.mapStatus(.requiresApproval) == .requiresApproval)
        #expect(SystemLaunchAtLoginService.mapStatus(.notFound) == .notFound)
    }

    @Test @MainActor
    func currentStateIsReadOnlyAndShowsPreservedLegacyRegistration() {
        let service = FakeLaunchAtLoginService(status: .notFound)
        let legacy = FakeLegacyLaunchAtLoginArtifact(isInstalled: true)
        let controller = LaunchAtLoginController(
            service: service,
            legacyArtifact: legacy,
            isRunningAsApplicationBundle: true
        )

        #expect(controller.currentState().isOn)
        #expect(controller.currentState().isOn)
        #expect(service.registerCallCount == 0)
        #expect(service.unregisterCallCount == 0)
        #expect(service.openSettingsCallCount == 0)
        #expect(legacy.removeCallCount == 0)
    }

    @Test @MainActor
    func notFoundMigrationRegistersModernServiceAndThenRemovesLegacyArtifact() {
        let service = FakeLaunchAtLoginService(status: .notFound, statusAfterRegister: .enabled)
        let legacy = FakeLegacyLaunchAtLoginArtifact(isInstalled: true)
        let controller = LaunchAtLoginController(
            service: service,
            legacyArtifact: legacy,
            isRunningAsApplicationBundle: true
        )

        #expect(controller.migrateLegacyRegistrationIfNeeded() == .migrated)
        #expect(service.registerCallCount == 1)
        #expect(legacy.removeCallCount == 1)
        #expect(!legacy.isInstalled)
    }

    @Test @MainActor
    func failedMigrationRetainsLegacyArtifact() {
        let service = FakeLaunchAtLoginService(status: .notFound)
        service.registerError = TestLaunchAtLoginError(errorDescription: "registration failed")
        let legacy = FakeLegacyLaunchAtLoginArtifact(isInstalled: true)
        let controller = LaunchAtLoginController(
            service: service,
            legacyArtifact: legacy,
            isRunningAsApplicationBundle: true
        )

        #expect(controller.migrateLegacyRegistrationIfNeeded() ==
            .retainedLegacy(.registrationFailed("registration failed")))
        #expect(legacy.removeCallCount == 0)
        #expect(legacy.isInstalled)
    }

    @Test @MainActor
    func approvalPendingMigrationRetainsLegacyAndDoesNotOpenSettings() {
        let service = FakeLaunchAtLoginService(
            status: .notFound,
            statusAfterRegister: .requiresApproval
        )
        let legacy = FakeLegacyLaunchAtLoginArtifact(isInstalled: true)
        let controller = LaunchAtLoginController(
            service: service,
            legacyArtifact: legacy,
            isRunningAsApplicationBundle: true
        )

        #expect(controller.migrateLegacyRegistrationIfNeeded() ==
            .retainedLegacy(.requiresApproval))
        #expect(service.openSettingsCallCount == 0)
        #expect(legacy.removeCallCount == 0)
    }

    @Test @MainActor
    func disabledLegacyRegistrationStaysOffAndIsNotMigratedAtStartup() {
        let service = FakeLaunchAtLoginService(status: .notFound)
        let legacy = FakeLegacyLaunchAtLoginArtifact(
            isInstalled: true,
            authorizationStatus: .requiresApproval
        )
        let controller = LaunchAtLoginController(
            service: service,
            legacyArtifact: legacy,
            isRunningAsApplicationBundle: true
        )

        #expect(controller.migrateLegacyRegistrationIfNeeded() ==
            .retainedLegacy(.requiresApproval))
        #expect(!controller.currentState().isOn)
        #expect(controller.currentState().canChange)
        #expect(controller.diagnostics().legacyAuthorizationStatus == .requiresApproval)
        #expect(controller.diagnostics().migrationNeeded == false)
        #expect(service.registerCallCount == 0)
        #expect(service.openSettingsCallCount == 0)
        #expect(legacy.removeCallCount == 0)
    }

    @Test(arguments: [LaunchAtLoginServiceStatus.notFound, .unknown])
    @MainActor
    func inactiveOrUnknownLegacyRegistrationIsNeverMigratedAtStartup(
        authorizationStatus: LaunchAtLoginServiceStatus
    ) {
        let service = FakeLaunchAtLoginService(status: .notFound)
        let legacy = FakeLegacyLaunchAtLoginArtifact(
            isInstalled: true,
            authorizationStatus: authorizationStatus
        )
        let controller = LaunchAtLoginController(
            service: service,
            legacyArtifact: legacy,
            isRunningAsApplicationBundle: true
        )

        #expect(controller.migrateLegacyRegistrationIfNeeded() ==
            .retainedLegacy(.modernServiceInactive))
        #expect(controller.diagnostics().legacyAuthorizationStatus == authorizationStatus)
        #expect(controller.diagnostics().migrationNeeded == false)
        #expect(!controller.currentState().isOn)
        if authorizationStatus == .unknown {
            #expect(!controller.currentState().canChange)
        } else {
            #expect(controller.currentState().canChange)
        }
        #expect(service.registerCallCount == 0)
        #expect(service.openSettingsCallCount == 0)
        #expect(legacy.removeCallCount == 0)
    }

    @Test(arguments: [
        LaunchAtLoginServiceStatus.requiresApproval,
        .notRegistered,
        .notFound,
        .unknown,
    ])
    @MainActor
    func modernEnabledRemovesStaleLegacyArtifactRegardlessOfAuthorization(
        authorizationStatus: LaunchAtLoginServiceStatus
    ) {
        let service = FakeLaunchAtLoginService(status: .enabled)
        let legacy = FakeLegacyLaunchAtLoginArtifact(
            isInstalled: true,
            authorizationStatus: authorizationStatus
        )
        let controller = LaunchAtLoginController(
            service: service,
            legacyArtifact: legacy,
            isRunningAsApplicationBundle: true
        )

        #expect(controller.migrateLegacyRegistrationIfNeeded() == .migrated)
        #expect(service.registerCallCount == 0)
        #expect(service.openSettingsCallCount == 0)
        #expect(legacy.removeCallCount == 1)
        #expect(!legacy.isInstalled)
    }

    @Test(arguments: [
        LaunchAtLoginServiceStatus.requiresApproval,
        .notRegistered,
        .notFound,
        .unknown,
    ])
    @MainActor
    func modernEnabledReportsCleanupFailureRegardlessOfLegacyAuthorization(
        authorizationStatus: LaunchAtLoginServiceStatus
    ) {
        let service = FakeLaunchAtLoginService(status: .enabled)
        let legacy = FakeLegacyLaunchAtLoginArtifact(
            isInstalled: true,
            authorizationStatus: authorizationStatus
        )
        legacy.removalError = TestLaunchAtLoginError(
            errorDescription: "cleanup failed"
        )
        let controller = LaunchAtLoginController(
            service: service,
            legacyArtifact: legacy,
            isRunningAsApplicationBundle: true
        )

        #expect(controller.migrateLegacyRegistrationIfNeeded() ==
            .retainedLegacy(.cleanupFailed("cleanup failed")))
        #expect(service.registerCallCount == 0)
        #expect(service.openSettingsCallCount == 0)
        #expect(legacy.removeCallCount == 1)
        #expect(legacy.isInstalled)
    }

    @Test(arguments: [
        LaunchAtLoginServiceStatus.requiresApproval,
        .notRegistered,
        .notFound,
        .unknown,
    ])
    @MainActor
    func modernEnabledDiagnosticsReportPendingCleanupWithoutMutating(
        authorizationStatus: LaunchAtLoginServiceStatus
    ) {
        let service = FakeLaunchAtLoginService(status: .enabled)
        let legacy = FakeLegacyLaunchAtLoginArtifact(
            isInstalled: true,
            authorizationStatus: authorizationStatus
        )
        let controller = LaunchAtLoginController(
            service: service,
            legacyArtifact: legacy,
            isRunningAsApplicationBundle: true
        )

        #expect(controller.diagnostics() == LaunchAtLoginDiagnostics(
            serviceStatus: .enabled,
            legacyArtifactInstalled: true,
            legacyAuthorizationStatus: authorizationStatus,
            migrationNeeded: true
        ))
        #expect(service.registerCallCount == 0)
        #expect(service.unregisterCallCount == 0)
        #expect(service.openSettingsCallCount == 0)
        #expect(legacy.removeCallCount == 0)
        #expect(legacy.isInstalled)
    }

    @Test @MainActor
    func userEnableFromNotFoundUsesModernServiceAndNeverCreatesLegacyArtifact() throws {
        let service = FakeLaunchAtLoginService(status: .notFound, statusAfterRegister: .enabled)
        let legacy = FakeLegacyLaunchAtLoginArtifact(isInstalled: false)
        let controller = LaunchAtLoginController(
            service: service,
            legacyArtifact: legacy,
            isRunningAsApplicationBundle: true
        )

        let state = try controller.setEnabled(true).get()
        #expect(service.registerCallCount == 1)
        #expect(state.isOn)
        #expect(legacy.removeCallCount == 0)
    }

    @Test @MainActor
    func userEnableRequiringApprovalOpensSettingsWhileStartupNeverDoes() throws {
        let userService = FakeLaunchAtLoginService(status: .requiresApproval)
        let userLegacy = FakeLegacyLaunchAtLoginArtifact(isInstalled: true)
        let userController = LaunchAtLoginController(
            service: userService,
            legacyArtifact: userLegacy,
            isRunningAsApplicationBundle: true
        )

        let userState = try userController.setEnabled(true).get()
        #expect(userState.isOn)
        #expect(userService.openSettingsCallCount == 1)
        #expect(userLegacy.removeCallCount == 0)

        let startupService = FakeLaunchAtLoginService(status: .requiresApproval)
        let startupLegacy = FakeLegacyLaunchAtLoginArtifact(isInstalled: true)
        let startupController = LaunchAtLoginController(
            service: startupService,
            legacyArtifact: startupLegacy,
            isRunningAsApplicationBundle: true
        )

        #expect(startupController.migrateLegacyRegistrationIfNeeded() ==
            .retainedLegacy(.requiresApproval))
        #expect(startupService.openSettingsCallCount == 0)
        #expect(startupLegacy.removeCallCount == 0)
    }

    @Test @MainActor
    func userDisableUnregistersModernServiceAndRemovesLegacyArtifact() throws {
        let service = FakeLaunchAtLoginService(status: .enabled)
        let legacy = FakeLegacyLaunchAtLoginArtifact(isInstalled: true)
        let controller = LaunchAtLoginController(
            service: service,
            legacyArtifact: legacy,
            isRunningAsApplicationBundle: true
        )

        let state = try controller.setEnabled(false).get()
        #expect(service.unregisterCallCount == 1)
        #expect(legacy.removeCallCount == 1)
        #expect(!state.isOn)
    }

    @Test @MainActor
    func userDisableStillRemovesLegacyArtifactWhenModernUnregisterFails() {
        let service = FakeLaunchAtLoginService(status: .enabled)
        service.unregisterError = TestLaunchAtLoginError(
            errorDescription: "unregister failed"
        )
        let legacy = FakeLegacyLaunchAtLoginArtifact(isInstalled: true)
        let controller = LaunchAtLoginController(
            service: service,
            legacyArtifact: legacy,
            isRunningAsApplicationBundle: true
        )

        guard case let .failure(error) = controller.setEnabled(false) else {
            Issue.record("Expected disable to report the unregister failure")
            return
        }
        #expect(error.message == "unregister failed")
        #expect(service.unregisterCallCount == 1)
        #expect(legacy.removeCallCount == 1)
        #expect(!legacy.isInstalled)
        #expect(controller.currentState().isOn)
    }

    @Test @MainActor
    func userDisableReportsLegacyCleanupFailureAfterModernUnregisterSucceeds() {
        let service = FakeLaunchAtLoginService(status: .enabled)
        let legacy = FakeLegacyLaunchAtLoginArtifact(isInstalled: true)
        legacy.removalError = TestLaunchAtLoginError(
            errorDescription: "cleanup failed"
        )
        let controller = LaunchAtLoginController(
            service: service,
            legacyArtifact: legacy,
            isRunningAsApplicationBundle: true
        )

        guard case let .failure(error) = controller.setEnabled(false) else {
            Issue.record("Expected disable to report the legacy cleanup failure")
            return
        }
        #expect(error.message == "cleanup failed")
        #expect(service.unregisterCallCount == 1)
        #expect(legacy.removeCallCount == 1)
        #expect(legacy.isInstalled)
        #expect(controller.currentState().isOn)
    }

    @Test @MainActor
    func userDisableReportsBothFailuresAfterAttemptingBothCleanups() {
        let service = FakeLaunchAtLoginService(status: .enabled)
        service.unregisterError = TestLaunchAtLoginError(
            errorDescription: "unregister failed"
        )
        let legacy = FakeLegacyLaunchAtLoginArtifact(isInstalled: true)
        legacy.removalError = TestLaunchAtLoginError(
            errorDescription: "cleanup failed"
        )
        let controller = LaunchAtLoginController(
            service: service,
            legacyArtifact: legacy,
            isRunningAsApplicationBundle: true
        )

        guard case let .failure(error) = controller.setEnabled(false) else {
            Issue.record("Expected disable to report both cleanup failures")
            return
        }
        #expect(error.message.contains("unregister failed"))
        #expect(error.message.contains("cleanup failed"))
        #expect(service.unregisterCallCount == 1)
        #expect(legacy.removeCallCount == 1)
        #expect(legacy.isInstalled)
        #expect(controller.currentState().isOn)
    }

    @Test @MainActor
    func cleanupFailureKeepsLegacyStateVisibleForRetry() {
        let service = FakeLaunchAtLoginService(status: .enabled)
        let legacy = FakeLegacyLaunchAtLoginArtifact(isInstalled: true)
        legacy.removalError = TestLaunchAtLoginError(errorDescription: "cleanup failed")
        let controller = LaunchAtLoginController(
            service: service,
            legacyArtifact: legacy,
            isRunningAsApplicationBundle: true
        )

        #expect(controller.migrateLegacyRegistrationIfNeeded() ==
            .retainedLegacy(.cleanupFailed("cleanup failed")))
        #expect(legacy.isInstalled)
    }

    @Test @MainActor
    func diagnosticsAreReadOnly() {
        let service = FakeLaunchAtLoginService(status: .notFound)
        let legacy = FakeLegacyLaunchAtLoginArtifact(isInstalled: true)
        let controller = LaunchAtLoginController(
            service: service,
            legacyArtifact: legacy,
            isRunningAsApplicationBundle: true
        )

        #expect(controller.diagnostics() == LaunchAtLoginDiagnostics(
            serviceStatus: .notFound,
            legacyArtifactInstalled: true,
            legacyAuthorizationStatus: .enabled,
            migrationNeeded: true
        ))
        #expect(service.registerCallCount == 0)
        #expect(service.unregisterCallCount == 0)
        #expect(legacy.removeCallCount == 0)
    }

    @Test @MainActor
    func legacyRemovalRejectsDirectoryWithoutDeletingItsContents() throws {
        let fileManager = FileManager.default
        let testRoot = fileManager.temporaryDirectory
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
        let legacyURL = testRoot
            .appendingPathComponent("legacy.plist", isDirectory: true)
        let sentinelURL = legacyURL.appendingPathComponent("keep.txt")
        try fileManager.createDirectory(
            at: legacyURL,
            withIntermediateDirectories: true
        )
        try Data("keep".utf8).write(to: sentinelURL)
        defer { try? fileManager.removeItem(at: testRoot) }

        let artifact = LegacyLaunchAtLoginArtifact(
            plistURL: legacyURL,
            fileManager: fileManager
        )

        do {
            try artifact.remove()
            Issue.record("Expected directory-shaped legacy artifact to be rejected")
        } catch {
            #expect(error.localizedDescription ==
                "The legacy Login Item path is not a removable file.")
        }
        #expect(fileManager.fileExists(atPath: legacyURL.path))
        #expect(fileManager.fileExists(atPath: sentinelURL.path))
    }
}
