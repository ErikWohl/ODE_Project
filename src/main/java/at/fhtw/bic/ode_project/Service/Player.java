package at.fhtw.bic.ode_project.Service;

import java.util.Objects;

// Wird rein für die Ausgabe benutzt
public class Player {
    private String UUID;
    private String username;
    private int points = 0;

    public Player(String UUID, String username) {
        this.UUID = UUID;
        this.username = username;
    }

    public Player(String UUID, String username, int points) {
        this.UUID = UUID;
        this.username = username;
        this.points = points;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public String getUUID() {
        return UUID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    @Override
    public String toString() {
        return username + ": " + points;
    }

    @Override
    public int hashCode() {
        return Objects.hash(UUID, username);
    }

    @Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof Player)) {
            return false;
        }
        Player player = (Player) o;
        return Objects.equals(UUID, player.UUID) && Objects.equals(username, player.username);
    }
}
