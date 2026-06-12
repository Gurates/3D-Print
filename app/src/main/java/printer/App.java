package printer;

import javafx.application.Application;

public class App {
    public static void main(String[] args) {
        System.out.println("=== 3D FARM JAVAFX SISTEMI BASLATIYOR ===");
        
        DatabaseManager.initDatabase();  
        Application.launch(FarmUI.class, args);
    }
}