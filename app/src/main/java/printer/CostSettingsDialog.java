package printer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CostSettingsDialog {

    private static final String BG    = "#141418";
    private static final String CARD  = "#1a1a22";
    private static final String BDR   = "#2e2e3e";
    private static final String MAIN  = "#e8e8f0";
    private static final String DIM   = "#8888a0";
    private static final String GREEN = "#00d68f";
    private static final String BLUE  = "#4d9fff";

    private final Stage dialog;
    private CostConfig config;
    private TextField tfFilamentPrice;
    private TextField tfElectricity;
    private TextField tfLabor;
    private TextField tfMargin;
    private TextField tfDensity;
    private Label     previewLabel;

    public CostSettingsDialog(Stage owner) {
        dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("Maliyet Ayarları");
        dialog.setResizable(false);

        config = DatabaseManager.loadCostConfig();
        dialog.setScene(new Scene(buildContent(), 520, 560));
    }

    public void show() { dialog.showAndWait(); }

    private VBox buildContent() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + BG + ";");

        HBox header = new HBox();
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setStyle("-fx-background-color: " + CARD + "; -fx-border-color: " + BDR + "; -fx-border-width: 0 0 1 0;");
        Label title = new Label("⚙  Maliyet Ayarları");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(MAIN));
        header.getChildren().add(title);
        VBox form = new VBox(18);
        form.setPadding(new Insets(28, 24, 24, 24));

        form.getChildren().addAll(
            fieldRow("Filament fiyatı (TL/kg)",
                "Örn: 500g makara 300 TL ise → 600 girin",
                tfFilamentPrice = numField(config.filamentPricePerKg)),

            fieldRow("Elektrik maliyeti (TL/saat)",
                "~200W yazıcı, 3.5 TL/kWh = ~0.70 TL/sa",
                tfElectricity = numField(config.electricityCostPerHour)),

            fieldRow("İşçilik / amortisman (TL/saat)",
                "Sıfır bırakabilirsiniz",
                tfLabor = numField(config.laborCostPerHour)),

            fieldRow("Kar marjı (%)",
                "Satış fiyatı = maliyet × (1 + marj/100)",
                tfMargin = numField(config.profitMarginPercent)),

            fieldRow("Filament yoğunluğu (g/cm³)",
                "PLA: 1.24 | PETG: 1.27 | ABS: 1.04 | TPU: 1.20",
                tfDensity = numField(config.filamentDensity))
        );
        VBox preview = new VBox(8);
        preview.setPadding(new Insets(16));
        preview.setStyle("-fx-background-color: " + CARD + "; -fx-border-color: " + BDR +
                         "; -fx-border-radius: 8; -fx-background-radius: 8;");
        Label prevTitle = new Label("ÖRNEK HESAPLAMA");
        prevTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        prevTitle.setTextFill(Color.web(DIM));
        previewLabel = new Label();
        previewLabel.setFont(Font.font("Consolas", 13));
        previewLabel.setTextFill(Color.web(GREEN));
        preview.getChildren().addAll(prevTitle, previewLabel);
        form.getChildren().add(preview);

        for (TextField tf : new TextField[]{tfFilamentPrice, tfElectricity, tfLabor, tfMargin, tfDensity}) {
            tf.textProperty().addListener((obs, o, n) -> updatePreview());
        }
        updatePreview();
        HBox buttons = new HBox(12);
        buttons.setPadding(new Insets(0, 24, 24, 24));
        buttons.setAlignment(Pos.CENTER_RIGHT);

        Button cancel = new Button("İptal");
        styleBtn(cancel, BDR);
        cancel.setOnAction(e -> dialog.close());

        Button save = new Button("Kaydet");
        styleBtn(save, BLUE);
        save.setOnAction(e -> saveAndClose());

        buttons.getChildren().addAll(cancel, save);

        root.getChildren().addAll(header, form, buttons);
        return root;
    }

    private VBox fieldRow(String label, String hint, TextField field) {
        VBox box = new VBox(5);
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lbl.setTextFill(Color.web(MAIN));
        Label hintLbl = new Label(hint);
        hintLbl.setFont(Font.font("Segoe UI", 11));
        hintLbl.setTextFill(Color.web(DIM));
        box.getChildren().addAll(lbl, hintLbl, field);
        return box;
    }

    private TextField numField(double value) {
        TextField tf = new TextField(String.format("%.2f", value).replace(",", "."));
        tf.setStyle("-fx-background-color: #232330; -fx-text-fill: " + MAIN + "; " +
                    "-fx-border-color: " + BDR + "; -fx-border-radius: 5; " +
                    "-fx-background-radius: 5; -fx-font-size: 14px; -fx-padding: 8 12 8 12;");
        tf.setPrefWidth(200);
        return tf;
    }

    private void updatePreview() {
        try {
            CostConfig tmp = readFields();
            CostCalculator calc = new CostCalculator(tmp);
            double exampleFilamentMm = 100.0 / tmp.mmToGrams(1.0);
            CostCalculator.CostBreakdown ex = calc.calculate(exampleFilamentMm, 7200);

            previewLabel.setText(String.format(
                "100g filament + 2 saatlik baskı:%n" +
                "  Filament: %.2f TL%n" +
                "  Elektrik: %.2f TL%n" +
                "  İşçilik : %.2f TL%n" +
                "  TOPLAM  : %.2f TL%n" +
                "  Satış   : %.2f TL (%%%.0f marj ile)",
                ex.filamentCostTL, ex.electricityCostTL, ex.laborCostTL,
                ex.totalCostTL, ex.sellingPriceTL, tmp.profitMarginPercent
            ));
        } catch (Exception e) {
            previewLabel.setText("Geçersiz değer — lütfen sayı girin.");
            previewLabel.setTextFill(Color.web("#ff4d6d"));
            return;
        }
        previewLabel.setTextFill(Color.web(GREEN));
    }

    private void saveAndClose() {
        try {
            CostConfig cfg = readFields();
            DatabaseManager.saveCostConfig(cfg);
            dialog.close();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Geçersiz değer: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private CostConfig readFields() {
        CostConfig cfg = new CostConfig();
        cfg.filamentPricePerKg    = Double.parseDouble(tfFilamentPrice.getText().replace(",", ".").trim());
        cfg.electricityCostPerHour = Double.parseDouble(tfElectricity.getText().replace(",", ".").trim());
        cfg.laborCostPerHour       = Double.parseDouble(tfLabor.getText().replace(",", ".").trim());
        cfg.profitMarginPercent    = Double.parseDouble(tfMargin.getText().replace(",", ".").trim());
        cfg.filamentDensity        = Double.parseDouble(tfDensity.getText().replace(",", ".").trim());
        return cfg;
    }

    private void styleBtn(Button btn, String color) {
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white;" +
                     "-fx-font-weight: bold; -fx-font-size: 13px;" +
                     "-fx-padding: 9 20 9 20; -fx-background-radius: 6;");
    }
}