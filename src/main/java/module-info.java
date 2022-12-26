module at.fhtw.bic.ode_project {
    requires javafx.controls;
    requires javafx.fxml;

    requires validatorfx;

    opens at.fhtw.bic.ode_project to javafx.fxml;
    exports at.fhtw.bic.ode_project;
    exports at.fhtw.bic.ode_project.Controller;
    opens at.fhtw.bic.ode_project.Controller to javafx.fxml;
}