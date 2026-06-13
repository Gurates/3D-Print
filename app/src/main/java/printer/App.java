package printer;

import javafx.application.Application;
import printer.UI.FarmUI;

public class App {
    public static void main(String[] args) {
        System.out.println("Farm Monitor");
        DatabaseManager.initDatabase();
        Application.launch(FarmUI.class, args);
    }
}