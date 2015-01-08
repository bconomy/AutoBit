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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public class FXMLController implements Initializable {
	public static FXMLController instance;
	private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS ");
	//df.format(new Date())

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
	private TextArea statusBox;
	private Button btnStart;
	private Button btnStop;

	@FXML
	private void onBtnStart(ActionEvent event) {
		System.out.println("******** Start");
		
		runThread = new RunThread();
		runThread.start();
	}
	@FXML
	private void onBtnStop(ActionEvent event) {
		System.out.println("******** Stop");
		
		if (runThread != null) {
			runThread.stop = true;
		}
	}

	
	//*******************Main
	@Override
	public void initialize(URL url, ResourceBundle rb) {
		instance = this;
		redirectSystemStreams(statusBox);
			
		
	}
    public void close() {
		if (runThread != null) {
			runThread.stop = true;
			runThread.interrupt();
		}
	}
	
	
	//*******************Util
	//redirect System.out to a textbox
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
