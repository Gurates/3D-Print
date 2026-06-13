package printer;

import javafx.application.Application;

public class App {
    public static void main(String[] args) {
        System.out.println("Farm Monitor");
        DatabaseManager.initDatabase();
        Application.launch(FarmUI.class, args);
    }
}