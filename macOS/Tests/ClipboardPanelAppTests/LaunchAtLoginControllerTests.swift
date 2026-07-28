import Foundation
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
    var removalError: Error?
    private(set) var removeCallCount = 0

    init(isInstalled: Bool) {
        self.isInstalled = isInstalled
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
            migrationNeeded: true
        ))
        #expect(service.registerCallCount == 0)
        #expect(service.unregisterCallCount == 0)
        #expect(legacy.removeCallCount == 0)
    }
}
