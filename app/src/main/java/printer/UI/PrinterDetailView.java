package printer.UI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import printer.CostCalculator;
import printer.DatabaseManager;
import printer.PrinterState;
import printer.CostCalculator.CostBreakdown;

import java.util.List;

public class PrinterDetailView extends ScrollPane {

    private VBox contentBox;
    private Label dNozzleCur, dNozzleTgt, dBedCur, dBedTgt, dProgress, dElapsed, dRaw;
    private ProgressBar dProgressBar;
    private Label dCostFilament, dCostElec, dCostTotal, dCostPrice, dCostTag;
    private VBox dJobsTable;

    public PrinterDetailView() {
        contentBox = new VBox(0);
        contentBox.setStyle("-fx-background-color: " + Theme.C_BG + ";");
        setFitToWidth(true);
        setStyle("-fx-background-color: " + Theme.C_BG + "; -fx-background: " + Theme.C_BG + ";");
        setContent(contentBox);
        showHint();
    }

    public void showHint() {
        contentBox.getChildren().clear();
        contentBox.setAlignment(Pos.CENTER);
        Label hint = new Label("← Select a printer from the list");
        hint.setTextFill(Color.web(Theme.C_DIM));
        hint.setFont(Font.font("Segoe UI", 15));
        contentBox.getChildren().add(hint);
        contentBox.setPrefHeight(600);
    }

