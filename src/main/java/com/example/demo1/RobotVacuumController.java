package com.example.demo1;

import javafx.animation.AnimationTimer;
import javafx.scene.input.MouseEvent;

public class RobotVacuumController {
    private final VacuumModel model;
    private final RobotVacuumView view;
    private AnimationTimer timer;
    private long lastLogicalUpdate = 0;

    public RobotVacuumController(VacuumModel model, RobotVacuumView view) {
        this.model = model;
        this.view = view;
        attachEventHandlers();
        startGameLoop();
        bindProperties();
    }

    private void bindProperties() {
        model.cleanedPercentProperty().addListener((obs, old, val) -> view.updateStats(model));
        model.elapsedTimeProperty().addListener((obs, old, val) -> view.updateStats(model));
        model.robotPosDirProperty().addListener((obs, old, val) -> view.updateStats(model));
        model.batteryProperty().addListener((obs, old, val) -> view.updateStats(model));
    }

    private void attachEventHandlers() {
        view.getCanvas().setOnMouseClicked(this::handleMouse);
        view.getCanvas().setOnMouseDragged(this::handleMouse);

        view.btnStart.setOnAction(e -> { model.setRunning(true); model.updateElapsedTime(); });
        view.btnPause.setOnAction(e -> model.setRunning(false));
        view.btnReset.setOnAction(e -> {
            model.reset();
            view.resetVisuals();
            view.draw(model);
            view.updateStats(model);
        });
        view.btnReturn.setOnAction(e -> {
            model.triggerReturnToBase();
        });

        view.algoGroup.selectedToggleProperty().addListener((obs, old, val) -> {
            if(val == view.rbAkilli) model.setCurrentAlgo(Algorithm.AKILLI);
            else if(val == view.rbRandom) model.setCurrentAlgo(Algorithm.RASTGELE);
            else if(val == view.rbSpiral) model.setCurrentAlgo(Algorithm.SPIRAL);
            else if(val == view.rbWall) model.setCurrentAlgo(Algorithm.DUVAR_TAKIP);
        });

        model.setCurrentAlgo(Algorithm.AKILLI);

        view.speedSlider.valueProperty().addListener((obs, old, val) -> {
            model.setSpeedMultiplier(val.doubleValue());
            view.speedValueLabel.setText(String.format("%.1fx", val.doubleValue()));
        });
    }

    private void handleMouse(MouseEvent e) {
        int x = (int) ((e.getX() - view.getMarginX()) / view.getCellSize());
        int y = (int) ((e.getY() - view.getMarginY()) / view.getCellSize());

        if (x < 0 || x >= VacuumModel.COLS || y < 0 || y >= VacuumModel.ROWS) return;

        Cell cell = model.getCell(x, y);
        if (cell.type == CellType.BASE || (x == model.getRobotX() && y == model.getRobotY())) return;

        if (view.btnAddFurn.isSelected()) {
            cell.type = CellType.FURNITURE;
            cell.dirt = DirtType.NONE;
            model.calculateAreas();
        } else if (view.btnAddDirt.isSelected() && cell.type != CellType.FURNITURE) {
            if (view.rbDust.isSelected()) cell.dirt = DirtType.DUST;
            else if (view.rbLiquid.isSelected()) cell.dirt = DirtType.LIQUID;
            else if (view.rbStain.isSelected()) cell.dirt = DirtType.STAIN;
            cell.isCleaned = false;
            model.calculateAreas();
        }
    }

    private void startGameLoop() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // 1. Mantıksal Güncelleme (Hıza bağlı olarak belli aralıklarla çalışır)
                double speed = model.getSpeedMultiplier();
                long interval = (long)(100_000_000 / (2.5 * speed));
                if (now - lastLogicalUpdate >= interval) {
                    if (model.isRunning()) {
                        model.updateElapsedTime();
                        model.updateTick();
                        view.updateStats(model);
                    }
                    lastLogicalUpdate = now;
                }

                // 2. Görsel Güncelleme (Saniyede 60 kare, her daim çalışır. Pürüzsüz animasyonu sağlar)
                view.draw(model);
            }
        };
        timer.start();
    }
}