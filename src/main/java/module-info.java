module com.aimtrainer {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.media;
    requires java.sql;
    
   

    // Optional, aber oft nötig für den SQL-Treiber im Module-Path:
    requires org.xerial.sqlitejdbc;


  

  

    opens com.aimtrainer to javafx.fxml;
    exports com.aimtrainer;

}
