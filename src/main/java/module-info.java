module org.seira {
    requires javafx.controls;
    requires javafx.fxml;

//    requires org.controlsfx.controls;
//    requires com.dlsc.formsfx;
//    requires org.kordamp.bootstrapfx.core;
    requires java.sql;
    requires jbcrypt;
    requires com.opencsv;

    opens com.seira to javafx.fxml;
    exports com.seira;
    exports com.seira.controllers;
    opens com.seira.controllers to javafx.fxml;
}
