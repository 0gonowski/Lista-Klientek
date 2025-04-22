module com.example.listaklientek {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.listaklientek to javafx.fxml;
    exports com.example.listaklientek;
}