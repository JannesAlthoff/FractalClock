/*
    Copyright (C) 2020  Jannes Althoff

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>
*/
package jolokhd.fractalclock.wallpaper

import android.content.DialogInterface
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.marcoscg.licenser.Library
import com.marcoscg.licenser.License
import com.marcoscg.licenser.LicenserDialog


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            findPreference<Preference>("license")?.setOnPreferenceClickListener { return@setOnPreferenceClickListener showPrefs(); }
        }

        private fun showPrefs (): Boolean {
                LicenserDialog(this.context)
                    .setTitle("Licenses")
                    //.setCustomNoticeTitle("Notices for files:")
                    .setLibrary(
                        Library("FractalClock LiveWallpaper",
                        "https://github.com/JolokHD/FractalClock",
                        License.GNU3))
                    .setLibrary(Library("AndroidX Support Libraries",
                        "https://developer.android.com/jetpack/androidx",
                        License.APACHE2))
                    .setLibrary(Library(
                        "Kotlin stdlib",
                        "https://kotlinlang.org",
                        License.APACHE2))
                    .setLibrary(Library("Color Picker",
                        "https://github.com/jaredrummler/ColorPicker",
                        License.APACHE2))
                    .setLibrary(Library("Licenser",
                        "https://github.com/marcoscgdev/Licenser",
                        License.MIT))
                    .setLibrary(Library("FractalClock",
                        "https://github.com/HackerPoet/FractalClock",
                        License.APACHE2))
                    .setPositiveButton(android.R.string.ok,
                        DialogInterface.OnClickListener { _, _ ->})
                    .show()
            return true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
