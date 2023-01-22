package at.fhtw.bic.ode_project.Controller;

import at.fhtw.bic.ode_project.Enums.CommandEnum;
import at.fhtw.bic.ode_project.Service.ClientObserver;
import at.fhtw.bic.ode_project.Service.GameObserver;
import at.fhtw.bic.ode_project.Service.GameService;
import at.fhtw.bic.ode_project.Service.TcpService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
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
public class HelloController implements ClientObserver, GameObserver {

    //todo: @ewohlrab: https://stackoverflow.com/questions/36088733/smooth-path-in-javafx
    // Smoother pathing with cubicCurve
    private Logger logger = LogManager.getLogger(HelloController.class);

    //###################### FXML Variablen #############################
    @FXML
    private TextArea textOutput;
    @FXML
    private TextField textInput;
    private Stage stage;
    private Scene scene;
    @FXML
    private Canvas canvas;
    private GraphicsContext graphicsContext;
    @FXML
    private ColorPicker colorPicker;
    @FXML
    private HBox buttonBar;
    @FXML
    private Label outputWord;

    //###################### Tcp Variablen #############################
    private TcpService client;
    private ExecutorService executor;
    //###################### Event Service Variablen #############################
    private GameService gameService;

    //###################### Drawing Variablen #############################
    private boolean outOfBound = false;
    private double currentPathXPosition = 0;
    private double currentPathYPosition = 0;
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    //###################### EventHandler Methods #############################

