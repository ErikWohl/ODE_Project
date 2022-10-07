package at.fhtw.bic.ode_project;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class HelloController {
    @FXML
    private Canvas canvas1;

    @FXML
    private Button buttonTest;

    @FXML
    private TextArea textOutput;
    @FXML
    private TextField textInput;

    @FXML
    protected void onTestClick() {
        var gc = canvas1.getGraphicsContext2D();
        gc.clearRect(0,0, canvas1.getWidth(), canvas1.getHeight());
        System.out.printf("Button was clicked!\n");
    }

    @FXML
    protected void onEnterText() {
        textOutput.setText(textOutput.getText() + "\n" + textInput.getText());
        textInput.clear();
    }
    public HelloController() {

    }

    @FXML
    private void initialize() {

    }
}