package at.fhtw.bic.ode_project.Service;

import at.fhtw.bic.ode_project.Enums.CommandEnum;
import at.fhtw.bic.ode_project.Enums.GameStateEnum;
import at.fhtw.bic.ode_project.Enums.PlayerStateEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

public class GameService implements ClientObserver {
    private Logger logger = LogManager.getLogger(GameService.class);
    private TcpService tcpService;
    private GameObserver gameObserver;
    private GameStatusObserver statusObserver;
    private GameStateEnum gameState = GameStateEnum.INITIAL;
    private PlayerStateEnum playerState = PlayerStateEnum.NONE;

    private String username;

    private boolean reset_occured = false;
    private List<String> words;
    private String chosenWord = "";
    public void setTcpService(TcpService tcpService) {
        this.tcpService = tcpService;
    }
    public void setGameObserver(GameObserver gameObserver) {
        this.gameObserver = gameObserver;
    }
    public void setStatusObserver(GameStatusObserver statusObserver) {
        this.statusObserver = statusObserver;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isInitial() {
        return gameState == GameStateEnum.INITIAL;
    }
    public boolean isStarting() {
        return gameState == GameStateEnum.STARTING;
    }
    public boolean isDrawer() {
        return playerState == PlayerStateEnum.DRAWER;
    }
    public boolean isGuesser() {
        return playerState == PlayerStateEnum.GUESSER;
    }
    public boolean hasWord(String word) {
        if(words == null) {
            return false;
        }
        if(words.contains(word)) {
            return true;
        }
        return false;
    }
    public void startGame() {
        if(tcpService.isConnected()) {
            logger.info("Trying to start a game. Sending a start game request.");
            tcpService.sendCommand(CommandEnum.START_GAME_REQUEST);
        }
    }

    public void drawerAcknowledge(String word) {
        if(!tcpService.isConnected() || !isDrawer() || !isStarting()) {
            logger.info("Current status isConnected: " + tcpService.isConnected()+ " isDrawer: " + isDrawer() + " isStarting: " + isStarting());
            return;
        }
        logger.info("Drawer chose word and send acknowledgement.");
        tcpService.sendCommand(CommandEnum.DRAWER_ACKNOWLEDGEMENT, word);
    }
    public void reset() {
        logger.error("Resetting to initial mode.");
        gameState = GameStateEnum.INITIAL;
        statusObserver.onGameStatusChange(gameState);
        playerState = PlayerStateEnum.NONE;
        statusObserver.onPlayerStatusChange(playerState);
        words = null;
        chosenWord = "";
        gameObserver.resetMode();
    }

    @Override
    public void onMessageReceive(String message) {
        // Abarbeitung der commands Start game request, start game acknowledgement
        // Commands sind immer 3 Zeichen lang.
        reset_occured = false;
        String command = message.substring(0, 3);
        logger.debug("command: " + command);
        CommandEnum commandEnum = CommandEnum.fromString(command);
        switch (commandEnum) {
            case MESSAGE: {
                gameObserver.outputWords(message.substring(3));
                break;
            }
            case DRAWING: {
                gameObserver.transformMessageToLine(message);
                break;
            }
            case CLEAR: {
                gameObserver.clearCanvas();
                break;
            }

            // Erster State, falls noch nichts gestartet ist
            // wird hier überprüft, ob jemand eine falsche state hat
            case START_GAME_REQUEST: {
                logger.info("Start request for the game ...");
                if(gameState != GameStateEnum.INITIAL) {
                    logger.info("Client is in another game state other than initial!");
                    logger.info("Send start request not acknowledged.");
                    tcpService.sendCommand(CommandEnum.START_GAME_NOTACKNOWLEDGEMENT);
                    return;
                }
                logger.info("Send start request acknowledged.");
                tcpService.sendCommand(CommandEnum.START_GAME_ACKNOWLEDGEMENT);

                gameState = GameStateEnum.STARTING;
                statusObserver.onGameStatusChange(gameState);
                break;
            }
            // Alle Clients haben Acknowledged, somit hat der Server
            // ausgewählt wer Guesser und Drawer ist
            // GUESSER warten einfach auf den Rundenstart oder einen Abbruch
            case GUESSER_REQUEST: {
                logger.info("Trying to set client to guesser mode");
                logger.debug("Game state: " + gameState + " Player state: " + playerState);

                if(gameState != GameStateEnum.STARTING || playerState != PlayerStateEnum.NONE) {
                    logger.info("Client is in another game or player state!");
                    return;
                }

                playerState = PlayerStateEnum.GUESSER;
                logger.info("Set client to guesser mode");
                statusObserver.onPlayerStatusChange(playerState);
                gameObserver.setGuesserMode();
                break;
            }
            // Drawer bekommen drei Wörter zugeschickt
            // Er muss einer der drei Wörter wählen
            case DRAWER_REQUEST: {
                logger.info("Trying to set client to drawer mode");
                logger.debug("Game state: " + gameState + " Player state: " + playerState);

                if(gameState != GameStateEnum.STARTING || playerState != PlayerStateEnum.NONE) {
                    logger.info("Client is in another game or player state!");
                    return;
                }

                logger.debug("Received words: " + message.substring(3));
                String[] split = message.substring(3).split(";");
                words = Arrays.stream(split).toList();

                playerState = PlayerStateEnum.DRAWER;
                statusObserver.onPlayerStatusChange(playerState);

                gameObserver.outputWords("You are the drawer choose a word: " + message.substring(3));
                gameObserver.setChoosableWords(words.get(0), words.get(1), words.get(2));

                logger.info("Set client to guesser mode for word choosing");
                gameObserver.setGuesserMode();
                break;
            }
            // Sobald der Drawer ein Wort ausgewählt hat schickt er ein Acknowledgement
            // Daraufhin requested der Server den Rundenstart
            case ROUND_START_REQUEST: {
                logger.info("Request to start the round ");
                logger.debug("Game state: " + gameState + " Player state: " + playerState);
                if(!isStarting() || !(isDrawer() || isGuesser())) {
                    logger.info("Client is in another game or player state!");
                    tcpService.sendCommand(CommandEnum.ROUND_START_NOTACKNOWLEDGEMENT);
                    return;
                }
                logger.info("All seems ready to rumble!");
                gameState = GameStateEnum.STARTED;
                statusObserver.onGameStatusChange(gameState);
                tcpService.sendCommand(CommandEnum.ROUND_START_ACKNOWLEDGEMENT);
                break;
            }
            // Alle haben den Rundenstart Acknowledged, somit erhalten sie das "Wort"
            // Guesser erhalten Unterstriche("_") als Wort, während der Drawer das eigentliche
            // Wort erhält
            case ROUND_STARTED: {
                logger.info("Round start was sent.");
                logger.debug("Game state: " + gameState + " Player state: " + playerState);

                if(gameState != GameStateEnum.STARTED) {
                    logger.error("Client is in another game!");
                    return;
                }

                if(isDrawer()) {
                    gameObserver.setDrawerMode();
                }

                chosenWord = message.substring(3);
                gameObserver.setDisplayWord(chosenWord);
                break;
            }
            case ERROR: {
                logger.error("An error has occured.");
                logger.debug("Game state: " + gameState + " Player state: " + playerState);
                reset();
            }
        }
    }

    @Override
    public void onDebugMessage(String message) {
        // Debug message wird nur aufgerufen, wenn es zu TCP Serverproblemen kommt
        // Wir setzen hier einfach preventiv den GameService und HelloController zurück
        gameObserver.outputWords(message);
        if(reset_occured) {
            reset_occured = true;
            reset();
        }
    }
}
