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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FarmUI extends Application {

    private static final String BG_DEEP     = "#0f0f14";
    private static final String BG_CARD     = "#1a1a22";
    private static final String BG_CARD2    = "#22222d";
    private static final String BORDER      = "#2e2e3e";
    private static final String ACCENT_GREEN= "#00d68f";
    private static final String ACCENT_RED  = "#ff4d6d";
    private static final String ACCENT_BLUE = "#4d9fff";
    private static final String ACCENT_ORG  = "#ff9f43";
    private static final String TEXT_MAIN   = "#e8e8f0";
    private static final String TEXT_DIM    = "#8888a0";

    private StackPane root;
    private BorderPane dashScreen, detailScreen, analyticsScreen;

    private Label detailTitle;
    private Label nozzleCurr, nozzleTgt;
    private Label bedCurr, bedTgt;
    private ProgressBar progressBar;
    private Label progressPct, elapsedLbl, rawLbl, connectionLbl;

    private PrinterState selectedPrinter;

    private FlowPane printerGrid;
    private Map<String, Label> cardStatusMap = new HashMap<>();

    @Override
    public void start(Stage stage) {
        root = new StackPane();
        root.setStyle("-fx-background-color: " + BG_DEEP + ";");

        dashScreen     = buildDashboard();
        detailScreen   = buildDetailScreen();
        analyticsScreen = buildAnalyticsScreen();

        detailScreen.setVisible(false);
        analyticsScreen.setVisible(false);

        root.getChildren().addAll(analyticsScreen, detailScreen, dashScreen);

        Scene scene = new Scene(root, 1280, 720);
        stage.setTitle("3D Farm Monitor");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();

        Thread monThread = new Thread(PrinterManager::startMonitoring);
        monThread.setDaemon(true);
        monThread.start();

        Timeline uiTimer = new Timeline(new KeyFrame(Duration.millis(500), e -> tickUI()));
        uiTimer.setCycleCount(Timeline.INDEFINITE);
        uiTimer.play();

        Timeline initTimer = new Timeline(new KeyFrame(Duration.seconds(3), e -> refreshDashboardCards()));
        initTimer.setCycleCount(1);
        initTimer.play();
    }

    private void show(BorderPane screen) {
        dashScreen.setVisible(false);
        detailScreen.setVisible(false);
        analyticsScreen.setVisible(false);
        screen.setVisible(true);
    }

    private BorderPane buildDashboard() {
        BorderPane pane = new BorderPane();
        pane.setStyle("-fx-background-color: " + BG_DEEP + ";");

        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(24, 40, 24, 40));
        top.setStyle("-fx-background-color: " + BG_CARD + "; -fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;");

        Label logo = new Label("⬡");
        logo.setFont(Font.font("Segoe UI", 28));
        logo.setTextFill(Color.web(ACCENT_GREEN));

        Label title = new Label("  3D FARM MONITOR");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.web(TEXT_MAIN));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button statsBtn = btn("📊  Genel İstatistikler", ACCENT_BLUE);
        statsBtn.setOnAction(e -> { refreshAnalytics(); show(analyticsScreen); });

        top.getChildren().addAll(logo, title, spacer, statsBtn);
        pane.setTop(top);

        printerGrid = new FlowPane();
        printerGrid.setAlignment(Pos.TOP_LEFT);
        printerGrid.setHgap(24);
        printerGrid.setVgap(24);
        printerGrid.setPadding(new Insets(40));

        Label loading = new Label("Yazıcılar taranıyor...");
        loading.setTextFill(Color.web(TEXT_DIM));
        loading.setFont(Font.font("Segoe UI", 16));
        printerGrid.getChildren().add(loading);

        ScrollPane scroll = new ScrollPane(printerGrid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        pane.setCenter(scroll);

        return pane;
    }

    private void refreshDashboardCards() {
        List<PrinterState> printers = PrinterManager.getPrinters();
        printerGrid.getChildren().clear();
        cardStatusMap.clear();

        if (printers.isEmpty()) {
            Label empty = new Label("Hiç yazıcı bulunamadı.\nUSB bağlantısını kontrol edin.");
            empty.setTextFill(Color.web(TEXT_DIM));
            empty.setFont(Font.font("Segoe UI", 16));
            printerGrid.getChildren().add(empty);
            return;
        }

        for (PrinterState p : printers) {
            VBox card = buildPrinterCard(p);
            printerGrid.getChildren().add(card);
        }
    }

    private VBox buildPrinterCard(PrinterState p) {
        VBox card = new VBox(14);
        card.setPrefSize(300, 230);
        card.setPadding(new Insets(24));
        card.setStyle(cardStyle(false));

        card.setOnMouseEntered(e -> card.setStyle(cardStyle(true)));
        card.setOnMouseExited(e  -> card.setStyle(cardStyle(false)));
        card.setOnMouseClicked(e -> { selectedPrinter = p; refreshDetailScreen(); show(detailScreen); });

        Label icon = new Label("🖨");
        icon.setFont(Font.font("Segoe UI", 36));

        Label name = new Label(p.displayName);
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        name.setTextFill(Color.web(TEXT_MAIN));

        Label status = new Label(p.connected ? "● Bağlı" : "○ Bağlı Değil");
        status.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        status.setTextFill(Color.web(p.connected ? ACCENT_GREEN : TEXT_DIM));
        cardStatusMap.put(p.portName, status);
        HBox temps = new HBox(16);
        Label noz = new Label("Nozzle: " + p.nozzleCurrent + "°C");
        noz.setFont(Font.font("Segoe UI", 13));
        noz.setTextFill(Color.web(ACCENT_RED));
        Label bed = new Label("Tabla: " + p.bedCurrent + "°C");
        bed.setFont(Font.font("Segoe UI", 13));
        bed.setTextFill(Color.web(ACCENT_BLUE));
        temps.getChildren().addAll(noz, bed);

        ProgressBar miniBar = new ProgressBar(p.progressPercent / 100.0);
        miniBar.setPrefWidth(250);
        miniBar.setStyle("-fx-accent: " + ACCENT_GREEN + ";");
        Label pctLbl = new Label(p.progressPercent + "%");
        pctLbl.setTextFill(Color.web(TEXT_DIM));
        pctLbl.setFont(Font.font("Segoe UI", 12));

        card.getChildren().addAll(icon, name, status, temps, miniBar, pctLbl);
        return card;
    }

    private String cardStyle(boolean hover) {
        String bg = hover ? "#20202c" : BG_CARD;
        String br = hover ? "#4a4a65" : BORDER;
        return "-fx-background-color: " + bg + "; -fx-border-color: " + br + ";" +
               "-fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;" +
               (hover ? "-fx-cursor: hand;" : "");
    }


    private BorderPane buildDetailScreen() {
        BorderPane pane = new BorderPane();
        pane.setStyle("-fx-background-color: " + BG_DEEP + ";");
        HBox top = new HBox(16);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(20, 40, 20, 40));
        top.setStyle("-fx-background-color: " + BG_CARD + "; -fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;");

        Button back = btn("◀  Geri", "#3a3a50");
        back.setOnAction(e -> { show(dashScreen); refreshDashboardCards(); });

        detailTitle = new Label("Yazıcı Detayı");
        detailTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        detailTitle.setTextFill(Color.web(TEXT_MAIN));

        connectionLbl = new Label();
        connectionLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));

        top.getChildren().addAll(back, detailTitle, connectionLbl);
        pane.setTop(top);

        VBox content = new VBox(24);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(40));
        content.setMaxWidth(860);

        HBox tempRow = new HBox(20);
        tempRow.setAlignment(Pos.CENTER);

        VBox nozzleBox = buildTempCard("NOZZLE", ACCENT_RED);
        nozzleCurr = (Label) ((VBox) nozzleBox).getChildren().get(1);
        nozzleTgt  = (Label) ((VBox) nozzleBox).getChildren().get(2);

        VBox bedBox = buildTempCard("TABLA", ACCENT_BLUE);
        bedCurr = (Label) ((VBox) bedBox).getChildren().get(1);
        bedTgt  = (Label) ((VBox) bedBox).getChildren().get(2);

        tempRow.getChildren().addAll(nozzleBox, bedBox);

        VBox progressSection = new VBox(10);
        progressSection.setStyle(sectionStyle());
        progressSection.setPadding(new Insets(20));

        Label progTitle = dimLabel("BASKI İLERLEMESİ");
        progressBar = new ProgressBar(0);
        progressBar.setPrefSize(Double.MAX_VALUE, 22);
        progressBar.setStyle("-fx-accent: " + ACCENT_GREEN + ";");

        HBox progRow = new HBox();
        progressPct = new Label("0%");
        progressPct.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        progressPct.setTextFill(Color.web(ACCENT_GREEN));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        elapsedLbl = new Label("Süre: 00:00:00");
        elapsedLbl.setFont(Font.font("Segoe UI", 16));
        elapsedLbl.setTextFill(Color.web(ACCENT_ORG));
        progRow.getChildren().addAll(progressPct, sp, elapsedLbl);

        progressSection.getChildren().addAll(progTitle, progressBar, progRow);

        VBox rawSection = new VBox(8);
        rawSection.setStyle(sectionStyle());
        rawSection.setPadding(new Insets(16));
        Label rawTitle = dimLabel("SERI PORT ÇIKTISI");
        rawLbl = new Label("> Bekleniyor...");
        rawLbl.setFont(Font.font("Consolas", 13));
        rawLbl.setTextFill(Color.web("#55aa77"));
        rawSection.getChildren().addAll(rawTitle, rawLbl);

        content.getChildren().addAll(tempRow, progressSection, rawSection);

        StackPane wrapper = new StackPane(content);
        pane.setCenter(wrapper);
        return pane;
    }

    private VBox buildTempCard(String label, String color) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(260, 130);
        box.setPadding(new Insets(20));
        box.setStyle(sectionStyle());

        Label title = dimLabel(label);
        Label cur = new Label("-- °C");
        cur.setFont(Font.font("Segoe UI", FontWeight.BOLD, 40));
        cur.setTextFill(Color.web(color));
        Label tgt = new Label("Hedef: -- °C");
        tgt.setFont(Font.font("Segoe UI", 14));
        tgt.setTextFill(Color.web(TEXT_DIM));

        box.getChildren().addAll(title, cur, tgt);
        return box;
    }

    private void refreshDetailScreen() {
        if (selectedPrinter == null) return;
        detailTitle.setText(selectedPrinter.displayName);
        connectionLbl.setText(selectedPrinter.connected ? "  ● Bağlı" : "  ○ Bağlı Değil");
        connectionLbl.setTextFill(Color.web(selectedPrinter.connected ? ACCENT_GREEN : ACCENT_RED));
    }


    private BorderPane buildAnalyticsScreen() {
        BorderPane pane = new BorderPane();
        pane.setStyle("-fx-background-color: " + BG_DEEP + ";");

        HBox top = new HBox(16);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(20, 40, 20, 40));
        top.setStyle("-fx-background-color: " + BG_CARD + "; -fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;");

        Button back = btn("◀  Geri", "#3a3a50");
        back.setOnAction(e -> show(dashScreen));
        Label title = new Label("Genel İstatistikler");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.web(TEXT_MAIN));
        top.getChildren().addAll(back, title);
        pane.setTop(top);

        VBox body = new VBox();
        body.setId("analyticsBody");
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        pane.setCenter(scroll);

        return pane;
    }

    private void refreshAnalytics() {
        ScrollPane scroll = (ScrollPane) analyticsScreen.getCenter();
        VBox body = (VBox) scroll.getContent();
        body.getChildren().clear();
        body.setSpacing(28);
        body.setPadding(new Insets(36, 40, 40, 40));

        List<DatabaseManager.PrinterSummary> summaries = DatabaseManager.getPrinterSummaries();

        Label secTitle1 = sectionHeader("Yazıcı Başına Özet");
        body.getChildren().add(secTitle1);

        if (summaries.isEmpty()) {
            body.getChildren().add(emptyNote("Henüz tamamlanan baskı kaydı yok."));
        } else {
            FlowPane summaryGrid = new FlowPane();
            summaryGrid.setHgap(20);
            summaryGrid.setVgap(20);
            for (DatabaseManager.PrinterSummary s : summaries) {
                summaryGrid.getChildren().add(buildSummaryCard(s));
            }
            body.getChildren().add(summaryGrid);
        }

        Label secTitle2 = sectionHeader("Son 20 Baskı");
        body.getChildren().add(secTitle2);

        List<String[]> recent = DatabaseManager.getRecentJobs(20);
        if (recent.isEmpty()) {
            body.getChildren().add(emptyNote("Henüz kayıtlı baskı yok."));
        } else {
            body.getChildren().add(buildRecentTable(recent));
        }

        Label secTitle3 = sectionHeader("En Sık Karşılaşılan Hatalar");
        body.getChildren().add(secTitle3);

        List<String[]> errors = DatabaseManager.getErrorBreakdown();
        if (errors.isEmpty()) {
            Label ok = new Label("🚀  Hiç hata kaydı yok — farm sorunsuz çalışıyor!");
            ok.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
            ok.setTextFill(Color.web(ACCENT_GREEN));
            ok.setPadding(new Insets(20));
            body.getChildren().add(ok);
        } else {
            body.getChildren().add(buildErrorBars(errors));
        }
    }

    private VBox buildSummaryCard(DatabaseManager.PrinterSummary s) {
        VBox card = new VBox(12);
        card.setPrefWidth(340);
        card.setPadding(new Insets(22));
        card.setStyle(sectionStyle());

        Label name = new Label(s.printerName);
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        name.setTextFill(Color.web(TEXT_MAIN));

        // Başarı oranı
        double rate = s.totalJobs > 0 ? (s.successJobs * 100.0 / s.totalJobs) : 0;
        ProgressBar bar = new ProgressBar(rate / 100.0);
        bar.setPrefWidth(Double.MAX_VALUE);
        bar.setPrefHeight(14);
        bar.setStyle("-fx-accent: " + (rate >= 80 ? ACCENT_GREEN : rate >= 50 ? ACCENT_ORG : ACCENT_RED) + ";");

        HBox rateRow = new HBox();
        Label rateL = new Label(String.format("Başarı Oranı  %.0f%%", rate));
        rateL.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        rateL.setTextFill(Color.web(rate >= 80 ? ACCENT_GREEN : rate >= 50 ? ACCENT_ORG : ACCENT_RED));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label totL = new Label(s.totalJobs + " baskı");
        totL.setFont(Font.font("Segoe UI", 13));
        totL.setTextFill(Color.web(TEXT_DIM));
        rateRow.getChildren().addAll(rateL, sp, totL);

        VBox stats = new VBox(6);
        stats.getChildren().addAll(
            statRow("✅  Başarılı",   String.valueOf(s.successJobs), ACCENT_GREEN),
            statRow("❌  Hatalı",     String.valueOf(s.failJobs),    ACCENT_RED),
            statRow("⏱  Toplam Süre", formatSeconds(s.totalSeconds), ACCENT_ORG),
            statRow("🧵  Filament",   String.format("%.1f m", s.totalFilamentM), ACCENT_BLUE)
        );

        card.getChildren().addAll(name, rateRow, bar, new Separator(), stats);
        return card;
    }

    private HBox statRow(String lbl, String val, String color) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(lbl);
        l.setFont(Font.font("Segoe UI", 13));
        l.setTextFill(Color.web(TEXT_DIM));
        l.setPrefWidth(160);
        Label v = new Label(val);
        v.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        v.setTextFill(Color.web(color));
        row.getChildren().addAll(l, v);
        return row;
    }

    private VBox buildRecentTable(List<String[]> rows) {
        VBox table = new VBox(0);
        table.setStyle(sectionStyle());

        // Header
        HBox header = tableRow(
            new String[]{"Yazıcı", "Model Adı", "Durum", "Süre", "Filament", "Tarih"},
            true
        );
        table.getChildren().add(header);

        for (int i = 0; i < rows.size(); i++) {
            HBox row = tableRow(rows.get(i), false);
            if (i % 2 == 1) row.setStyle("-fx-background-color: #1f1f2a;");
            table.getChildren().add(row);
        }
        return table;
    }

    private HBox tableRow(String[] cols, boolean isHeader) {
        HBox row = new HBox();
        row.setPadding(new Insets(10, 16, 10, 16));
        double[] widths = {180, 240, 90, 100, 100, 180};
        for (int i = 0; i < cols.length; i++) {
            Label l = new Label(cols[i] != null ? cols[i] : "—");
            l.setPrefWidth(widths[i]);
            l.setWrapText(true);
            if (isHeader) {
                l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                l.setTextFill(Color.web(TEXT_DIM));
            } else {
                l.setFont(Font.font("Segoe UI", 13));
                // Duruma göre renk
                if (i == 2) {
                    String status = cols[2];
                    l.setTextFill(Color.web(
                        "BAŞARILI".equals(status) ? ACCENT_GREEN :
                        "HATALI".equals(status)   ? ACCENT_RED   : ACCENT_ORG
                    ));
                    l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
                } else {
                    l.setTextFill(Color.web(TEXT_MAIN));
                }
            }
            row.getChildren().add(l);
        }
        return row;
    }

    private VBox buildErrorBars(List<String[]> errors) {
        VBox container = new VBox(10);
        container.setStyle(sectionStyle());
        container.setPadding(new Insets(20));

        // Maksimum sayıyı bul (normalleştirme için)
        int max = errors.stream().mapToInt(r -> Integer.parseInt(r[1])).max().orElse(1);

        for (String[] row : errors) {
            int count = Integer.parseInt(row[1]);
            double ratio = (double) count / max;

            HBox errRow = new HBox(12);
            errRow.setAlignment(Pos.CENTER_LEFT);

            Label errLabel = new Label(row[0] != null ? row[0] : "Bilinmeyen hata");
            errLabel.setPrefWidth(280);
            errLabel.setFont(Font.font("Consolas", 13));
            errLabel.setTextFill(Color.web(TEXT_MAIN));

            ProgressBar errBar = new ProgressBar(ratio);
            errBar.setPrefWidth(300);
            errBar.setPrefHeight(16);
            errBar.setStyle("-fx-accent: " + ACCENT_RED + ";");

            Label cntLabel = new Label(count + "x");
            cntLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            cntLabel.setTextFill(Color.web(ACCENT_RED));

            errRow.getChildren().addAll(errLabel, errBar, cntLabel);
            container.getChildren().add(errRow);
        }
        return container;
    }

    // ── Periyodik UI güncellemesi ─────────────────────────────────────────

    private void tickUI() {
        // Detay ekranı
        if (detailScreen.isVisible() && selectedPrinter != null) {
            PrinterState p = selectedPrinter;
            nozzleCurr.setText(p.nozzleCurrent + " °C");
            nozzleTgt.setText("Hedef: " + p.nozzleTarget + " °C");
            bedCurr.setText(p.bedCurrent + " °C");
            bedTgt.setText("Hedef: " + p.bedTarget + " °C");
            progressBar.setProgress(p.progressPercent / 100.0);
            progressPct.setText(p.progressPercent + "%");
            elapsedLbl.setText("Süre: " + p.elapsedTime);
            rawLbl.setText("> " + (p.rawData != null ? p.rawData : ""));
            connectionLbl.setText(p.connected ? "  ● Bağlı" : "  ○ Bağlı Değil");
            connectionLbl.setTextFill(Color.web(p.connected ? ACCENT_GREEN : ACCENT_RED));
        }

        // Dashboard'daki mini kartları güncelle (sadece durum etiketi)
        if (dashScreen.isVisible()) {
            for (PrinterState p : PrinterManager.getPrinters()) {
                Label lbl = cardStatusMap.get(p.portName);
                if (lbl != null) {
                    lbl.setText(p.connected ? "● Bağlı" : "○ Bağlı Değil");
                    lbl.setTextFill(Color.web(p.connected ? ACCENT_GREEN : TEXT_DIM));
                }
            }
        }
    }

    // ── Yardımcı stil / widget metodları ─────────────────────────────────

    private String sectionStyle() {
        return "-fx-background-color: " + BG_CARD + "; -fx-border-color: " + BORDER + ";" +
               "-fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10;";
    }

    private Label sectionHeader(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        l.setTextFill(Color.web(TEXT_MAIN));
        l.setPadding(new Insets(8, 0, 4, 0));
        return l;
    }

    private Label dimLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        l.setTextFill(Color.web(TEXT_DIM));
        return l;
    }

    private Label emptyNote(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", 15));
        l.setTextFill(Color.web(TEXT_DIM));
        l.setPadding(new Insets(16));
        return l;
    }

    private Button btn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white;" +
                   "-fx-font-weight: bold; -fx-font-size: 13px;" +
                   "-fx-padding: 9 18 9 18; -fx-background-radius: 6;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: derive(" + color + ",20%); -fx-text-fill: white;" +
                "-fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 9 18 9 18; -fx-background-radius: 6; -fx-cursor: hand;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white;" +
                "-fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 9 18 9 18; -fx-background-radius: 6;"));
        return b;
    }

    private String formatSeconds(long secs) {
        if (secs <= 0) return "—";
        long h = secs / 3600, m = (secs % 3600) / 60;
        return h > 0 ? h + " sa " + m + " dk" : m + " dk";
    }

    private static class Separator extends Region {
        Separator() {
            setPrefHeight(1);
            setMaxWidth(Double.MAX_VALUE);
            setStyle("-fx-background-color: " + BORDER + ";");
            VBox.setMargin(this, new Insets(4, 0, 4, 0));
        }
    }
}