package com.example.demo1;

import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import java.util.Arrays;

public class RobotVacuumView {
    private final Canvas canvas;
    // Haritanın yüksekliğini ekranınıza daha iyi oturtmak için marginY biraz küçültüldü
    private final int cellSize = 42;
    private final int marginX = 35;
    private final int marginY = 25;

    private double visualX = -1;
    private double visualY = -1;
    private double visualAngle = 0;

    private final VBox leftPanel;
    private final HBox bottomPanel;
    private final HBox headerPanel;

    public final ToggleButton btnAddDirt = new ToggleButton("❄ Kir Ekle");
    public final ToggleButton btnAddFurn = new ToggleButton("🛋 Mobilya Ekle");
    public final ToggleGroup toolGroup = new ToggleGroup();

    public final ToggleGroup dirtGroup = new ToggleGroup();
    public final ToggleButton rbDust = new ToggleButton("Toz");
    public final ToggleButton rbLiquid = new ToggleButton("Sıvı");
    public final ToggleButton rbStain = new ToggleButton("Leke");

    public final ToggleGroup algoGroup = new ToggleGroup();
    public final RadioButton rbAkilli = new RadioButton("Akıllı (LIDAR)");
    public final RadioButton rbRandom = new RadioButton("Rastgele");
    public final RadioButton rbSpiral = new RadioButton("Spiral");
    public final RadioButton rbWall = new RadioButton("Duvar Takip");

    public final Slider speedSlider = new Slider(1.0, 3.0, 1.5);
    public final Label speedValueLabel = new Label("1.5x");

    public final ProgressBar batteryBar = new ProgressBar(1.0);
    public final Label lblPos = new Label();
    public final Label lblDir = new Label();
    public final Label lblBatteryPercent = new Label();

    public final Label lblTotalArea = new Label();
    public final Label lblCleanedArea = new Label();
    public final Label lblRemainingArea = new Label();
    public final Label lblTime = new Label();
    public final Label lblDustCollected = new Label();

    public final Button btnStart = new Button("▶ Başlat");
    public final Button btnPause = new Button("⏸ Duraklat");
    public final Button btnReset = new Button("⏹ Sıfırla");
    public final Button btnReturn = new Button("🏠 İstasyona Dön");

    private final String colorBg = "#0B1120";
    private final String colorPanelBg = "#151F32";
    private final String colorTextMuted = "#94A3B8";
    private final String colorSuccess = "#10B981";

    public RobotVacuumView() {
        canvas = new Canvas(VacuumModel.COLS * cellSize + marginX * 2, VacuumModel.ROWS * cellSize + marginY * 2);
        leftPanel = new VBox(8);
        bottomPanel = new HBox(30);
        headerPanel = new HBox();
        setupHeader();
        setupLeftPanel();
        setupBottomPanel();
    }

    private void setupHeader() {
        headerPanel.setPadding(new Insets(10, 20, 10, 20)); // Dikey boşluk daraltıldı
        headerPanel.setAlignment(Pos.CENTER);
        headerPanel.setStyle("-fx-background-color: " + colorBg + ";");
        headerPanel.setMinHeight(50); // Minimum yükseklik koruması

        Label title = new Label("🤖 Akıllı Robot Süpürge Simülasyonu ✨");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        headerPanel.getChildren().add(title);
    }

