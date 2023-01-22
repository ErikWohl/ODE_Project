package at.fhtw.bic.ode_project.Service;

import at.fhtw.bic.ode_project.Enums.TcpStateEnum;

public interface ClientStatusObserver {
    public void onClientStatusChange(TcpStateEnum status);
}
