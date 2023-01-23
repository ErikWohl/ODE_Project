package at.fhtw.bic.ode_project.Enums;

public enum CommandEnum {
    // Controller Commands
    MESSAGE("MSG"),
    DRAWING("DRW"),
    CLEAR("CLR"),

    // GameService Client Commands
    ADD_USER_REQUEST("ADR"),
    INITIAL_GAME_REQUEST("IGR"),
    START_GAME_ACKNOWLEDGEMENT("SGA"),
    START_GAME_NOTACKNOWLEDGEMENT("SGN"),
    DRAWER_ACKNOWLEDGEMENT("DWA"),
    ROUND_START_ACKNOWLEDGEMENT("RSA"),
    ROUND_START_NOTACKNOWLEDGEMENT("RSN"),
    ROUND_END_ACKNOWLEDGEMENT("REA"),

    // GameService Server REQUEST Commands
    START_GAME_REQUEST("SGR"),
    GUESSER_REQUEST("GSR"),
    DRAWER_REQUEST("DWR"),
    ROUND_START_REQUEST("RSR"),

    // GameService Server STATUS Commands
    USER_ADDED("USA"),
    USER_UPDATED("USU"),
    USER_REMOVED("USR"),
    ROUND_STARTED("RST"),
    CLOSE_GUESS("CLG"),
    CORRECT_GUESS("CRG"),
    ROUND_END_SUCCESS("RES"),
    ROUND_END_TIMEOUT("RET"),
    GAME_ENDED("GME"),
    ERROR("ERR");


    public final String command;
    private CommandEnum(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
    public static CommandEnum fromString(String text) {
        for(CommandEnum c : CommandEnum.values()) {
            if(c.command.equals(text)) {
                return c;
            }
        }
        throw new IllegalArgumentException();
    }
}
