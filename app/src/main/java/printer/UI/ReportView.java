package printer.UI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import printer.DatabaseManager;
import printer.PrinterState;
import printer.DatabaseManager.PrinterSummary;

import java.util.List;

public class ReportView extends BorderPane {

    private Label reportTitleLabel;
    private ScrollPane scroll;

    public ReportView() {
        setStyle("-fx-background-color:" + Theme.C_BG + ";");
        HBox subBar = new HBox(12);
        subBar.setAlignment(Pos.CENTER_LEFT);
        subBar.setPadding(new Insets(12, 28, 12, 28));
        subBar.setStyle("-fx-background-color:" + Theme.C_SURFACE + "; -fx-border-color:" + Theme.C_BORDER + "; -fx-border-width:0 0 1 0;");
        reportTitleLabel = new Label("REPORTS");
        reportTitleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        reportTitleLabel.setTextFill(Color.web(Theme.C_BRIGHT));
        subBar.getChildren().add(reportTitleLabel);
        setTop(subBar);

        scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:" + Theme.C_BG + "; -fx-background:" + Theme.C_BG + ";");
        setCenter(scroll);
    }

    public void refresh(PrinterState filter) {
        VBox content = new VBox(28);
        content.setPadding(new Insets(32, 36, 40, 36));
        String pName = filter != null ? filter.displayName : null;

        reportTitleLabel.setText(filter == null ? "FARM GENERAL REPORT" : filter.displayName.toUpperCase() + " — REPORT");

        content.getChildren().add(reportSection("PRINTER PERFORMANCE SUMMARY"));
        List<DatabaseManager.PrinterSummary> sums = pName == null ? DatabaseManager.getPrinterSummaries() : asList(DatabaseManager.getSinglePrinterSummary(pName));
        
        if (sums.isEmpty()) {
            content.getChildren().add(UIHelper.emptyNote("No completed print records yet."));
        } else {
            FlowPane summGrid = new FlowPane();
            summGrid.setHgap(16); summGrid.setVgap(16);
            for (DatabaseManager.PrinterSummary s : sums) if (s != null) summGrid.getChildren().add(buildReportCard(s));
            content.getChildren().add(summGrid);
        }

        content.getChildren().add(reportSection("RECENT OPERATIONS (Last 30)"));
        List<String[]> recent = DatabaseManager.getRecentJobs(30, pName);
        if (recent.isEmpty()) content.getChildren().add(UIHelper.emptyNote("No records."));
        else content.getChildren().add(buildRecentTable(recent));

        scroll.setContent(content);
    }

    private VBox buildReportCard(DatabaseManager.PrinterSummary s) {
        VBox card = new VBox(12); card.setPrefWidth(300); card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color:" + Theme.C_CARD + "; -fx-border-color:" + Theme.C_BORDER + "; -fx-border-width:1; -fx-border-radius:8; -fx-background-radius:8;");

        Label name = new Label(s.printerName); name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14)); name.setTextFill(Color.web(Theme.C_BRIGHT));
        double rate = s.totalJobs > 0 ? (s.successJobs * 100.0 / s.totalJobs) : 0;
        String rc = rate >= 80 ? Theme.C_GREEN : rate >= 50 ? Theme.C_AMBER : Theme.C_RED;

        ProgressBar bar = new ProgressBar(rate / 100.0); bar.setMaxWidth(Double.MAX_VALUE); bar.setPrefHeight(5); bar.setStyle("-fx-accent:" + rc + ";");
        HBox rateRow = new HBox(); Label rL = new Label(String.format("Success: %.0f%%", rate)); rL.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12)); rL.setTextFill(Color.web(rc));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label tL = new Label(s.totalJobs + " prints"); tL.setFont(Font.font("Segoe UI", 12)); tL.setTextFill(Color.web(Theme.C_DIM));
        rateRow.getChildren().addAll(rL, sp, tL);

        VBox stats = new VBox(7);
        stats.getChildren().addAll(statLine("✓  Successful", String.valueOf(s.successJobs), Theme.C_GREEN), statLine("✗  Failed", String.valueOf(s.failJobs), Theme.C_RED));

        card.getChildren().addAll(name, rateRow, bar, stats);
        return card;
    }

    private VBox buildRecentTable(List<String[]> rows) {
        VBox table = new VBox(0); table.setStyle("-fx-background-color:" + Theme.C_CARD + "; -fx-border-color:" + Theme.C_BORDER + "; -fx-border-width:1; -fx-border-radius:8; -fx-background-radius:8;");
        double[] w = {150, 200, 80, 90, 80, 80, 80, 160};
        String[] heads = {"Printer", "Model", "Status", "Duration", "Filament", "Cost", "Price", "Date"};
        HBox header = new HBox(0); header.setPadding(new Insets(9, 16, 9, 16)); header.setStyle("-fx-background-color:" + Theme.C_SURFACE + "; -fx-border-color:" + Theme.C_BORDER + "; -fx-border-width:0 0 1 0;");
        for (int i = 0; i < heads.length; i++) header.getChildren().add(UIHelper.colHead(heads[i], w[i]));
        table.getChildren().add(header);

        for (int i = 0; i < rows.size(); i++) {
            HBox row = new HBox(0); row.setPadding(new Insets(9, 16, 9, 16)); row.setAlignment(Pos.CENTER_LEFT);
            if (i % 2 == 1) row.setStyle("-fx-background-color:" + Theme.C_SURFACE + ";");
            for (int j = 0; j < Math.min(rows.get(i).length, w.length); j++) {
                Label l = new Label(rows.get(i)[j] != null ? rows.get(i)[j] : "—"); l.setPrefWidth(w[j]); l.setFont(Font.font("Segoe UI", 12)); l.setTextFill(Color.web(Theme.C_TEXT));
                row.getChildren().add(l);
            }
            table.getChildren().add(row);
        }
        return table;
    }

    private Label reportSection(String t) { Label l = new Label(t); l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12)); l.setTextFill(Color.web(Theme.C_DIM)); l.setPadding(new Insets(0,0,8,0)); return l; }
    private HBox statLine(String lbl, String val, String color) { HBox r = new HBox(); r.setAlignment(Pos.CENTER_LEFT); Label l = new Label(lbl); l.setPrefWidth(160); l.setTextFill(Color.web(Theme.C_DIM)); Label v = new Label(val); v.setTextFill(Color.web(color)); r.getChildren().addAll(l, v); return r; }
    private <T> List<T> asList(T item) { List<T> l = new java.util.ArrayList<>(); if (item != null) l.add(item); return l; }
}