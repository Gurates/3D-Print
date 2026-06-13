package printer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FarmUI extends Application {

    private static final String C_BG      = "#0d0d12";
    private static final String C_SURFACE = "#13131a";
    private static final String C_CARD    = "#1a1a24";
    private static final String C_BORDER  = "#252535";
    private static final String C_SIDEBAR = "#10101a";
    private static final String C_SEL     = "#1e1e30";
    private static final String C_GREEN   = "#00e5a0";
    private static final String C_RED     = "#ff4060";
    private static final String C_BLUE    = "#4d8fff";
    private static final String C_AMBER   = "#ffaa00";
    private static final String C_PURPLE  = "#9d7fff";
    private static final String C_DIM     = "#50506a";
    private static final String C_TEXT    = "#d0d0e0";
    private static final String C_BRIGHT  = "#f0f0ff";

    private Stage primaryStage;

    private BorderPane rootPane;

    private BorderPane farmView;
    private BorderPane reportView;
    private BorderPane settingsView;

    private Label reportTitleLabel;

    private final Map<String, Button> sidebarBtns = new HashMap<>();
    private PrinterState selectedPrinter = null;
    private VBox detailPane;

    private Label barOnline, barPrinting, barJobsToday, barErrors;

    private Label dNozzleCur, dNozzleTgt, dBedCur, dBedTgt;
    private Label dProgress, dElapsed, dRaw;
    private ProgressBar dProgressBar;
    private Label dCostFilament, dCostElec, dCostTotal, dCostPrice, dCostTag;
    private VBox dJobsTable;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        rootPane = new BorderPane();
        rootPane.setStyle("-fx-background-color: " + C_BG + ";");
        rootPane.setTop(buildTopBar());

        farmView     = buildFarmView();
        reportView   = buildReportView();
        settingsView = buildSettingsView();

        rootPane.setCenter(farmView);

        Scene scene = new Scene(rootPane, 1280, 800);
        stage.setTitle("3D Farm Monitor");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();

        Thread mon = new Thread(PrinterManager::startMonitoring);
        mon.setDaemon(true);
        mon.start();

        Timeline init = new Timeline(new KeyFrame(Duration.seconds(2), e -> populateSidebar()));
        init.setCycleCount(1);
        init.play();

        Timeline tick = new Timeline(new KeyFrame(Duration.millis(500), e -> tick()));
        tick.setCycleCount(Timeline.INDEFINITE);
        tick.play();
    }

    private void showPage(BorderPane page) {
        rootPane.setCenter(page);
    }
    private void showFarm()     { showPage(farmView); }
    private void showReport()   { showPage(reportView); }
    private void showSettings() { showPage(settingsView); }


    private HBox buildTopBar() {
        HBox bar = new HBox(0);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPrefHeight(52);
        bar.setStyle("-fx-background-color: " + C_SURFACE + "; -fx-border-color: " + C_BORDER
                + "; -fx-border-width: 0 0 1 0;");

        HBox logo = new HBox(8);
        logo.setAlignment(Pos.CENTER);
        logo.setPadding(new Insets(0, 24, 0, 20));
        logo.setPrefWidth(220);
        logo.setStyle("-fx-border-color: " + C_BORDER + "; -fx-border-width: 0 1 0 0;");
        Label logoMark = new Label("⬡");
        logoMark.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        logoMark.setTextFill(Color.web(C_GREEN));
        Label logoText = new Label("FARM\nMONITOR");
        logoText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        logoText.setTextFill(Color.web(C_BRIGHT));
        logo.getChildren().addAll(logoMark, logoText);

        HBox counters = new HBox(0);
        counters.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(counters, Priority.ALWAYS);
        barOnline    = new Label("0");
        barPrinting  = new Label("0");
        barJobsToday = new Label("0");
        barErrors    = new Label("0");
        counters.getChildren().addAll(
            counterCell("ONLINE",        barOnline,    C_GREEN),
            counterCell("PRINTING",    barPrinting,  C_BLUE),
            counterCell("PRINTED TODAY", barJobsToday, C_TEXT),
            counterCell("ERRORS",          barErrors,    C_RED)
        );

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(0, 16, 0, 16));

        Button farmBtn     = topBtn("🖨 Farm");
        Button reportBtn   = topBtn("📊 Reports");
        Button settingsBtn = topBtn("⚙ Settings");

        farmBtn.setOnAction(e -> showFarm());
        reportBtn.setOnAction(e -> { refreshReportView(null); showReport(); });
        settingsBtn.setOnAction(e -> showSettings());

        actions.getChildren().addAll(farmBtn, reportBtn, settingsBtn);
        bar.getChildren().addAll(logo, counters, actions);
        return bar;
    }

    private HBox counterCell(String label, Label valueLabel, String color) {
        HBox cell = new HBox(0);
        cell.setAlignment(Pos.CENTER);
        cell.setPadding(new Insets(0, 24, 0, 24));
        cell.setStyle("-fx-border-color: " + C_BORDER + "; -fx-border-width: 0 1 0 0;");
        cell.setPrefHeight(52);
        VBox inner = new VBox(1);
        inner.setAlignment(Pos.CENTER_LEFT);
        valueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        valueLabel.setTextFill(Color.web(color));
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Segoe UI", 9));
        lbl.setTextFill(Color.web(C_DIM));
        inner.getChildren().addAll(valueLabel, lbl);
        cell.getChildren().add(inner);
        return cell;
    }

    private BorderPane buildFarmView() {
        BorderPane pane = new BorderPane();
        pane.setStyle("-fx-background-color: " + C_BG + ";");
        pane.setLeft(buildSidebar());
        pane.setCenter(buildDetailArea());
        return pane;
    }


    private VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: " + C_SIDEBAR + "; -fx-border-color: " + C_BORDER
                + "; -fx-border-width: 0 1 0 0;");

        Label sec = new Label("PRINTERS");
        sec.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        sec.setTextFill(Color.web(C_DIM));
        sec.setPadding(new Insets(16, 16, 8, 16));
        sidebar.getChildren().add(sec);
        return sidebar;
    }

    private VBox getSidebar() { return (VBox) farmView.getLeft(); }

    private void populateSidebar() {
        VBox sidebar = getSidebar();
        sidebar.getChildren().removeIf(n -> n instanceof Button);
        sidebarBtns.clear();

        List<PrinterState> printers = PrinterManager.getPrinters();
        if (printers.isEmpty()) {
            Label empty = new Label("No devices found");
            empty.setFont(Font.font("Segoe UI", 11));
            empty.setTextFill(Color.web(C_DIM));
            empty.setPadding(new Insets(12, 16, 0, 16));
            sidebar.getChildren().add(empty);
            return;
        }
        for (PrinterState p : printers) {
            Button btn = buildSidebarBtn(p);
            sidebarBtns.put(p.portName, btn);
            sidebar.getChildren().add(btn);
        }
        if (selectedPrinter == null) selectPrinter(printers.get(0));
    }

    private Button buildSidebarBtn(PrinterState p) {
        Button btn = new Button();
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(64);
        btn.setPadding(new Insets(0, 12, 0, 16));
        applySidebarBtnStyle(btn, false);

        HBox content = new HBox(10);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setMaxWidth(Double.MAX_VALUE);

        Circle dot = new Circle(5);
        dot.setFill(Color.web(p.connected ? C_GREEN : C_DIM));

        VBox info = new VBox(3);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nameL = new Label(p.displayName);
        nameL.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        nameL.setTextFill(Color.web(C_BRIGHT));
        Label tempL = new Label(p.nozzleCurrent + "° / " + p.bedCurrent + "°");
        tempL.setFont(Font.font("Segoe UI", 10));
        tempL.setTextFill(Color.web(C_DIM));
        info.getChildren().addAll(nameL, tempL);

        ProgressBar mini = new ProgressBar(p.progressPercent / 100.0);
        mini.setPrefSize(200, 3);
        mini.setMaxWidth(Double.MAX_VALUE);
        mini.setStyle("-fx-accent: " + C_BLUE + "; -fx-background-color: " + C_BORDER + ";");

        VBox wrap = new VBox(6);
        wrap.setMaxWidth(Double.MAX_VALUE);
        content.getChildren().addAll(dot, info);
        wrap.getChildren().addAll(content, mini);
        btn.setGraphic(wrap);
        btn.setOnAction(e -> selectPrinter(p));
        return btn;
    }

    private void applySidebarBtnStyle(Button btn, boolean selected) {
        btn.setStyle(
            "-fx-background-color: " + (selected ? C_SEL : "transparent") + ";" +
            "-fx-border-color: transparent transparent transparent " + (selected ? C_BLUE : "transparent") + ";" +
            "-fx-border-width: 0 0 0 3; -fx-cursor: hand;"
        );
    }

    private void selectPrinter(PrinterState p) {
        selectedPrinter = p;
        sidebarBtns.forEach((port, btn) -> applySidebarBtnStyle(btn, port.equals(p.portName)));
        renderDetailPane(p);
    }


    private ScrollPane buildDetailArea() {
        detailPane = new VBox(0);
        detailPane.setStyle("-fx-background-color: " + C_BG + ";");
        Label hint = new Label("← Select a printer from the list");
        hint.setTextFill(Color.web(C_DIM));
        hint.setFont(Font.font("Segoe UI", 15));
        detailPane.setAlignment(Pos.CENTER);
        detailPane.getChildren().add(hint);
        detailPane.setPrefHeight(600);
        ScrollPane scroll = new ScrollPane(detailPane);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: " + C_BG + "; -fx-background: " + C_BG + ";");
        return scroll;
    }

    private void renderDetailPane(PrinterState p) {
        detailPane.getChildren().clear();
        detailPane.setAlignment(Pos.TOP_LEFT);
        detailPane.setSpacing(0);

        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 28, 20, 28));
        header.setStyle("-fx-background-color: " + C_SURFACE + "; -fx-border-color: " + C_BORDER
                + "; -fx-border-width: 0 0 1 0;");

        Circle dot = new Circle(7);
        dot.setFill(Color.web(p.connected ? C_GREEN : C_RED));

        VBox titleBox = new VBox(3);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label title = new Label(p.displayName);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        title.setTextFill(Color.web(C_BRIGHT));
        Label sub = new Label(p.connected ? "Connected — " + p.portName : "Not Connected");
        sub.setFont(Font.font("Segoe UI", 11));
        sub.setTextFill(Color.web(p.connected ? C_GREEN : C_DIM));
        titleBox.getChildren().addAll(title, sub);

        Button rptBtn = topBtn("📊 Printer Report");
        rptBtn.setOnAction(e -> { refreshReportView(p); showReport(); });

        HBox testRow = new HBox(6);
        testRow.setAlignment(Pos.CENTER_RIGHT);
        Button tStart   = smallBtn("▶ Start",   "#1a3040", C_BLUE);
        Button tSuccess = smallBtn("✓ Success",  "#102820", C_GREEN);
        Button tFail    = smallBtn("✗ Error",     "#301020", C_RED);
        tStart.setOnAction(e -> {
            new GCodeParser(p).parseLine("File opened: Test_Parca.gcode Size: 145672");
            p.nozzleCurrent = "215.0"; p.nozzleTarget = "215.0";
            p.bedCurrent = "60.0"; p.bedTarget = "60.0";
            p.progressPercent = 34; p.elapsedTime = "00:42:10";
            p.usedFilamentMm = 8400;
        });
        tSuccess.setOnAction(e -> {
            p.isPrinting = true; p.elapsedTime = "01:15:32"; p.usedFilamentMm = 18300;
            new GCodeParser(p).parseLine("Print finished successfully");
        });
        tFail.setOnAction(e -> {
            p.isPrinting = true; p.elapsedTime = "00:22:14"; p.usedFilamentMm = 4200;
            new GCodeParser(p).parseLine("Error:Thermal Runaway, system stopped! heater_id: 0");
        });
        testRow.getChildren().addAll(tStart, tSuccess, tFail);
        header.getChildren().addAll(dot, titleBox, testRow, rptBtn);
        detailPane.getChildren().add(header);

        // Content grid
        HBox grid = new HBox(20);
        grid.setAlignment(Pos.TOP_LEFT);
        grid.setPadding(new Insets(24, 28, 24, 28));

        VBox leftCol = new VBox(16);
        leftCol.setPrefWidth(340);
        leftCol.setMinWidth(300);
        leftCol.getChildren().addAll(buildTempSection(), buildProgressSection(), buildCostSection());

        VBox rightCol = new VBox(0);
        HBox.setHgrow(rightCol, Priority.ALWAYS);
        rightCol.getChildren().add(buildJobsSection(p));

        grid.getChildren().addAll(leftCol, rightCol);
        detailPane.getChildren().add(grid);
    }


    private VBox buildTempSection() {
        VBox section = card();
        section.setSpacing(14);
        HBox tempRow = new HBox(12);

        VBox nozBox = tempCell("NOZZLE", C_RED);
        dNozzleCur = (Label)((VBox)nozBox.getChildren().get(1)).getChildren().get(0);
        dNozzleTgt = (Label)((VBox)nozBox.getChildren().get(1)).getChildren().get(1);

        VBox bedBox = tempCell("BED", C_AMBER);
        dBedCur = (Label)((VBox)bedBox.getChildren().get(1)).getChildren().get(0);
        dBedTgt = (Label)((VBox)bedBox.getChildren().get(1)).getChildren().get(1);

        HBox.setHgrow(nozBox, Priority.ALWAYS);
        HBox.setHgrow(bedBox, Priority.ALWAYS);
        tempRow.getChildren().addAll(nozBox, bedBox);
        section.getChildren().addAll(sectionLabel("TEMPERATURE"), tempRow);
        return section;
    }

    private VBox tempCell(String label, String color) {
        VBox outer = new VBox(0);
        HBox.setHgrow(outer, Priority.ALWAYS);
        outer.setStyle("-fx-background-color:" + C_BG + "; -fx-border-color:" + C_BORDER
                + "; -fx-border-width:1; -fx-border-radius:6; -fx-background-radius:6;");
        outer.setPadding(new Insets(12));
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        lbl.setTextFill(Color.web(C_DIM));
        VBox vals = new VBox(2);
        Label cur = new Label("--°C");
        cur.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        cur.setTextFill(Color.web(color));
        Label tgt = new Label("→ --°C");
        tgt.setFont(Font.font("Segoe UI", 11));
        tgt.setTextFill(Color.web(C_DIM));
        vals.getChildren().addAll(cur, tgt);
        outer.getChildren().addAll(lbl, vals);
        return outer;
    }

    private VBox buildProgressSection() {
        VBox section = card();
        section.setSpacing(10);
        dProgressBar = new ProgressBar(0);
        dProgressBar.setMaxWidth(Double.MAX_VALUE);
        dProgressBar.setPrefHeight(8);
        dProgressBar.setStyle("-fx-accent:" + C_BLUE + "; -fx-background-color:" + C_BORDER + ";");
        HBox row = new HBox();
        dProgress = new Label("0%");
        dProgress.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        dProgress.setTextFill(Color.web(C_BLUE));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        dElapsed = new Label("00:00:00");
        dElapsed.setFont(Font.font("Segoe UI", 13));
        dElapsed.setTextFill(Color.web(C_DIM));
        row.getChildren().addAll(dProgress, sp, dElapsed);
        dRaw = new Label(">");
        dRaw.setFont(Font.font("Consolas", 10));
        dRaw.setTextFill(Color.web(C_DIM));
        dRaw.setWrapText(true);
        dRaw.setMaxWidth(300);
        section.getChildren().addAll(sectionLabel("PRINT PROGRESS"), dProgressBar, row, dRaw);
        return section;
    }

    private VBox buildCostSection() {
        VBox section = card();
        section.setSpacing(12);
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        dCostTag = new Label("ESTIMATE");
        dCostTag.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        dCostTag.setStyle("-fx-background-color:#252535; -fx-text-fill:#50506a;"
                + "-fx-padding:2 7 2 7; -fx-background-radius:8;");
        titleRow.getChildren().addAll(sectionLabel("COST ESTIMATE"), dCostTag);

        GridPane g = new GridPane();
        g.setHgap(12); g.setVgap(10);
        dCostFilament = costVal("--");
        dCostElec     = costVal("--");
        dCostTotal    = bigCostVal("--", C_AMBER);
        dCostPrice    = bigCostVal("--", C_PURPLE);
        g.add(costCell("Filament",    dCostFilament), 0, 0);
        g.add(costCell("Electricity",    dCostElec),     1, 0);
        g.add(costCell("Cost",     dCostTotal),    0, 1);
        g.add(costCell("Selling Price",dCostPrice),    1, 1);
        javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
        cc.setHgrow(Priority.ALWAYS); cc.setPercentWidth(50);
        javafx.scene.layout.ColumnConstraints cc2 = new javafx.scene.layout.ColumnConstraints();
        cc2.setHgrow(Priority.ALWAYS); cc2.setPercentWidth(50);
        g.getColumnConstraints().addAll(cc, cc2);
        section.getChildren().addAll(titleRow, g);
        return section;
    }

    private VBox costCell(String label, Label valLabel) {
        VBox box = new VBox(3);
        box.setStyle("-fx-background-color:" + C_BG + "; -fx-border-color:" + C_BORDER
                + "; -fx-border-width:1; -fx-border-radius:6; -fx-background-radius:6;");
        box.setPadding(new Insets(10));
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        lbl.setTextFill(Color.web(C_DIM));
        box.getChildren().addAll(lbl, valLabel);
        return box;
    }

    private Label costVal(String init) {
        Label l = new Label(init);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        l.setTextFill(Color.web(C_TEXT));
        return l;
    }

    private Label bigCostVal(String init, String color) {
        Label l = new Label(init);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        l.setTextFill(Color.web(color));
        return l;
    }

    private VBox buildJobsSection(PrinterState p) {
        VBox section = new VBox(0);
        section.setStyle("-fx-background-color:" + C_CARD + "; -fx-border-color:" + C_BORDER
                + "; -fx-border-width:1; -fx-border-radius:8; -fx-background-radius:8;");

        HBox tableHeader = new HBox(16);
        tableHeader.setPadding(new Insets(16, 20, 14, 20));
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        tableHeader.setStyle("-fx-border-color:" + C_BORDER + "; -fx-border-width:0 0 1 0;");
        Label tableTitle = new Label("PRINTED MODELS");
        tableTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        tableTitle.setTextFill(Color.web(C_BRIGHT));
        HBox.setHgrow(tableTitle, Priority.ALWAYS);
        Label sub = new Label("Every model previously printed on this printer");
        sub.setFont(Font.font("Segoe UI", 10));
        sub.setTextFill(Color.web(C_DIM));
        tableHeader.getChildren().addAll(tableTitle, sub);

        HBox colHeaders = new HBox(0);
        colHeaders.setPadding(new Insets(8, 20, 8, 20));
        colHeaders.setStyle("-fx-background-color:" + C_SURFACE + "; -fx-border-color:" + C_BORDER
                + "; -fx-border-width:0 0 1 0;");
        colHeaders.getChildren().addAll(
            colHead("MODEL NAME", 260), colHead("TOTAL", 80),
            colHead("SUCCESSFUL",  80),  colHead("FAILED",  80),
            colHead("SUCCESS %",  90),  colHead("LAST STATUS", 120)
        );

        dJobsTable = new VBox(0);
        fillJobsTable(p);
        section.getChildren().addAll(tableHeader, colHeaders, dJobsTable);
        return section;
    }

    private void fillJobsTable(PrinterState p) {
        dJobsTable.getChildren().clear();
        List<String[]> models = DatabaseManager.getModelSummaries(p.displayName);
        if (models.isEmpty()) {
            HBox empty = new HBox();
            empty.setPadding(new Insets(32));
            empty.setAlignment(Pos.CENTER);
            Label msg = new Label("No completed print records for this printer yet.\nYou can test it with the simulator.");
            msg.setTextFill(Color.web(C_DIM));
            msg.setFont(Font.font("Segoe UI", 13));
            msg.setStyle("-fx-text-alignment:center;");
            empty.getChildren().add(msg);
            dJobsTable.getChildren().add(empty);
            return;
        }
        for (int i = 0; i < models.size(); i++) {
            String[] m = models.get(i);
            int total = Integer.parseInt(m[1]);
            int succ  = Integer.parseInt(m[2]);
            int fail  = Integer.parseInt(m[3]);
            double rate = total > 0 ? succ * 100.0 / total : 0;
            String lastStatus = (fail > 0 && succ == 0) ? "FAILED" : (fail > 0 ? "MIXED" : "SUCCESSFUL");
            String sc = "SUCCESSFUL".equals(lastStatus) ? C_GREEN : "FAILED".equals(lastStatus) ? C_RED : C_AMBER;

            HBox row = new HBox(0);
            row.setPadding(new Insets(12, 20, 12, 20));
            row.setAlignment(Pos.CENTER_LEFT);
            if (i % 2 == 1) row.setStyle("-fx-background-color:" + C_SURFACE + ";");

            String dn = m[0] != null && m[0].length() > 32 ? m[0].substring(0, 30) + "…" : (m[0] != null ? m[0] : "—");
            Label nameL = new Label(dn);
            nameL.setFont(Font.font("Segoe UI", 12));
            nameL.setTextFill(Color.web(C_TEXT));
            nameL.setPrefWidth(260);

            VBox rateBox = new VBox(3);
            rateBox.setPrefWidth(90);
            Label rateL = new Label(String.format("%.0f%%", rate));
            rateL.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            rateL.setTextFill(Color.web(rate >= 80 ? C_GREEN : rate >= 50 ? C_AMBER : C_RED));
            ProgressBar rateBar = new ProgressBar(rate / 100.0);
            rateBar.setPrefWidth(70); rateBar.setPrefHeight(3);
            rateBar.setStyle("-fx-accent:" + (rate >= 80 ? C_GREEN : rate >= 50 ? C_AMBER : C_RED) + ";");
            rateBox.getChildren().addAll(rateL, rateBar);

            Label statusL = new Label(lastStatus);
            statusL.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            statusL.setPrefWidth(120);
            statusL.setStyle("-fx-background-color:" + sc + "22; -fx-text-fill:" + sc
                    + "; -fx-padding:3 10 3 10; -fx-background-radius:10;");

            row.getChildren().addAll(nameL,
                numCell(String.valueOf(total), C_TEXT, 80),
                numCell(String.valueOf(succ),  C_GREEN, 80),
                numCell(String.valueOf(fail),  fail > 0 ? C_RED : C_DIM, 80),
                rateBox, statusL);
            dJobsTable.getChildren().add(row);
        }
    }

    private BorderPane buildReportView() {
        BorderPane pane = new BorderPane();
        pane.setStyle("-fx-background-color:" + C_BG + ";");

        HBox subBar = new HBox(12);
        subBar.setAlignment(Pos.CENTER_LEFT);
        subBar.setPadding(new Insets(12, 28, 12, 28));
        subBar.setStyle("-fx-background-color:" + C_SURFACE + "; -fx-border-color:" + C_BORDER
                + "; -fx-border-width:0 0 1 0;");
        reportTitleLabel = new Label("REPORTS");
        reportTitleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        reportTitleLabel.setTextFill(Color.web(C_BRIGHT));
        subBar.getChildren().add(reportTitleLabel);
        pane.setTop(subBar);

        VBox body = new VBox();
        body.setId("reportBody");
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:" + C_BG + "; -fx-background:" + C_BG + ";");
        pane.setCenter(scroll);
        return pane;
    }

    private void refreshReportView(PrinterState filter) {
        ScrollPane scroll = (ScrollPane) reportView.getCenter();
        VBox body = (VBox) scroll.getContent();
        body.getChildren().clear();

        reportTitleLabel.setText(filter == null
            ? "FARM GENERAL REPORT"
            : filter.displayName.toUpperCase() + " — REPORT");

        VBox content = new VBox(28);
        content.setPadding(new Insets(32, 36, 40, 36));
        String pName = filter != null ? filter.displayName : null;

        content.getChildren().add(reportSection("PRINTER PERFORMANCE SUMMARY"));
        List<DatabaseManager.PrinterSummary> sums = pName == null
            ? DatabaseManager.getPrinterSummaries()
            : asList(DatabaseManager.getSinglePrinterSummary(pName));
        if (sums.isEmpty()) {
            content.getChildren().add(emptyNote("No completed print records yet."));
        } else {
            FlowPane summGrid = new FlowPane();
            summGrid.setHgap(16); summGrid.setVgap(16);
            for (DatabaseManager.PrinterSummary s : sums)
                if (s != null) summGrid.getChildren().add(buildReportCard(s));
            content.getChildren().add(summGrid);
        }

        content.getChildren().add(reportSection("RECENT OPERATIONS (Last 30)"));
        List<String[]> recent = DatabaseManager.getRecentJobs(30, pName);
        if (recent.isEmpty()) content.getChildren().add(emptyNote("No records."));
        else content.getChildren().add(buildRecentTable(recent));

        content.getChildren().add(reportSection("ERROR DISTRIBUTION"));
        List<String[]> errors = DatabaseManager.getErrorBreakdown(pName);
        if (errors.isEmpty()) {
            Label ok = new Label("✓  No error records — system working fine.");
            ok.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            ok.setTextFill(Color.web(C_GREEN));
            ok.setPadding(new Insets(16));
            content.getChildren().add(ok);
        } else {
            content.getChildren().add(buildErrorBars(errors));
        }

        body.getChildren().add(content);
    }

    private VBox buildReportCard(DatabaseManager.PrinterSummary s) {
        VBox card = new VBox(12);
        card.setPrefWidth(300);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color:" + C_CARD + "; -fx-border-color:" + C_BORDER
                + "; -fx-border-width:1; -fx-border-radius:8; -fx-background-radius:8;");

        Label name = new Label(s.printerName);
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        name.setTextFill(Color.web(C_BRIGHT));

        double rate = s.totalJobs > 0 ? (s.successJobs * 100.0 / s.totalJobs) : 0;
        String rc = rate >= 80 ? C_GREEN : rate >= 50 ? C_AMBER : C_RED;

        ProgressBar bar = new ProgressBar(rate / 100.0);
        bar.setMaxWidth(Double.MAX_VALUE); bar.setPrefHeight(5);
        bar.setStyle("-fx-accent:" + rc + ";");

        HBox rateRow = new HBox();
        Label rL = new Label(String.format("Success: %.0f%%", rate));
        rL.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        rL.setTextFill(Color.web(rc));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label tL = new Label(s.totalJobs + " prints");
        tL.setFont(Font.font("Segoe UI", 12)); tL.setTextFill(Color.web(C_DIM));
        rateRow.getChildren().addAll(rL, sp, tL);

        VBox stats = new VBox(7);
        stats.getChildren().addAll(
            statLine("✓  Successful",    String.valueOf(s.successJobs), C_GREEN),
            statLine("✗  Failed",      String.valueOf(s.failJobs),    C_RED),
            statLine("⏱  Duration",        formatSec(s.totalSeconds),     C_TEXT),
            statLine("🧵  Filament",    String.format("%.1f m", s.totalFilamentM), C_TEXT),
            statLine("💰  Total Cost", String.format("%.2f ₺", s.totalCostTL), C_AMBER),
            statLine("🏷  Selling Price",  String.format("%.2f ₺", s.totalRevenueTL), C_PURPLE)
        );

        Region div = new Region();
        div.setPrefHeight(1); div.setMaxWidth(Double.MAX_VALUE);
        div.setStyle("-fx-background-color:" + C_BORDER + ";");
        VBox.setMargin(div, new Insets(4, 0, 4, 0));

        card.getChildren().addAll(name, rateRow, bar, div, stats);
        return card;
    }

    private VBox buildRecentTable(List<String[]> rows) {
        VBox table = new VBox(0);
        table.setStyle("-fx-background-color:" + C_CARD + "; -fx-border-color:" + C_BORDER
                + "; -fx-border-width:1; -fx-border-radius:8; -fx-background-radius:8;");

        double[] w = {150, 200, 80, 90, 80, 80, 80, 160};
        String[] heads = {"Printer", "Model", "Status", "Duration", "Filament", "Cost", "Price", "Date"};

        HBox header = new HBox(0);
        header.setPadding(new Insets(9, 16, 9, 16));
        header.setStyle("-fx-background-color:" + C_SURFACE + "; -fx-border-color:" + C_BORDER
                + "; -fx-border-width:0 0 1 0; -fx-background-radius:8 8 0 0;");
        for (int i = 0; i < heads.length; i++) header.getChildren().add(colHead(heads[i], w[i]));
        table.getChildren().add(header);

        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            HBox row = new HBox(0);
            row.setPadding(new Insets(9, 16, 9, 16));
            row.setAlignment(Pos.CENTER_LEFT);
            if (i % 2 == 1) row.setStyle("-fx-background-color:" + C_SURFACE + ";");
            for (int j = 0; j < Math.min(r.length, w.length); j++) {
                String val = r[j] != null ? r[j] : "—";
                Label l = new Label(val);
                l.setPrefWidth(w[j]); l.setWrapText(true);
                l.setFont(Font.font("Segoe UI", j == 2 ? FontWeight.BOLD : FontWeight.NORMAL, 12));
                if (j == 2) {
                    l.setTextFill(Color.web("SUCCESSFUL".equals(val) ? C_GREEN : "FAILED".equals(val) ? C_RED : C_AMBER));
                } else if (j == 5 || j == 6) {
                    l.setTextFill(Color.web(j == 5 ? C_AMBER : C_PURPLE));
                } else {
                    l.setTextFill(Color.web(j == 0 ? C_DIM : C_TEXT));
                }
                row.getChildren().add(l);
            }
            table.getChildren().add(row);
        }
        return table;
    }

    private VBox buildErrorBars(List<String[]> errors) {
        VBox c = new VBox(10);
        c.setPadding(new Insets(18));
        c.setStyle("-fx-background-color:" + C_CARD + "; -fx-border-color:" + C_BORDER
                + "; -fx-border-width:1; -fx-border-radius:8; -fx-background-radius:8;");
        int max = errors.stream().mapToInt(r -> Integer.parseInt(r[1])).max().orElse(1);
        for (String[] row : errors) {
            int cnt = Integer.parseInt(row[1]);
            HBox er = new HBox(12); er.setAlignment(Pos.CENTER_LEFT);
            Label el = new Label(row[0] != null ? row[0] : "Bilinmeyen");
            el.setPrefWidth(280); el.setFont(Font.font("Consolas", 11));
            el.setTextFill(Color.web(C_TEXT));
            ProgressBar eb = new ProgressBar((double)cnt / max);
            eb.setPrefWidth(260); eb.setPrefHeight(5);
            eb.setStyle("-fx-accent:" + C_RED + ";");
            Label cl = new Label(cnt + "×");
            cl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            cl.setTextFill(Color.web(C_RED));
            er.getChildren().addAll(el, eb, cl);
            c.getChildren().add(er);
        }
        return c;
    }

    private BorderPane buildSettingsView() {
        BorderPane pane = new BorderPane();
        pane.setStyle("-fx-background-color:" + C_BG + ";");

        HBox subBar = new HBox(12);
        subBar.setAlignment(Pos.CENTER_LEFT);
        subBar.setPadding(new Insets(12, 28, 12, 28));
        subBar.setStyle("-fx-background-color:" + C_SURFACE + "; -fx-border-color:" + C_BORDER
                + "; -fx-border-width:0 0 1 0;");
        Label title = new Label("SETTINGS");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        title.setTextFill(Color.web(C_BRIGHT));
        subBar.getChildren().add(title);
        pane.setTop(subBar);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:" + C_BG + "; -fx-background:" + C_BG + ";");

        HBox body = new HBox(24);
        body.setPadding(new Insets(36));
        body.setAlignment(Pos.TOP_LEFT);

        body.getChildren().addAll(buildTelegramCard(), buildCostCard());

        scroll.setContent(body);
        pane.setCenter(scroll);
        return pane;
    }

    private VBox buildTelegramCard() {
        VBox card = settingsCard("📬  TELEGRAM NOTIFICATIONS");

        Label info = new Label(
            "Get instant notifications on Telegram\nwhen a print finishes or an error occurs.\n\n" +
            "1. @BotFather → /newbot → Get Bot Token\n" +
            "2. Send a message to your bot\n" +
            "3. @userinfobot → Get Chat ID"
        );
        info.setFont(Font.font("Segoe UI", 12));
        info.setTextFill(Color.web(C_DIM));
        info.setWrapText(true);

        TextField tokenField = settingsField(DatabaseManager.getSetting("telegram_bot_token", ""), "1234567890:ABCdef...");
        TextField chatField  = settingsField(DatabaseManager.getSetting("telegram_chat_id", ""),   "123456789");

        Label status = new Label("");
        status.setFont(Font.font("Segoe UI", 12));

        Button saveBtn = accentBtn("💾 Save", C_BLUE);
        Button testBtn = accentBtn("📨 Send Test Message", "#2a2a3e");

        saveBtn.setOnAction(e -> {
            DatabaseManager.saveSetting("telegram_bot_token", tokenField.getText().trim());
            DatabaseManager.saveSetting("telegram_chat_id",   chatField.getText().trim());
            status.setText("✓ Saved");
            status.setTextFill(Color.web(C_GREEN));
            new Timeline(new KeyFrame(Duration.seconds(3), ev -> status.setText(""))).play();
        });
        testBtn.setOnAction(e -> {
            String token = tokenField.getText().trim();
            String chat  = chatField.getText().trim();
            if (token.isEmpty() || chat.isEmpty()) {
                status.setText("⚠ Token and Chat ID cannot be empty");
                status.setTextFill(Color.web(C_AMBER));
                return;
            }
            DatabaseManager.saveSetting("telegram_bot_token", token);
            DatabaseManager.saveSetting("telegram_chat_id", chat);
            TelegramNotifier.sendMessage("✅ <b>3D Farm Monitor</b> connection test successful!\n\nNotifications active.");
            status.setText("✓ Test message sent");
            status.setTextFill(Color.web(C_GREEN));
        });

        HBox btns = new HBox(10);
        btns.getChildren().addAll(saveBtn, testBtn);

        card.getChildren().addAll(
            info, settingsDivider(),
            settingsFieldRow("Bot Token", tokenField),
            settingsFieldRow("Chat ID", chatField),
            btns, status
        );
        return card;
    }

    private VBox buildCostCard() {
        VBox card = settingsCard("💰  COST CALCULATION");

        Label info = new Label("Automatically calculates cost and\nsuggested selling price for each print.");
        info.setFont(Font.font("Segoe UI", 12));
        info.setTextFill(Color.web(C_DIM));
        info.setWrapText(true);

        CostConfig cfg = DatabaseManager.loadCostConfig();
        TextField tfFilament    = settingsField(fmt(cfg.filamentPricePerKg),     "600.00");
        TextField tfElectricity = settingsField(fmt(cfg.electricityCostPerHour), "0.70");
        TextField tfLabor       = settingsField(fmt(cfg.laborCostPerHour),       "0.00");
        TextField tfMargin      = settingsField(fmt(cfg.profitMarginPercent),     "30.00");
        TextField tfDensity     = settingsField(fmt(cfg.filamentDensity),         "1.24");

        Label previewLbl = new Label("Calculating example...");
        previewLbl.setFont(Font.font("Consolas", 12));
        previewLbl.setTextFill(Color.web(C_GREEN));
        previewLbl.setWrapText(true);

        Runnable updatePreview = () -> {
            try {
                CostConfig tmp = new CostConfig();
                tmp.filamentPricePerKg     = Double.parseDouble(tfFilament.getText().replace(",", ".").trim());
                tmp.electricityCostPerHour = Double.parseDouble(tfElectricity.getText().replace(",", ".").trim());
                tmp.laborCostPerHour       = Double.parseDouble(tfLabor.getText().replace(",", ".").trim());
                tmp.profitMarginPercent    = Double.parseDouble(tfMargin.getText().replace(",", ".").trim());
                tmp.filamentDensity        = Double.parseDouble(tfDensity.getText().replace(",", ".").trim());
                CostCalculator calc = new CostCalculator(tmp);
                double mm = 100.0 / Math.max(tmp.mmToGrams(1.0), 0.0001);
                CostCalculator.CostBreakdown ex = calc.calculate(mm, 7200);
                previewLbl.setText(String.format(
                    "100g filament + 2 hours print:%n" +
                    "  Filament   : %.2f ₺%n" +
                    "  Electricity: %.2f ₺%n" +
                    "  ─────────────────%n" +
                    "  Cost       : %.2f ₺%n" +
                    "  Selling    : %.2f ₺  (+%%%.0f)",
                    ex.filamentCostTL, ex.electricityCostTL,
                    ex.totalCostTL, ex.sellingPriceTL, tmp.profitMarginPercent
                ));
                previewLbl.setTextFill(Color.web(C_GREEN));
            } catch (Exception ex) {
                previewLbl.setText("⚠ Invalid value");
                previewLbl.setTextFill(Color.web(C_AMBER));
            }
        };
        for (TextField tf : new TextField[]{tfFilament, tfElectricity, tfLabor, tfMargin, tfDensity})
            tf.textProperty().addListener((o, ov, nv) -> updatePreview.run());
        updatePreview.run();

        Label costStatus = new Label("");
        costStatus.setFont(Font.font("Segoe UI", 12));

        Button saveBtn = accentBtn("💾 Save", C_BLUE);
        saveBtn.setOnAction(e -> {
            try {
                CostConfig save = new CostConfig();
                save.filamentPricePerKg     = Double.parseDouble(tfFilament.getText().replace(",", ".").trim());
                save.electricityCostPerHour = Double.parseDouble(tfElectricity.getText().replace(",", ".").trim());
                save.laborCostPerHour       = Double.parseDouble(tfLabor.getText().replace(",", ".").trim());
                save.profitMarginPercent    = Double.parseDouble(tfMargin.getText().replace(",", ".").trim());
                save.filamentDensity        = Double.parseDouble(tfDensity.getText().replace(",", ".").trim());
                DatabaseManager.saveCostConfig(save);
                costStatus.setText("✓ Saved");
                costStatus.setTextFill(Color.web(C_GREEN));
                new Timeline(new KeyFrame(Duration.seconds(3), ev -> costStatus.setText(""))).play();
            } catch (Exception ex) {
                costStatus.setText("⚠ " + ex.getMessage());
                costStatus.setTextFill(Color.web(C_RED));
            }
        });

        VBox previewBox = new VBox(8);
        previewBox.setPadding(new Insets(14));
        previewBox.setStyle("-fx-background-color:#080810; -fx-border-color:" + C_BORDER
                + "; -fx-border-radius:6; -fx-background-radius:6;");
        Label prevTitle = new Label("LIVE PREVIEW");
        prevTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        prevTitle.setTextFill(Color.web(C_DIM));
        previewBox.getChildren().addAll(prevTitle, previewLbl);

        card.getChildren().addAll(
            info, settingsDivider(),
            settingsFieldRow("Filament (₺/kg)",    tfFilament),
            settingsFieldRow("Electricity (₺/hour)",  tfElectricity),
            settingsFieldRow("Labor (₺/hour)",   tfLabor),
            settingsFieldRow("Profit Margin (%)",       tfMargin),
            settingsFieldRow("Filament Density", tfDensity),
            previewBox, saveBtn, costStatus
        );
        return card;
    }

    private VBox settingsCard(String title) {
        VBox card = new VBox(14);
        card.setPrefWidth(420);
        card.setPadding(new Insets(24));
        card.setStyle("-fx-background-color:" + C_CARD + "; -fx-border-color:" + C_BORDER
                + "; -fx-border-width:1; -fx-border-radius:10; -fx-background-radius:10;");
        Label lbl = new Label(title);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lbl.setTextFill(Color.web(C_BRIGHT));
        card.getChildren().add(lbl);
        return card;
    }

    private VBox settingsFieldRow(String label, TextField field) {
        VBox box = new VBox(5);
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        lbl.setTextFill(Color.web(C_TEXT));
        box.getChildren().addAll(lbl, field);
        return box;
    }

    private TextField settingsField(String value, String prompt) {
        TextField tf = new TextField(value);
        tf.setPromptText(prompt);
        String base = "-fx-background-color:#080810; -fx-text-fill:" + C_BRIGHT
                + "; -fx-border-radius:5; -fx-background-radius:5;"
                + "-fx-font-size:13px; -fx-padding:8 12 8 12;";
        tf.setStyle(base + "-fx-border-color:" + C_BORDER + ";");
        tf.focusedProperty().addListener((o, ov, focused) ->
            tf.setStyle(base + "-fx-border-color:" + (focused ? C_BLUE : C_BORDER) + ";"));
        return tf;
    }

    private Button accentBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white;"
                + "-fx-font-family:'Segoe UI'; -fx-font-weight:bold; -fx-font-size:12px;"
                + "-fx-padding:9 20 9 20; -fx-background-radius:5; -fx-cursor:hand;");
        return b;
    }

    private Region settingsDivider() {
        Region r = new Region();
        r.setPrefHeight(1); r.setMaxWidth(Double.MAX_VALUE);
        r.setStyle("-fx-background-color:" + C_BORDER + ";");
        VBox.setMargin(r, new Insets(2, 0, 2, 0));
        return r;
    }

    private String fmt(double v) { return String.format("%.2f", v).replace(",", "."); }

    private void tick() {
        List<PrinterState> printers = PrinterManager.getPrinters();
        for (PrinterState p : printers) {
            Button btn = sidebarBtns.get(p.portName);
            if (btn == null) continue;
            try {
                VBox wrap  = (VBox) btn.getGraphic();
                HBox row   = (HBox) wrap.getChildren().get(0);
                Circle dot = (Circle) row.getChildren().get(0);
                dot.setFill(Color.web(p.connected ? C_GREEN : C_DIM));
                VBox info  = (VBox) row.getChildren().get(1);
                ((Label) info.getChildren().get(1)).setText(p.nozzleCurrent + "° / " + p.bedCurrent + "°");
                ((ProgressBar) wrap.getChildren().get(1)).setProgress(p.progressPercent / 100.0);
            } catch (Exception ignored) {}
        }

        long online   = printers.stream().filter(p -> p.connected).count();
        long printing = printers.stream().filter(p -> p.isPrinting).count();
        barOnline.setText(String.valueOf(online));
        barPrinting.setText(String.valueOf(printing));
        if (selectedPrinter != null && dNozzleCur != null && detailPane.getChildren().size() > 1) {
            PrinterState p = selectedPrinter;
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
                boolean est = cb.isEstimate;
                dCostTag.setText(est ? "ESTIMATE" : "ACTUAL");
                dCostTag.setStyle("-fx-background-color:" + (est ? "#252535" : "#102820") + ";"
                        + "-fx-text-fill:" + (est ? C_DIM : C_GREEN) + ";"
                        + "-fx-padding:2 7 2 7; -fx-background-radius:8;"
                        + "-fx-font-size:9px; -fx-font-weight:bold;");
            }
            fillJobsTable(p);
        }
    }

    private VBox card() {
        VBox c = new VBox();
        c.setPadding(new Insets(16));
        c.setStyle("-fx-background-color:" + C_CARD + "; -fx-border-color:" + C_BORDER
                + "; -fx-border-width:1; -fx-border-radius:8; -fx-background-radius:8;");
        return c;
    }
    private Label sectionLabel(String t) {
        Label l = new Label(t); l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        l.setTextFill(Color.web(C_DIM)); return l;
    }
    private Label reportSection(String t) {
        Label l = new Label(t); l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        l.setTextFill(Color.web(C_DIM)); l.setPadding(new Insets(0,0,8,0)); return l;
    }
    private Label colHead(String t, double w) {
        Label l = new Label(t); l.setPrefWidth(w);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        l.setTextFill(Color.web(C_DIM)); return l;
    }
    private Label numCell(String v, String color, double w) {
        Label l = new Label(v); l.setPrefWidth(w);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        l.setTextFill(Color.web(color)); return l;
    }
    private HBox statLine(String lbl, String val, String color) {
        HBox row = new HBox(); row.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(lbl); l.setFont(Font.font("Segoe UI", 11));
        l.setTextFill(Color.web(C_DIM)); l.setPrefWidth(160);
        Label v = new Label(val); v.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        v.setTextFill(Color.web(color)); row.getChildren().addAll(l, v); return row;
    }
    private Button topBtn(String text) {
        Button b = new Button(text);
        String base = "-fx-font-family:'Segoe UI'; -fx-font-size:12px; -fx-font-weight:bold;"
                + "-fx-padding:7 14 7 14; -fx-background-radius:5; -fx-border-radius:5; -fx-border-width:1;";
        b.setStyle(base + "-fx-background-color:" + C_CARD + "; -fx-text-fill:" + C_TEXT + "; -fx-border-color:" + C_BORDER + ";");
        b.setOnMouseEntered(e -> b.setStyle(base + "-fx-background-color:" + C_SEL + "; -fx-text-fill:" + C_BRIGHT + "; -fx-border-color:" + C_BLUE + "; -fx-cursor:hand;"));
        b.setOnMouseExited(e  -> b.setStyle(base + "-fx-background-color:" + C_CARD + "; -fx-text-fill:" + C_TEXT + "; -fx-border-color:" + C_BORDER + ";"));
        return b;
    }
    private Button smallBtn(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg + ";"
                + "-fx-font-family:'Segoe UI'; -fx-font-size:10px; -fx-font-weight:bold;"
                + "-fx-padding:5 10 5 10; -fx-background-radius:4; -fx-cursor:hand;");
        return b;
    }
    private Label emptyNote(String t) {
        Label l = new Label(t); l.setFont(Font.font("Segoe UI", 13));
        l.setTextFill(Color.web(C_DIM)); l.setPadding(new Insets(20)); return l;
    }
    private String formatSec(long secs) {
        if (secs <= 0) return "—";
        long h = secs / 3600, m = (secs % 3600) / 60;
        return h > 0 ? h + "s " + m + "d" : m + "d";
    }
    @SuppressWarnings("unchecked")
    private <T> List<T> asList(T item) {
        List<T> l = new java.util.ArrayList<>();
        if (item != null) l.add(item); return l;
    }
}