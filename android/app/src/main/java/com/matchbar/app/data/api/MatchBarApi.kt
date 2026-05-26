package com.matchbar.app.data.api

import com.matchbar.app.data.model.*
import okhttp3.MultipartBody
import retrofit2.http.*

interface MatchBarApi {

    // ----- Auth -----
    @POST("api/auth/login")
    suspend fun login(@Body req: LoginRequest): AuthResponse

    @POST("api/auth/register")
    suspend fun register(@Body req: RegisterRequest): AuthResponse

    // ----- Public -----
    @GET("api/public/competitions")
    suspend fun competitions(): List<Competition>

    @GET("api/public/teams")
    suspend fun teams(@Query("competitionId") competitionId: Long? = null): List<Team>

    // ----- Matches -----
    @GET("api/matches")
    suspend fun matches(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("competitionId") competitionId: Long? = null,
        @Query("teamId") teamId: Long? = null
    ): List<Match>

    @GET("api/matches/{id}")
    suspend fun match(@Path("id") id: Long): Match

    @POST("api/matches/{id}/schedule")
    suspend fun scheduleMatch(@Path("id") id: Long)

    @DELETE("api/matches/{id}/schedule")
    suspend fun cancelSchedule(@Path("id") id: Long)

    @GET("api/matches/scheduled")
    suspend fun myScheduledMatches(): List<Match>

    // ----- Bars -----
    @GET("api/bars/nearby")
    suspend fun nearbyBars(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("matchId") matchId: Long? = null,
        @Query("radiusMeters") radiusMeters: Int? = null
    ): List<Bar>

    @GET("api/bars/{id}")
    suspend fun bar(@Path("id") id: Long): Bar

    @GET("api/bars/me")
    suspend fun myBar(): Bar

    @POST("api/bars/me")
    suspend fun upsertMyBar(@Body req: BarUpsertRequest): Bar

    @Multipart
    @POST("api/bars/me/license")
    suspend fun uploadLicense(@Part file: MultipartBody.Part): LicenseUploadResponse

    // ----- Reviews -----
    @GET("api/bars/{id}/reviews")
    suspend fun barReviews(@Path("id") id: Long): List<Review>

    @POST("api/bars/{id}/reviews")
    suspend fun addReview(@Path("id") id: Long, @Body req: ReviewRequest): Review

    // ----- Favorites -----
    @GET("api/users/me/favorites")
    suspend fun favorites(): List<Bar>

    @POST("api/users/me/favorites/{barId}")
    suspend fun addFavorite(@Path("barId") barId: Long)

    @DELETE("api/users/me/favorites/{barId}")
    suspend fun removeFavorite(@Path("barId") barId: Long)

    // ----- Admin -----
    @GET("api/admin/bars/pending")
    suspend fun pendingBars(): List<Bar>

    @PATCH("api/admin/bars/{id}/approve")
    suspend fun approveBar(@Path("id") id: Long): Bar

    @PATCH("api/admin/bars/{id}/reject")
    suspend fun rejectBar(@Path("id") id: Long): Bar
}
