package at.fhtw.bic.ode_project.Service;

public interface ClientObserver {
    public void onMessageReceive(String message);
    public void onDebugMessage(String message);
}
