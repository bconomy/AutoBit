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

import com.coinbase.api.Coinbase;
import com.coinbase.api.CoinbaseBuilder;
import com.coinbase.api.entity.Transaction;
import com.coinbase.api.entity.Transfer;
import com.coinbase.api.exception.CoinbaseException;
import info.blockchain.api.APIException;
import info.blockchain.api.blockexplorer.BlockExplorer;
import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

public class FXMLController implements Initializable {
	public static FXMLController instance;
	
	private static final long BC_BTC_SCALE = 100000000; //1 BTC
	private static final Money BTC_1 = Money.parse("BTC 1");
	private static final Money BTC_0 = Money.parse("BTC 0");
	private static final Money BTC_FEE = Money.parse("BTC 0.0001");
	private static final Money BTC_SIGNIFICANT = Money.parse("BTC 0.02");
	private static final Money USD_BUY_ACC = Money.parse("USD 0.01");
	
	private static Money BCBTCtoMoney(long BCBTC) {
		return BTC_1.multipliedBy(BCBTC, RoundingMode.HALF_EVEN).dividedBy(BC_BTC_SCALE, RoundingMode.HALF_EVEN);
	}

	public RunThread runThread;
	public class RunThread extends Thread {
		public Boolean stop = false;
		public Boolean running = true;
		@Override
		public void run() {
			String ProfitWallet = fxTextFieldProfitWallet.getText();
			String BCwallet = fxTextFieldBCWallet.getText();
			
			//check BCaddress/keys and get initial balance from Blockchain
			long BCbalanceLast = -1;
			BlockExplorer blockExplorer = new BlockExplorer();
			try {
				info.blockchain.api.blockexplorer.Address BCaddress = blockExplorer.getAddress(BCwallet);
				BCbalanceLast = BCaddress.getFinalBalance();
				System.out.println(String.format("Blockchain BTC balance [%s]", BCBTCtoMoney(BCbalanceLast)));
			} catch (APIException | IOException ex) {
				ex.printStackTrace();
				return;
			}
//			Wallet BCwallet = new Wallet(fxTextFieldBCWalletIdent.getText(), fxPasswordFieldBCWalletPass.getText(), fxPasswordFieldBCWallet2Pass.getText());
//			try {
//				BCbalanceLast = BCwallet.getBalance();
//			} catch (APIException | IOException ex) {
//				ex.printStackTrace();
//				return;
//			}
			
			//check Coinbase access and get initial balance
			Coinbase cb = new CoinbaseBuilder()
				.withApiKey(fxTextFieldCBAPIKey.getText(), fxPasswordFieldCBAPISecret.getText())
				.withAccountId(fxTextFieldCBAcctId.getText())
				.build();
			try {
				Money CBbal = cb.getBalance();
				System.out.println(String.format("Coinbase BTC balance [%s]", CBbal));
			} catch (IOException | CoinbaseException ex) {
				ex.printStackTrace();
				return;
			}
			
			//how often to topup Blockchain wallet
			int topupFreq = 1440*60000; //one day
			try {
				topupFreq = Integer.parseInt(fxTextFieldTopupFreqMin.getText())*60000; //minutes
			} catch (NumberFormatException ex) {
				//ex.printStackTrace();
			}
			int day = (int)(System.currentTimeMillis()/topupFreq);
			
			System.out.println("******** Started");
			int runs = 0;
			while (true) {
				try {
					if (stop) break;
					
					//check Blockchain wallet for change in balance
					info.blockchain.api.blockexplorer.Address BCaddress = blockExplorer.getAddress(BCwallet);
					long BCbalanceCurrent = BCaddress.getFinalBalance();
//					long BCbalanceCurrent = BCaddress.getBalance();
					long BCbalanceDiff = BCbalanceCurrent - BCbalanceLast;
					BCbalanceLast = BCbalanceCurrent;
					//TODO replace with blinking indicator
					//System.out.println(String.format("Blockchain BTC balance current[%s] diff[%s]", BCbalanceCurrent, BCbalanceDiff));
					
					//long BCbalanceDiff = -3700000; //0.037 BTC //amount lost in BC wallet
					
					if (BCbalanceDiff < 0) { //sold bitcoin
						BCbalanceDiff = -BCbalanceDiff;
						
						System.out.println(String.format("Sold BTC [%1$s] [%2$tF %2$tT]", BCbalanceDiff, new java.util.Date()));

						Money CBspot = cb.getSpotPrice(CurrencyUnit.USD);
						System.out.println(String.format("Coinbase spot price [%s]", CBspot));

						double percentMarkup = 10.0; //default
						percentMarkup = Double.parseDouble(fxTextFieldPercentMarkup.getText());
						percentMarkup = percentMarkup/100+1;
						System.out.println(String.format("Machine percent markup [%s]", percentMarkup));

						Money VMsellPrice = CBspot.multipliedBy(percentMarkup, RoundingMode.HALF_EVEN);
						System.out.println(String.format("Machine sell price [%s]", VMsellPrice));

						Money VMBTCsold = BCBTCtoMoney(BCbalanceDiff);
						System.out.println(String.format("Machine BTC sold [%s]", VMBTCsold));

						Money VMUSDin = VMsellPrice.multipliedBy(VMBTCsold.getAmount(), RoundingMode.HALF_EVEN).rounded(0, RoundingMode.HALF_EVEN); //even dollar amount
						System.out.println(String.format("Machine USD put in [%s]", VMUSDin));

						//figure out how much BTC VMUSDin will get me
						Money calcBuy = VMBTCsold;
						Money calcBuyDiff = Money.ofMajor(CurrencyUnit.USD, 0);
						Money calcBuyDiffBTC = BTC_0;
						for (int i=0; i < 20; i++) {
							Money CBquotex = cb.getBuyQuote(calcBuy).getTotal();
							calcBuyDiff = VMUSDin.minus(CBquotex);

							System.out.println(String.format("calc CBquotex[%s] calcBuyDiff[%s] calcBuyDiffBTC[%s] calcBuy[%s]", CBquotex, calcBuyDiff, calcBuyDiffBTC, calcBuy));

							if (calcBuyDiff.abs().isLessThan(USD_BUY_ACC)) break;

							calcBuyDiffBTC = BTC_1.multipliedBy(calcBuyDiff.getAmount(), RoundingMode.HALF_EVEN).dividedBy(cb.getSpotPrice(CurrencyUnit.USD).getAmount(), RoundingMode.HALF_EVEN);

							calcBuy = calcBuy.plus(calcBuyDiffBTC);
						}

//						Quote CBquote = cb.getBuyQuote(calcBuy);
//						//Map<String, Money> fees = CBquote.getFees();
//						//System.out.println(String.format("Coinbase quote payout date [%s]", CBquote.getPayoutDate()));
//						System.out.println(String.format("Coinbase quote subtotal [%s]", CBquote.getSubtotal()));
//						System.out.println(String.format("Coinbase quote total [%s]", CBquote.getTotal()));

						//buy bitcoin
						Transfer CBbuy = cb.buy(calcBuy);
						System.out.println(String.format("Coinbase buy status [%s]", CBbuy.getStatus()));
						System.out.println(String.format("Coinbase buy id/code [%s]", CBbuy.getCode())); // "6H7GYLXZ"
						System.out.println(String.format("Coinbase buy payout date [%s]", CBbuy.getPayoutDate())); // "2013-02-01T18:00:00-0800"
						System.out.println(String.format("Coinbase buy subtotal [%s]", CBbuy.getSubtotal()));
						System.out.println(String.format("Coinbase buy total [%s]", CBbuy.getTotal())); // "USD 3"

						//send profit
						Money profit = calcBuy.minus(VMBTCsold);
						System.out.println(String.format("****BTC profit [%s]", profit));
						
						Transaction CBsend = new Transaction();
						CBsend.setTo(ProfitWallet);
						CBsend.setAmount(profit);
						CBsend.setInstantBuy(true);
						CBsend.setUserFee(BTC_FEE.getAmount()); //TODO make this dynamic, if error add fee. coinbase says above 0.01 BTC they pay fee, so don't need to set
						//CBsend.setNotes(CBspot.toString());
						Transaction r = cb.sendMoney(CBsend);
						System.out.println(String.format("Coinbase send BTC profit status [%s]", r.getStatus()));
						System.out.println(String.format("Coinbase send BTC profit detailed status [%s]", r.getDetailedStatus()));
						
						System.out.println("BTC rebuy: done.");
					}
					
					
					//check time, if day change then send bitcoin from Coinbase to Blockchain once a day to top up BTC
					int now = (int)(System.currentTimeMillis()/topupFreq);
					if (day != now) { //happens at midnight
						day = now;
						System.out.println(String.format("****Topup [%1$tF %1$tT]", new java.util.Date()));
						
						double USDMaxDaily = 300.0; //default
						USDMaxDaily = Double.parseDouble(fxTextFieldUSDMaxDaily.getText());
						
						Money topup = BTC_1.multipliedBy(USDMaxDaily, RoundingMode.HALF_EVEN).dividedBy(cb.getSpotPrice(CurrencyUnit.USD).getAmount(), RoundingMode.HALF_EVEN);
						System.out.println(String.format("topup: how much BTC do we want [%s]", topup));
						
						Money BCbalance = BCBTCtoMoney(BCbalanceCurrent);
						System.out.println(String.format("Blockchain BTC balance [%s]", BCbalance));
						
						topup = topup.minus(BCbalance);
						System.out.println(String.format("topup: how much BTC do we need [%s]", topup));
						
						if (topup.isGreaterThan(BTC_SIGNIFICANT)) { //only if positive and significant
							Money CBbalx = cb.getBalance().minus(BTC_FEE);
							System.out.println(String.format("Coinbase BTC balance (minus fee) [%s]", CBbalx));
							if (topup.isGreaterThan(CBbalx)) topup = CBbalx; //cant send somthing we dont have
							System.out.println(String.format("topup: sending [%s]", topup));
							
							Transaction CBsend = new Transaction();
							CBsend.setTo(BCwallet);
							CBsend.setAmount(topup);
							CBsend.setInstantBuy(true);
							CBsend.setUserFee(BTC_FEE.getAmount()); //TODO make this dynamic, if error add fee. coinbase says above 0.01 BTC they pay fee, so don't need to set
							//CBsend.setNotes(CBspot.toString());
							Transaction r = cb.sendMoney(CBsend);
							System.out.println(String.format("Coinbase send BTC status [%s]", r.getStatus()));
							System.out.println(String.format("Coinbase send BTC detailed status [%s]", r.getDetailedStatus()));
						}
						System.out.println("topup: done.");
						
					}
					
					//System.out.println("Thread Running " + runs);
					Thread.sleep(4400+(int)(Math.random()*800)); //TODO maybe use secureRandom
				} catch (InterruptedException ex) {
					//ex.printStackTrace();
				} catch (APIException | IOException | CoinbaseException ex) {
					ex.printStackTrace();
				}
				runs++;
			}
			System.out.println("******** Stopped");
			running = false;
		}
	}
	
	
	//*******************GUI
	@FXML private TextArea fxStatusBox;
	@FXML private CheckBox fxCheckBoxAutostart;
	@FXML private TextField fxTextFieldPercentMarkup;
	@FXML private TextField fxTextFieldUSDMaxDaily;
	@FXML private TextField fxTextFieldTopupFreqMin;
	@FXML private TextField fxTextFieldProfitWallet;
	@FXML private TextField fxTextFieldCBAcctId;
	@FXML private TextField fxTextFieldCBAPIKey;
	@FXML private PasswordField fxPasswordFieldCBAPISecret;
	@FXML private TextField fxTextFieldBCWallet;
	@FXML private TextField fxTextFieldBCWalletIdent;
	@FXML private PasswordField fxPasswordFieldBCWalletPass;
	@FXML private PasswordField fxPasswordFieldBCWallet2Pass;
	
