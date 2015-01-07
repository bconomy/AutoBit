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
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public class EncryptionStuff {
	private static final String algorithm = "DES";
	private static final SecureRandom sr = new SecureRandom();
	private final SecretKey secretKey;
	private final Cipher cipher;

	public EncryptionStuff(SecretKey secretKey) throws GeneralSecurityException {
		this.secretKey = secretKey;
		cipher = Cipher.getInstance( algorithm );
	}

	public String arrayToString(byte raw[]) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<raw.length; ++i) {
			short s = (short)raw[i];
			if (s>0) s += 256;
			int hi = s>>4;
			int lo = s&0xf;
			sb.append((char)('a'+hi));
			sb.append((char)('a'+lo));
		}
		return sb.toString();
	}

	public byte[] stringToArray(String string) {
		StringBuilder sb = new StringBuilder(string);
		int len = sb.length();

		if ((len&1)==1) throw new RuntimeException("String must be of even length! "+string);

		byte raw[] = new byte[len/2];
		int ii=0;
		for (int i=0; i<len; i+=2) {
			int hic = sb.charAt(i) - 'a';
			int loc = sb.charAt(i+1) - 'a';
			byte b = (byte)((hic<<4) | loc);
			raw[ii++] = b;
		}
		return raw;
	}

	synchronized public String obfuscateString(String string) throws GeneralSecurityException {
		cipher.init(Cipher.ENCRYPT_MODE, secretKey, sr );
		byte raw[] = string.getBytes();
		byte oraw[] = cipher.doFinal(raw);
		String ostring = arrayToString(oraw);
		return ostring;
	}

	synchronized public String deObfuscateString(String string) throws GeneralSecurityException {
		cipher.init(Cipher.DECRYPT_MODE, secretKey, sr );
		byte raw[] = stringToArray(string);
		byte draw[] = cipher.doFinal(raw);
		String dstring = new String(draw);
		return dstring;
	}
}