    private void setupLeftPanel() {
        leftPanel.setPadding(new Insets(15));
        leftPanel.setPrefWidth(290);
        leftPanel.setMinWidth(290); // Sol panel daralmasın
        leftPanel.setStyle("-fx-background-color: " + colorPanelBg + "; -fx-border-color: #1E293B; -fx-border-width: 0 1 0 0;");

        String toggleStyleBase = "-fx-background-radius: 6; -fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 8 12; -fx-cursor: hand;";
        String blueBtnStyle = toggleStyleBase + "-fx-background-color: linear-gradient(to bottom, #3B82F6, #1D4ED8);";
        String greenBtnStyle = toggleStyleBase + "-fx-background-color: linear-gradient(to bottom, #34D399, #059669);";
        String inactiveBtnStyle = toggleStyleBase + "-fx-background-color: #1E293B; -fx-text-fill: #94A3B8; -fx-border-color: #334155; -fx-border-radius: 6;";
        String darkBtnStyle = toggleStyleBase + "-fx-background-color: #1E293B; -fx-border-color: #334155; -fx-border-radius: 6;";
        String redBtnStyle = toggleStyleBase + "-fx-background-color: linear-gradient(to bottom, #EF4444, #B91C1C);";

        Label titleTools = createSectionTitle("🔧 Araçlar");

        btnAddDirt.setToggleGroup(toolGroup);
        btnAddFurn.setToggleGroup(toolGroup);
        btnAddDirt.setMaxWidth(Double.MAX_VALUE);
        btnAddFurn.setMaxWidth(Double.MAX_VALUE);
        btnAddDirt.setSelected(true);

        updateToolButtonStyles(blueBtnStyle, greenBtnStyle, inactiveBtnStyle);
        toolGroup.selectedToggleProperty().addListener((obs, old, val) -> {
            if (val == null) old.setSelected(true);
            updateToolButtonStyles(blueBtnStyle, greenBtnStyle, inactiveBtnStyle);
        });

        Label dirtTypeLabel = new Label("Kir Türü");
        dirtTypeLabel.setTextFill(Color.web(colorTextMuted));
        dirtTypeLabel.setFont(Font.font("Segoe UI", 11));

        rbDust.setToggleGroup(dirtGroup); rbLiquid.setToggleGroup(dirtGroup); rbStain.setToggleGroup(dirtGroup);
        rbDust.setSelected(true);
        HBox dirtOptions = new HBox(5, rbDust, rbLiquid, rbStain);
        dirtOptions.setAlignment(Pos.CENTER);
        for(ToggleButton tb : Arrays.asList(rbDust, rbLiquid, rbStain)) {
            tb.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: #334155; -fx-border-radius: 5; -fx-padding: 4 8; -fx-cursor: hand;");
            tb.selectedProperty().addListener((obs, old, val) ->
                    tb.setStyle(val ? "-fx-background-color: #1D4ED8; -fx-text-fill: white; -fx-border-color: #1D4ED8; -fx-border-radius: 5; -fx-padding: 4 8;"
                            : "-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: #334155; -fx-border-radius: 5; -fx-padding: 4 8;")
            );
        }

        Label speedLabelTitle = createSectionTitle("⏱ Robot Hızı");
        HBox speedBox = new HBox(10, speedSlider, speedValueLabel);
        speedBox.setAlignment(Pos.CENTER_LEFT);
        speedValueLabel.setTextFill(Color.WHITE);
        speedValueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        Label algoLabel = createSectionTitle("⚙ Temizlik Algoritması");
        VBox algoBox = new VBox(6);
        rbAkilli.setToggleGroup(algoGroup); rbRandom.setToggleGroup(algoGroup); rbSpiral.setToggleGroup(algoGroup); rbWall.setToggleGroup(algoGroup);
        rbAkilli.setSelected(true);
        for(RadioButton rb : Arrays.asList(rbAkilli, rbRandom, rbSpiral, rbWall)) {
            rb.setTextFill(Color.WHITE);
            rb.setFont(Font.font("Segoe UI", 12));
        }
        algoBox.getChildren().addAll(rbAkilli, rbRandom, rbSpiral, rbWall);

        Label statusLabel = createSectionTitle("🤖 Robot Durumu");
        GridPane statusGrid = new GridPane();
        statusGrid.setVgap(6);
        statusGrid.setHgap(15);
        Label l1 = new Label("Konum (x,y)"); l1.setTextFill(Color.web(colorTextMuted));
        Label l2 = new Label("Yön"); l2.setTextFill(Color.web(colorTextMuted));
        Label l3 = new Label("Batarya"); l3.setTextFill(Color.web(colorTextMuted));
        lblPos.setTextFill(Color.WHITE); lblDir.setTextFill(Color.web(colorSuccess)); lblBatteryPercent.setTextFill(Color.web("#FBBF24"));
        lblPos.setFont(Font.font("Consolas", 12)); lblDir.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        batteryBar.setPrefWidth(130);
        batteryBar.setPrefHeight(10);
        batteryBar.setStyle("-fx-accent: #FBBF24; -fx-control-inner-background: #1E293B; -fx-background-color: transparent; -fx-border-color: #FBBF24; -fx-border-radius: 8; -fx-background-radius: 8;");

        statusGrid.add(l1, 0, 0); statusGrid.add(lblPos, 1, 0);
        statusGrid.add(l2, 0, 1); statusGrid.add(lblDir, 1, 1);
        statusGrid.add(l3, 0, 2); statusGrid.add(lblBatteryPercent, 1, 2);
        statusGrid.add(batteryBar, 0, 3, 2, 1);

        Label controlsLabel = createSectionTitle("🎮 Kontroller");
        btnStart.setStyle(blueBtnStyle); btnStart.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(btnStart, Priority.ALWAYS);
        btnPause.setStyle(darkBtnStyle); btnPause.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(btnPause, Priority.ALWAYS);
        HBox controlBox1 = new HBox(8, btnStart, btnPause);
        btnReset.setStyle(redBtnStyle); btnReset.setMaxWidth(Double.MAX_VALUE);
        btnReturn.setStyle(darkBtnStyle); btnReturn.setMaxWidth(Double.MAX_VALUE);

        leftPanel.getChildren().addAll(
                titleTools, btnAddDirt, dirtTypeLabel, dirtOptions, btnAddFurn,
                createDivider(), speedLabelTitle, speedBox,
                createDivider(), algoLabel, algoBox,
                createDivider(), statusLabel, statusGrid,
                createDivider(), controlsLabel, controlBox1, btnReset, btnReturn
        );
    }

