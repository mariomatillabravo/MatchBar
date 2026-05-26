package com.matchbar.app

import android.app.Application
import com.matchbar.app.data.api.MatchBarApi
import com.matchbar.app.data.api.NetworkModule
import com.matchbar.app.data.local.SessionStore

class MatchBarApp : Application() {

    lateinit var sessionStore: SessionStore
        private set
    lateinit var api: MatchBarApi
        private set

    override fun onCreate() {
        super.onCreate()
        sessionStore = SessionStore(this)
        api = NetworkModule.provideApi(sessionStore)
    }
}
