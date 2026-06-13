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

    // ── Renk sistemi ────────────────────────────────────────────────────────
    private static final String C_BG        = "#0d0d12";   // En derin arka plan
    private static final String C_SURFACE   = "#13131a";   // Panel zeminleri
    private static final String C_CARD      = "#1a1a24";   // Kart yüzeyleri
    private static final String C_BORDER    = "#252535";   // Kenarlıklar
    private static final String C_SIDEBAR   = "#10101a";   // Sol kenar çubuk
    private static final String C_SEL       = "#1e1e30";   // Seçili yazıcı arka planı

    private static final String C_GREEN     = "#00e5a0";   // Başarı / online
    private static final String C_RED       = "#ff4060";   // Hata / offline
    private static final String C_BLUE      = "#4d8fff";   // İlerleme / vurgu
    private static final String C_AMBER     = "#ffaa00";   // Uyarı / sıcaklık
    private static final String C_PURPLE    = "#9d7fff";   // Maliyet / fiyat
    private static final String C_DIM       = "#50506a";   // İkincil metin
    private static final String C_TEXT      = "#d0d0e0";   // Ana metin
    private static final String C_BRIGHT    = "#f0f0ff";   // Başlık metni

    // ── State ────────────────────────────────────────────────────────────────
    private Stage primaryStage;
    private StackPane root;

    // İki ana "sayfa": Farm görünümü ve Raporlar
    private BorderPane farmView;
    private BorderPane reportView;

    // Sol sidebar yazıcı butonları (port → buton)
    private final Map<String, Button> sidebarBtns = new HashMap<>();
    private PrinterState selectedPrinter = null;

    // Sağ panel içeriği (tek bir ScrollPane, içi dinamik)
    private VBox detailPane;
    private ScrollPane detailScroll;

    // Üst bar sayaçları
    private Label barOnline, barPrinting, barJobsToday, barErrors;

    // Detay paneli canlı etiketler
    private Label dNozzleCur, dNozzleTgt, dBedCur, dBedTgt;
    private Label dProgress, dElapsed, dRaw;
    private ProgressBar dProgressBar;
    private Label dCostFilament, dCostElec, dCostTotal, dCostPrice;
    private Label dCostTag;
    private VBox  dJobsTable;   // Yazıcıya ait model tablosu

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        root = new StackPane();
        root.setStyle("-fx-background-color: " + C_BG + ";");

        farmView   = buildFarmView();
        reportView = buildReportView();
        reportView.setVisible(false);

        root.getChildren().addAll(reportView, farmView);

        Scene scene = new Scene(root, 1280, 800);
        stage.setTitle("3D Farm Monitor");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();

        // Arka plan izleme başlat
        Thread mon = new Thread(PrinterManager::startMonitoring);
        mon.setDaemon(true);
        mon.start();

        // 2 sn sonra sidebar doldur
        Timeline init = new Timeline(new KeyFrame(Duration.seconds(2), e -> populateSidebar()));
        init.setCycleCount(1);
        init.play();

        // 500ms tick
        Timeline tick = new Timeline(new KeyFrame(Duration.millis(500), e -> tick()));
        tick.setCycleCount(Timeline.INDEFINITE);
        tick.play();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  FARM VIEW — sol sidebar + sağ detay
    // ════════════════════════════════════════════════════════════════════════

    private BorderPane buildFarmView() {
        BorderPane pane = new BorderPane();
        pane.setStyle("-fx-background-color: " + C_BG + ";");
        pane.setTop(buildTopBar());
        pane.setLeft(buildSidebar());
        pane.setCenter(buildDetailArea());
        return pane;
    }

    // ── Üst bar ─────────────────────────────────────────────────────────────

    private HBox buildTopBar() {
        HBox bar = new HBox(0);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPrefHeight(52);
        bar.setStyle("-fx-background-color: " + C_SURFACE + "; -fx-border-color: " + C_BORDER
                + "; -fx-border-width: 0 0 1 0;");

        // Logo
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

        // Sayaçlar
        HBox counters = new HBox(0);
        counters.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(counters, Priority.ALWAYS);

        barOnline    = new Label("0");
        barPrinting  = new Label("0");
        barJobsToday = new Label("0");
        barErrors    = new Label("0");

        counters.getChildren().addAll(
            counterCell("ONLINE",       barOnline,    C_GREEN),
            counterCell("YAZDIRIYOR",   barPrinting,  C_BLUE),
            counterCell("BUGÜN BASILAN",barJobsToday, C_TEXT),
            counterCell("HATA",         barErrors,    C_RED)
        );

        // Sağ butonlar
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(0, 16, 0, 16));
        Button reportBtn  = topBtn("📊 Raporlar");
        Button settingsBtn = topBtn("⚙ Ayarlar");
        reportBtn.setOnAction(e -> { refreshReportView(null); showReport(); });
        settingsBtn.setOnAction(e -> new CostSettingsDialog(primaryStage).show());
        actions.getChildren().addAll(reportBtn, settingsBtn);

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

    // ── Sol sidebar ──────────────────────────────────────────────────────────

    private VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: " + C_SIDEBAR + "; -fx-border-color: " + C_BORDER
                + "; -fx-border-width: 0 1 0 0;");
        sidebar.setId("sidebar");

        Label sectionLbl = new Label("YAZICILAR");
        sectionLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        sectionLbl.setTextFill(Color.web(C_DIM));
        sectionLbl.setPadding(new Insets(16, 16, 8, 16));

        sidebar.getChildren().add(sectionLbl);
        return sidebar;
    }

    private VBox getSidebar() {
        return (VBox) farmView.getLeft();
    }

    private void populateSidebar() {
        VBox sidebar = getSidebar();
        // Üstteki başlık label'ını koru, geri kalanı temizle
        sidebar.getChildren().removeIf(n -> n instanceof Button);
        sidebarBtns.clear();

        List<PrinterState> printers = PrinterManager.getPrinters();
        if (printers.isEmpty()) {
            Label empty = new Label("Cihaz bulunamadı");
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

        // İlk yazıcıyı seç
        if (selectedPrinter == null && !printers.isEmpty()) {
            selectPrinter(printers.get(0));
        }
    }

    private Button buildSidebarBtn(PrinterState p) {
        Button btn = new Button();
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(64);
        btn.setPadding(new Insets(0, 12, 0, 16));
        applySidebarBtnStyle(btn, false);

        // İçerik
        HBox content = new HBox(10);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setMaxWidth(Double.MAX_VALUE);

        // Durum noktası
        Circle dot = new Circle(5);
        dot.setFill(Color.web(p.connected ? C_GREEN : C_DIM));

        VBox info = new VBox(3);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nameL = new Label(p.displayName);
        nameL.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        nameL.setTextFill(Color.web(C_BRIGHT));

        // Sıcaklık satırı
        Label tempL = new Label(p.nozzleCurrent + "° / " + p.bedCurrent + "°");
        tempL.setFont(Font.font("Segoe UI", 10));
        tempL.setTextFill(Color.web(C_DIM));
        info.getChildren().addAll(nameL, tempL);

        // Mini progress
        ProgressBar mini = new ProgressBar(p.progressPercent / 100.0);
        mini.setPrefSize(200, 3);
        mini.setMaxWidth(Double.MAX_VALUE);
        mini.setStyle("-fx-accent: " + C_BLUE + "; -fx-background-color: " + C_BORDER + ";");

        VBox wrap = new VBox(6);
        wrap.setMaxWidth(Double.MAX_VALUE);
        wrap.getChildren().addAll(content, mini);
        content.getChildren().addAll(dot, info);
        btn.setGraphic(wrap);

        btn.setOnAction(e -> selectPrinter(p));
        return btn;
    }

    private void applySidebarBtnStyle(Button btn, boolean selected) {
        String bg = selected ? C_SEL : "transparent";
        String border = selected ? C_BLUE : "transparent";
        btn.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-border-color: transparent transparent transparent " + border + ";" +
            "-fx-border-width: 0 0 0 3;" +
            "-fx-cursor: hand;"
        );
    }

    private void selectPrinter(PrinterState p) {
        selectedPrinter = p;
        // Sidebar görünüm güncelle
        sidebarBtns.forEach((port, btn) ->
            applySidebarBtnStyle(btn, port.equals(p.portName)));
        // Sağ paneli doldur
        renderDetailPane(p);
    }

    // ── Sağ detay alanı ──────────────────────────────────────────────────────

    private ScrollPane buildDetailArea() {
        detailPane = new VBox(0);
        detailPane.setStyle("-fx-background-color: " + C_BG + ";");

        detailScroll = new ScrollPane(detailPane);
        detailScroll.setFitToWidth(true);
        detailScroll.setStyle("-fx-background-color: " + C_BG + "; -fx-background: " + C_BG + ";");

        // Başlangıç boş state
        Label hint = new Label("← Soldaki listeden bir yazıcı seçin");
        hint.setTextFill(Color.web(C_DIM));
        hint.setFont(Font.font("Segoe UI", 15));
        detailPane.setAlignment(Pos.CENTER);
        detailPane.getChildren().add(hint);
        detailPane.setPrefHeight(600);

        return detailScroll;
    }

    private void renderDetailPane(PrinterState p) {
        detailPane.getChildren().clear();
        detailPane.setAlignment(Pos.TOP_LEFT);
        detailPane.setSpacing(0);

        // ── A. Yazıcı başlık şeridi ─────────────────────────────────────────
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 28, 20, 28));
        header.setStyle("-fx-background-color: " + C_SURFACE + "; -fx-border-color: " + C_BORDER
                + "; -fx-border-width: 0 0 1 0;");

        Circle statusDot = new Circle(7);
        statusDot.setFill(Color.web(p.connected ? C_GREEN : C_RED));

        VBox titleBox = new VBox(3);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label title = new Label(p.displayName);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        title.setTextFill(Color.web(C_BRIGHT));
        Label sub = new Label(p.connected ? "Bağlı — " + p.portName : "Bağlantı yok");
        sub.setFont(Font.font("Segoe UI", 11));
        sub.setTextFill(Color.web(p.connected ? C_GREEN : C_DIM));
        titleBox.getChildren().addAll(title, sub);

        Button reportBtn2 = topBtn("📊 Bu Yazıcının Raporu");
        reportBtn2.setOnAction(e -> { refreshReportView(p); showReport(); });

        // Test butonları (sadece sim modunda göster)
        HBox testRow = new HBox(6);
        testRow.setAlignment(Pos.CENTER_RIGHT);
        Button tStart   = smallBtn("▶ Başlat",  "#1a3040", C_BLUE);
        Button tSuccess = smallBtn("✓ Başarılı", "#102820", C_GREEN);
        Button tFail    = smallBtn("✗ Hata",    "#301020", C_RED);
        tStart.setOnAction(e -> {
            GCodeParser sp = new GCodeParser(p);
            sp.parseLine("File opened: Parca_v2.gcode Size: 145672");
            p.nozzleCurrent = "215.0"; p.nozzleTarget = "215.0";
            p.bedCurrent = "60.0"; p.bedTarget = "60.0";
            p.progressPercent = 34; p.elapsedTime = "00:42:10";
            p.usedFilamentMm = 8400;
        });
        tSuccess.setOnAction(e -> {
            p.isPrinting = true; p.elapsedTime = "01:15:32"; p.usedFilamentMm = 18300;
            GCodeParser sp = new GCodeParser(p); sp.parseLine("Done printing file");
        });
        tFail.setOnAction(e -> {
            p.isPrinting = true; p.elapsedTime = "00:22:14"; p.usedFilamentMm = 4200;
            GCodeParser sp = new GCodeParser(p);
            sp.parseLine("Error:Thermal Runaway, system stopped! heater_id: 0");
        });
        testRow.getChildren().addAll(tStart, tSuccess, tFail);

        header.getChildren().addAll(statusDot, titleBox, testRow, reportBtn2);
        detailPane.getChildren().add(header);

        // ── B. İçerik ızgarası ──────────────────────────────────────────────
        HBox grid = new HBox(0);
        grid.setAlignment(Pos.TOP_LEFT);
        grid.setPadding(new Insets(24, 28, 24, 28));
        grid.setSpacing(20);

        // Sol kolon: sıcaklık + ilerleme + maliyet
        VBox leftCol = new VBox(16);
        leftCol.setPrefWidth(340);
        leftCol.setMinWidth(300);

        leftCol.getChildren().addAll(
            buildTempSection(p),
            buildProgressSection(p),
            buildCostSection(p)
        );

        // Sağ kolon: bu yazıcıda basılan modeller tablosu
        VBox rightCol = new VBox(0);
        HBox.setHgrow(rightCol, Priority.ALWAYS);
        rightCol.getChildren().add(buildJobsSection(p));

        grid.getChildren().addAll(leftCol, rightCol);
        detailPane.getChildren().add(grid);
    }

    // ── Sıcaklık kartı ──────────────────────────────────────────────────────

    private VBox buildTempSection(PrinterState p) {
        VBox section = card();
        section.setSpacing(14);

        Label sec = sectionLabel("SICAKLIK");
        HBox tempRow = new HBox(12);

        VBox nozBox = tempCell("NOZZLE", C_RED);
        dNozzleCur = (Label) ((VBox) nozBox.getChildren().get(1)).getChildren().get(0);
        dNozzleTgt = (Label) ((VBox) nozBox.getChildren().get(1)).getChildren().get(1);

        VBox bedBox = tempCell("TABLA", C_AMBER);
        dBedCur = (Label) ((VBox) bedBox.getChildren().get(1)).getChildren().get(0);
        dBedTgt = (Label) ((VBox) bedBox.getChildren().get(1)).getChildren().get(1);

        tempRow.getChildren().addAll(nozBox, bedBox);
        HBox.setHgrow(nozBox, Priority.ALWAYS);
        HBox.setHgrow(bedBox, Priority.ALWAYS);

        section.getChildren().addAll(sec, tempRow);
        return section;
    }

    private VBox tempCell(String label, String color) {
        VBox outer = new VBox(0);
        HBox.setHgrow(outer, Priority.ALWAYS);
        outer.setStyle("-fx-background-color: " + C_BG + "; -fx-border-color: " + C_BORDER
                + "; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");
        outer.setPadding(new Insets(12));
        outer.setAlignment(Pos.TOP_LEFT);

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

    // ── İlerleme kartı ──────────────────────────────────────────────────────

    private VBox buildProgressSection(PrinterState p) {
        VBox section = card();
        section.setSpacing(10);

        Label sec = sectionLabel("BASKI İLERLEMESİ");

        dProgressBar = new ProgressBar(0);
        dProgressBar.setMaxWidth(Double.MAX_VALUE);
        dProgressBar.setPrefHeight(8);
        dProgressBar.setStyle("-fx-accent: " + C_BLUE + "; -fx-background-color: " + C_BORDER + ";");

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

        section.getChildren().addAll(sec, dProgressBar, row, dRaw);
        return section;
    }

    // ── Maliyet kartı ────────────────────────────────────────────────────────

    private VBox buildCostSection(PrinterState p) {
        VBox section = card();
        section.setSpacing(12);

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label sec = sectionLabel("MALİYET TAHMİNİ");
        dCostTag = new Label("TAHMİNİ");
        dCostTag.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        dCostTag.setStyle("-fx-background-color: #252535; -fx-text-fill: #50506a;"
                + "-fx-padding: 2 7 2 7; -fx-background-radius: 8;");
        titleRow.getChildren().addAll(sec, dCostTag);

        GridPane g = new GridPane();
        g.setHgap(12); g.setVgap(10);

        dCostFilament = costVal("--");
        dCostElec     = costVal("--");
        dCostTotal    = bigCostVal("--", C_AMBER);
        dCostPrice    = bigCostVal("--", C_PURPLE);

        g.add(costCell("Filament", dCostFilament), 0, 0);
        g.add(costCell("Elektrik", dCostElec),     1, 0);
        g.add(costCell("Maliyet",  dCostTotal),    0, 1);
        g.add(costCell("Satış Fiyatı", dCostPrice),1, 1);

        ColumnConstraints cc = new ColumnConstraints();
        cc.setHgrow(Priority.ALWAYS);
        cc.setPercentWidth(50);
        g.getColumnConstraints().addAll(cc, new ColumnConstraints() {{ setHgrow(Priority.ALWAYS); setPercentWidth(50); }});

        section.getChildren().addAll(titleRow, g);
        return section;
    }

    private VBox costCell(String label, Label valLabel) {
        VBox box = new VBox(3);
        box.setStyle("-fx-background-color: " + C_BG + "; -fx-border-color: " + C_BORDER
                + "; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");
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

    // ── Bu yazıcıdaki baskılar tablosu ──────────────────────────────────────

    private VBox buildJobsSection(PrinterState p) {
        VBox section = new VBox(0);
        section.setStyle("-fx-background-color: " + C_CARD + "; -fx-border-color: " + C_BORDER
                + "; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");

        HBox tableHeader = new HBox(16);
        tableHeader.setPadding(new Insets(16, 20, 14, 20));
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        tableHeader.setStyle("-fx-border-color: " + C_BORDER + "; -fx-border-width: 0 0 1 0;");

        Label tableTitle = new Label("BASILAN MODELLER");
        tableTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        tableTitle.setTextFill(Color.web(C_BRIGHT));
        HBox.setHgrow(tableTitle, Priority.ALWAYS);

        Label sub = new Label("Bu yazıcıda daha önce basılan her model");
        sub.setFont(Font.font("Segoe UI", 10));
        sub.setTextFill(Color.web(C_DIM));

        tableHeader.getChildren().addAll(tableTitle, sub);

        // Sütun başlıkları
        HBox colHeaders = buildJobColHeader();

        dJobsTable = new VBox(0);
        fillJobsTable(p);

        section.getChildren().addAll(tableHeader, colHeaders, dJobsTable);
        return section;
    }

    private HBox buildJobColHeader() {
        HBox row = new HBox(0);
        row.setPadding(new Insets(8, 20, 8, 20));
        row.setStyle("-fx-background-color: " + C_SURFACE + "; -fx-border-color: " + C_BORDER
                + "; -fx-border-width: 0 0 1 0;");
        row.getChildren().addAll(
            colHead("MODEL ADI",      260),
            colHead("TOPLAM",          80),
            colHead("BAŞARILI",        80),
            colHead("HATALI",          80),
            colHead("BAŞARI %",        90),
            colHead("SON DURUM",      120)
        );
        return row;
    }

    private void fillJobsTable(PrinterState p) {
        dJobsTable.getChildren().clear();
        List<String[]> models = DatabaseManager.getModelSummaries(p.displayName);

        if (models.isEmpty()) {
            HBox empty = new HBox();
            empty.setPadding(new Insets(32));
            empty.setAlignment(Pos.CENTER);
            Label msg = new Label("Henüz bu yazıcıda tamamlanan bir baskı kaydı yok.\nSimülasyonla test edebilirsiniz.");
            msg.setTextFill(Color.web(C_DIM));
            msg.setFont(Font.font("Segoe UI", 13));
            msg.setStyle("-fx-text-alignment: center;");
            empty.getChildren().add(msg);
            dJobsTable.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < models.size(); i++) {
            String[] m = models.get(i);
            String name  = m[0];
            int    total = Integer.parseInt(m[1]);
            int    succ  = Integer.parseInt(m[2]);
            int    fail  = Integer.parseInt(m[3]);
            double rate  = total > 0 ? succ * 100.0 / total : 0;

            String lastStatus = fail > 0 && succ == 0 ? "HATALI"
                              : fail > 0 ? "KARMA"
                              : "BAŞARILI";
            String statusColor = "BAŞARILI".equals(lastStatus) ? C_GREEN
                               : "HATALI".equals(lastStatus)   ? C_RED : C_AMBER;

            HBox row = new HBox(0);
            row.setPadding(new Insets(12, 20, 12, 20));
            row.setAlignment(Pos.CENTER_LEFT);
            if (i % 2 == 1)
                row.setStyle("-fx-background-color: " + C_SURFACE + ";");

            // Model adı (kısalt)
            String displayName = name != null && name.length() > 32
                    ? name.substring(0, 30) + "…" : (name != null ? name : "—");
            Label nameL = new Label(displayName);
            nameL.setFont(Font.font("Segoe UI", 12));
            nameL.setTextFill(Color.web(C_TEXT));
            nameL.setPrefWidth(260);

            Label totalL = numCell(String.valueOf(total), C_TEXT, 80);
            Label succL  = numCell(String.valueOf(succ),  C_GREEN, 80);
            Label failL  = numCell(String.valueOf(fail),  fail > 0 ? C_RED : C_DIM, 80);

            // Başarı yüzdesi ile mini bar
            VBox rateBox = new VBox(3);
            rateBox.setPrefWidth(90);
            Label rateL = new Label(String.format("%.0f%%", rate));
            rateL.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            rateL.setTextFill(Color.web(rate >= 80 ? C_GREEN : rate >= 50 ? C_AMBER : C_RED));
            ProgressBar rateBar = new ProgressBar(rate / 100.0);
            rateBar.setPrefWidth(70);
            rateBar.setPrefHeight(3);
            rateBar.setStyle("-fx-accent: " + (rate >= 80 ? C_GREEN : rate >= 50 ? C_AMBER : C_RED) + ";");
            rateBox.getChildren().addAll(rateL, rateBar);

            // Durum badge
            Label statusL = new Label(lastStatus);
            statusL.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            statusL.setPrefWidth(120);
            statusL.setStyle("-fx-background-color: " + statusColor + "22; -fx-text-fill: " + statusColor
                    + "; -fx-padding: 3 10 3 10; -fx-background-radius: 10;");

            row.getChildren().addAll(nameL, totalL, succL, failL, rateBox, statusL);
            dJobsTable.getChildren().add(row);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  RAPOR VIEW — tüm farm veya tek yazıcı
    // ════════════════════════════════════════════════════════════════════════

    private BorderPane buildReportView() {
        BorderPane pane = new BorderPane();
        pane.setStyle("-fx-background-color: " + C_BG + ";");

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(16, 28, 16, 28));
        top.setStyle("-fx-background-color: " + C_SURFACE + "; -fx-border-color: " + C_BORDER
                + "; -fx-border-width: 0 0 1 0;");

        Button back = topBtn("← Farm'a Dön");
        back.setOnAction(e -> { reportView.setVisible(false); farmView.setVisible(true); });
        Label title = new Label("RAPORLAR");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_BRIGHT));
        title.setId("reportTitle");

        top.getChildren().addAll(back, title);
        pane.setTop(top);

        VBox body = new VBox();
        body.setId("reportBody");
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: " + C_BG + "; -fx-background: " + C_BG + ";");
        pane.setCenter(scroll);
        return pane;
    }

    private void showReport() {
        farmView.setVisible(false);
        reportView.setVisible(true);
    }

    private void refreshReportView(PrinterState filter) {
        ScrollPane scroll = (ScrollPane) reportView.getCenter();
        VBox body = (VBox) scroll.getContent();
        body.getChildren().clear();
        body.setSpacing(0);

        HBox top = (HBox) reportView.getTop();
        Label title = (Label) top.getChildren().get(1);
        title.setText(filter == null ? "FARM GENEL RAPORU" : filter.displayName.toUpperCase() + " — RAPOR");

        VBox content = new VBox(28);
        content.setPadding(new Insets(32, 36, 40, 36));

        String pName = filter != null ? filter.displayName : null;

        // ── 1. Özet kartlar ─────────────────────────────────────────────────
        content.getChildren().add(reportSection("YAZICI PERFORMANS ÖZETİ"));

        List<DatabaseManager.PrinterSummary> sums = pName == null
                ? DatabaseManager.getPrinterSummaries()
                : asList(DatabaseManager.getSinglePrinterSummary(pName));

        if (sums.isEmpty()) {
            content.getChildren().add(emptyNote("Veri bulunamadı."));
        } else {
            FlowPane summGrid = new FlowPane();
            summGrid.setHgap(16); summGrid.setVgap(16);
            for (DatabaseManager.PrinterSummary s : sums) {
                if (s != null) summGrid.getChildren().add(buildReportCard(s));
            }
            content.getChildren().add(summGrid);
        }

        // ── 2. Son baskılar tablosu ──────────────────────────────────────────
        content.getChildren().add(reportSection("SON İŞLEMLER"));
        List<String[]> recent = DatabaseManager.getRecentJobs(30, pName);
        if (recent.isEmpty()) content.getChildren().add(emptyNote("Kayıt yok."));
        else content.getChildren().add(buildRecentTable(recent));

        // ── 3. Hata dağılımı ─────────────────────────────────────────────────
        content.getChildren().add(reportSection("HATA DAĞILIMI"));
        List<String[]> errors = DatabaseManager.getErrorBreakdown(pName);
        if (errors.isEmpty()) {
            Label ok = new Label("✓  Hiç hata kaydı yok — sistem sorunsuz çalışıyor.");
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
        card.setPrefWidth(320);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: " + C_CARD + "; -fx-border-color: " + C_BORDER
                + "; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label name = new Label(s.printerName);
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        name.setTextFill(Color.web(C_BRIGHT));

        double rate = s.totalJobs > 0 ? (s.successJobs * 100.0 / s.totalJobs) : 0;
        String rateColor = rate >= 80 ? C_GREEN : rate >= 50 ? C_AMBER : C_RED;

        ProgressBar bar = new ProgressBar(rate / 100.0);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(5);
        bar.setStyle("-fx-accent: " + rateColor + ";");

        HBox rateRow = new HBox();
        Label rL = new Label(String.format("Başarı: %.0f%%", rate));
        rL.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        rL.setTextFill(Color.web(rateColor));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label tL = new Label(s.totalJobs + " baskı");
        tL.setFont(Font.font("Segoe UI", 12));
        tL.setTextFill(Color.web(C_DIM));
        rateRow.getChildren().addAll(rL, sp, tL);

        VBox stats = new VBox(7);
        stats.getChildren().addAll(
            statLine("✓  Başarılı",   String.valueOf(s.successJobs), C_GREEN),
            statLine("✗  Hatalı",     String.valueOf(s.failJobs),    C_RED),
            statLine("⏱  Toplam Süre", formatSec(s.totalSeconds),    C_TEXT),
            statLine("🧵  Filament",   String.format("%.1f m", s.totalFilamentM), C_TEXT)
        );

        divider(card);
        card.getChildren().addAll(name, rateRow, bar, stats);
        return card;
    }

    private VBox buildRecentTable(List<String[]> rows) {
        VBox table = new VBox(0);
        table.setStyle("-fx-background-color: " + C_CARD + "; -fx-border-color: " + C_BORDER
                + "; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");

        double[] w = {160, 240, 90, 100, 110, 170};
        String[] heads = {"Yazıcı", "Model", "Durum", "Süre", "Filament", "Tarih"};

        HBox header = new HBox(0);
        header.setPadding(new Insets(10, 16, 10, 16));
        header.setStyle("-fx-background-color: " + C_SURFACE + "; -fx-border-color: " + C_BORDER
                + "; -fx-border-width: 0 0 1 0; -fx-background-radius: 8 8 0 0;");
        for (int i = 0; i < heads.length; i++) header.getChildren().add(colHead(heads[i], w[i]));
        table.getChildren().add(header);

        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            HBox row = new HBox(0);
            row.setPadding(new Insets(10, 16, 10, 16));
            row.setAlignment(Pos.CENTER_LEFT);
            if (i % 2 == 1) row.setStyle("-fx-background-color: " + C_SURFACE + ";");

            for (int j = 0; j < r.length; j++) {
                String val = r[j] != null ? r[j] : "—";
                Label l = new Label(val);
                l.setPrefWidth(w[j]);
                l.setWrapText(true);
                l.setFont(Font.font("Segoe UI", j == 2 ? FontWeight.BOLD : FontWeight.NORMAL, 12));
                if (j == 2) {
                    l.setTextFill(Color.web("BAŞARILI".equals(val) ? C_GREEN : "HATALI".equals(val) ? C_RED : C_AMBER));
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
        c.setStyle("-fx-background-color: " + C_CARD + "; -fx-border-color: " + C_BORDER
                + "; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
        int max = errors.stream().mapToInt(r -> Integer.parseInt(r[1])).max().orElse(1);
        for (String[] row : errors) {
            int cnt = Integer.parseInt(row[1]);
            HBox er = new HBox(12); er.setAlignment(Pos.CENTER_LEFT);
            Label el = new Label(row[0] != null ? row[0] : "Bilinmeyen");
            el.setPrefWidth(280); el.setFont(Font.font("Consolas", 11));
            el.setTextFill(Color.web(C_TEXT));
            ProgressBar eb = new ProgressBar((double) cnt / max);
            eb.setPrefWidth(260); eb.setPrefHeight(5);
            eb.setStyle("-fx-accent: " + C_RED + ";");
            Label cl = new Label(cnt + "×");
            cl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            cl.setTextFill(Color.web(C_RED));
            er.getChildren().addAll(el, eb, cl);
            c.getChildren().add(er);
        }
        return c;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TICK — 500ms güncelleme döngüsü
    // ════════════════════════════════════════════════════════════════════════

    private void tick() {
        List<PrinterState> printers = PrinterManager.getPrinters();

        // Sidebar butonlarını güncelle
        for (PrinterState p : printers) {
            Button btn = sidebarBtns.get(p.portName);
            if (btn == null) continue;
            // Buton içindeki VBox → HBox (ilk çocuk) → Circle (ilk çocuk)
            try {
                VBox wrap = (VBox) btn.getGraphic();
                HBox row  = (HBox) wrap.getChildren().get(0);
                Circle dot = (Circle) row.getChildren().get(0);
                dot.setFill(Color.web(p.connected ? C_GREEN : C_DIM));
                VBox info = (VBox) row.getChildren().get(1);
                Label tempL = (Label) info.getChildren().get(1);
                tempL.setText(p.nozzleCurrent + "° / " + p.bedCurrent + "°");
                ProgressBar mini = (ProgressBar) wrap.getChildren().get(1);
                mini.setProgress(p.progressPercent / 100.0);
            } catch (Exception ignored) {}
        }

        // Üst bar sayaçları
        long online   = printers.stream().filter(p -> p.connected).count();
        long printing = printers.stream().filter(p -> p.isPrinting).count();
        barOnline.setText(String.valueOf(online));
        barPrinting.setText(String.valueOf(printing));

        // Detay paneli canlı güncelle
        if (selectedPrinter != null && detailPane.getChildren().size() > 1) {
            PrinterState p = selectedPrinter;

            if (dNozzleCur != null) {
                dNozzleCur.setText(p.nozzleCurrent + "°C");
                dNozzleTgt.setText("→ " + p.nozzleTarget + "°C");
                dBedCur.setText(p.bedCurrent + "°C");
                dBedTgt.setText("→ " + p.bedTarget + "°C");

                dProgressBar.setProgress(p.progressPercent / 100.0);
                dProgress.setText(p.progressPercent + "%");
                dElapsed.setText(p.elapsedTime);
                dRaw.setText(p.rawData != null ? "> " + p.rawData : ">");

                // Maliyet
                if (p.lastCostBreakdown != null) {
                    CostCalculator.CostBreakdown cb = p.lastCostBreakdown;
                    dCostFilament.setText(String.format("%.1fg / %.2f₺", cb.filamentGrams, cb.filamentCostTL));
                    dCostElec.setText(String.format("%.2f₺", cb.electricityCostTL));
                    dCostTotal.setText(String.format("%.2f₺", cb.totalCostTL));
                    dCostPrice.setText(String.format("%.2f₺", cb.sellingPriceTL));
                    boolean est = cb.isEstimate;
                    dCostTag.setText(est ? "TAHMİNİ" : "GERÇEK");
                    dCostTag.setStyle("-fx-background-color: " + (est ? "#252535" : "#102820") + "; "
                            + "-fx-text-fill: " + (est ? C_DIM : C_GREEN) + "; "
                            + "-fx-padding: 2 7 2 7; -fx-background-radius: 8; "
                            + "-fx-font-size: 9px; -fx-font-weight: bold;");
                }

                // Model tablosunu yenile (basit: her saniyede bir)
                fillJobsTable(p);
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  YARDIMCI METODLAR
    // ════════════════════════════════════════════════════════════════════════

    private VBox card() {
        VBox c = new VBox();
        c.setPadding(new Insets(16));
        c.setStyle("-fx-background-color: " + C_CARD + "; -fx-border-color: " + C_BORDER
                + "; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
        return c;
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        l.setTextFill(Color.web(C_DIM));
        return l;
    }

    private Label reportSection(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        l.setTextFill(Color.web(C_DIM));
        l.setPadding(new Insets(0, 0, 8, 0));
        return l;
    }

    private Label colHead(String text, double width) {
        Label l = new Label(text);
        l.setPrefWidth(width);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        l.setTextFill(Color.web(C_DIM));
        return l;
    }

    private Label numCell(String val, String color, double width) {
        Label l = new Label(val);
        l.setPrefWidth(width);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        l.setTextFill(Color.web(color));
        return l;
    }

    private HBox statLine(String lbl, String val, String color) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(lbl);
        l.setFont(Font.font("Segoe UI", 11));
        l.setTextFill(Color.web(C_DIM));
        l.setPrefWidth(140);
        Label v = new Label(val);
        v.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        v.setTextFill(Color.web(color));
        row.getChildren().addAll(l, v);
        return row;
    }

    private Button topBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + C_CARD + "; -fx-text-fill: " + C_TEXT + "; "
                + "-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-font-weight: bold; "
                + "-fx-padding: 7 14 7 14; -fx-background-radius: 5; "
                + "-fx-border-color: " + C_BORDER + "; -fx-border-radius: 5; -fx-border-width: 1;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: " + C_SEL + "; -fx-text-fill: " + C_BRIGHT + "; "
                + "-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-font-weight: bold; "
                + "-fx-padding: 7 14 7 14; -fx-background-radius: 5; -fx-cursor: hand; "
                + "-fx-border-color: " + C_BLUE + "; -fx-border-radius: 5; -fx-border-width: 1;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: " + C_CARD + "; -fx-text-fill: " + C_TEXT + "; "
                + "-fx-font-family: 'Segoe UI'; -fx-font-size: 12px; -fx-font-weight: bold; "
                + "-fx-padding: 7 14 7 14; -fx-background-radius: 5; "
                + "-fx-border-color: " + C_BORDER + "; -fx-border-radius: 5; -fx-border-width: 1;"));
        return b;
    }

    private Button smallBtn(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; "
                + "-fx-font-family: 'Segoe UI'; -fx-font-size: 10px; -fx-font-weight: bold; "
                + "-fx-padding: 5 10 5 10; -fx-background-radius: 4; -fx-cursor: hand;");
        return b;
    }

    private Label emptyNote(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", 13));
        l.setTextFill(Color.web(C_DIM));
        l.setPadding(new Insets(20));
        return l;
    }

    private void divider(VBox parent) {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setMaxWidth(Double.MAX_VALUE);
        r.setStyle("-fx-background-color: " + C_BORDER + ";");
        VBox.setMargin(r, new Insets(4, 0, 4, 0));
        parent.getChildren().add(r);
    }

    private String formatSec(long secs) {
        if (secs <= 0) return "—";
        long h = secs / 3600, m = (secs % 3600) / 60;
        return h > 0 ? h + "s " + m + "d" : m + "d";
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> asList(T item) {
        List<T> l = new java.util.ArrayList<>();
        if (item != null) l.add(item);
        return l;
    }

    // ── GridPane ColumnConstraints yardımcısı ────────────────────────────────
    private static class ColumnConstraints extends javafx.scene.layout.ColumnConstraints {
        ColumnConstraints() { super(); }
    }
}