package printer.UI;

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
import printer.PrinterManager;
import printer.PrinterState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FarmUI extends Application {

    private BorderPane rootPane;
    private BorderPane farmView;
    private ReportView reportView;
    private SettingsView settingsView;
    private PrinterDetailView detailView;

    private Label barOnline, barPrinting, barJobsToday, barErrors;
    private final Map<String, Button> sidebarBtns = new HashMap<>();
    private PrinterState selectedPrinter = null;

    @Override
    public void start(Stage stage) {
        rootPane = new BorderPane();
        rootPane.setStyle("-fx-background-color: " + Theme.C_BG + ";");

        reportView   = new ReportView();
        settingsView = new SettingsView();
        detailView   = new PrinterDetailView();
        farmView     = buildFarmView();

        rootPane.setTop(buildTopBar());
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
        init.play();

        Timeline tick = new Timeline(new KeyFrame(Duration.millis(500), e -> tick()));
        tick.setCycleCount(Timeline.INDEFINITE);
        tick.play();
    }

    private void showPage(Pane page) {
        rootPane.setCenter(page);
    }

    private HBox buildTopBar() {
        HBox bar = new HBox(0); bar.setAlignment(Pos.CENTER_LEFT); bar.setPrefHeight(52);
        bar.setStyle("-fx-background-color: " + Theme.C_SURFACE + "; -fx-border-color: " + Theme.C_BORDER + "; -fx-border-width: 0 0 1 0;");

        HBox logo = new HBox(8); logo.setAlignment(Pos.CENTER); logo.setPadding(new Insets(0, 24, 0, 20)); logo.setPrefWidth(220);
        logo.setStyle("-fx-border-color: " + Theme.C_BORDER + "; -fx-border-width: 0 1 0 0;");
        Label logoMark = new Label("⬡"); logoMark.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20)); logoMark.setTextFill(Color.web(Theme.C_GREEN));
        Label logoText = new Label("FARM\nMONITOR"); logoText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11)); logoText.setTextFill(Color.web(Theme.C_BRIGHT));
        logo.getChildren().addAll(logoMark, logoText);

        HBox counters = new HBox(0); counters.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(counters, Priority.ALWAYS);
        barOnline = new Label("0"); barPrinting = new Label("0"); barJobsToday = new Label("0"); barErrors = new Label("0");
        counters.getChildren().addAll(
            counterCell("ONLINE", barOnline, Theme.C_GREEN), counterCell("PRINTING", barPrinting, Theme.C_BLUE),
            counterCell("PRINTED TODAY", barJobsToday, Theme.C_TEXT), counterCell("ERRORS", barErrors, Theme.C_RED)
        );

        HBox actions = new HBox(8); actions.setAlignment(Pos.CENTER); actions.setPadding(new Insets(0, 16, 0, 16));
        Button farmBtn = UIHelper.topBtn("🖨 Farm"); farmBtn.setOnAction(e -> showPage(farmView));
        Button reportBtn = UIHelper.topBtn("📊 Reports"); reportBtn.setOnAction(e -> { reportView.refresh(null); showPage(reportView); });
        Button settingsBtn = UIHelper.topBtn("⚙ Settings"); settingsBtn.setOnAction(e -> showPage(settingsView));
        
        actions.getChildren().addAll(farmBtn, reportBtn, settingsBtn);
        bar.getChildren().addAll(logo, counters, actions);
        return bar;
    }

    private HBox counterCell(String label, Label valueLabel, String color) {
        HBox cell = new HBox(0); cell.setAlignment(Pos.CENTER); cell.setPadding(new Insets(0, 24, 0, 24));
        cell.setStyle("-fx-border-color: " + Theme.C_BORDER + "; -fx-border-width: 0 1 0 0;"); cell.setPrefHeight(52);
        VBox inner = new VBox(1); inner.setAlignment(Pos.CENTER_LEFT);
        valueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18)); valueLabel.setTextFill(Color.web(color));
        Label lbl = new Label(label); lbl.setFont(Font.font("Segoe UI", 9)); lbl.setTextFill(Color.web(Theme.C_DIM));
        inner.getChildren().addAll(valueLabel, lbl); cell.getChildren().add(inner); return cell;
    }

    private BorderPane buildFarmView() {
        BorderPane pane = new BorderPane();
        pane.setStyle("-fx-background-color: " + Theme.C_BG + ";");
        
        VBox sidebar = new VBox(0); sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: " + Theme.C_SIDEBAR + "; -fx-border-color: " + Theme.C_BORDER + "; -fx-border-width: 0 1 0 0;");
        Label sec = new Label("PRINTERS"); sec.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9)); sec.setTextFill(Color.web(Theme.C_DIM)); sec.setPadding(new Insets(16, 16, 8, 16));
        sidebar.getChildren().add(sec);
        
        pane.setLeft(sidebar);
        pane.setCenter(detailView);
        return pane;
    }

    private void populateSidebar() {
        VBox sidebar = (VBox) farmView.getLeft();
        sidebar.getChildren().removeIf(n -> n instanceof Button);
        sidebarBtns.clear();

        List<PrinterState> printers = PrinterManager.getPrinters();
        for (PrinterState p : printers) {
            Button btn = new Button(); btn.setMaxWidth(Double.MAX_VALUE); btn.setPrefHeight(64); btn.setPadding(new Insets(0, 12, 0, 16));
            applySidebarBtnStyle(btn, false);
            
            HBox content = new HBox(10); content.setAlignment(Pos.CENTER_LEFT);
            Circle dot = new Circle(5); dot.setFill(Color.web(p.connected ? Theme.C_GREEN : Theme.C_DIM));
            VBox info = new VBox(3); HBox.setHgrow(info, Priority.ALWAYS);
            Label nameL = new Label(p.displayName); nameL.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12)); nameL.setTextFill(Color.web(Theme.C_BRIGHT));
            Label tempL = new Label(p.nozzleCurrent + "° / " + p.bedCurrent + "°"); tempL.setFont(Font.font("Segoe UI", 10)); tempL.setTextFill(Color.web(Theme.C_DIM));
            info.getChildren().addAll(nameL, tempL);
            
            ProgressBar mini = new ProgressBar(p.progressPercent / 100.0); mini.setMaxWidth(Double.MAX_VALUE);
            mini.setStyle("-fx-accent: " + Theme.C_BLUE + "; -fx-background-color: " + Theme.C_BORDER + ";");
            
            VBox wrap = new VBox(6); wrap.getChildren().addAll(content, mini);
            content.getChildren().addAll(dot, info); btn.setGraphic(wrap);
            btn.setOnAction(e -> selectPrinter(p));
            
            sidebarBtns.put(p.portName, btn);
            sidebar.getChildren().add(btn);
        }
        if (selectedPrinter == null && !printers.isEmpty()) selectPrinter(printers.get(0));
    }

    private void applySidebarBtnStyle(Button btn, boolean selected) {
        btn.setStyle("-fx-background-color: " + (selected ? Theme.C_SEL : "transparent") + ";" +
            "-fx-border-color: transparent transparent transparent " + (selected ? Theme.C_BLUE : "transparent") + ";" +
            "-fx-border-width: 0 0 0 3; -fx-cursor: hand;");
    }

    private void selectPrinter(PrinterState p) {
        selectedPrinter = p;
        sidebarBtns.forEach((port, btn) -> applySidebarBtnStyle(btn, port.equals(p.portName)));
        detailView.renderPrinter(p, () -> { reportView.refresh(p); showPage(reportView); });
    }

    private void tick() {
        List<PrinterState> printers = PrinterManager.getPrinters();
        long online = 0, printing = 0;
        
        for (PrinterState p : printers) {
            if (p.connected) online++;
            if (p.isPrinting) printing++;
            
            Button btn = sidebarBtns.get(p.portName);
            if (btn != null) {
                try {
                    VBox wrap = (VBox) btn.getGraphic();
                    HBox row = (HBox) wrap.getChildren().get(0);
                    ((Circle) row.getChildren().get(0)).setFill(Color.web(p.connected ? Theme.C_GREEN : Theme.C_DIM));
                    ((Label) ((VBox) row.getChildren().get(1)).getChildren().get(1)).setText(p.nozzleCurrent + "° / " + p.bedCurrent + "°");
                    ((ProgressBar) wrap.getChildren().get(1)).setProgress(p.progressPercent / 100.0);
                } catch (Exception ignored) {}
            }
        }

        barOnline.setText(String.valueOf(online));
        barPrinting.setText(String.valueOf(printing));
        detailView.updateData(selectedPrinter);
    }
}