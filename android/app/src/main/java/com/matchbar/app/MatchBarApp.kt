package com.matchbar.app

import android.app.Application
import com.matchbar.app.data.api.MatchBarApi
import com.matchbar.app.data.api.NetworkModule
import com.matchbar.app.data.local.SessionStore
import org.osmdroid.config.Configuration

class MatchBarApp : Application() {

    lateinit var sessionStore: SessionStore
        private set
    lateinit var api: MatchBarApi
        private set

    override fun onCreate() {
        super.onCreate()
        sessionStore = SessionStore(this)
        api = NetworkModule.provideApi(sessionStore)

        // osmdroid exige un user-agent para descargar los tiles de OpenStreetMap.
        Configuration.getInstance().userAgentValue = packageName
    }
}
