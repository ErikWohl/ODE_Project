package at.fhtw.bic.ode_project.Exceptions;

public class NumberOfRetriesExceededException extends Exception {
    public NumberOfRetriesExceededException () {
        super("Maximum number of retries exceeded.");
    }
}
