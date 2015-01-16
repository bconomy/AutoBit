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
package com.bconomy.autobit;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;

public class Encryption {
	public static byte[] key = null;
	private static SecretKeySpec keySpec = null;

	public static void makeRandomKey() {
		SecureRandom random;
		try {
			random = SecureRandom.getInstanceStrong();
		} catch (NoSuchAlgorithmException ex) {
			random = new SecureRandom();
		}
		key = new byte[16];
		random.nextBytes(key);
	}

	public static String encrypt(String cleartext) {
		if (cleartext == null || cleartext.equals("")) return "";
		try {
			return Base64.encodeBase64String(encrypt(cleartext.getBytes("UTF-8")));
		} catch (UnsupportedEncodingException ex) {
			ex.printStackTrace();
		}
		return "";
	}
	public static String encrypt(char[] cleartext) {
		if (cleartext.length == 0) return "";
		return Base64.encodeBase64String(encrypt(charsToBytes(cleartext)));
	}
	public static byte[] encrypt(byte[] cleartext) {
		if (key == null) return new byte[0];
		return encrypt(cleartext, key);
	}
	public static byte[] encrypt(byte[] cleartext, byte[] key) {
		if (keySpec == null) keySpec = new SecretKeySpec(key, "AES");
		Cipher aes;
		try {
			aes = Cipher.getInstance("AES/ECB/PKCS5Padding");
			aes.init(Cipher.ENCRYPT_MODE, keySpec);
			return aes.doFinal(cleartext);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
			ex.printStackTrace();
		}
		return new byte[0];
	}

	public static String decrypt(String cyphertext) {
		if (cyphertext == null || cyphertext.equals("") || !Base64.isBase64(cyphertext)) return "";
		try {
			return new String(decrypt(Base64.decodeBase64(cyphertext)),"UTF-8");
		} catch (UnsupportedEncodingException ex) {
			ex.printStackTrace();
		}
		return "";
	}
	public static byte[] decrypt(byte[] cyphertext) {
		if (key == null) return new byte[0];
		return decrypt(cyphertext, key);
	}
	public static byte[] decrypt(byte[] cyphertext, byte[] key) {
		if (keySpec == null) keySpec = new SecretKeySpec(key, "AES");
		Cipher aes;
		try {
			aes = Cipher.getInstance("AES/ECB/PKCS5Padding");
			aes.init(Cipher.DECRYPT_MODE, keySpec);
			return aes.doFinal(cyphertext);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
			ex.printStackTrace();
		}
		return new byte[0];
	}

	private static byte[] charsToBytes(char[] chars) {
		CharBuffer charBuffer = CharBuffer.wrap(chars);
		ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
		byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
		Arrays.fill(charBuffer.array(), '\u0000'); // clear sensitive data
		Arrays.fill(byteBuffer.array(), (byte)0); // clear sensitive data
		Arrays.fill(chars, '\u0000'); // clear sensitive data
		return bytes;
	}
}
