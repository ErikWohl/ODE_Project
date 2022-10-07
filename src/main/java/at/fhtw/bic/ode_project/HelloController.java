package at.fhtw.bic.ode_project;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;

public class HelloController {
    @FXML
    private Canvas canvas1;

    @FXML
    private Button buttonTest;

    @FXML
    protected void onTestClick() {
        var gc = canvas1.getGraphicsContext2D();
        gc.clearRect(0,0, canvas1.getWidth(), canvas1.getHeight());
        System.out.printf("Button was clicked!\n");
    }

    public HelloController() {

    }

    @FXML
    private void initialize() {

    }
}