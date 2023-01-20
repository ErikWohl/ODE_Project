package at.fhtw.bic.ode_project.Controller;

public enum CommandEnum {
    MESSAGE("MSG"),
    DRAWING("DRW"),
    CLEAR("CLR");

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
