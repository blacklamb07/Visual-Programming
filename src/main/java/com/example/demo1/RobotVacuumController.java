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

        // Controller artık doğrudan view.btnStart yerine view.getBtnStart() kullanıyor
        view.getBtnStart().setOnAction(e -> { model.setRunning(true); model.updateElapsedTime(); });
        view.getBtnPause().setOnAction(e -> model.setRunning(false));
        view.getBtnReset().setOnAction(e -> {
            model.reset();
            view.resetVisuals();
            view.draw(model);
            view.updateStats(model);
        });
        view.getBtnReturn().setOnAction(e -> {
            model.triggerReturnToBase();
        });

        view.getAlgoGroup().selectedToggleProperty().addListener((obs, old, val) -> {
            if(val == view.getRbAkilli()) model.setCurrentAlgo(Algorithm.AKILLI);
            else if(val == view.getRbRandom()) model.setCurrentAlgo(Algorithm.RASTGELE);
            else if(val == view.getRbSpiral()) model.setCurrentAlgo(Algorithm.SPIRAL);
            else if(val == view.getRbWall()) model.setCurrentAlgo(Algorithm.DUVAR_TAKIP);
        });

        model.setCurrentAlgo(Algorithm.AKILLI);

        view.getSpeedSlider().valueProperty().addListener((obs, old, val) -> {
            model.setSpeedMultiplier(val.doubleValue());
            view.getSpeedValueLabel().setText(String.format("%.1fx", val.doubleValue()));
        });
    }

    private void handleMouse(MouseEvent e) {
        int x = (int) ((e.getX() - view.getMarginX()) / view.getCellSize());
        int y = (int) ((e.getY() - view.getMarginY()) / view.getCellSize());

        if (x < 0 || x >= VacuumModel.COLS || y < 0 || y >= VacuumModel.ROWS) return;

        Cell cell = model.getGrid()[x][y];
        if (cell.type == CellType.BASE || (x == model.getRobotX() && y == model.getRobotY())) return;

        if (view.getBtnAddFurn().isSelected()) {
            cell.type = CellType.FURNITURE;
            cell.dirt = DirtType.NONE;
            model.calculateAreas();
        } else if (view.getBtnAddDirt().isSelected() && cell.type != CellType.FURNITURE) {
            if (view.getRbDust().isSelected()) cell.dirt = DirtType.DUST;
            else if (view.getRbLiquid().isSelected()) cell.dirt = DirtType.LIQUID;
            else if (view.getRbStain().isSelected()) cell.dirt = DirtType.STAIN;
            cell.isCleaned = false;
            model.calculateAreas();
        }
    }

    private void startGameLoop() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
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
                view.draw(model);
            }
        };
        timer.start();
    }
}