    public void renderPrinter(PrinterState p, Runnable onReportClick) {
        contentBox.getChildren().clear();
        contentBox.setAlignment(Pos.TOP_LEFT);
        
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 28, 20, 28));
        header.setStyle("-fx-background-color: " + Theme.C_SURFACE + "; -fx-border-color: " + Theme.C_BORDER + "; -fx-border-width: 0 0 1 0;");

        Circle dot = new Circle(7); dot.setFill(Color.web(p.connected ? Theme.C_GREEN : Theme.C_RED));
        VBox titleBox = new VBox(3); HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label title = new Label(p.displayName); title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20)); title.setTextFill(Color.web(Theme.C_BRIGHT));
        Label sub = new Label(p.connected ? "Connected — " + p.portName : "Not Connected"); sub.setFont(Font.font("Segoe UI", 11)); sub.setTextFill(Color.web(p.connected ? Theme.C_GREEN : Theme.C_DIM));
        titleBox.getChildren().addAll(title, sub);

        Button rptBtn = UIHelper.topBtn("📊 Printer Report");
        rptBtn.setOnAction(e -> onReportClick.run());

        header.getChildren().addAll(dot, titleBox, rptBtn);
        contentBox.getChildren().add(header);

        HBox grid = new HBox(20); grid.setAlignment(Pos.TOP_LEFT); grid.setPadding(new Insets(24, 28, 24, 28));
        VBox leftCol = new VBox(16); leftCol.setPrefWidth(340); leftCol.setMinWidth(300);
        leftCol.getChildren().addAll(buildTempSection(), buildProgressSection(), buildCostSection());
        VBox rightCol = new VBox(0); HBox.setHgrow(rightCol, Priority.ALWAYS);
        rightCol.getChildren().add(buildJobsSection(p));
        
        grid.getChildren().addAll(leftCol, rightCol);
        contentBox.getChildren().add(grid);
    }

    public void updateData(PrinterState p) {
        if (p == null || contentBox.getChildren().size() <= 1) return;
        dNozzleCur.setText(p.nozzleCurrent + "°C");
        dNozzleTgt.setText("→ " + p.nozzleTarget + "°C");
        dBedCur.setText(p.bedCurrent + "°C");
        dBedTgt.setText("→ " + p.bedTarget + "°C");
        dProgressBar.setProgress(p.progressPercent / 100.0);
        dProgress.setText(p.progressPercent + "%");
        dElapsed.setText(p.elapsedTime);
        dRaw.setText(p.rawData != null ? "> " + p.rawData : ">");

        if (p.lastCostBreakdown != null) {
            CostCalculator.CostBreakdown cb = p.lastCostBreakdown;
            dCostFilament.setText(String.format("%.1fg / %.2f₺", cb.filamentGrams, cb.filamentCostTL));
            dCostElec.setText(String.format("%.2f₺", cb.electricityCostTL));
            dCostTotal.setText(String.format("%.2f₺", cb.totalCostTL));
            dCostPrice.setText(String.format("%.2f₺", cb.sellingPriceTL));
            dCostTag.setText(cb.isEstimate ? "ESTIMATE" : "ACTUAL");
        }
        fillJobsTable(p);
    }

    private VBox buildTempSection() {
        VBox section = UIHelper.card(); section.setSpacing(14);
        HBox tempRow = new HBox(12);
        VBox nozBox = tempCell("NOZZLE", Theme.C_RED); dNozzleCur = (Label)((VBox)nozBox.getChildren().get(1)).getChildren().get(0); dNozzleTgt = (Label)((VBox)nozBox.getChildren().get(1)).getChildren().get(1);
        VBox bedBox = tempCell("BED", Theme.C_AMBER); dBedCur = (Label)((VBox)bedBox.getChildren().get(1)).getChildren().get(0); dBedTgt = (Label)((VBox)bedBox.getChildren().get(1)).getChildren().get(1);
        HBox.setHgrow(nozBox, Priority.ALWAYS); HBox.setHgrow(bedBox, Priority.ALWAYS);
        tempRow.getChildren().addAll(nozBox, bedBox);
        section.getChildren().addAll(UIHelper.sectionLabel("TEMPERATURE"), tempRow);
        return section;
    }

    private VBox tempCell(String label, String color) {
        VBox outer = new VBox(0); HBox.setHgrow(outer, Priority.ALWAYS); outer.setStyle("-fx-background-color:" + Theme.C_BG + "; -fx-border-color:" + Theme.C_BORDER + "; -fx-border-width:1; -fx-border-radius:6; -fx-background-radius:6;"); outer.setPadding(new Insets(12));
        Label lbl = new Label(label); lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9)); lbl.setTextFill(Color.web(Theme.C_DIM));
        VBox vals = new VBox(2); Label cur = new Label("--°C"); cur.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28)); cur.setTextFill(Color.web(color));
        Label tgt = new Label("→ --°C"); tgt.setFont(Font.font("Segoe UI", 11)); tgt.setTextFill(Color.web(Theme.C_DIM));
        vals.getChildren().addAll(cur, tgt); outer.getChildren().addAll(lbl, vals); return outer;
    }

    private VBox buildProgressSection() {
        VBox section = UIHelper.card(); section.setSpacing(10);
        dProgressBar = new ProgressBar(0); dProgressBar.setMaxWidth(Double.MAX_VALUE); dProgressBar.setPrefHeight(8); dProgressBar.setStyle("-fx-accent:" + Theme.C_BLUE + "; -fx-background-color:" + Theme.C_BORDER + ";");
        HBox row = new HBox(); dProgress = new Label("0%"); dProgress.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20)); dProgress.setTextFill(Color.web(Theme.C_BLUE));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS); dElapsed = new Label("00:00:00"); dElapsed.setFont(Font.font("Segoe UI", 13)); dElapsed.setTextFill(Color.web(Theme.C_DIM));
        row.getChildren().addAll(dProgress, sp, dElapsed);
        dRaw = new Label(">"); dRaw.setFont(Font.font("Consolas", 10)); dRaw.setTextFill(Color.web(Theme.C_DIM)); dRaw.setWrapText(true); dRaw.setMaxWidth(300);
        section.getChildren().addAll(UIHelper.sectionLabel("PRINT PROGRESS"), dProgressBar, row, dRaw); return section;
    }

    private VBox buildCostSection() {
        VBox section = UIHelper.card(); section.setSpacing(12);
        HBox titleRow = new HBox(8); titleRow.setAlignment(Pos.CENTER_LEFT);
        dCostTag = new Label("ESTIMATE"); dCostTag.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9)); dCostTag.setStyle("-fx-background-color:#252535; -fx-text-fill:#50506a; -fx-padding:2 7 2 7; -fx-background-radius:8;");
        titleRow.getChildren().addAll(UIHelper.sectionLabel("COST ESTIMATE"), dCostTag);

        GridPane g = new GridPane(); g.setHgap(12); g.setVgap(10);
        dCostFilament = costVal("--"); dCostElec = costVal("--"); dCostTotal = costVal("--", Theme.C_AMBER); dCostPrice = costVal("--", Theme.C_PURPLE);
        g.add(costCell("Filament", dCostFilament), 0, 0); g.add(costCell("Electricity", dCostElec), 1, 0); g.add(costCell("Cost", dCostTotal), 0, 1); g.add(costCell("Selling Price", dCostPrice), 1, 1);
        section.getChildren().addAll(titleRow, g); return section;
    }

    private VBox costCell(String label, Label valLabel) {
        VBox box = new VBox(3); box.setStyle("-fx-background-color:" + Theme.C_BG + "; -fx-border-color:" + Theme.C_BORDER + "; -fx-border-width:1; -fx-border-radius:6; -fx-background-radius:6;"); box.setPadding(new Insets(10));
        Label lbl = new Label(label); lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9)); lbl.setTextFill(Color.web(Theme.C_DIM)); box.getChildren().addAll(lbl, valLabel); return box;
    }
    private Label costVal(String init) { Label l = new Label(init); l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14)); l.setTextFill(Color.web(Theme.C_TEXT)); return l; }
    private Label costVal(String init, String color) { Label l = costVal(init); l.setTextFill(Color.web(color)); l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18)); return l; }

    private VBox buildJobsSection(PrinterState p) {
        VBox section = new VBox(0); section.setStyle("-fx-background-color:" + Theme.C_CARD + "; -fx-border-color:" + Theme.C_BORDER + "; -fx-border-width:1; -fx-border-radius:8; -fx-background-radius:8;");
        HBox header = new HBox(16); header.setPadding(new Insets(16, 20, 14, 20)); header.setStyle("-fx-border-color:" + Theme.C_BORDER + "; -fx-border-width:0 0 1 0;");
        Label title = new Label("PRINTED MODELS"); title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12)); title.setTextFill(Color.web(Theme.C_BRIGHT));
        header.getChildren().add(title);

        HBox colHeaders = new HBox(0); colHeaders.setPadding(new Insets(8, 20, 8, 20)); colHeaders.setStyle("-fx-background-color:" + Theme.C_SURFACE + "; -fx-border-color:" + Theme.C_BORDER + "; -fx-border-width:0 0 1 0;");
        colHeaders.getChildren().addAll(UIHelper.colHead("MODEL NAME", 260), UIHelper.colHead("TOTAL", 80), UIHelper.colHead("SUCCESSFUL", 80), UIHelper.colHead("FAILED", 80), UIHelper.colHead("SUCCESS %", 90));

        dJobsTable = new VBox(0); fillJobsTable(p);
        section.getChildren().addAll(header, colHeaders, dJobsTable); return section;
    }

    private void fillJobsTable(PrinterState p) {
        dJobsTable.getChildren().clear();
        List<String[]> models = DatabaseManager.getModelSummaries(p.displayName);
        for (int i = 0; i < models.size(); i++) {
            HBox row = new HBox(0); row.setPadding(new Insets(12, 20, 12, 20));
            if (i % 2 == 1) row.setStyle("-fx-background-color:" + Theme.C_SURFACE + ";");
            String[] m = models.get(i); int total = Integer.parseInt(m[1]), succ = Integer.parseInt(m[2]), fail = Integer.parseInt(m[3]);
            Label name = new Label(m[0]); name.setPrefWidth(260); name.setTextFill(Color.web(Theme.C_TEXT));
            row.getChildren().addAll(name, UIHelper.numCell(String.valueOf(total), Theme.C_TEXT, 80), UIHelper.numCell(String.valueOf(succ), Theme.C_GREEN, 80), UIHelper.numCell(String.valueOf(fail), Theme.C_RED, 80));
            dJobsTable.getChildren().add(row);
        }
    }
}