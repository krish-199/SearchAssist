package com.krishdev.searchassist

import android.content.Context
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class QuickSettingsService : TileService() {

    private val PREFS_NAME = "GestureLoggerPrefs"

    private fun isGestureActive(): Boolean {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("isGestureDetectionActive", false)
    }

    private fun setGestureActive(active: Boolean) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("isGestureDetectionActive", active).apply()
        MainActivity.isGestureDetectionActive = active
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (isGestureActive()) {
            ServiceSharedInstance.sendOverlayStatus(false)
            setGestureActive(false)
        } else {
            ServiceSharedInstance.sendOverlayStatus(true)
            setGestureActive(true)
        }
        updateTile()
    }

    private fun updateTile() {
        qsTile?.let {
            it.state =
                if (isGestureActive()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            it.updateTile()
        }
    }
}
