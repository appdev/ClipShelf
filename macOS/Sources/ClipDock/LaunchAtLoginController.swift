import AppKit
import ClipboardPanelApp
import Foundation
import ServiceManagement

typealias LaunchAtLoginState = LaunchAtLoginPresentation

struct LaunchAtLoginError: LocalizedError {
    let message: String

    var errorDescription: String? {
        message
    }
}

enum LaunchAtLoginServiceStatus: Equatable {
    case enabled
    case notRegistered
    case requiresApproval
    case notFound
    case unknown
}

@MainActor
protocol LaunchAtLoginServicing: AnyObject {
    var status: LaunchAtLoginServiceStatus { get }
    func register() throws
    func unregister() throws
    func openSystemSettingsLoginItems()
}

@MainActor
protocol LegacyLaunchAtLoginArtifactHandling: AnyObject {
    var isInstalled: Bool { get }
    func remove() throws
}

enum LaunchAtLoginMigrationIssue: Equatable {
    case requiresApproval
    case modernServiceInactive
    case registrationFailed(String)
    case cleanupFailed(String)
}

enum LaunchAtLoginMigrationOutcome: Equatable {
    case notNeeded
    case migrated
    case retainedLegacy(LaunchAtLoginMigrationIssue)
}

struct LaunchAtLoginDiagnostics: Equatable {
    let serviceStatus: LaunchAtLoginServiceStatus
    let legacyArtifactInstalled: Bool
    let migrationNeeded: Bool
}

extension LaunchAtLoginServiceStatus {
    var rawDiagnosticValue: String {
        switch self {
        case .enabled: "enabled"
        case .notRegistered: "notRegistered"
        case .requiresApproval: "requiresApproval"
        case .notFound: "notFound"
        case .unknown: "unknown"
        }
    }
}

extension LaunchAtLoginMigrationOutcome {
    var diagnosticDescription: String {
        switch self {
        case .notNeeded: "notNeeded"
        case .migrated: "migrated"
        case .retainedLegacy(.requiresApproval): "retainedLegacy:requiresApproval"
        case .retainedLegacy(.modernServiceInactive): "retainedLegacy:modernServiceInactive"
        case .retainedLegacy(.registrationFailed): "retainedLegacy:registrationFailed"
        case .retainedLegacy(.cleanupFailed): "retainedLegacy:cleanupFailed"
        }
    }
}

@MainActor
final class SystemLaunchAtLoginService: LaunchAtLoginServicing {
    var status: LaunchAtLoginServiceStatus {
        switch SMAppService.mainApp.status {
        case .enabled:
            .enabled
        case .notRegistered:
            .notRegistered
        case .requiresApproval:
            .requiresApproval
        case .notFound:
            .notFound
        @unknown default:
            .unknown
        }
    }

    func register() throws {
        try SMAppService.mainApp.register()
    }

    func unregister() throws {
        try SMAppService.mainApp.unregister()
    }

    func openSystemSettingsLoginItems() {
        SMAppService.openSystemSettingsLoginItems()
    }
}

@MainActor
final class LegacyLaunchAtLoginArtifact: LegacyLaunchAtLoginArtifactHandling {
    private let plistURL: URL
    private let fileManager: FileManager

    init(bundleIdentifier: String, fileManager: FileManager = .default) {
        plistURL = fileManager.homeDirectoryForCurrentUser
            .appendingPathComponent("Library", isDirectory: true)
            .appendingPathComponent("LaunchAgents", isDirectory: true)
            .appendingPathComponent("\(bundleIdentifier).launch-at-login.plist")
        self.fileManager = fileManager
    }

    var isInstalled: Bool {
        fileManager.fileExists(atPath: plistURL.path)
    }

    func remove() throws {
        guard isInstalled else { return }
        try fileManager.removeItem(at: plistURL)
    }
}

@MainActor
final class LaunchAtLoginController {
    private let service: any LaunchAtLoginServicing
    private let legacyArtifact: (any LegacyLaunchAtLoginArtifactHandling)?
    private let isRunningAsApplicationBundle: Bool

