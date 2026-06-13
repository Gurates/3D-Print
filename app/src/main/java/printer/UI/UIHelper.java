package printer.UI;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class UIHelper {
    public static VBox card() {
        VBox c = new VBox();
        c.setPadding(new Insets(16));
        c.setStyle("-fx-background-color:" + Theme.C_CARD + "; -fx-border-color:" + Theme.C_BORDER
                + "; -fx-border-width:1; -fx-border-radius:8; -fx-background-radius:8;");
        return c;
    }

    public static Label sectionLabel(String t) {
        Label l = new Label(t);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        l.setTextFill(Color.web(Theme.C_DIM));
        return l;
    }

    public static Button topBtn(String text) {
        Button b = new Button(text);
        String base = "-fx-font-family:'Segoe UI'; -fx-font-size:12px; -fx-font-weight:bold;"
                + "-fx-padding:7 14 7 14; -fx-background-radius:5; -fx-border-radius:5; -fx-border-width:1;";
        b.setStyle(base + "-fx-background-color:" + Theme.C_CARD + "; -fx-text-fill:" + Theme.C_TEXT + "; -fx-border-color:" + Theme.C_BORDER + ";");
        b.setOnMouseEntered(e -> b.setStyle(base + "-fx-background-color:" + Theme.C_SEL + "; -fx-text-fill:" + Theme.C_BRIGHT + "; -fx-border-color:" + Theme.C_BLUE + "; -fx-cursor:hand;"));
        b.setOnMouseExited(e  -> b.setStyle(base + "-fx-background-color:" + Theme.C_CARD + "; -fx-text-fill:" + Theme.C_TEXT + "; -fx-border-color:" + Theme.C_BORDER + ";"));
        return b;
    }

    public static Button smallBtn(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg + ";"
                + "-fx-font-family:'Segoe UI'; -fx-font-size:10px; -fx-font-weight:bold;"
                + "-fx-padding:5 10 5 10; -fx-background-radius:4; -fx-cursor:hand;");
        return b;
    }

    public static Label colHead(String t, double w) {
        Label l = new Label(t); l.setPrefWidth(w);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        l.setTextFill(Color.web(Theme.C_DIM)); return l;
    }

    public static Label numCell(String v, String color, double w) {
        Label l = new Label(v); l.setPrefWidth(w);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        l.setTextFill(Color.web(color)); return l;
    }

    public static Label emptyNote(String t) {
        Label l = new Label(t); l.setFont(Font.font("Segoe UI", 13));
        l.setTextFill(Color.web(Theme.C_DIM)); l.setPadding(new Insets(20)); return l;
    }
}