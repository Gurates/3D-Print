package printer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class FarmUI extends Application {

    private StackPane mainContainer;
    private BorderPane dashboardScreen, detailScreen, analyticsScreen;

    // Canlı Veri Elemanları
    private Label nozzleCurrentLabel, nozzleTargetLabel;
    private Label bedCurrentLabel, bedTargetLabel;
    private Label timeLabel, rawLabel;
    private ProgressBar progressBar;
    
    private VBox analyticsChartContainer;

    @Override
    public void start(Stage primaryStage) {
        mainContainer = new StackPane();
        mainContainer.setStyle("-fx-background-color: #141418;"); // Derin karanlık arka plan

        // Sahneleri Oluştur
        dashboardScreen = createDashboardScreen();
        detailScreen = createPrinterDetailScreen();
        analyticsScreen = createAnalyticsScreen();

        // Başlangıçta sadece Dashboard görünür olsun
        detailScreen.setVisible(false);
        analyticsScreen.setVisible(false);

        mainContainer.getChildren().addAll(analyticsScreen, detailScreen, dashboardScreen);

        // Tam ekran ve Pencere ayarları
        Scene scene = new Scene(mainContainer, 1280, 720);
        primaryStage.setTitle("Monitor");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true); // Tam ekran başlatır
        primaryStage.show();

        // Arka plan seri port okumasını başlat
        Thread serialThread = new Thread(PrinterManager::startMonitoring);
        serialThread.setDaemon(true);
        serialThread.start();

        // JavaFX Timeline (Swing Timer yerine kullanılır, pürüzsüz UI günceller)
        Timeline uiUpdater = new Timeline(new KeyFrame(Duration.millis(500), e -> updateLiveUI()));
        uiUpdater.setCycleCount(Timeline.INDEFINITE);
        uiUpdater.play();
    }

    // Ekranlar arası geçiş fonksiyonu
    private void switchScreen(BorderPane screenToShow) {
        dashboardScreen.setVisible(false);
        detailScreen.setVisible(false);
        analyticsScreen.setVisible(false);
        screenToShow.setVisible(true);
    }

    // =====================================================================
    // SAHNE 1: DASHBOARD
    // =====================================================================
    private BorderPane createDashboardScreen() {
        BorderPane pane = new BorderPane();
        
        Label title = new Label("DASHBOARD");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
        title.setTextFill(Color.WHITE);
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(50, 0, 50, 0));
        pane.setTop(title);

        FlowPane grid = new FlowPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(40);
        grid.setVgap(40);

        VBox printerCard = createPrinterCard("Ender 3 Neo", "COM Port Bağlı", "#2ecc71");
        printerCard.setOnMouseClicked(e -> switchScreen(detailScreen));
        
        grid.getChildren().add(printerCard);
        pane.setCenter(grid);

        return pane;
    }

    private VBox createPrinterCard(String name, String status, String colorHex) {
        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(280, 220);
        card.setStyle("-fx-background-color: #232328; -fx-border-color: #3c3c46; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");

        // Hover Efekti (Mouse üstüne gelince)
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #2f2f36; -fx-border-color: #4a4a55; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #232328; -fx-border-color: #3c3c46; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;"));

        Label icon = new Label("🖨️");
        icon.setFont(Font.font("Segoe UI", 48));

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        nameLabel.setTextFill(Color.WHITE);

        Label statusLabel = new Label("● " + status);
        statusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        statusLabel.setTextFill(Color.web(colorHex));

        card.getChildren().addAll(icon, nameLabel, statusLabel);
        return card;
    }

    // =====================================================================
    // SAHNE 2: DETAY EKRANI
    // =====================================================================
    private BorderPane createPrinterDetailScreen() {
        BorderPane pane = new BorderPane();

        // Üst Menü
        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(20, 40, 20, 40));
        topBar.setStyle("-fx-background-color: #1e1e24;");

        Button backBtn = new Button("◀ Ana Ekrana Dön");
        styleButton(backBtn, "#464650");
        backBtn.setOnAction(e -> switchScreen(dashboardScreen));

        Label title = new Label("Ender 3 Neo - Canlı Takip");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.WHITE);

        topBar.getChildren().addAll(backBtn, title);
        pane.setTop(topBar);

        // Merkez Veri Alanı
        VBox centerBox = new VBox(30);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setMaxWidth(700);
        centerBox.setStyle("-fx-background-color: #1e1e24; -fx-background-radius: 15; -fx-border-color: #3c3c46; -fx-border-radius: 15; -fx-padding: 40;");

        // Sıcaklık Kutuları Yan Yana
        HBox tempsBox = new HBox(30);
        tempsBox.setAlignment(Pos.CENTER);

        VBox nozzleBox = createTempBox("NOZZLE", "#ff6b6b");
        nozzleCurrentLabel = (Label) nozzleBox.getChildren().get(1);
        nozzleTargetLabel = (Label) nozzleBox.getChildren().get(2);

        VBox bedBox = createTempBox("TABLA", "#4dadf7");
        bedCurrentLabel = (Label) bedBox.getChildren().get(1);
        bedTargetLabel = (Label) bedBox.getChildren().get(2);

        tempsBox.getChildren().addAll(nozzleBox, bedBox);

        // Progress Bar
        progressBar = new ProgressBar(0);
        progressBar.setPrefSize(600, 30);
        progressBar.setStyle("-fx-accent: #2ecc71;"); // İlerleme rengi

        timeLabel = new Label("Geçen Süre: 00:00:00");
        timeLabel.setFont(Font.font("Segoe UI", 18));
        timeLabel.setTextFill(Color.web("#f5a623"));

        Button analyticsBtn = new Button("📊 İstatistikleri ve Hata Geçmişini Gör");
        styleButton(analyticsBtn, "#6c5ce7");
        analyticsBtn.setPrefWidth(600);
        analyticsBtn.setOnAction(e -> {
            refreshAnalyticsChart();
            switchScreen(analyticsScreen);
        });

        rawLabel = new Label("> Bekleniyor...");
        rawLabel.setFont(Font.font("Consolas", 14));
        rawLabel.setTextFill(Color.GRAY);

        centerBox.getChildren().addAll(tempsBox, new Label("Baskı İlerlemesi:"){{setTextFill(Color.LIGHTGRAY);}}, progressBar, timeLabel, analyticsBtn, rawLabel);
        
        // Merkeze oturtmak için bir Wrapper kullanıyoruz
        StackPane wrapper = new StackPane(centerBox);
        pane.setCenter(wrapper);

        return pane;
    }

    private VBox createTempBox(String title, String colorHex) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(250, 120);
        box.setStyle("-fx-background-color: #26262d; -fx-border-color: #3c3c46; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label titleLabel = new Label(title);
        titleLabel.setTextFill(Color.GRAY);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        Label currentLabel = new Label("-- °C");
        currentLabel.setTextFill(Color.web(colorHex));
        currentLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));

        Label targetLabel = new Label("Hedef: -- °C");
        targetLabel.setTextFill(Color.LIGHTGRAY);

        box.getChildren().addAll(titleLabel, currentLabel, targetLabel);
        return box;
    }

    // =====================================================================
    // SAHNE 3: JAVAFX NATIVE GRAFİK EKRANI
    // =====================================================================
    private BorderPane createAnalyticsScreen() {
        BorderPane pane = new BorderPane();

        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(20, 40, 20, 40));
        topBar.setStyle("-fx-background-color: #1e1e24;");

        Button backBtn = new Button("◀ Yazıcıya Dön");
        styleButton(backBtn, "#464650");
        backBtn.setOnAction(e -> switchScreen(detailScreen));

        Label title = new Label("Sistem Hata Raporları");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.WHITE);

        topBar.getChildren().addAll(backBtn, title);
        pane.setTop(topBar);

        analyticsChartContainer = new VBox();
        analyticsChartContainer.setAlignment(Pos.CENTER);
        pane.setCenter(analyticsChartContainer);

        return pane;
    }

    private void refreshAnalyticsChart() {
        analyticsChartContainer.getChildren().clear();

        // JavaFX X ve Y Eksenlerini oluştur
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Model Adı");
        xAxis.setTickLabelFill(Color.WHITE);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Hata Sayısı");
        yAxis.setTickLabelFill(Color.WHITE);

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("İptal Edilen / Hatalı Baskılar");
        barChart.setLegendVisible(false);
        barChart.setStyle("-fx-background-color: transparent;"); // Koyu temaya uygun şeffaflık
        barChart.setMaxSize(1000, 600);

        XYChart.Series<String, Number> dataSeries = new XYChart.Series<>();

        String query = "SELECT print_name, COUNT(*) as fail_count FROM print_jobs WHERE status = 'HATALI' GROUP BY print_name";
        boolean hasData = false;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:farm_stats.db");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                hasData = true;
                dataSeries.getData().add(new XYChart.Data<>(rs.getString("print_name"), rs.getInt("fail_count")));
            }
        } catch (Exception e) {
            System.out.println("[Grafik Hata] " + e.getMessage());
        }

        if (hasData) {
            barChart.getData().add(dataSeries);
            analyticsChartContainer.getChildren().add(barChart);
        } else {
            Label noDataLabel = new Label("Hatalı baskı kaydı bulunamadı. Farm sorunsuz çalışıyor! 🚀");
            noDataLabel.setTextFill(Color.web("#2ecc71"));
            noDataLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
            analyticsChartContainer.getChildren().add(noDataLabel);
        }
    }

    // =====================================================================
    // GÜNCELLEME VE STİL
    // =====================================================================
    private void updateLiveUI() {
        nozzleCurrentLabel.setText(PrinterState.nozzleCurrent + " °C");
        nozzleTargetLabel.setText("Hedef: " + PrinterState.nozzleTarget + " °C");
        
        bedCurrentLabel.setText(PrinterState.bedCurrent + " °C");
        bedTargetLabel.setText("Hedef: " + PrinterState.bedTarget + " °C");

        progressBar.setProgress(PrinterState.progressPercent / 100.0); // JavaFX progress 0.0 - 1.0 arası çalışır
        timeLabel.setText("Geçen Süre: " + PrinterState.elapsedTime);
        
        if (PrinterState.rawData != null && !PrinterState.rawData.isEmpty()) {
            rawLabel.setText("> " + PrinterState.rawData);
        }
    }

    private void styleButton(Button btn, String colorHex) {
        btn.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 20 10 20; -fx-background-radius: 5;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: derive(" + colorHex + ", 20%); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 20 10 20; -fx-background-radius: 5; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 20 10 20; -fx-background-radius: 5;"));
    }
}