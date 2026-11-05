module com.example.demo {
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.media;

    opens com.example.demo to javafx.fxml;
    exports com.example.demo;
}