    convenience init() {
        let isRunningAsApplicationBundle = Bundle.main.bundleURL.pathExtension == "app"
            && Bundle.main.bundleIdentifier != nil
        let legacyArtifact = Bundle.main.bundleIdentifier.map { bundleIdentifier in
            LegacyLaunchAtLoginArtifact(bundleIdentifier: bundleIdentifier)
        }
        self.init(
            service: SystemLaunchAtLoginService(),
            legacyArtifact: legacyArtifact,
            isRunningAsApplicationBundle: isRunningAsApplicationBundle
        )
    }

    init(
        service: any LaunchAtLoginServicing,
        legacyArtifact: (any LegacyLaunchAtLoginArtifactHandling)?,
        isRunningAsApplicationBundle: Bool
    ) {
        self.service = service
        self.legacyArtifact = legacyArtifact
        self.isRunningAsApplicationBundle = isRunningAsApplicationBundle
    }

    func migrateLegacyRegistrationIfNeeded() -> LaunchAtLoginMigrationOutcome {
        guard isRunningAsApplicationBundle,
              let legacyArtifact,
              legacyArtifact.isInstalled else {
            return .notNeeded
        }

        switch service.status {
        case .enabled:
            return removeLegacyAfterEnablement()
        case .notRegistered, .notFound:
            do {
                try service.register()
            } catch {
                return .retainedLegacy(.registrationFailed(error.localizedDescription))
            }
            switch service.status {
            case .enabled:
                return removeLegacyAfterEnablement()
            case .requiresApproval:
                return .retainedLegacy(.requiresApproval)
            default:
                return .retainedLegacy(.modernServiceInactive)
            }
        case .requiresApproval:
            return .retainedLegacy(.requiresApproval)
        case .unknown:
            return .retainedLegacy(.modernServiceInactive)
        }
    }

    func currentState() -> LaunchAtLoginState {
        LaunchAtLoginPresenter.presentation(
            isRunningAsApplicationBundle: isRunningAsApplicationBundle,
            status: currentSystemStatus()
        )
    }

    func setEnabled(_ enabled: Bool) -> Result<LaunchAtLoginState, LaunchAtLoginError> {
        guard isRunningAsApplicationBundle else {
            return .failure(LaunchAtLoginError(
                message: AppLocalization.text(
                    "launchAtLogin.swiftRunUnavailable",
                    defaultValue: "当前 swift run 形态不能注册登录项"
                )
            ))
        }

        do {
            if enabled {
                try enable()
            } else {
                try disable()
            }
            return .success(currentState())
        } catch {
            return .failure(LaunchAtLoginError(message: error.localizedDescription))
        }
    }

    func diagnostics() -> LaunchAtLoginDiagnostics {
        let legacyArtifactInstalled = legacyArtifact?.isInstalled ?? false
        return LaunchAtLoginDiagnostics(
            serviceStatus: service.status,
            legacyArtifactInstalled: legacyArtifactInstalled,
            migrationNeeded: isRunningAsApplicationBundle && legacyArtifactInstalled
        )
    }

    private func enable() throws {
        switch service.status {
        case .notRegistered, .notFound:
            try service.register()
            if service.status == .requiresApproval {
                service.openSystemSettingsLoginItems()
            }
        case .requiresApproval:
            service.openSystemSettingsLoginItems()
        case .enabled:
            break
        case .unknown:
            break
        }

        if service.status == .enabled {
            try removeLegacyArtifactIfInstalled()
        }
    }

    private func disable() throws {
        switch service.status {
        case .enabled, .requiresApproval:
            try service.unregister()
        case .notRegistered, .notFound, .unknown:
            break
        }
        try removeLegacyArtifactIfInstalled()
    }

    private func removeLegacyAfterEnablement() -> LaunchAtLoginMigrationOutcome {
        do {
            try legacyArtifact?.remove()
            return .migrated
        } catch {
            return .retainedLegacy(.cleanupFailed(error.localizedDescription))
        }
    }

    private func removeLegacyArtifactIfInstalled() throws {
        guard legacyArtifact?.isInstalled == true else { return }
        try legacyArtifact?.remove()
    }

    private func currentSystemStatus() -> LaunchAtLoginSystemStatus {
        switch service.status {
        case .enabled:
            return .enabled
        case .notRegistered:
            return legacyArtifact?.isInstalled == true ? .legacyEnabled : .notRegistered
        case .requiresApproval:
            return .requiresApproval
        case .notFound:
            return legacyArtifact?.isInstalled == true ? .legacyEnabled : .notFound
        case .unknown:
            return .unknown
        }
    }
}
