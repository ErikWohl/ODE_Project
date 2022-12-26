package at.fhtw.bic.ode_project;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Skribbl.io");
        stage.setScene(scene);
        stage.show();

        initUI(stage);
    }
    //todo: https://docs.oracle.com/javafx/2/events/filters.htm
    // https://stackoverflow.com/questions/46649406/custom-javafx-events
    // https://docs.oracle.com/javafx/2/events/processing.htm
    public void initUI(Stage stage) {
        var scene = stage.getScene();
        var canvas = (Canvas) scene.lookup("#canvas");
        var gc = canvas.getGraphicsContext2D();
        var textOutput = (TextArea) scene.lookup("#textOutput");

        gc.setLineWidth(1.0);

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if(mouseEvent.getTarget() instanceof Canvas) {
                    gc.beginPath();
                    String cd = mouseEvent.getX() + " " + mouseEvent.getY();
                    textOutput.setText(textOutput.getText() + "\n" + cd);
                    System.out.printf("mouse was pressed!\n%s\n", mouseEvent.getTarget());
                }
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if(mouseEvent.getTarget() instanceof Canvas) {
                    gc.lineTo(mouseEvent.getX(), mouseEvent.getY());
                    gc.stroke();
                    gc.moveTo(mouseEvent.getX(), mouseEvent.getY());
                    String cd = mouseEvent.getX() + " " + mouseEvent.getY();
                    textOutput.setText(textOutput.getText() + "\n" + cd);
                    System.out.printf("mouse was pressed!\n%s\n", mouseEvent.getTarget());
                }
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if(mouseEvent.getTarget() instanceof Canvas) {
                    gc.closePath();
                    String cd = mouseEvent.getX() + " " + mouseEvent.getY();
                    textOutput.setText(textOutput.getText() + "\n" + cd);
                    System.out.printf("mouse was pressed!\n%s\n", mouseEvent.getTarget());
                }
            }
        });


        //Colorpicker
        var colorPicker = (ColorPicker) scene.lookup("#colorPicker");

        // create an event handler
        EventHandler<ActionEvent> event = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e)
            {
                // color
                Color c = colorPicker.getValue();
                gc.setStroke(c);
            }
        };

        // set listener
        colorPicker.setOnAction(event);

        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args) {
        launch();
    }
}