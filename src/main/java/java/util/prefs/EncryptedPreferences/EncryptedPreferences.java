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

import java.security.GeneralSecurityException;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;
import javax.crypto.SecretKey;

public class EncryptedPreferences extends ObfuscatedPreferences {
	
	private EncryptionStuff stuff;
	private void setStuff(EncryptionStuff stuff) { this.stuff = stuff; }
	private EncryptionStuff getStuff() { return stuff; }

	protected EncryptedPreferences(AbstractPreferences parent, String name, AbstractPreferences target) {
		super(parent, name, target);
	}

	@Override
	public String obfuscateString(String string) {
		try {
			return getStuff().obfuscateString(string);
		} catch(GeneralSecurityException gse) {
			gse.printStackTrace();
		}
		return null;
	}

	@Override
	public String deObfuscateString(String string) {
		try {
			return getStuff().deObfuscateString(string);
		} catch(GeneralSecurityException gse) {
			gse.printStackTrace();
		}
		return null;
	}

	@Override
	public WrappedPreferences wrapChild(WrappedPreferences parent, String name, AbstractPreferences child) {
		EncryptedPreferences ep = new EncryptedPreferences(parent, name, child);
		ep.setStuff(stuff);
		return ep;
	}

	public static Preferences userNodeForPackage(Class clasz, SecretKey secretKey) {
		AbstractPreferences ap = (AbstractPreferences)Preferences.userNodeForPackage(clasz);
		EncryptedPreferences ep = new EncryptedPreferences(null, "", ap);
		try {
			ep.setStuff(new EncryptionStuff(secretKey));
			return ep;
		} catch(GeneralSecurityException gse) {
			gse.printStackTrace();
		}
		return null;
	}

	public static Preferences systemNodeForPackage(Class clasz, SecretKey secretKey) {
		AbstractPreferences ap = (AbstractPreferences)Preferences.systemNodeForPackage(clasz);
		EncryptedPreferences ep = new EncryptedPreferences(null, "", ap);
		try {
			ep.setStuff(new EncryptionStuff(secretKey));
			return ep;
		} catch(GeneralSecurityException gse) {
			gse.printStackTrace();
		}
		return null;
	}
}
