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

public class DelegatedPreferences extends AbstractPreferences {
	private final AbstractPreferences target;
	private static final boolean verbose = false;

	protected DelegatedPreferences(AbstractPreferences parent, String name, AbstractPreferences target) {
		super(parent, name);
		this.target = target;
	}

	@Override
	protected String getSpi(String key) {
		if (verbose) System.out.println("DP["+target+"]:getSpi("+key+")");
		return target.get( key, null );
	}

	@Override
	protected void putSpi(String key, String value) {
		if (verbose) System.out.println("DP["+target+"]:putSpi("+key+", "+value+")");
		target.put( key, value );
	}

	@Override
	protected void removeSpi(String key) {
		if (verbose) System.out.println("DP["+target+"]:removeSpi("+key+")");
		target.remove(key);
	}

	@Override
	protected AbstractPreferences childSpi(String name) {
		if (verbose) System.out.println("DP["+target+"]:chlidSpi("+name+")");
		return (AbstractPreferences)target.node(name);
	}

	@Override
	protected void removeNodeSpi() throws BackingStoreException {
		if (verbose) System.out.println("DP["+target+"]:removeNode()");
		target.removeNode();
	}

	@Override
	protected String[] keysSpi() throws BackingStoreException {
		if (verbose) System.out.println("DP["+target+"]:keysSpi()");
		return target.keys();
	}

	@Override
	protected String[] childrenNamesSpi() throws BackingStoreException {
		if (verbose) System.out.println("DP["+target+"]:childrenNamesSpi()");
		return target.childrenNames();
	}

	@Override
	protected void syncSpi() throws BackingStoreException {
		if (verbose) System.out.println("DP["+target+"]:sync()");
		target.sync();
	}

	@Override
	protected void flushSpi() throws BackingStoreException {
		if (verbose) System.out.println("DP["+target+"]:flush()");
		target.flush();
	}
}
