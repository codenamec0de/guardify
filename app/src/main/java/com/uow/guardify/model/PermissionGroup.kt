package com.uow.guardify.model

/**
 * Defines a logical grouping of related Android permissions.
 * For example, "Location" groups fine, coarse, and background location.
 */
data class PermissionGroup(
    val id: String,
    val displayName: String,
    val iconRes: Int,
    val permissions: List<String>
) {
    /** Apps that hold at least one permission in this group, paired with which ones. */
    var apps: List<PermissionGroupApp> = emptyList()

    val appCount: Int get() = apps.size
}

/**
 * An app entry within a permission group, showing which specific
 * permissions from the group it holds.
 */
data class PermissionGroupApp(
    val appInfo: AppInfo,
    val matchedPermissions: List<String>   // human-readable names
)
