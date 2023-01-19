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
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import javafx.embed.swing.SwingFXUtils;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
public class HelloController implements ClientObserver {

    private Logger logger = LogManager.getLogger(HelloController.class);
    @FXML
    private Canvas canvas;
    @FXML
    private TextArea textOutput;
    @FXML
    private TextField textInput;
    private Scene scene;
    private Stage stage;
    private TcpService client;
    private boolean outOfBound = false;

    public void setScene(Scene scene) {
        this.scene = scene;
    }
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    @FXML
    protected void onGraphicClearButtonClick() {
        var gc = canvas.getGraphicsContext2D();
        gc.clearRect(0,0, canvas.getWidth(), canvas.getHeight());
        logger.debug("Button was clicked!");
    }

    @FXML
    protected void onTextClearButtonClick() {
        textOutput.clear();
        logger.debug("Button was clicked!");
    }

    @FXML
    protected void onStrokeIncreaseButtonClick() {
        canvas.getGraphicsContext2D().setLineWidth(10);
        logger.debug("Stroke width is 10!");
    }

    @FXML
    protected void onStrokeDecreaseButtonClick() {
        canvas.getGraphicsContext2D().setLineWidth(1);
        logger.debug("Stroke width is 1!");
    }

    @FXML
    protected void onSaveImageButtonClick() {
        logger.info("Trying to safe file.");
        FileChooser savefile = new FileChooser();
        savefile.setTitle("Save File");
        savefile.initialFileNameProperty().setValue("example.png");
        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("PNG files", "*PNG");
        savefile.getExtensionFilters().add(extensionFilter);
        File file = savefile.showSaveDialog(stage);
        logger.info("Filename: " + file.getName() + " Path:" + file.getAbsolutePath());

        if (file != null) {
            try {
                WritableImage writableImage = new WritableImage((int)canvas.getWidth(), (int)canvas.getHeight());
                canvas.snapshot(null, writableImage);
                RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);
                if(ImageIO.write(renderedImage, "png", file)) {
                    logger.info("Filesave was a success.");
                } else {
                    logger.info("Filesave failure.");
                }
            } catch (IOException e) {
                logger.error("Error while saving the picture.");
                e.printStackTrace();
            }
        }
    }

    @FXML
    protected void onEnterText() {
        //todo: Verarbeitung des geschriebenen
        // Commands etc.
        logger.info("Entered text: " + textInput.getText());

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
        logger.trace("Trace Message!");
        logger.debug("Debug Message!");
        logger.info("Info Message!");
        logger.warn("Warn Message!");
        logger.error("Error Message!");
        logger.fatal("Fatal Message!");

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
                if(outOfBound) {
                    return;
                }

                if(mouseEvent.getTarget() instanceof Canvas) {
                    gc.beginPath();
                    logger.trace("mouse was pressed.");
                }
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if(outOfBound) {
                    return;
                }
                if(mouseEvent.getTarget() instanceof Canvas) {
                    gc.lineTo(mouseEvent.getX(), mouseEvent.getY());
                    gc.stroke();
                    gc.moveTo(mouseEvent.getX(), mouseEvent.getY());
                    logger.trace("mouse was dragged: " + mouseEvent.getX() + "|" + mouseEvent.getY());
                }
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if(outOfBound) {
                    return;
                }

                if(mouseEvent.getTarget() instanceof Canvas) {
                    gc.closePath();
                    logger.trace("mouse was released.");
                }
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_EXITED_TARGET, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                gc.closePath();
                outOfBound = true;
                logger.trace("mouse is out of bound: " +  mouseEvent.getTarget().toString());
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_ENTERED_TARGET, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                gc.beginPath();
                outOfBound = false;
                logger.trace("mouse is in bound: " +  mouseEvent.getTarget().toString());
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
                logger.debug("Stroke color was set to " + c.toString());
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

    @Override
    public void onDebugMessage(String message) {
        //todo Abarbeitung von debug Messages

        textOutput.setText(textOutput.getText() + "\n" + message);
    }
}