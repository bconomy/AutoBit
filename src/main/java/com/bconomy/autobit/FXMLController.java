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

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;

public class FXMLController implements Initializable {
	public static FXMLController instance;
	
	//TODO create timestamped logging
	//private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS ");
	//df.format(new Date())
	//String ln = String.format("%1$tF %1$tT %2$-20s %3$s\r\n", new java.util.Date(), loc, status);

	public RunThread runThread;
	public class RunThread extends Thread {
		public Boolean stop = false;
		@Override
		public void run() {
			while (true) {
				try {
					if (stop) break;
					
					System.out.println("Thread Running " + Math.random());
					Thread.sleep(1000);
				} catch (InterruptedException ex) {
				} finally {
				}
			}
		}
	}
	
	
	//*******************GUI
	@FXML
	private TextArea fxStatusBox;
	@FXML
	private TextField fxTextFieldCBAPIKey;
	@FXML
	private PasswordField fxPasswordFieldCBAPISecret;
	@FXML
	private TextField fxTextFieldBCWallet;
	
	
	@FXML
	private void onTabSettingsSelected(Event event) {
		if (!((Tab)event.getSource()).isSelected()) {
			saveSettings();
			System.out.println("Settings saved.");
		}
	}

	@FXML
	private void onButtonStart(ActionEvent event) {
		System.out.println("******** Start");
		
		runThread = new RunThread();
		runThread.start();
	}
	@FXML
	private void onButtonStop(ActionEvent event) {
		System.out.println("******** Stop");
		
		if (runThread != null) {
			runThread.stop = true;
		}
	}
	@FXML
	private void onHyperlinkCBGetAPIKey(ActionEvent event) {
		System.out.println("******** onHyperlinkCBGetAPIKey");
		
		if (Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().browse(new URI("https://coinbase.com/settings/api"));
			} catch (IOException | URISyntaxException ex) {
				ex.printStackTrace();
			}
		}
		//javafx.application.HostServices test = new HostServices(this);
//		HostServicesDelegate hostServices = HostServicesFactory.getInstance(this);
//		hostServices.showDocument("http://www.yahoo.com");
		//this.getHostServices().showDocument("http://www.yahoo.com");
//		try {
//			new ProcessBuilder("x-www-browser", "\"https://coinbase.com/settings/api\"").start();
//		} catch (IOException ex) {
//			ex.printStackTrace();
//		}
	}

	
	//*******************Main
	@Override
	public void initialize(URL url, ResourceBundle rb) {
		instance = this;
		redirectSystemStreams(fxStatusBox);
		
		
		//settings
//		Preferences root = Preferences.userRoot();
//		//Preferences root = Preferences.userRoot().node("/com/coremake/advertising");
//		//Preferences root = Preferences.userNodeForPackage(this.getClass());
//		System.out.println(root.absolutePath());
		
//		Encryption.key = root.getByteArray("item", null);
//		if (Encryption.key == null) {
//			Encryption.makeKey();
//			root.putByteArray("item", Encryption.key);
//		}
//		try {
//			root.flush();
//		} catch (BackingStoreException ex) {
//			ex.printStackTrace();
//		}
		
//		if (!settingsFile.exists()) settingsFile.getParentFile().mkdirs();
//		else loadSettings();
	}
    public void close() {
		if (runThread != null) {
			runThread.stop = true;
			runThread.interrupt();
		}
		saveSettings();
	}
	
	
	//*******************Util
	private final static File settingsFile = new File(System.getProperty("user.home"), "AutoBitSettings.json");
    private void saveSettings() {
		try (FileOutputStream fs = new FileOutputStream(settingsFile); JsonWriter json = Json.createWriter(fs);) {
			JsonObjectBuilder builder = Json.createObjectBuilder()
				.add("BCWallet", fxTextFieldBCWallet.getText())
				.add("CBAPISecret", Encryption.encrypt(fxPasswordFieldCBAPISecret.getText()))
			;
			json.writeObject(builder.build());
		} catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
			ex.printStackTrace();
		}
    }
    private void loadSettings() {
		if (!settingsFile.exists()) return;
		try (FileInputStream fs = new FileInputStream(settingsFile); JsonReader json = Json.createReader(fs);) {
			JsonObject model = json.readObject();
			if (model.containsKey("BCWallet")) fxTextFieldBCWallet.setText(model.getString("BCWallet", ""));
			if (model.containsKey("CBAPISecret")) fxPasswordFieldCBAPISecret.setText(Encryption.decrypt(model.getString("CBAPISecret", "")));
		} catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
			ex.printStackTrace();
		}
	}
	
	//redirect System.out to a TextArea
	private static void updateTextArea(final TextArea ta, final String text) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				int len = ta.getLength();
				if (len > 30000) ta.setText(ta.getText(len - 20000, len));
				ta.appendText(text);
			}
		});
	}
	public static void redirectSystemStreams(final TextArea ta) {
		OutputStream out = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				updateTextArea(ta, String.valueOf((char) b));
			}
			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				updateTextArea(ta, new String(b, off, len));
			}
			@Override
			public void write(byte[] b) throws IOException {
				write(b, 0, b.length);
			}
		};
		System.setOut(new PrintStream(out, true));
		System.setErr(new PrintStream(out, true));
	}
}
