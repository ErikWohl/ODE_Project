package at.fhtw.bic.ode_project.Controller;

import at.fhtw.bic.ode_project.Service.ClientObserver;
import at.fhtw.bic.ode_project.Service.TcpService;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloController implements ClientObserver {
    @FXML
    private Canvas canvas;
    @FXML
    private TextArea textOutput;
    @FXML
    private TextField textInput;

    private Scene scene;
    private TcpService client;

    public void setScene(Scene scene) {
        this.scene = scene;
    }
    @FXML
    protected void onGraphicClearButtonClick() {
        var gc = canvas.getGraphicsContext2D();
        gc.clearRect(0,0, canvas.getWidth(), canvas.getHeight());
        System.out.printf("Button was clicked!\n");
    }

    @FXML
    protected void onTextClearButtonClick() {
        textOutput.clear();
        System.out.printf("Button was clicked!\n");
    }

    @FXML
    protected void onStrokeIncreaseButtonClick() {
        canvas.getGraphicsContext2D().setLineWidth(10);
        System.out.printf("Stroke width is 10!\n");
    }

    @FXML
    protected void onStrokeDecreaseButtonClick() {
        canvas.getGraphicsContext2D().setLineWidth(1);
        System.out.printf("Stroke width is 1!\n");
    }

    @FXML
    protected void onEnterText() {
        //todo: Verarbeitung des geschriebenen
        // Commands etc.


        if(!checkInputText(textInput.getText())) {
            textOutput.setText(textOutput.getText() + "\n" + textInput.getText());
            if(client.isStarted()) {
                client.sendMessage(textInput.getText());
            }
        }
        textInput.clear();
    }

    private boolean checkInputText(String txt) {
        //Commands starten mit -- und sollen nicht im Textfenster ausgegeben werden
        //Command Struktur: --command:var:var:var;
        if(!txt.startsWith("--") && !txt.endsWith(";"))
            return false;
        txt = txt.replace("-", "").replace(";", "");
        String[] commandsegments = txt.split(":");

        switch (commandsegments[0]) {
            case "sethost": {
                client.setSocketAddress(new InetSocketAddress(commandsegments[1], Integer.parseInt(commandsegments[2])));
                break;
            }
            case "startclient": {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> client.run());
            }
        }

        return true;
    }

    public HelloController() {
        client = new TcpService("localhost", 8080);
        client.setClientObserver(this);
    }

    @FXML
    private void initialize() {
    }

    public void initUI(Stage stage) {
        var scene = stage.getScene();

        addMouseEvents(scene);
        addColorPickerEvent(scene);

        stage.setScene(scene);
        stage.show();
    }
    public void addMouseEvents(Scene scene) {
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
    }
    public void addColorPickerEvent(Scene scene) {
        var canvas = (Canvas) scene.lookup("#canvas");
        var gc = canvas.getGraphicsContext2D();
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
    }

    @Override
    public void onMessageReceive(String message) {
        //todo Abarbeitung von erhaltenen Messages

        textOutput.setText(textOutput.getText() + "\n" + message);
    }
}