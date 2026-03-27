package com.uow.guardify.model

import com.google.gson.annotations.SerializedName

data class ExodusResponse(
    @SerializedName("handle")
    val handle: String?,
    @SerializedName("app_name")
    val appName: String?,
    @SerializedName("trackers")
    val trackers: List<Int>?,
    @SerializedName("permissions")
    val permissions: List<String>?,
    @SerializedName("version")
    val version: String?,
    @SerializedName("version_code")
    val versionCode: String?,
    @SerializedName("source")
    val source: String?,
    @SerializedName("report")
    val report: Int?,
    @SerializedName("creator")
    val creator: String?,
    @SerializedName("downloads")
    val downloads: String?,
    @SerializedName("created")
    val created: String?,
    @SerializedName("updated")
    val updated: String?
)

data class ExodusSearchResponse(
    @SerializedName("handle")
    val handle: String?,
    @SerializedName("applications")
    val applications: List<ExodusApp>?
)

data class ExodusApp(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("handle")
    val handle: String?,
    @SerializedName("app_name")
    val appName: String?,
    @SerializedName("creator")
    val creator: String?,
    @SerializedName("downloads")
    val downloads: String?
)

data class TrackerInfo(
    val id: Int,
    val name: String,
    val description: String?,
    val creationDate: String?,
    val codeSignature: String?,
    val networkSignature: String?,
    val website: String?,
    val categories: List<String>?
)

data class ExodusTrackerResponse(
    @SerializedName("trackers")
    val trackers: Map<String, TrackerDetail>?
)

data class TrackerDetail(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("creation_date")
    val creationDate: String?,
    @SerializedName("code_signature")
    val codeSignature: String?,
    @SerializedName("network_signature")
    val networkSignature: String?,
    @SerializedName("website")
    val website: String?,
    @SerializedName("categories")
    val categories: List<String>?
)

data class ExodusReportResponse(
    @SerializedName("reports")
    val reports: List<ExodusReport>?
)

data class ExodusReport(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("downloads")
    val downloads: String?,
    @SerializedName("version")
    val version: String?,
    @SerializedName("version_code")
    val versionCode: Long?,
    @SerializedName("trackers")
    val trackers: List<Int>?,
    @SerializedName("creation_date")
    val creationDate: String?,
    @SerializedName("updated")
    val updated: String?
)
