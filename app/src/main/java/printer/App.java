package printer;

import javafx.application.Application;

public class App {
    public static void main(String[] args) {
        System.out.println("=== 3D FARM MONITOR BAŞLATILIYOR ===");
        DatabaseManager.initDatabase();
        Application.launch(FarmUI.class, args);
    }
}