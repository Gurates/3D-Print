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

    private static final String BG_DEEP     = "#181824";
    private static final String BG_CARD     = "#222233";
    private static final String BORDER      = "#32324e";
    private static final String ACCENT_GREEN= "#24b37d";
    private static final String ACCENT_RED  = "#e35d76";
    private static final String ACCENT_BLUE = "#3a8ee6";
    private static final String ACCENT_ORG  = "#e68a4e";
    private static final String TEXT_MAIN   = "#ffffff";
    private static final String TEXT_DIM    = "#9696b0";

    private StackPane root;
    private BorderPane dashScreen, detailScreen, analyticsScreen, settingsScreen;

    private Label detailTitle, nozzleCurr, nozzleTgt, bedCurr, bedTgt, progressPct, elapsedLbl, rawLbl, connectionLbl;
    private ProgressBar progressBar;

    private PrinterState selectedPrinter;
    private FlowPane printerGrid;
    private Map<String, Label> cardStatusMap = new HashMap<>();

    @Override
    public void start(Stage stage) {
        root = new StackPane();
        root.setStyle("-fx-background-color: " + BG_DEEP + "; -fx-font-family: 'Segoe UI';");

        dashScreen      = buildDashboard();
        detailScreen    = buildDetailScreen();
        analyticsScreen = buildAnalyticsScreen();
        settingsScreen  = buildSettingsScreen();

        detailScreen.setVisible(false);
        analyticsScreen.setVisible(false);
        settingsScreen.setVisible(false);

        root.getChildren().addAll(settingsScreen, analyticsScreen, detailScreen, dashScreen);

        Scene scene = new Scene(root, 1280, 720);
        stage.setTitle("3D Farm Monitor - Enterprise Edition");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();

        Thread monThread = new Thread(PrinterManager::startMonitoring);
        monThread.setDaemon(true); monThread.start();

        Timeline uiTimer = new Timeline(new KeyFrame(Duration.millis(500), e -> tickUI()));
        uiTimer.setCycleCount(Timeline.INDEFINITE); uiTimer.play();

        Timeline initTimer = new Timeline(new KeyFrame(Duration.seconds(2), e -> refreshDashboardCards()));
        initTimer.setCycleCount(1); initTimer.play();
    }

    private void show(BorderPane screen) {
        dashScreen.setVisible(false); detailScreen.setVisible(false); analyticsScreen.setVisible(false); settingsScreen.setVisible(false);
        screen.setVisible(true);
    }

    private BorderPane buildSettingsScreen() {
        BorderPane pane = new BorderPane(); pane.setStyle("-fx-background-color: " + BG_DEEP + ";");
        HBox top = new HBox(16); top.setAlignment(Pos.CENTER_LEFT); top.setPadding(new Insets(20, 40, 20, 40)); top.setStyle("-fx-background-color: " + BG_CARD + "; -fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;");
        
        Button back = btn("GERİ DÖN", BORDER, TEXT_MAIN); back.setOnAction(e -> show(dashScreen));
        Label title = new Label("SİSTEM AYARLARI"); title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18)); title.setTextFill(Color.web(TEXT_MAIN));
        top.getChildren().addAll(back, title); pane.setTop(top);

        VBox content = new VBox(24); content.setAlignment(Pos.TOP_CENTER); content.setPadding(new Insets(60));
        VBox formCard = new VBox(20); formCard.setMaxWidth(600); formCard.setPadding(new Insets(40)); formCard.setStyle(sectionStyle());
        
        Label sectionTitle = new Label("BİLDİRİM ENTEGRASYONU (TELEGRAM)"); sectionTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14)); sectionTitle.setTextFill(Color.web(ACCENT_BLUE));
        Label info = new Label("Sistem hataları ve üretim tamamlanma raporları için entegrasyon ayarları."); info.setTextFill(Color.web(TEXT_DIM));

        VBox tokenBox = new VBox(8);
        Label tokenLbl = new Label("API Token (BotFather):"); tokenLbl.setTextFill(Color.web(TEXT_MAIN));
        TextField tokenField = new TextField(DatabaseManager.getSetting("telegram_bot_token", "")); 
        tokenField.setStyle(inputStyle()); tokenBox.getChildren().addAll(tokenLbl, tokenField);

        VBox chatBox = new VBox(8);
        Label chatLbl = new Label("Kullanıcı ID (Chat ID):"); chatLbl.setTextFill(Color.web(TEXT_MAIN));
        TextField chatField = new TextField(DatabaseManager.getSetting("telegram_chat_id", "")); 
        chatField.setStyle(inputStyle()); chatBox.getChildren().addAll(chatLbl, chatField);

        Label saveStatus = new Label(""); saveStatus.setTextFill(Color.web(ACCENT_GREEN)); saveStatus.setFont(Font.font("Segoe UI", 12));
        Button saveBtn = btn("AYARLARI KAYDET", ACCENT_BLUE, TEXT_MAIN); saveBtn.setPrefWidth(200);
        saveBtn.setOnAction(e -> {
            DatabaseManager.saveSetting("telegram_bot_token", tokenField.getText().trim());
            DatabaseManager.saveSetting("telegram_chat_id", chatField.getText().trim());
            saveStatus.setText("Sistem ayarları güncellendi.");
            Timeline clearTxt = new Timeline(new KeyFrame(Duration.seconds(3), evt -> saveStatus.setText(""))); clearTxt.play();
        });

        formCard.getChildren().addAll(sectionTitle, info, new Separator(), tokenBox, chatBox, saveBtn, saveStatus);
        content.getChildren().add(formCard); pane.setCenter(new StackPane(content)); return pane;
    }

    private String inputStyle() {
        return "-fx-background-color: #1a1a24; -fx-text-fill: white; -fx-padding: 10; -fx-border-color: " + BORDER + "; -fx-border-radius: 4; -fx-background-radius: 4;";
    }

    private BorderPane buildDashboard() {
        BorderPane pane = new BorderPane(); pane.setStyle("-fx-background-color: " + BG_DEEP + ";");

        HBox top = new HBox(); top.setAlignment(Pos.CENTER_LEFT); top.setPadding(new Insets(20, 40, 20, 40)); top.setStyle("-fx-background-color: " + BG_CARD + "; -fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;");
        
        Label logo = new Label("///"); logo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20)); logo.setTextFill(Color.web(ACCENT_BLUE));
        Label title = new Label("  3D FARM KONTROL MERKEZİ"); title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18)); title.setTextFill(Color.web(TEXT_MAIN));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Button statsBtn = btn("GENEL RAPORLAR", BORDER, TEXT_MAIN); statsBtn.setOnAction(e -> { refreshAnalytics(null); show(analyticsScreen); });
        Button settingsBtn = btn("AYARLAR", BORDER, TEXT_MAIN); settingsBtn.setOnAction(e -> show(settingsScreen));
        HBox.setMargin(settingsBtn, new Insets(0, 0, 0, 10));

        top.getChildren().addAll(logo, title, spacer, statsBtn, settingsBtn); pane.setTop(top);

        printerGrid = new FlowPane(); printerGrid.setAlignment(Pos.TOP_LEFT); printerGrid.setHgap(20); printerGrid.setVgap(20); printerGrid.setPadding(new Insets(40));
        ScrollPane scroll = new ScrollPane(printerGrid); scroll.setFitToWidth(true); scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        pane.setCenter(scroll); return pane;
    }

    private void refreshDashboardCards() {
        List<PrinterState> printers = PrinterManager.getPrinters();
        printerGrid.getChildren().clear(); cardStatusMap.clear();
        if (printers.isEmpty()) { Label empty = new Label("Aktif cihaz bulunamadı. Bağlantıları kontrol edin."); empty.setTextFill(Color.web(TEXT_DIM)); empty.setFont(Font.font("Segoe UI", 14)); printerGrid.getChildren().add(empty); return; }
        for (PrinterState p : printers) printerGrid.getChildren().add(buildPrinterCard(p));
    }

    private VBox buildPrinterCard(PrinterState p) {
        VBox card = new VBox(12); card.setPrefSize(280, 200); card.setPadding(new Insets(20)); card.setStyle(cardStyle(false));
        card.setOnMouseEntered(e -> card.setStyle(cardStyle(true))); card.setOnMouseExited(e  -> card.setStyle(cardStyle(false)));
        card.setOnMouseClicked(e -> { selectedPrinter = p; refreshDetailScreen(); show(detailScreen); });

        HBox header = new HBox(); header.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(p.displayName.toUpperCase()); name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14)); name.setTextFill(Color.web(TEXT_MAIN));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label status = new Label(p.connected ? "ONLINE" : "OFFLINE"); status.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10)); 
        status.setStyle("-fx-background-color: " + (p.connected ? "#1e3b32" : "#3b1e22") + "; -fx-text-fill: " + (p.connected ? ACCENT_GREEN : ACCENT_RED) + "; -fx-padding: 3 8 3 8; -fx-background-radius: 4;");
        cardStatusMap.put(p.portName, status);
        header.getChildren().addAll(name, sp, status);

        HBox temps = new HBox(15);
        Label noz = new Label("NZL: " + p.nozzleCurrent + "°C"); noz.setFont(Font.font("Segoe UI", 12)); noz.setTextFill(Color.web(TEXT_MAIN));
        Label bed = new Label("BED: " + p.bedCurrent + "°C"); bed.setFont(Font.font("Segoe UI", 12)); bed.setTextFill(Color.web(TEXT_DIM));
        temps.getChildren().addAll(noz, bed);

        ProgressBar miniBar = new ProgressBar(p.progressPercent / 100.0); miniBar.setPrefWidth(240); miniBar.setPrefHeight(6); miniBar.setStyle("-fx-accent: " + ACCENT_BLUE + ";");
        Label pctLbl = new Label("Baskı: %" + p.progressPercent); pctLbl.setTextFill(Color.web(TEXT_DIM)); pctLbl.setFont(Font.font("Segoe UI", 11));

        card.getChildren().addAll(header, new Separator(), temps, new Region(), miniBar, pctLbl); return card;
    }

    private String cardStyle(boolean hover) { return "-fx-background-color: " + (hover ? "#28283d" : BG_CARD) + "; -fx-border-color: " + (hover ? "#464666" : BORDER) + "; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;" + (hover ? "-fx-cursor: hand;" : ""); }

    private BorderPane buildDetailScreen() {
        BorderPane pane = new BorderPane(); pane.setStyle("-fx-background-color: " + BG_DEEP + ";");
        HBox top = new HBox(16); top.setAlignment(Pos.CENTER_LEFT); top.setPadding(new Insets(20, 40, 20, 40)); top.setStyle("-fx-background-color: " + BG_CARD + "; -fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;");

        Button back = btn("GERİ DÖN", BORDER, TEXT_MAIN); back.setOnAction(e -> { show(dashScreen); refreshDashboardCards(); });
        detailTitle = new Label("Yazıcı Detayı"); detailTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18)); detailTitle.setTextFill(Color.web(TEXT_MAIN));
        connectionLbl = new Label(); connectionLbl.setFont(Font.font("Segoe UI", 12));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button printerStatsBtn = btn("CİHAZ RAPORU", BORDER, TEXT_MAIN); printerStatsBtn.setOnAction(e -> { refreshAnalytics(selectedPrinter); show(analyticsScreen); });

        top.getChildren().addAll(back, detailTitle, connectionLbl, spacer, printerStatsBtn); pane.setTop(top);

        VBox content = new VBox(20); content.setAlignment(Pos.TOP_CENTER); content.setPadding(new Insets(40)); content.setMaxWidth(800);
        
        HBox tempRow = new HBox(20); tempRow.setAlignment(Pos.CENTER);
        VBox nozzleBox = buildTempCard("NOZZLE SICAKLIĞI", ACCENT_RED); nozzleCurr = (Label) nozzleBox.getChildren().get(1); nozzleTgt  = (Label) nozzleBox.getChildren().get(2);
        VBox bedBox = buildTempCard("TABLA SICAKLIĞI", ACCENT_ORG); bedCurr = (Label) bedBox.getChildren().get(1); bedTgt  = (Label) bedBox.getChildren().get(2);
        tempRow.getChildren().addAll(nozzleBox, bedBox);

        VBox progressSection = new VBox(10); progressSection.setStyle(sectionStyle()); progressSection.setPadding(new Insets(20));
        progressBar = new ProgressBar(0); progressBar.setPrefSize(Double.MAX_VALUE, 12); progressBar.setStyle("-fx-accent: " + ACCENT_BLUE + ";"); // Daha ince bar
        HBox progRow = new HBox(); progressPct = new Label("0%"); progressPct.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16)); progressPct.setTextFill(Color.web(ACCENT_BLUE));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS); elapsedLbl = new Label("Geçen Süre: 00:00:00"); elapsedLbl.setFont(Font.font("Segoe UI", 14)); elapsedLbl.setTextFill(Color.web(TEXT_DIM));
        progRow.getChildren().addAll(progressPct, sp, elapsedLbl); progressSection.getChildren().addAll(dimLabel("BASKI İLERLEME DURUMU"), progressBar, progRow);

        VBox testSection = new VBox(12); testSection.setStyle("-fx-background-color: #1c1c28; -fx-border-color: " + BORDER + "; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;"); testSection.setPadding(new Insets(16));
        Label testTitle = new Label("SİSTEM KONTROL SİMÜLASYONU"); testTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11)); testTitle.setTextFill(Color.web(TEXT_DIM));
        HBox testButtonsRow = new HBox(10); testButtonsRow.setAlignment(Pos.CENTER_LEFT);
        
        Button tStart = btn("İŞLEMİ BAŞLAT", "#253b47", ACCENT_BLUE);
        Button tSuccess = btn("BAŞARILI SONLANDIR", "#1d382d", ACCENT_GREEN);
        Button tFail = btn("HATA SİMÜLASYONU", "#3b1e22", ACCENT_RED);

        tStart.setOnAction(e -> { if (selectedPrinter == null) return; GCodeParser simParser = new GCodeParser(selectedPrinter); simParser.parseLine("File opened: Industrial_Part_V2.gcode Size: 145672"); pompSahteData(selectedPrinter, "T:215.0 /215.0 B:60.0 /60.0", 45, "00:45:12"); });
        tSuccess.setOnAction(e -> { if (selectedPrinter == null) return; selectedPrinter.isPrinting = true; selectedPrinter.elapsedTime = "01:15:32"; GCodeParser simParser = new GCodeParser(selectedPrinter); simParser.parseLine("Done printing file"); pompSahteData(selectedPrinter, "T:0.0 /0.0 B:0.0 /0.0", 100, "01:15:32"); });
        tFail.setOnAction(e -> { if (selectedPrinter == null) return; selectedPrinter.isPrinting = true; selectedPrinter.elapsedTime = "00:22:14"; GCodeParser simParser = new GCodeParser(selectedPrinter); simParser.parseLine("Error:Thermal Runaway, system stopped! heater_id: 0"); pompSahteData(selectedPrinter, "T:234.2 /0.0 B:59.8 /0.0", 22, "00:22:14"); });
        testButtonsRow.getChildren().addAll(tStart, tSuccess, tFail); testSection.getChildren().addAll(testTitle, testButtonsRow);

        VBox rawSection = new VBox(8); rawSection.setStyle(sectionStyle()); rawSection.setPadding(new Insets(16));
        rawLbl = new Label("> Sistem Hazır..."); rawLbl.setFont(Font.font("Consolas", 12)); rawLbl.setTextFill(Color.web(TEXT_DIM));
        rawSection.getChildren().addAll(dimLabel("SERİ HABERLEŞME TERMINALI"), rawLbl);

        content.getChildren().addAll(tempRow, progressSection, testSection, rawSection);
        pane.setCenter(new StackPane(content)); return pane;
    }

    private void pompSahteData(PrinterState p, String raw, int pct, String time) {
        p.rawData = raw; p.progressPercent = pct; p.elapsedTime = time;
        if(raw.contains("T:")) { String[] parts = raw.split(" "); p.nozzleCurrent = parts[0].substring(2).split("/")[0]; p.nozzleTarget = parts[0].substring(2).split("/")[1]; p.bedCurrent = parts[1].substring(2).split("/")[0]; p.bedTarget = parts[1].substring(2).split("/")[1]; }
    }

    private VBox buildTempCard(String label, String color) {
        VBox box = new VBox(8); box.setAlignment(Pos.CENTER); box.setPrefSize(380, 110); box.setPadding(new Insets(16)); box.setStyle(sectionStyle());
        Label cur = new Label("-- °C"); cur.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32)); cur.setTextFill(Color.web(color));
        Label tgt = new Label("Hedef: -- °C"); tgt.setFont(Font.font("Segoe UI", 12)); tgt.setTextFill(Color.web(TEXT_DIM));
        box.getChildren().addAll(dimLabel(label), cur, tgt); return box;
    }

    private void refreshDetailScreen() {
        if (selectedPrinter == null) return; detailTitle.setText(selectedPrinter.displayName.toUpperCase()); 
        connectionLbl.setText(selectedPrinter.connected ? " ONLINE" : " OFFLINE"); 
        connectionLbl.setTextFill(Color.web(selectedPrinter.connected ? ACCENT_GREEN : ACCENT_RED));
    }

    private BorderPane buildAnalyticsScreen() {
        BorderPane pane = new BorderPane(); pane.setStyle("-fx-background-color: " + BG_DEEP + ";");
        HBox top = new HBox(16); top.setAlignment(Pos.CENTER_LEFT); top.setPadding(new Insets(20, 40, 20, 40)); top.setStyle("-fx-background-color: " + BG_CARD + "; -fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;");
        Button back = btn("GERİ DÖN", BORDER, TEXT_MAIN); back.setOnAction(e -> show(selectedPrinter != null ? detailScreen : dashScreen));
        Label title = new Label("SİSTEM ANALİZ RAPORU"); title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18)); title.setTextFill(Color.web(TEXT_MAIN));
        top.getChildren().addAll(back, title); pane.setTop(top);
        ScrollPane scroll = new ScrollPane(new VBox()); scroll.setFitToWidth(true); scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;"); pane.setCenter(scroll); return pane;
    }

    private void refreshAnalytics(PrinterState filterPrinter) {
        ScrollPane scroll = (ScrollPane) analyticsScreen.getCenter(); VBox body = (VBox) scroll.getContent(); body.getChildren().clear(); body.setSpacing(28); body.setPadding(new Insets(36, 40, 40, 40));
        String pName = filterPrinter != null ? filterPrinter.displayName : null;
        HBox top = (HBox) analyticsScreen.getTop(); Label titleLbl = (Label) top.getChildren().get(1); titleLbl.setText(filterPrinter == null ? "FARM GENEL ÜRETİM RAPORU" : filterPrinter.displayName.toUpperCase() + " CİHAZ RAPORU");

        if (filterPrinter != null) {
            DatabaseManager.PrinterSummary s = DatabaseManager.getSinglePrinterSummary(pName); body.getChildren().add(sectionHeader("CİHAZ PERFORMANS ÖZETİ"));
            if (s != null && s.totalJobs > 0) body.getChildren().add(buildSummaryCard(s)); else body.getChildren().add(emptyNote("Veri bulunamadı."));
        } else {
            List<DatabaseManager.PrinterSummary> summaries = DatabaseManager.getPrinterSummaries(); body.getChildren().add(sectionHeader("FARM GENEL ÖZETİ"));
            if (summaries.isEmpty()) body.getChildren().add(emptyNote("Veri bulunamadı."));
            else { FlowPane grid = new FlowPane(); grid.setHgap(20); grid.setVgap(20); for (DatabaseManager.PrinterSummary s : summaries) grid.getChildren().add(buildSummaryCard(s)); body.getChildren().add(grid); }
        }

        body.getChildren().add(sectionHeader("BASKI MODELİ ÜRETİM DAĞILIMI"));
        List<String[]> models = DatabaseManager.getModelSummaries(pName);
        if (models.isEmpty()) body.getChildren().add(emptyNote("Kayıt yok.")); else body.getChildren().add(buildModelTable(models));

        body.getChildren().add(sectionHeader("SON İŞLEM GEÇMİŞİ"));
        List<String[]> recent = DatabaseManager.getRecentJobs(20, pName);
        if (recent.isEmpty()) body.getChildren().add(emptyNote("Kayıt yok.")); else body.getChildren().add(buildRecentTable(recent));

        body.getChildren().add(sectionHeader("HATA ANALİZ DAĞILIMI"));
        List<String[]> errors = DatabaseManager.getErrorBreakdown(pName);
        if (errors.isEmpty()) { Label ok = new Label("Sistemde kayıtlı donanım hatası bulunmamaktadır."); ok.setFont(Font.font("Segoe UI", 14)); ok.setTextFill(Color.web(TEXT_DIM)); ok.setPadding(new Insets(10)); body.getChildren().add(ok); } else body.getChildren().add(buildErrorBars(errors));
    }

    private VBox buildSummaryCard(DatabaseManager.PrinterSummary s) {
        VBox card = new VBox(12); card.setPrefWidth(320); card.setPadding(new Insets(20)); card.setStyle(sectionStyle());
        Label name = new Label(s.printerName.toUpperCase()); name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14)); name.setTextFill(Color.web(TEXT_MAIN));

        double rate = s.totalJobs > 0 ? (s.successJobs * 100.0 / s.totalJobs) : 0;
        ProgressBar bar = new ProgressBar(rate / 100.0); bar.setPrefWidth(Double.MAX_VALUE); bar.setPrefHeight(6);
        bar.setStyle("-fx-accent: " + (rate >= 80 ? ACCENT_GREEN : rate >= 50 ? ACCENT_ORG : ACCENT_RED) + ";");

        HBox rateRow = new HBox(); Label rateL = new Label(String.format("Başarı Oranı: %%%.0f", rate)); rateL.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12)); rateL.setTextFill(Color.web(TEXT_MAIN)); Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS); Label totL = new Label("Toplam İş: " + s.totalJobs); totL.setFont(Font.font("Segoe UI", 12)); totL.setTextFill(Color.web(TEXT_DIM)); rateRow.getChildren().addAll(rateL, sp, totL);

        VBox stats = new VBox(8);
        stats.getChildren().addAll(statRow("Başarılı İşlemler:", String.valueOf(s.successJobs), ACCENT_GREEN), statRow("İptal/Hatalı:", String.valueOf(s.failJobs), ACCENT_RED), statRow("Operasyon Süresi:", formatSeconds(s.totalSeconds), TEXT_MAIN), statRow("Tüketim (Filament):", String.format("%.1f m", s.totalFilamentM), TEXT_MAIN));
        card.getChildren().addAll(name, new Separator(), rateRow, bar, new Region(), stats); return card;
    }

    private HBox statRow(String lbl, String val, String color) { HBox row = new HBox(); row.setAlignment(Pos.CENTER_LEFT); Label l = new Label(lbl); l.setFont(Font.font("Segoe UI", 12)); l.setTextFill(Color.web(TEXT_DIM)); l.setPrefWidth(140); Label v = new Label(val); v.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12)); v.setTextFill(Color.web(color)); row.getChildren().addAll(l, v); return row; }

    private VBox buildModelTable(List<String[]> rows) {
        VBox table = new VBox(0); table.setStyle(sectionStyle()); double[] widths = {280, 150, 150, 150, 150}; table.getChildren().add(tableRow(new String[]{"Model Dosyası", "Toplam Üretim", "Başarılı", "Hatalı", "Başarı Oranı"}, true, widths));
        for (int i = 0; i < rows.size(); i++) { String[] r = rows.get(i); int tot = Integer.parseInt(r[1].split(" ")[0]); int suc = Integer.parseInt(r[2].replace("[+] ", "")); double rate = tot > 0 ? (suc * 100.0 / tot) : 0; HBox row = tableRow(new String[]{r[0], r[1], "[+] " + r[2], "[-] " + r[3], String.format("%%%d", (int)rate)}, false, widths); if (i % 2 == 1) row.setStyle("-fx-background-color: #1f1f2e;"); table.getChildren().add(row); } return table;
    }

    private VBox buildRecentTable(List<String[]> rows) {
        VBox table = new VBox(0); table.setStyle(sectionStyle()); double[] widths = {150, 260, 100, 100, 100, 160}; table.getChildren().add(tableRow(new String[]{"İstasyon", "Dosya", "Durum", "Süre", "Filament", "Kayıt Zamanı"}, true, widths));
        for (int i = 0; i < rows.size(); i++) { HBox row = tableRow(rows.get(i), false, widths); if (i % 2 == 1) row.setStyle("-fx-background-color: #1f1f2e;"); table.getChildren().add(row); } return table;
    }

    private HBox tableRow(String[] cols, boolean isHeader, double[] widths) {
        HBox row = new HBox(); row.setPadding(new Insets(10, 16, 10, 16));
        for (int i = 0; i < cols.length; i++) {
            Label l = new Label(cols[i] != null ? cols[i] : "—"); l.setPrefWidth(widths[i]); l.setWrapText(true);
            if (isHeader) { l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11)); l.setTextFill(Color.web(TEXT_DIM)); } 
            else { 
                l.setFont(Font.font("Segoe UI", 12)); 
                if (cols[i] != null && cols[i].startsWith("[+]")) l.setTextFill(Color.web(ACCENT_GREEN)); 
                else if (cols[i] != null && cols[i].startsWith("[-]")) l.setTextFill(Color.web(ACCENT_RED)); 
                else if (cols[i] != null && cols[i].startsWith("%")) { double val = Double.parseDouble(cols[i].substring(1)); l.setTextFill(Color.web(val >= 80 ? ACCENT_GREEN : val >= 50 ? ACCENT_ORG : ACCENT_RED)); l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12)); } 
                else if (i == 2 && (cols[2].equals("BAŞARILI") || cols[2].equals("HATALI"))) { l.setTextFill(Color.web(cols[2].equals("BAŞARILI") ? ACCENT_GREEN : ACCENT_RED)); l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12)); } 
                else l.setTextFill(Color.web(TEXT_MAIN)); 
            }
            row.getChildren().add(l);
        } return row;
    }

    private VBox buildErrorBars(List<String[]> errors) {
        VBox container = new VBox(10); container.setStyle(sectionStyle()); container.setPadding(new Insets(20)); int max = errors.stream().mapToInt(r -> Integer.parseInt(r[1])).max().orElse(1);
        for (String[] row : errors) { int count = Integer.parseInt(row[1]); double ratio = (double) count / max; HBox errRow = new HBox(12); errRow.setAlignment(Pos.CENTER_LEFT); Label errLabel = new Label(row[0] != null ? row[0] : "Bilinmeyen Sistem Hatası"); errLabel.setPrefWidth(280); errLabel.setFont(Font.font("Consolas", 12)); errLabel.setTextFill(Color.web(TEXT_MAIN)); ProgressBar errBar = new ProgressBar(ratio); errBar.setPrefWidth(300); errBar.setPrefHeight(6); errBar.setStyle("-fx-accent: " + ACCENT_RED + ";"); Label cntLabel = new Label(count + " vaka"); cntLabel.setFont(Font.font("Segoe UI", 12)); cntLabel.setTextFill(Color.web(TEXT_DIM)); errRow.getChildren().addAll(errLabel, errBar, cntLabel); container.getChildren().add(errRow); } return container;
    }

    private void tickUI() {
        if (detailScreen.isVisible() && selectedPrinter != null) {
            PrinterState p = selectedPrinter; nozzleCurr.setText(p.nozzleCurrent + " °C"); nozzleTgt.setText("Hedef: " + p.nozzleTarget + " °C"); bedCurr.setText(p.bedCurrent + " °C"); bedTgt.setText("Hedef: " + p.bedTarget + " °C"); progressBar.setProgress(p.progressPercent / 100.0); progressPct.setText("%" + p.progressPercent); elapsedLbl.setText("Çalışma Zamanı: " + p.elapsedTime); rawLbl.setText("> " + (p.rawData != null ? p.rawData : "")); connectionLbl.setText(p.connected ? " ONLINE" : " OFFLINE"); connectionLbl.setTextFill(Color.web(p.connected ? ACCENT_GREEN : ACCENT_RED));
        }
        if (dashScreen.isVisible()) { for (PrinterState p : PrinterManager.getPrinters()) { Label lbl = cardStatusMap.get(p.portName); if (lbl != null) { lbl.setText(p.connected ? "ONLINE" : "OFFLINE"); lbl.setStyle("-fx-background-color: " + (p.connected ? "#1e3b32" : "#3b1e22") + "; -fx-text-fill: " + (p.connected ? ACCENT_GREEN : ACCENT_RED) + "; -fx-padding: 3 8 3 8; -fx-background-radius: 4;"); } } }
    }

    private String sectionStyle() { return "-fx-background-color: " + BG_CARD + "; -fx-border-color: " + BORDER + "; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;"; }
    private Label sectionHeader(String text) { Label l = new Label(text); l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13)); l.setTextFill(Color.web(TEXT_DIM)); l.setPadding(new Insets(8, 0, 4, 0)); return l; }
    private Label dimLabel(String text) { Label l = new Label(text); l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10)); l.setTextFill(Color.web(TEXT_DIM)); return l; }
    private Label emptyNote(String text) { Label l = new Label(text); l.setFont(Font.font("Segoe UI", 13)); l.setTextFill(Color.web(TEXT_DIM)); l.setPadding(new Insets(16)); return l; }
    private Button btn(String text, String bgHex, String textHex) { Button b = new Button(text); b.setStyle("-fx-background-color: " + bgHex + "; -fx-text-fill: " + textHex + "; -fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 8 16 8 16; -fx-background-radius: 4; -fx-border-radius: 4;"); b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: derive(" + bgHex + ",20%); -fx-text-fill: " + textHex + "; -fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 8 16 8 16; -fx-background-radius: 4; -fx-border-radius: 4; -fx-cursor: hand;")); b.setOnMouseExited(e -> b.setStyle("-fx-background-color: " + bgHex + "; -fx-text-fill: " + textHex + "; -fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 8 16 8 16; -fx-background-radius: 4; -fx-border-radius: 4;")); return b; }
    private String formatSeconds(long secs) { if (secs <= 0) return "—"; long h = secs / 3600, m = (secs % 3600) / 60; return h > 0 ? h + "h " + m + "m" : m + "m"; }
    private static class Separator extends Region { Separator() { setPrefHeight(1); setMaxWidth(Double.MAX_VALUE); setStyle("-fx-background-color: " + BORDER + ";"); VBox.setMargin(this, new Insets(4, 0, 4, 0)); } }
}