	@FXML
	private void onTabSettingsSelected(Event event) {
		if (!((Tab)event.getSource()).isSelected()) {
			saveSettings();
			System.out.println("Settings saved.");
		}
	}
	@FXML
	private void onButtonStart(ActionEvent event) {
		if (runThread != null && runThread.running) return;
		runThread = new RunThread();
		runThread.start();
	}
	@FXML
	private void onButtonStop(ActionEvent event) {
		if (runThread != null) runThread.stop = true;
	}
	@FXML
	private void onButtonDeleteAll(ActionEvent event) {
		try {
			appSettingsFile.delete();
			Files.deleteIfExists(appSettingsKeyPath);
			Files.deleteIfExists(appPath);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	@FXML
	private void onHyperlinkCBGetAPIKey(ActionEvent event) {
		//System.out.println("******** onHyperlinkCBGetAPIKey");
		
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
	private final static Path appPath = Paths.get(System.getProperty("user.home"), ".AutoBit");
	private final static Path appSettingsKeyPath = Paths.get(appPath.toString(), ".AutoBitKey");
	@Override
	public void initialize(URL url, ResourceBundle rb) {
		instance = this;
		try {
			Files.createDirectories(appPath);
		} catch (IOException ex) {
			ex.printStackTrace();
			return;
		}
		redirectSystemStreams(fxStatusBox);
		
		//info.blockchain.api.HttpClient.TIMEOUT_MS = 60000;
		
		//settings encryption
		//TODO make this more secure, possibly store in java.util.prefs.Preferences or System.getenv
		byte[] keyLocal = {(byte)0x12, (byte)0xa8, (byte)0xd9, (byte)0x83, (byte)0x4c, (byte)0x67, (byte)0x23, (byte)0x82, 
						(byte)0x87, (byte)0x04, (byte)0xd4, (byte)0x7a, (byte)0x11, (byte)0x83, (byte)0xee, (byte)0x3b};
		boolean keyFileRead = false;
		try {
			Encryption.key = Encryption.decrypt(Files.readAllBytes(appSettingsKeyPath), keyLocal);
			if (Encryption.key.length == 16) keyFileRead = true;
		} catch (IOException ex) { }
		if (!keyFileRead) {
			Encryption.makeRandomKey();
			try {
				Files.write(appSettingsKeyPath, Encryption.encrypt(Encryption.key, keyLocal));
				Object hidden = Files.getAttribute(appSettingsKeyPath, "dos:hidden");
				if (hidden != null && !(Boolean)hidden) Files.setAttribute(appSettingsKeyPath, "dos:hidden", true);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		if (appSettingsFile.exists()) loadSettings();
		
		if (fxCheckBoxAutostart.isSelected()) {
			runThread = new RunThread();
			runThread.start();
		}
	}
    public void close() {
		if (runThread != null) {
			runThread.stop = true;
			runThread.interrupt();
		}
		saveSettings();
	}
	
	
	//*******************Utilities
	private final static File appSettingsFile = new File(appPath.toString(), "AutoBitSettings.json");
    private void saveSettings() {
		try (FileOutputStream fs = new FileOutputStream(appSettingsFile); JsonWriter json = Json.createWriter(fs);) {
			JsonObjectBuilder builder = Json.createObjectBuilder()
				.add("Autostart", fxCheckBoxAutostart.isSelected())
				.add("PercentMarkup", Encryption.encrypt(fxTextFieldPercentMarkup.getText()))
				.add("USDMaxDaily", Encryption.encrypt(fxTextFieldUSDMaxDaily.getText()))
				.add("TopupFreqMin", Encryption.encrypt(fxTextFieldTopupFreqMin.getText()))
				.add("ProfitWallet", Encryption.encrypt(fxTextFieldProfitWallet.getText()))
				.add("CBAcctId", Encryption.encrypt(fxTextFieldCBAcctId.getText()))
				.add("CBAPIKey", Encryption.encrypt(fxTextFieldCBAPIKey.getText()))
				.add("CBAPISecret", Encryption.encrypt(fxPasswordFieldCBAPISecret.getText()))
				.add("BCWallet", Encryption.encrypt(fxTextFieldBCWallet.getText()))
				.add("BCWalletIdent", Encryption.encrypt(fxTextFieldBCWalletIdent.getText()))
				.add("BCWalletPass", Encryption.encrypt(fxPasswordFieldBCWalletPass.getText()))
				.add("BCWallet2Pass", Encryption.encrypt(fxPasswordFieldBCWallet2Pass.getText()))
			;
			json.writeObject(builder.build());
		} catch (IOException ex) {
			ex.printStackTrace();
		}
    }
    private void loadSettings() {
		if (!appSettingsFile.exists()) return;
		try (FileInputStream fs = new FileInputStream(appSettingsFile); JsonReader json = Json.createReader(fs);) {
			JsonObject model = json.readObject();
			if (model.containsKey("Autostart")) fxCheckBoxAutostart.setSelected(model.getBoolean("Autostart", false));
			if (model.containsKey("PercentMarkup")) fxTextFieldPercentMarkup.setText(Encryption.decrypt(model.getString("PercentMarkup", "")));
			if (model.containsKey("USDMaxDaily")) fxTextFieldUSDMaxDaily.setText(Encryption.decrypt(model.getString("USDMaxDaily", "")));
			if (model.containsKey("TopupFreqMin")) fxTextFieldTopupFreqMin.setText(Encryption.decrypt(model.getString("TopupFreqMin", "")));
			if (model.containsKey("ProfitWallet")) fxTextFieldProfitWallet.setText(Encryption.decrypt(model.getString("ProfitWallet", "")));
			if (model.containsKey("CBAcctId")) fxTextFieldCBAcctId.setText(Encryption.decrypt(model.getString("CBAcctId", "")));
			if (model.containsKey("CBAPIKey")) fxTextFieldCBAPIKey.setText(Encryption.decrypt(model.getString("CBAPIKey", "")));
			if (model.containsKey("CBAPISecret")) fxPasswordFieldCBAPISecret.setText(Encryption.decrypt(model.getString("CBAPISecret", "")));
			if (model.containsKey("BCWallet")) fxTextFieldBCWallet.setText(Encryption.decrypt(model.getString("BCWallet", "")));
			if (model.containsKey("BCWalletIdent")) fxTextFieldBCWalletIdent.setText(Encryption.decrypt(model.getString("BCWalletIdent", "")));
			if (model.containsKey("BCWalletPass")) fxPasswordFieldBCWalletPass.setText(Encryption.decrypt(model.getString("BCWalletPass", "")));
			if (model.containsKey("BCWallet2Pass")) fxPasswordFieldBCWallet2Pass.setText(Encryption.decrypt(model.getString("BCWallet2Pass", "")));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	//TODO create timestamped and file based logging
	//private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS ");
	//df.format(new Date())
	//String ln = String.format("%1$tF %1$tT %2$-20s %3$s\r\n", new java.util.Date(), loc, status);
	
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
