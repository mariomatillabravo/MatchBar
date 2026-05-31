package com.matchbar.app.data.model

import kotlinx.serialization.Serializable

enum class Role { USER, BAR, ADMIN }

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val role: Role = Role.USER
)

@Serializable
data class AuthResponse(
    val token: String,
    val userId: String,
    val email: String,
    val name: String,
    val role: Role
)

@Serializable
data class Bar(
    val id: String,
    val name: String,
    val description: String? = null,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val ownerPhone: String? = null,
    val licenseDocFilename: String? = null,
    val photoUrls: List<String> = emptyList(),
    val menuUrls: List<String> = emptyList(),
    val status: String,
    val distanceMeters: Double? = null,
    val averageRating: Double? = null
)

@Serializable
data class BarUpsertRequest(
    val name: String,
    val description: String? = null,
    val address: String,
    val ownerPhone: String? = null
)

@Serializable
data class Match(
    val id: String,
    val competitionId: String,
    val competitionName: String,
    val homeTeamId: String,
    val homeTeamName: String,
    val homeTeamLogo: String? = null,
    val awayTeamId: String,
    val awayTeamName: String,
    val awayTeamLogo: String? = null,
    val kickoffAt: String
)

@Serializable
data class Review(
    val id: String,
    val userId: String,
    val userName: String,
    val ratingAtmosphere: Int,
    val ratingFood: Int,
    val ratingPrice: Int,
    val average: Double,
    val comment: String? = null,
    val createdAt: String
)

@Serializable
data class ReviewRequest(
    val ratingAtmosphere: Int,
    val ratingFood: Int,
    val ratingPrice: Int,
    val comment: String? = null
)

@Serializable
data class Competition(
    val id: String,
    val name: String,
    val country: String? = null,
    val logoUrl: String? = null
)

@Serializable
data class Team(
    val id: String,
    val name: String,
    val logoUrl: String? = null
)

@Serializable
data class LicenseUploadResponse(
    val filename: String
)

@Serializable
data class MyReview(
    val id: String,
    val barId: String,
    val barName: String,
    val ratingAtmosphere: Int,
    val ratingFood: Int,
    val ratingPrice: Int,
    val average: Double,
    val comment: String? = null,
    val createdAt: String
)
