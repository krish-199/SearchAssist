package com.krishdev.searchassist

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class QuickSettingsService : TileService() {
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
        if (MainActivity.isGestureDetectionActive.value) {
            MainActivity().stopAccessibilityService()
        } else {
            MainActivity().startAccessibilityService() // Default values
        }
        updateTile()
    }

    private fun updateTile() {
        qsTile?.let {
            it.state =
                if (MainActivity.isGestureDetectionActive.value) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            it.updateTile()
        }
    }
}
