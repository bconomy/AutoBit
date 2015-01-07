/* 
 * Copyright (C) 2015 BownCo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package java.util.prefs.EncryptedPreferences;

import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

public abstract class ObfuscatedPreferences extends WrappedPreferences {
	protected ObfuscatedPreferences(AbstractPreferences parent, String name, AbstractPreferences target) {
		super(parent, name, target);
	}

	@Override
	protected String getSpi(String key) {
		return deObfuscateString(super.getSpi(obfuscateString(key)));
	}

	@Override
	protected void putSpi(String key, String value) {
		super.putSpi(obfuscateString(key), obfuscateString(value));
	}

	@Override
	protected void removeSpi(String key) {
		super.removeSpi(obfuscateString(key));
	}

	@Override
	protected String[] keysSpi() throws BackingStoreException {
		String keys[] = super.keysSpi();
		String dkeys[] = (String[])keys.clone();
		for (int i=0; i < dkeys.length; ++i) {
			dkeys[i] = deObfuscateString(dkeys[i]);
		}
		return dkeys;
	}

	abstract public String obfuscateString(String string);
	abstract public String deObfuscateString(String string);
}
