package com.matchbar.app.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"

    const val MATCHES = "matches"
    const val MATCH_DETAIL = "match/{matchId}"
    fun matchDetail(matchId: String) = "match/$matchId"

    const val BAR_DETAIL = "bar/{barId}"
    fun barDetail(barId: String) = "bar/$barId"

    const val FAVORITES = "favorites"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val MAP_ALL = "map-all"
    const val MAP = "map?matchId={matchId}"
    fun map(matchId: String?) = if (matchId == null) "map?matchId=" else "map?matchId=$matchId"

    // Bar (rol BAR)
    const val MY_BAR = "my-bar"
    const val MY_BAR_EDIT = "my-bar/edit"
    const val MY_BROADCASTS = "my-broadcasts"

    // Admin
    const val ADMIN_PENDING = "admin/pending"
}
