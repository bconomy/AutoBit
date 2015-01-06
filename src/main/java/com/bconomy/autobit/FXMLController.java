package com.bconomy.autobit;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class FXMLController implements Initializable {
    
    @FXML
    private TextArea statusBox;
	private Button btnStart;
	private Button btnStop;
    
    @FXML
    private void onBtnStart(ActionEvent event) {
        System.out.println("You clicked Start!");
    }
	
    @FXML
    private void onBtnStop(ActionEvent event) {
        System.out.println("You clicked Stop!");
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    
}
