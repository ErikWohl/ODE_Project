package at.fhtw.bic.ode_project.Service;

import at.fhtw.bic.ode_project.Enums.CommandEnum;
import at.fhtw.bic.ode_project.Enums.GameStateEnum;
import at.fhtw.bic.ode_project.Enums.PlayerStateEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class GameService implements ClientObserver{
    private Logger logger = LogManager.getLogger(GameService.class);

    //###################### TCP Variablen #############################
    private TcpService tcpService;

    //###################### Observer Variablen #############################
    private GameObserver gameObserver;
    private GameStatusObserver statusObserver;

    //###################### Status Variablen #############################
    private GameStateEnum gameState = GameStateEnum.INITIAL;
    private PlayerStateEnum playerState = PlayerStateEnum.NONE;

    //###################### Initialization Methods #############################
    private String username;
    private HashSet<Player> playerList;
    private int round_max = 1;
    private int current_round = 1;

    private boolean reset_occured = false;
    private List<String> words;
    private String chosenWord = "";

    //###################### Initialization Methods #############################
    public GameService(){
        playerList = new HashSet<>();
    }

    //###################### Setter Methods #############################
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

    //###################### Boolean Methods #############################

    public boolean isInitial() {
        return gameState == GameStateEnum.INITIAL;
    }
    public boolean isStarting() {
        return gameState == GameStateEnum.STARTING;
    }
    public boolean isFinished() {
        return gameState == GameStateEnum.FINISHED;
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

    //###################### Gameloop Methods #############################
    public void startGame() {
        if(tcpService.isConnected()) {
            logger.info("Trying to start a game. Sending a start game request.");
            tcpService.sendCommand(CommandEnum.INITIAL_GAME_REQUEST);
        } else {
            logger.info("Not connected to server! Cannot start game!");
            gameObserver.outputWords("Not connected to server! Cannot start game!");
        }
    }
    public void sendUsername() {
        if(tcpService.isConnected()) {
            logger.info("Sending username to server.");
            tcpService.sendCommand(CommandEnum.ADD_USER_REQUEST, username);
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
    public void softReset() {
        logger.error("Soft resetting to initial mode.");
        gameState = GameStateEnum.INITIAL;
        statusObserver.onGameStatusChange(gameState);
        playerState = PlayerStateEnum.NONE;
        statusObserver.onPlayerStatusChange(playerState);
        words = null;
        chosenWord = "";
        gameObserver.setDisplayWord("");
        gameObserver.resetMode();
    }
    public void hardReset() {
        logger.error("Hard resetting to initial mode.");
        softReset();
        playerList = new HashSet<>();
    }
    public void addOrUpdatePlayerList(String UUID, String username, int points) {
        Player player = new Player(UUID, username);
        for(var p : playerList) {
            if(p.equals(player)) {
                logger.debug("Updated player in list.");
                p.setPoints(points);
                return;
            }
        }
        logger.debug("Added player to list.");
        player.setPoints(points);
        playerList.add(player);
    }
    public void removePlayerFromList(String UUID, String username, int points) {
        Player player = new Player(UUID, username, points);
        if(!playerList.contains(player)) {
            logger.error("Player doesn't exist.");
            throw new IllegalArgumentException();
        }
        logger.debug("Removed player from list.");
        playerList.remove(player);
    }

    //###################### Gameloop #############################
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

            // Erster "Kontakt" von anderem User
            case USER_ADDED: {
                logger.info("A new player joined the server.");
                String split[] = message.substring(3).split(";");
                logger.debug("Player UUID: (" + split[0] + ") Username: " + split[1]);
                addOrUpdatePlayerList(split[0], split[1], 0);
                gameObserver.outputWords(split[1] + " joined the lobby!");
                break;
            }

            case USER_UPDATED: {
                logger.info("Server updated player.");
                String split[] = message.substring(3).split(";");
                logger.debug("Player UUID: (" + split[0] + ") Username: " + split[1] + " Points: " + split[2]);
                addOrUpdatePlayerList(split[0], split[1], Integer.parseInt(split[2]));
                gameObserver.updatePlayerOutput(playerList.stream().map(Player::toString).collect(Collectors.joining("\n")));
                break;
            }

            case USER_REMOVED: {
                logger.info("Server removed player.");
                String split[] = message.substring(3).split(";");
                logger.debug("Player UUID: (" + split[0] + ") Username: " + split[1] + " Points: " + split[2]);
                removePlayerFromList(split[0], split[1], Integer.parseInt(split[2]));
                gameObserver.updatePlayerOutput(playerList.stream().map(Player::toString).collect(Collectors.joining("\n")));
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
                String split[] = message.substring(3).split(";");
                round_max = Integer.parseInt(split[0]);
                current_round = Integer.parseInt(split[1]);
                gameState = GameStateEnum.STARTING;
                statusObserver.onGameStatusChange(gameState);
                gameObserver.updateRoundCounter(current_round + " / " + round_max);
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
                gameObserver.outputWords("You are a guesser. Try to guess the drawn word.");

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
                gameObserver.clearCanvas();
                gameObserver.clearText();
                break;
            }

            case CLOSE_GUESS: {
                gameObserver.outputWords(message.substring(3) + " is close!");
                break;
            }

            case CORRECT_GUESS: {
                logger.info("Word was guessed correctly.");
                logger.debug("Game state: " + gameState + " Player state: " + playerState);

                gameState = GameStateEnum.FINISHED;
                statusObserver.onGameStatusChange(gameState);

                gameObserver.setWinMode();
                break;
            }

            case ROUND_END_SUCCESS: {
                if((isGuesser() && isFinished()) || isDrawer()) {
                    playerState = PlayerStateEnum.NONE;
                    statusObserver.onPlayerStatusChange(playerState);

                    gameState = GameStateEnum.INITIAL;
                    statusObserver.onGameStatusChange(gameState);

                    gameObserver.clearCanvas();
                    gameObserver.clearText();

                    tcpService.sendCommand(commandEnum.ROUND_END_ACKNOWLEDGEMENT);
                }
                break;
            }
            case GAME_ENDED: {
                softReset();
                break;
            }
            case ERROR: {
                logger.error("An error has occured.");
                logger.debug("Game state: " + gameState + " Player state: " + playerState);
                softReset();
                break;
            }
        }
    }

    @Override
    public void onDebugMessage(String message) {
        // Debug message wird nur aufgerufen, wenn es zu TCP Serverproblemen kommt
        // Wir setzen hier einfach preventiv den GameService und HelloController zurück
        gameObserver.outputWords(message);
        if(!reset_occured) {
            reset_occured = true;
            hardReset();
        }
    }
}
