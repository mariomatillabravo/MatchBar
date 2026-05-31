package com.matchbar.app.data.api

import com.matchbar.app.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
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
    suspend fun teams(@Query("competitionId") competitionId: String? = null): List<Team>

    // ----- Matches -----
    @GET("api/matches")
    suspend fun matches(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("competitionId") competitionId: String? = null,
        @Query("teamId") teamId: String? = null
    ): List<Match>

    @GET("api/matches/{id}")
    suspend fun match(@Path("id") id: String): Match

    @POST("api/matches/{id}/schedule")
    suspend fun scheduleMatch(@Path("id") id: String)

    @DELETE("api/matches/{id}/schedule")
    suspend fun cancelSchedule(@Path("id") id: String)

    @GET("api/matches/scheduled")
    suspend fun myScheduledMatches(): List<Match>

    // ----- Bars -----
    @GET("api/bars")
    suspend fun allBars(): List<Bar>

    @GET("api/bars/{id}/matches")
    suspend fun barUpcomingMatches(@Path("id") barId: String): List<Match>

    @GET("api/bars/nearby")
    suspend fun nearbyBars(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("matchId") matchId: String? = null,
        @Query("radiusMeters") radiusMeters: Int? = null
    ): List<Bar>

    @GET("api/bars/{id}")
    suspend fun bar(@Path("id") id: String): Bar

    @GET("api/bars/me")
    suspend fun myBar(): Bar

    @POST("api/bars/me")
    suspend fun upsertMyBar(@Body req: BarUpsertRequest): Bar

    @Multipart
    @POST("api/bars/me/license")
    suspend fun uploadLicense(@Part file: MultipartBody.Part): LicenseUploadResponse

    @Multipart
    @POST("api/bars/me/photos")
    suspend fun uploadBarPhoto(@Part file: MultipartBody.Part): Bar

    @DELETE("api/bars/me/photos/{fileId}")
    suspend fun deleteBarPhoto(@Path("fileId") fileId: String): Bar

    @Multipart
    @POST("api/bars/me/menu")
    suspend fun uploadBarMenu(@Part file: MultipartBody.Part): Bar

    @DELETE("api/bars/me/menu/{fileId}")
    suspend fun deleteBarMenu(@Path("fileId") fileId: String): Bar

    // ----- Reviews -----
    @GET("api/bars/{id}/reviews")
    suspend fun barReviews(@Path("id") id: String): List<Review>

    @POST("api/bars/{id}/reviews")
    suspend fun addReview(@Path("id") id: String, @Body req: ReviewRequest): Review

    // ----- Mis reseñas -----
    @GET("api/users/me/reviews")
    suspend fun myReviews(): List<MyReview>

    // ----- Incidencias -----
    @Multipart
    @POST("api/incidents")
    suspend fun submitIncident(
        @Part("subject") subject: RequestBody,
        @Part("message") message: RequestBody,
        @Part photos: List<MultipartBody.Part>
    )

    // ----- Favorites -----
    @GET("api/users/me/favorites")
    suspend fun favorites(): List<Bar>

    @POST("api/users/me/favorites/{barId}")
    suspend fun addFavorite(@Path("barId") barId: String)

    @DELETE("api/users/me/favorites/{barId}")
    suspend fun removeFavorite(@Path("barId") barId: String)

    // ----- Admin -----
    @GET("api/admin/bars/pending")
    suspend fun pendingBars(): List<Bar>

    @PATCH("api/admin/bars/{id}/approve")
    suspend fun approveBar(@Path("id") id: String): Bar

    @PATCH("api/admin/bars/{id}/reject")
    suspend fun rejectBar(@Path("id") id: String): Bar
}