    private EventHandler<WindowEvent> window_close = new EventHandler<WindowEvent>() {
        @Override
        public void handle(WindowEvent event) {
            Platform.exit();
            System.exit(0);
        }
    };
    private EventHandler<MouseEvent> mouse_pressed = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent mouseEvent) {
            if(outOfBound) {
                return;
            }
            graphicsContext.beginPath();
            currentPathXPosition = mouseEvent.getX();
            currentPathYPosition = mouseEvent.getY();
            logger.trace("mouse was pressed.");
        }
    };
    private EventHandler<MouseEvent> mouse_dragged = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent mouseEvent) {
            if(outOfBound) {
                return;
            }

            //logger.debug("Initial value: " + graphicsContext.getStroke().toString());
            //logger.debug("Initial value: " + transformColorToCSharpHex(graphicsContext.getStroke().toString()));
            //logger.debug("Initial value: " + transformColorToJavaHex(transformColorToCSharpHex(graphicsContext.getStroke().toString())));

            String message = String.join(";",
                    String.valueOf((int)currentPathXPosition),
                    String.valueOf((int)currentPathYPosition),
                    String.valueOf((int)mouseEvent.getX()),
                    String.valueOf((int)mouseEvent.getY()),
                    String.valueOf((int)canvas.getGraphicsContext2D().getLineWidth()),
                    transformColorToCSharpHex(graphicsContext.getStroke().toString())
            );

            currentPathXPosition = mouseEvent.getX();
            currentPathYPosition = mouseEvent.getY();


            graphicsContext.lineTo(mouseEvent.getX(), mouseEvent.getY());
            graphicsContext.stroke();
            graphicsContext.moveTo(mouseEvent.getX(), mouseEvent.getY());
            if(client.isConnected()) {
                client.sendCommand(CommandEnum.DRAWING, message);
            }
            logger.trace("mouse was dragged: " + mouseEvent.getX() + "|" + mouseEvent.getY());
        }
    };
    private EventHandler<MouseEvent> mouse_release = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent mouseEvent) {
            if(outOfBound) {
                return;
            }
            graphicsContext.closePath();
            logger.trace("mouse was released.");
        }
    };
    private EventHandler<MouseEvent> mouse_exit = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent mouseEvent) {
            graphicsContext.closePath();
            outOfBound = true;
            logger.trace("mouse is out of bound: " +  mouseEvent.getTarget().toString());
        }
    };
    private EventHandler<MouseEvent> mouse_enter = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent mouseEvent) {
            graphicsContext.beginPath();
            outOfBound = false;
            logger.trace("mouse is in bound: " +  mouseEvent.getTarget().toString());
        }
    };
    private EventHandler<ActionEvent> color_set = new EventHandler<ActionEvent>() {
        public void handle(ActionEvent e)
        {
            // color
            Color c = colorPicker.getValue();
            logger.debug("Stroke color was set to " + c.toString());
            graphicsContext.setStroke(c);
        }
    };

    //###################### ButtonClick Methods #############################
    @FXML
    protected void onGraphicClearButtonClick() {
        var gc = canvas.getGraphicsContext2D();
        gc.clearRect(0,0, canvas.getWidth(), canvas.getHeight());
        logger.debug("onGraphicClearButtonClick was clicked!");
        if(client.isConnected()) {
            client.sendCommand(CommandEnum.CLEAR);
        }
    }
    @FXML
    protected void onTextClearButtonClick() {
        textOutput.clear();
        logger.debug("onTextClearButtonClick was clicked!");
    }
    @FXML
    protected void onStrokeIncreaseButtonClick() {
        double length = canvas.getGraphicsContext2D().getLineWidth();
        if(length <= 90) {
            length += 10;
            canvas.getGraphicsContext2D().setLineWidth(length);
        }
        logger.debug("Stroke width: " + length);
    }
    @FXML
    protected void onStrokeDecreaseButtonClick() {
        double length = canvas.getGraphicsContext2D().getLineWidth();
        if(length >= 11) {
            length -= 10;
            canvas.getGraphicsContext2D().setLineWidth(length);
        }
        logger.debug("Stroke width: " + length);
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

        if (file != null) {
            logger.info("Filename: " + file.getName() + " Path:" + file.getAbsolutePath());
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

    //###################### Initialization Methods #############################

    public HelloController() {
        client = new TcpService("localhost", 8080);
        client.addClientObserver(this);
        executor = Executors.newSingleThreadExecutor();

        gameService = new GameService();
        gameService.setTcpService(client);
        client.addClientObserver(gameService);
        gameService.setGameObserver(this);
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

        scene = stage.getScene();

        stage.setOnCloseRequest(window_close);

        graphicsContext = canvas.getGraphicsContext2D();

        graphicsContext.setLineWidth(1.0);

        addMouseEvents();
        addColorPickerEvent();

        // Set the text enter action
        enableTextInput();
        enableButtonBar();

        stage.setTitle("Skribbl.io");
        stage.show();
    }
    public void enableTextInput() {
        textInput.setOnAction(e -> onEnterText());
        textInput.setEditable(true);
    }
    public void disableTextInput() {
        textInput.setOnAction(null);
        textInput.setEditable(false);
    }
    public void disableCanvas() {
        canvas.setDisable(true);
    }
    public void enableCanvas() {
        canvas.setDisable(false);
    }
    public void enableButtonBar() {
        buttonBar.setDisable(false);
    }
    public void disableButtonBar() {
        buttonBar.setDisable(true);
    }
    public void addMouseEvents() {
        canvas.addEventFilter(MouseEvent.MOUSE_PRESSED, mouse_pressed);

        canvas.addEventFilter(MouseEvent.MOUSE_DRAGGED, mouse_dragged);

        canvas.addEventFilter(MouseEvent.MOUSE_RELEASED, mouse_release);

        canvas.addEventFilter(MouseEvent.MOUSE_EXITED_TARGET, mouse_exit);

        canvas.addEventFilter(MouseEvent.MOUSE_ENTERED_TARGET, mouse_enter);
    }
    public void removeMouseEvents() {
        canvas.removeEventFilter(MouseEvent.MOUSE_PRESSED, mouse_pressed);

        canvas.removeEventFilter(MouseEvent.MOUSE_DRAGGED, mouse_dragged);

        canvas.removeEventFilter(MouseEvent.MOUSE_RELEASED, mouse_release);

        canvas.removeEventFilter(MouseEvent.MOUSE_EXITED_TARGET, mouse_exit);

        canvas.removeEventFilter(MouseEvent.MOUSE_ENTERED_TARGET, mouse_enter);
    }
    public void addColorPickerEvent() {
        colorPicker.setOnAction(color_set);
    }
    public void removeColorPickerEvent() {
        colorPicker.setOnAction(null);
    }

    //###################### Send Methods #############################
    @FXML
    protected void onEnterText() {
        //todo: Verarbeitung des geschriebenen
        // Commands etc.
        logger.info("Entered text: " + textInput.getText());

        if(!checkInputText(textInput.getText())) {
            addTextToOutput(textInput.getText());
            if(client.isConnected()) {
                client.sendCommand(CommandEnum.MESSAGE, textInput.getText());
            }
        }
        textInput.clear();
    }
    private boolean checkInputText(String txt) {
        //Commands starten mit -- und sollen nicht im Textfenster ausgegeben werden
        //Command Struktur: --command:var:var:var;
        if(!txt.startsWith("--") && !txt.endsWith(";"))
            return false;

        if(!txt.startsWith("--") || !txt.endsWith(";")) {
            addTextToOutput("Wrong commandstructure!");
            addTextToOutput("--command:values; or --command;");
            return true;
        }


        txt = txt.replace("-", "").replace(";", "");
        String[] commandsegments = txt.split(":");

        switch (commandsegments[0]) {
            case "sethost": {
                logger.info("Host set to: " + commandsegments[1] + ":" + commandsegments);
                client.setSocketAddress(new InetSocketAddress(commandsegments[1], Integer.parseInt(commandsegments[2])));
                break;
            }
            case "startclient": {
                if(client.isDisconnected()) {
                    executor.submit(() -> client.run());
                } else {
                    logger.info("Starting another client not possible, client is already running!");
                }
                break;
            }
            case "startgame": {
                if(client.isConnected() && gameService.isInitial()) {
                    gameService.startGame();
                }
                break;
            }
            case "beginround": {
                if(commandsegments.length < 2 || commandsegments[1].isEmpty() || !gameService.hasWord(commandsegments[1])) {
                    logger.info("No word or wrong word was chosen!");
                    break;
                }

                gameService.drawerAcknowledge(commandsegments[1]);
                break;
            }
            case "disableTest": {
                setGuesserMode();
                break;
            }
            case "enableTest": {
                resetMode();
                break;
            }
        }
        return true;
    }

    //###################### TCP Observer Receive Methods #############################
    @Override
    public void onMessageReceive(String message) {
        // Abarbeitung der commands Message, Drawing und Clear
        // Commands sind immer 3 Zeichen lang.
        String command = message.substring(0, 3);
        logger.debug("command: " + command);
        CommandEnum commandEnum = CommandEnum.fromString(command);
        switch (commandEnum) {
            case MESSAGE: {
                addTextToOutput(message.substring(3));
                break;
            }
            case DRAWING: {
                String[] points = message.substring(3).split(";");
                // Da WPF keine double Koordinaten besitzt machen wir int
                int x1,x2,y1,y2, size;
                x1 = Integer.parseInt(points[0]);
                y1 = Integer.parseInt(points[1]);
                x2 = Integer.parseInt(points[2]);
                y2 = Integer.parseInt(points[3]);
                size = Integer.parseInt(points[4]);
                Color color = Color.valueOf(transformColorToJavaHex(points[5]));
                drawLine(x1,y1,x2,y2, size, color);

                break;
            }
            case CLEAR: {
                var gc = canvas.getGraphicsContext2D();
                gc.clearRect(0,0, canvas.getWidth(), canvas.getHeight());
                logger.debug("Graphic was cleared.");
            }
        }
    }
    @Override
    public void onDebugMessage(String message) {
        //todo Abarbeitung von debug Messages
        addTextToOutput(message);
    }

    //###################### Text Methods #############################
    public void addTextToOutput(String message) {
        textOutput.setText(textOutput.getText() + "\n" + message);
    }

    //###################### Transformation Methods #############################

    public void drawLine(int x1, int y1, int x2, int y2, int size, Color color) {
        var gc = canvas.getGraphicsContext2D();
        // Current values, will be set again after the line was drawn
        Color c = (Color) gc.getStroke();
        int s = (int)canvas.getGraphicsContext2D().getLineWidth();

        gc.setStroke(color);
        canvas.getGraphicsContext2D().setLineWidth(size);

        gc.strokeLine(x1, y1,x2,y2);


        gc.setStroke(c);
        canvas.getGraphicsContext2D().setLineWidth(s);
    }

    public String transformColorToCSharpHex(String color) {
        return "#" + color.substring(8) + color.substring(2,8);
    }

    public String transformColorToJavaHex(String color) {
        return "0x" + color.substring(3,9) + color.substring(1,3);
    }

    //###################### Game Observer Methods #############################
    @Override
    public void setGuesserMode() {
        logger.debug("Setting guesser mode");
        disableButtonBar();
        disableCanvas();
    }

    @Override
    public void setDrawerMode() {
        logger.debug("Setting drawer mode");
        disableTextInput();
        enableButtonBar();
        enableCanvas();
    }

    @Override
    public void resetMode() {
        logger.debug("Resetting mode.");
        enableButtonBar();
        enableTextInput();
        enableCanvas();
    }

    @Override
    public void outputWords(String words) {
        addTextToOutput(words);
    }

    @Override
    public void setDisplayWord(String word) {
        Platform.runLater(() -> outputWord.setText(word));
    }
}