    private void updateToolButtonStyles(String blue, String green, String inactive) {
        if(btnAddDirt.isSelected()) {
            btnAddDirt.setStyle(blue);
            btnAddFurn.setStyle(inactive);
        } else {
            btnAddDirt.setStyle(inactive);
            btnAddFurn.setStyle(green);
        }
    }

    private Label createSectionTitle(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.WHITE);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        return l;
    }

    private Region createDivider() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setStyle("-fx-background-color: #1E293B;");
        VBox.setMargin(r, new Insets(3, 0, 3, 0));
        return r;
    }

    private void setupBottomPanel() {
        bottomPanel.setPadding(new Insets(10, 30, 10, 30));
        bottomPanel.setStyle("-fx-background-color: " + colorPanelBg + "; -fx-border-color: #1E293B; -fx-border-width: 1 0 0 0; -fx-background-radius: 12; -fx-border-radius: 12;");
        bottomPanel.setAlignment(Pos.CENTER);
        bottomPanel.setSpacing(35);
        bottomPanel.setMinHeight(85); // HATA DÜZELTME: Ekran darsa bile bu panelin yazıları ezilmeyecek

        bottomPanel.getChildren().addAll(
                createStatItem(Color.web("#3B82F6"), "Toplam Alan", lblTotalArea),
                createStatItem(Color.web("#10B981"), "Temizlenen Alan", lblCleanedArea),
                createStatItem(Color.web("#9CA3AF"), "Kalan Alan", lblRemainingArea),
                createStatItem(null, "🕒 Geçen Süre", lblTime),
                createStatItem(null, "🧹 Toplanan Toz", lblDustCollected)
        );
    }

    private HBox createStatItem(Color dotColor, String title, Label valueLabel) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);

        VBox textBox = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.setTextFill(Color.web(colorTextMuted));
        titleLabel.setFont(Font.font("Segoe UI", 12));

        valueLabel.setTextFill(Color.WHITE);
        valueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        textBox.getChildren().addAll(titleLabel, valueLabel);

        if(dotColor != null) {
            Circle dot = new Circle(6, dotColor);
            box.getChildren().addAll(dot, textBox);
        } else {
            box.getChildren().add(textBox);
        }
        return box;
    }

    public VBox getLeftPanel() { return leftPanel; }
    public HBox getBottomPanel() { return bottomPanel; }
    public HBox getHeaderPanel() { return headerPanel; }
    public Canvas getCanvas() { return canvas; }
    public int getCellSize() { return cellSize; }
    public int getMarginX() { return marginX; }
    public int getMarginY() { return marginY; }

    private double getTargetAngle(Direction dir) {
        switch (dir) {
            case EAST: return 0;
            case SOUTH: return 90;
            case WEST: return 180;
            case NORTH: return -90;
        }
        return 0;
    }

    private void drawArrowHead(GraphicsContext gc, double x, double y, double angleStr) {
        double len = 6;
        double a = Math.toRadians(angleStr);
        gc.strokeLine(x, y, x - len * Math.cos(a - Math.PI / 6), y - len * Math.sin(a - Math.PI / 6));
        gc.strokeLine(x, y, x - len * Math.cos(a + Math.PI / 6), y - len * Math.sin(a + Math.PI / 6));
    }

    public void draw(VacuumModel model) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setFill(Color.web(colorBg));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setFill(Color.web(colorTextMuted));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        gc.setTextAlign(TextAlignment.CENTER);
        for (int i = 0; i < VacuumModel.COLS; i++) {
            gc.fillText(String.valueOf(i), marginX + i * cellSize + cellSize / 2.0, marginY - 10);
        }
        gc.setTextAlign(TextAlignment.RIGHT);
        for (int j = 0; j < VacuumModel.ROWS; j++) {
            gc.fillText(String.valueOf(j), marginX - 10, marginY + j * cellSize + cellSize / 2.0 + 4);
        }
        gc.setTextAlign(TextAlignment.LEFT);

        gc.setFill(Color.web("#E8D5B5"));
        gc.fillRect(marginX, marginY, VacuumModel.COLS * cellSize, VacuumModel.ROWS * cellSize);

        for (int i = 0; i < VacuumModel.COLS; i++) {
            for (int j = 0; j < VacuumModel.ROWS; j++) {
                if ((i + j) % 2 == 0) {
                    gc.setFill(Color.web("#F3E2C4"));
                    gc.fillRect(marginX + i * cellSize, marginY + j * cellSize, cellSize, cellSize);
                }
            }
        }

        gc.setStroke(Color.web("#D1B894"));
        gc.setLineWidth(1);
        for (int i = 0; i <= VacuumModel.COLS; i++) gc.strokeLine(marginX + i * cellSize, marginY, marginX + i * cellSize, marginY + VacuumModel.ROWS * cellSize);
        for (int j = 0; j <= VacuumModel.ROWS; j++) gc.strokeLine(marginX, marginY + j * cellSize, marginX + VacuumModel.COLS * cellSize, marginY + j * cellSize);

        gc.setStroke(Color.web("#374151"));
        gc.setLineWidth(14);
        gc.setEffect(new DropShadow(10, Color.rgb(0,0,0,0.5)));
        gc.strokeRect(marginX, marginY, VacuumModel.COLS * cellSize, VacuumModel.ROWS * cellSize);
        gc.setEffect(null);

        for (int i = 0; i < VacuumModel.COLS; i++) {
            for (int j = 0; j < VacuumModel.ROWS; j++) {
                Cell c = model.getCell(i, j);
                int x = marginX + i * cellSize;
                int y = marginY + j * cellSize;

                if (c.type == CellType.FURNITURE) {
                    gc.setEffect(new DropShadow(5, Color.rgb(0,0,0,0.4)));
                    gc.setFill(Color.web("#A0522D"));
                    gc.fillRoundRect(x + 2, y + 2, cellSize - 4, cellSize - 4, 8, 8);
                    gc.setFill(Color.web("#8B4513"));
                    gc.fillRoundRect(x + 6, y + 6, cellSize - 12, cellSize - 12, 4, 4);
                    gc.setFill(Color.web("#228B22"));
                    gc.fillOval(x + cellSize/2.0 - 4, y + cellSize/2.0 - 4, 8, 8);
                    gc.setEffect(null);
                } else if (c.type == CellType.BASE) {
                    gc.setEffect(new DropShadow(5, Color.rgb(0,0,0,0.6)));
                    gc.setFill(Color.web("#1E293B"));
                    gc.fillRect(x - 5, y - 5, cellSize + 10, cellSize + 10);
                    gc.setFill(Color.web("#0F172A"));
                    gc.fillOval(x + 5, y + 5, cellSize - 10, cellSize - 10);
                    gc.setFill(Color.web("#34D399"));
                    gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
                    gc.fillText("⚡", x + 10, y + 30);
                    gc.setEffect(null);
                }

                if (c.dirt == DirtType.DUST) {
                    gc.setFill(Color.web("#5C4033"));
                    for (int k = 0; k < 6; k++) {
                        gc.fillOval(x + 8 + (k*13)%(cellSize-16), y + 8 + (k*17)%(cellSize-16), 4, 4);
                    }
                } else if (c.dirt == DirtType.LIQUID) {
                    gc.setFill(Color.rgb(59, 130, 246, 0.6));
                    gc.fillOval(x + 5, y + 5, cellSize - 10, cellSize - 10);
                } else if (c.dirt == DirtType.STAIN) {
                    gc.setFill(Color.rgb(101, 67, 33, 0.8));
                    gc.fillOval(x + 10, y + 10, cellSize - 20, cellSize - 15);
                }
            }
        }

        gc.setLineWidth(2.5);

        gc.setStroke(Color.web("#3B82F6"));
        gc.setLineDashes(8, 8);
        Point2D prev = null;
        for (Point2D p : model.getPathHistory()) {
            double cx = marginX + p.getX() * cellSize + cellSize/2.0;
            double cy = marginY + p.getY() * cellSize + cellSize/2.0;
            if (prev != null) {
                gc.strokeLine(prev.getX(), prev.getY(), cx, cy);
                double angle = Math.toDegrees(Math.atan2(cy - prev.getY(), cx - prev.getX()));
                drawArrowHead(gc, (prev.getX() + cx)/2, (prev.getY() + cy)/2, angle);
            }
            prev = new Point2D(cx, cy);
        }

        if(!model.getReturnPath().isEmpty()) {
            gc.setStroke(Color.web("#10B981"));
            double startX = marginX + model.getRobotX() * cellSize + cellSize/2.0;
            double startY = marginY + model.getRobotY() * cellSize + cellSize/2.0;
            for(Cell c : model.getReturnPath()) {
                double cx = marginX + c.x * cellSize + cellSize/2.0;
                double cy = marginY + c.y * cellSize + cellSize/2.0;
                gc.strokeLine(startX, startY, cx, cy);
                double angle = Math.toDegrees(Math.atan2(cy - startY, cx - startX));
                drawArrowHead(gc, (startX + cx)/2, (startY + cy)/2, angle);
                startX = cx;
                startY = cy;
            }
        }
        gc.setLineDashes(null);

        double targetX = marginX + model.getRobotX() * cellSize;
        double targetY = marginY + model.getRobotY() * cellSize;
        double targetAngle = getTargetAngle(model.getRobotDir());

        if (visualX == -1) {
            visualX = targetX;
            visualY = targetY;
            visualAngle = targetAngle;
        }

        visualX += (targetX - visualX) * 0.25;
        visualY += (targetY - visualY) * 0.25;

        double diff = targetAngle - visualAngle;
        while (diff < -180) diff += 360;
        while (diff > 180) diff -= 360;
        visualAngle += diff * 0.25;

        gc.save();
        gc.translate(visualX + cellSize/2.0, visualY + cellSize/2.0);
        gc.rotate(visualAngle);

        gc.setEffect(new DropShadow(8, Color.rgb(0,0,0,0.6)));

        gc.setFill(Color.web("#1F2937"));
        gc.fillOval(-cellSize/2.0 + 1, -cellSize/2.0 + 1, cellSize - 2, cellSize - 2);

        gc.setEffect(null);
        gc.setFill(Color.WHITE);
        gc.fillOval(-cellSize/2.0 + 4, -cellSize/2.0 + 4, cellSize - 10, cellSize - 10);

        gc.setFill(Color.web("#F3F4F6"));
        gc.setStroke(Color.web("#9CA3AF"));
        gc.setLineWidth(1);
        gc.fillOval(-6, -6, 12, 12);
        gc.strokeOval(-6, -6, 12, 12);

        gc.setFill(Color.web("#1D4ED8"));
        gc.fillOval(cellSize/2.0 - 12, -3, 6, 6);

        gc.restore();

        gc.setFill(Color.rgb(15, 23, 42, 0.7));
        gc.fillRoundRect(visualX + 2, visualY - 18, 38, 16, 5, 5);
        gc.setFill(Color.web("#FBBF24"));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        gc.fillText(String.format("%.0f%%", model.getBattery()), visualX + 5, visualY - 6);
    }

    public void resetVisuals() {
        visualX = -1;
        visualY = -1;
        visualAngle = 0;
    }

    public void updateStats(VacuumModel model) {
        int total = model.getTotalArea();
        int cleaned = model.getCleanedArea();
        int remaining = total - cleaned;
        int percent = total == 0 ? 0 : (cleaned * 100) / total;

        lblTotalArea.setText(total + " m²");
        lblCleanedArea.setText(cleaned + " m² (" + percent + "%)");
        lblRemainingArea.setText(remaining + " m² (" + (100 - percent) + "%)");
        long sec = model.elapsedTimeProperty().get() / 1000;
        lblTime.setText(String.format("%02d:%02d", sec / 60, sec % 60));
        lblDustCollected.setText(percent + "%");

        String[] posParts = model.robotPosDirProperty().get().split("\n");
        if(posParts.length >= 2) {
            lblPos.setText(posParts[0]);
            lblDir.setText(posParts[1]);
        }

        double bat = model.getBattery();
        batteryBar.setProgress(bat / 100.0);
        lblBatteryPercent.setText(String.format("%.0f%%", bat));
        if (bat < 25) batteryBar.setStyle("-fx-accent: #EF4444; -fx-control-inner-background: #1E293B; -fx-background-color: transparent; -fx-border-color: #EF4444; -fx-border-radius: 8; -fx-background-radius: 8;");
        else batteryBar.setStyle("-fx-accent: #FBBF24; -fx-control-inner-background: #1E293B; -fx-background-color: transparent; -fx-border-color: #FBBF24; -fx-border-radius: 8; -fx-background-radius: 8;");
    }
}