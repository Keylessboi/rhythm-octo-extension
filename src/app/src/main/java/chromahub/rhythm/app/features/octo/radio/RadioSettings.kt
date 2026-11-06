// SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
// SPDX-License-Identifier: GPL-3.0-or-later
package chromahub.rhythm.app.features.octo.radio

import android.content.Context

/** Fork (Octo): how the user tuned radio, kept as the names of the choices so a renamed one falls back to the default. */
object RadioSettings {
    private const val FILE = "octo_radio"

    fun parse(discovery: String?, adventure: String?, variety: String?, favorites: Boolean) = RadioTuning(
        discovery = RadioDiscovery.entries.firstOrNull { it.name == discovery } ?: RadioDiscovery.Balanced,
        adventure = RadioAdventure.entries.firstOrNull { it.name == adventure } ?: RadioAdventure.Balanced,
        variety = RadioVariety.entries.firstOrNull { it.name == variety } ?: RadioVariety.Normal,
        favorites = favorites,
    )

    fun load(context: Context): RadioTuning {
        val p = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return parse(p.getString("discovery", null), p.getString("adventure", null), p.getString("variety", null), p.getBoolean("favorites", false))
    }

    fun save(context: Context, tuning: RadioTuning) {
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString("discovery", tuning.discovery.name).putString("adventure", tuning.adventure.name)
            .putString("variety", tuning.variety.name).putBoolean("favorites", tuning.favorites).apply()
    }
}
