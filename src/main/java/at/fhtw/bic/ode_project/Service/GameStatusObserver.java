package at.fhtw.bic.ode_project.Service;

import at.fhtw.bic.ode_project.Enums.GameStateEnum;
import at.fhtw.bic.ode_project.Enums.PlayerStateEnum;

public interface GameStatusObserver {
    public void onPlayerStatusChange(PlayerStateEnum status);
    public void onGameStatusChange(GameStateEnum status);

}
