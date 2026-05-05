module org.seira {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;

    opens org.seira to javafx.fxml;
    exports org.seira;
    exports org.seira.controller;
    opens org.seira.controller to javafx.fxml;
}
