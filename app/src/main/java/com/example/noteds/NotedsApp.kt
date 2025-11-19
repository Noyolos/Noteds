package com.example.noteds

import android.app.Application
import com.example.noteds.di.AppContainer

class NotedsApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
