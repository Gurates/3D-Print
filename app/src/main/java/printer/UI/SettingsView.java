package printer.UI;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import printer.CostCalculator;
import printer.CostConfig;
import printer.*;


public class SettingsView extends BorderPane {

    public SettingsView() {
        setStyle("-fx-background-color:" + Theme.C_BG + ";");
        HBox subBar = new HBox(12);
        subBar.setAlignment(Pos.CENTER_LEFT);
        subBar.setPadding(new Insets(12, 28, 12, 28));
        subBar.setStyle("-fx-background-color:" + Theme.C_SURFACE + "; -fx-border-color:" + Theme.C_BORDER + "; -fx-border-width:0 0 1 0;");
        Label title = new Label("SETTINGS");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        title.setTextFill(Color.web(Theme.C_BRIGHT));
        subBar.getChildren().add(title);
        setTop(subBar);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:" + Theme.C_BG + "; -fx-background:" + Theme.C_BG + ";");

        HBox body = new HBox(24);
        body.setPadding(new Insets(36));
        body.setAlignment(Pos.TOP_LEFT);
        body.getChildren().addAll(buildTelegramCard(), buildCostCard());

        scroll.setContent(body);
        setCenter(scroll);
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
        info.setTextFill(Color.web(Theme.C_DIM)); 
        info.setWrapText(true);

        TextField tokenField = settingsField(DatabaseManager.getSetting("telegram_bot_token", ""), "1234567890:ABCdef...");
        TextField chatField  = settingsField(DatabaseManager.getSetting("telegram_chat_id", ""),   "123456789");
        
        Label status = new Label(""); 
        status.setFont(Font.font("Segoe UI", 12));

        // GERİ EKLENEN BUTONLAR
        Button saveBtn = accentBtn("💾 Save", Theme.C_BLUE);
        Button testBtn = accentBtn("📨 Send Test Message", "#2a2a3e");

        saveBtn.setOnAction(e -> {
            DatabaseManager.saveSetting("telegram_bot_token", tokenField.getText().trim());
            DatabaseManager.saveSetting("telegram_chat_id",   chatField.getText().trim());
            status.setText("✓ Saved"); 
            status.setTextFill(Color.web(Theme.C_GREEN));
            new Timeline(new KeyFrame(Duration.seconds(3), ev -> status.setText(""))).play();
        });

        testBtn.setOnAction(e -> {
            String token = tokenField.getText().trim();
            String chat  = chatField.getText().trim();
            if (token.isEmpty() || chat.isEmpty()) {
                status.setText("⚠ Token and Chat ID cannot be empty");
                status.setTextFill(Color.web(Theme.C_AMBER));
                return;
            }
            DatabaseManager.saveSetting("telegram_bot_token", token);
            DatabaseManager.saveSetting("telegram_chat_id", chat);
            TelegramNotifier.sendMessage("✅ <b>3D Farm Monitor</b> connection test successful!\n\nNotifications active.");
            status.setText("✓ Test message sent"); 
            status.setTextFill(Color.web(Theme.C_GREEN));
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
        info.setTextFill(Color.web(Theme.C_DIM));
        info.setWrapText(true);

        CostConfig cfg = DatabaseManager.loadCostConfig();
        TextField tfFilament    = settingsField(fmt(cfg.filamentPricePerKg), "600.00");
        TextField tfElectricity = settingsField(fmt(cfg.electricityCostPerHour), "0.70");
        TextField tfLabor       = settingsField(fmt(cfg.laborCostPerHour), "0.00");
        TextField tfMargin      = settingsField(fmt(cfg.profitMarginPercent), "30.00");
        TextField tfDensity     = settingsField(fmt(cfg.filamentDensity), "1.24");

        // GERİ EKLENEN CANLI ÖNİZLEME KUTUSU (LIVE PREVIEW)
        Label previewLbl = new Label("Calculating example...");
        previewLbl.setFont(Font.font("Consolas", 12));
        previewLbl.setTextFill(Color.web(Theme.C_GREEN));
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
                previewLbl.setTextFill(Color.web(Theme.C_GREEN));
            } catch (Exception ex) {
                previewLbl.setText("⚠ Invalid value");
                previewLbl.setTextFill(Color.web(Theme.C_AMBER));
            }
        };

        for (TextField tf : new TextField[]{tfFilament, tfElectricity, tfLabor, tfMargin, tfDensity})
            tf.textProperty().addListener((o, ov, nv) -> updatePreview.run());
        updatePreview.run();

        Label costStatus = new Label(""); 
        costStatus.setFont(Font.font("Segoe UI", 12));
        
        Button saveBtn = accentBtn("💾 Save", Theme.C_BLUE);
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
                costStatus.setTextFill(Color.web(Theme.C_GREEN));
                new Timeline(new KeyFrame(Duration.seconds(3), ev -> costStatus.setText(""))).play();
            } catch (Exception ex) {
                costStatus.setText("⚠ " + ex.getMessage()); 
                costStatus.setTextFill(Color.web(Theme.C_RED));
            }
        });

        VBox previewBox = new VBox(8);
        previewBox.setPadding(new Insets(14));
        previewBox.setStyle("-fx-background-color:#080810; -fx-border-color:" + Theme.C_BORDER + "; -fx-border-radius:6; -fx-background-radius:6;");
        Label prevTitle = new Label("LIVE PREVIEW");
        prevTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
        prevTitle.setTextFill(Color.web(Theme.C_DIM));
        previewBox.getChildren().addAll(prevTitle, previewLbl);

        card.getChildren().addAll(
            info, settingsDivider(),
            settingsFieldRow("Filament (₺/kg)", tfFilament),
            settingsFieldRow("Electricity (₺/hour)", tfElectricity),
            settingsFieldRow("Labor (₺/hour)", tfLabor),
            settingsFieldRow("Profit Margin (%)", tfMargin),
            settingsFieldRow("Filament Density", tfDensity),
            previewBox, saveBtn, costStatus
        );
        return card;
    }

    private VBox settingsCard(String title) {
        VBox card = new VBox(14); card.setPadding(new Insets(24)); card.setPrefWidth(420);
        card.setStyle("-fx-background-color:" + Theme.C_CARD + "; -fx-border-color:" + Theme.C_BORDER + "; -fx-border-width:1; -fx-border-radius:10; -fx-background-radius:10;");
        Label lbl = new Label(title); lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13)); lbl.setTextFill(Color.web(Theme.C_BRIGHT));
        card.getChildren().add(lbl); return card;
    }

    private VBox settingsFieldRow(String label, TextField field) {
        VBox box = new VBox(5);
        Label lbl = new Label(label); lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11)); lbl.setTextFill(Color.web(Theme.C_TEXT));
        box.getChildren().addAll(lbl, field); return box;
    }

    private TextField settingsField(String value, String prompt) {
        TextField tf = new TextField(value); tf.setPromptText(prompt);
        String base = "-fx-background-color:#080810; -fx-text-fill:" + Theme.C_BRIGHT + "; -fx-border-radius:5; -fx-background-radius:5; -fx-font-size:13px; -fx-padding:8 12 8 12;";
        tf.setStyle(base + "-fx-border-color:" + Theme.C_BORDER + ";");
        tf.focusedProperty().addListener((o, ov, focused) -> tf.setStyle(base + "-fx-border-color:" + (focused ? Theme.C_BLUE : Theme.C_BORDER) + ";"));
        return tf;
    }

    private Button accentBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white; -fx-font-family:'Segoe UI'; -fx-font-weight:bold; -fx-font-size:12px; -fx-padding:9 20 9 20; -fx-background-radius:5; -fx-cursor:hand;");
        return b;
    }

    private Region settingsDivider() {
        Region r = new Region(); r.setPrefHeight(1); r.setMaxWidth(Double.MAX_VALUE); r.setStyle("-fx-background-color:" + Theme.C_BORDER + ";");
        VBox.setMargin(r, new Insets(2, 0, 2, 0)); return r;
    }

    private String fmt(double v) { return String.format("%.2f", v).replace(",", "."); }
}