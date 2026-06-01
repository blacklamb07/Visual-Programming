package com.example.demo1;

import javafx.beans.property.*;
import javafx.geometry.Point2D;
import java.util.*;

public class VacuumModel {
    public static final int COLS = 20, ROWS = 14;
    private final Cell[][] grid;
    private final int baseX = 1, baseY = 12;
    private int robotX, robotY;
    private Direction robotDir = Direction.EAST;
    private double battery = 100.0;
    private double speedMultiplier = 1.0;
    private int totalArea = 0, cleanedArea = 0;
    private long startTimeMillis;
    private long elapsedMillis = 0;
    private boolean isRunning = false;
    private boolean isReturning = false;
    private int cleaningTimer = 0;

    private Queue<Cell> returnPath = new LinkedList<>();
    private Queue<Cell> smartPath = new LinkedList<>();
    private Algorithm currentAlgo = Algorithm.AKILLI;

    private int spiralSteps = 1, stepsTaken = 0, turns = 0;
    private final Queue<Point2D> pathHistory = new LinkedList<>();
    private static final int MAX_PATH_HISTORY = 500;

    private final DoubleProperty batteryProperty = new SimpleDoubleProperty();
    private final IntegerProperty cleanedPercentProperty = new SimpleIntegerProperty();
    private final LongProperty elapsedTimeProperty = new SimpleLongProperty();
    private final StringProperty robotPosDirProperty = new SimpleStringProperty();

    public VacuumModel() {
        grid = new Cell[COLS][ROWS];
        for (int i = 0; i < COLS; i++)
            for (int j = 0; j < ROWS; j++)
                grid[i][j] = new Cell(i, j);
        grid[baseX][baseY].type = CellType.BASE;
        grid[baseX][baseY].isCleaned = true;
        robotX = baseX;
        robotY = baseY;
        calculateAreas();
        updateObservables();
    }

    // --- EKLENEN EKSİK METOT: Controller'ın haritayı okuyabilmesi için ---
    public Cell[][] getGrid() {
        return grid;
    }

    public Cell getCell(int x, int y) {
        if (x >= 0 && x < COLS && y >= 0 && y < ROWS) return grid[x][y];
        return null;
    }

    public int getRobotX() { return robotX; }
    public int getRobotY() { return robotY; }
    public Direction getRobotDir() { return robotDir; }
    public double getBattery() { return battery; }
    public void setBattery(double value) { battery = Math.min(100.0, Math.max(0.0, value)); updateObservables(); }
    public double getSpeedMultiplier() { return speedMultiplier; }
    public void setSpeedMultiplier(double mult) { speedMultiplier = mult; }
    public Algorithm getCurrentAlgo() { return currentAlgo; }
    public void setCurrentAlgo(Algorithm algo) { currentAlgo = algo; }
    public boolean isRunning() { return isRunning; }
    public void setRunning(boolean running) {
        if (running && !isRunning) startTimeMillis = System.currentTimeMillis() - elapsedMillis;
        isRunning = running;
    }
    public int getCleanedArea() { return cleanedArea; }
    public int getTotalArea() { return totalArea; }
    public Queue<Point2D> getPathHistory() { return pathHistory; }
    public Queue<Cell> getReturnPath() { return returnPath; }

    public void addPathPoint(int x, int y) {
        pathHistory.add(new Point2D(x, y));
        while (pathHistory.size() > MAX_PATH_HISTORY) pathHistory.poll();
    }
    public void clearPathHistory() { pathHistory.clear(); }

    public void calculateAreas() {
        totalArea = 0; cleanedArea = 0;
        for (int i = 0; i < COLS; i++) {
            for (int j = 0; j < ROWS; j++) {
                if (grid[i][j].type != CellType.FURNITURE && grid[i][j].type != CellType.WALL) {
                    totalArea++;
                    if (grid[i][j].isCleaned) cleanedArea++;
                }
            }
        }
        updateObservables();
    }

    private void updateObservables() {
        batteryProperty.set(battery);
        int percent = totalArea == 0 ? 0 : (cleanedArea * 100) / totalArea;
        cleanedPercentProperty.set(percent);
        elapsedTimeProperty.set(elapsedMillis);
        String arrow = switch (robotDir) {
            case NORTH -> "↑"; case EAST -> "→"; case SOUTH -> "↓"; case WEST -> "←";
        };
        robotPosDirProperty.set(String.format("(%d, %d) %s %s", robotX, robotY, robotDir.name, arrow));
    }

    public DoubleProperty batteryProperty() { return batteryProperty; }
    public IntegerProperty cleanedPercentProperty() { return cleanedPercentProperty; }
    public LongProperty elapsedTimeProperty() { return elapsedTimeProperty; }
    public StringProperty robotPosDirProperty() { return robotPosDirProperty; }

    public void updateTick() {
        if (!isRunning || battery <= 0) return;

        if (battery <= 20.0 && !isReturning && (robotX != baseX || robotY != baseY)) {
            triggerReturnToBase();
        }

        if (cleaningTimer > 0) {
            cleaningTimer -= speedMultiplier;
            if (cleaningTimer <= 0) {
                grid[robotX][robotY].dirt = DirtType.NONE;
                grid[robotX][robotY].isCleaned = true;
                calculateAreas();
            }
            return;
        }

        if (grid[robotX][robotY].dirt != DirtType.NONE && !isReturning) {
            startCleaning(grid[robotX][robotY].dirt);
            return;
        }

        if (isReturning) {
            moveAlongPath(returnPath);
        } else {
            moveRobotNormal();
        }

        updateObservables();
    }

    private void startCleaning(DirtType type) {
        switch (type) {
            case DUST:   cleaningTimer = 10; battery -= 0.2; break;
            case LIQUID: cleaningTimer = 30; battery -= 0.5; break;
            case STAIN:  cleaningTimer = 60; battery -= 1.0; break;
        }
        battery = Math.max(0, battery);
    }

    private void moveRobotNormal() {
        battery -= 0.1 * speedMultiplier;
        if (battery < 0) battery = 0;

        switch (currentAlgo) {
            case AKILLI:
                if (!smartPath.isEmpty()) {
                    moveAlongPath(smartPath);
                    break;
                }

                if (canMove(robotDir) && !getCell(robotX + robotDir.dx, robotY + robotDir.dy).isCleaned) {
                    moveForward();
                } else {
                    Direction rightDir = robotDir.turnRight();
                    Direction leftDir = robotDir.turnLeft();

                    boolean rightClean = !canMove(rightDir) || getCell(robotX + rightDir.dx, robotY + rightDir.dy).isCleaned;
                    boolean leftClean = !canMove(leftDir) || getCell(robotX + leftDir.dx, robotY + leftDir.dy).isCleaned;

                    if (!rightClean) {
                        robotDir = rightDir;
                    } else if (!leftClean) {
                        robotDir = leftDir;
                    } else {
                        findPathToNearestUncleaned();
                        if (smartPath.isEmpty()) triggerReturnToBase();
                    }
                }
                break;

            case RASTGELE:
                if (canMove(robotDir)) {
                    moveForward();
                } else {
                    List<Direction> sideDirs = new ArrayList<>();
                    if (canMove(robotDir.turnLeft())) sideDirs.add(robotDir.turnLeft());
                    if (canMove(robotDir.turnRight())) sideDirs.add(robotDir.turnRight());

                    if (!sideDirs.isEmpty()) {
                        robotDir = sideDirs.get(new Random().nextInt(sideDirs.size()));
                    } else {
                        robotDir = robotDir.turnRight().turnRight();
                    }
                }
                break;

            case DUVAR_TAKIP:
                if (!canMove(robotDir.turnLeft()) && canMove(robotDir)) {
                    moveForward();
                } else if (canMove(robotDir.turnLeft())) {
                    robotDir = robotDir.turnLeft();
                } else {
                    robotDir = robotDir.turnRight();
                }
                break;

            case SPIRAL:
                if (stepsTaken < spiralSteps && canMove(robotDir)) {
                    moveForward();
                    stepsTaken++;
                } else {
                    robotDir = robotDir.turnRight();
                    turns++;
                    stepsTaken = 0;
                    if (turns == 2) { spiralSteps++; turns = 0; }
                    if (!canMove(robotDir)) spiralSteps = 1;
                }
                break;
        }
    }

    private boolean canMove(Direction dir) {
        int nx = robotX + dir.dx, ny = robotY + dir.dy;
        Cell c = getCell(nx, ny);
        return c != null && c.type != CellType.FURNITURE && c.type != CellType.WALL;
    }

    private void moveForward() {
        addPathPoint(robotX, robotY);
        robotX += robotDir.dx;
        robotY += robotDir.dy;
        grid[robotX][robotY].isCleaned = true;
        calculateAreas();
    }

    private void moveAlongPath(Queue<Cell> pathQueue) {
        if (pathQueue.isEmpty()) {
            if(isReturning) {
                isReturning = false;
                battery = 100.0;
                updateObservables();
            }
            return;
        }

        Cell next = pathQueue.peek();
        Direction neededDir = null;
        if (next.x > robotX) neededDir = Direction.EAST;
        else if (next.x < robotX) neededDir = Direction.WEST;
        else if (next.y > robotY) neededDir = Direction.SOUTH;
        else if (next.y < robotY) neededDir = Direction.NORTH;

        if (neededDir != null && robotDir != neededDir) {
            robotDir = neededDir;
            battery -= 0.05 * speedMultiplier;
        } else {
            pathQueue.poll();
            moveForward();
        }
    }

    private void findPathToNearestUncleaned() {
        smartPath.clear();
        Queue<Cell> queue = new LinkedList<>();
        Map<Cell, Cell> parent = new HashMap<>();
        Set<Cell> visited = new HashSet<>();

        Cell start = grid[robotX][robotY];
        queue.add(start);
        visited.add(start);

        Cell target = null;

        while (!queue.isEmpty()) {
            Cell curr = queue.poll();
            if (!curr.isCleaned && curr.type != CellType.FURNITURE && curr.type != CellType.WALL && curr.type != CellType.BASE) {
                target = curr;
                break;
            }
            for (Direction d : Direction.values()) {
                Cell neighbor = getCell(curr.x + d.dx, curr.y + d.dy);
                if (neighbor != null && neighbor.type != CellType.FURNITURE && !visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parent.put(neighbor, curr);
                    queue.add(neighbor);
                }
            }
        }

        if (target != null) {
            List<Cell> path = new ArrayList<>();
            Cell c = target;
            while (c != start) {
                path.add(c);
                c = parent.get(c);
            }
            Collections.reverse(path);
            smartPath.addAll(path);
        }
    }

    public void triggerReturnToBase() {
        if(isReturning) return;
        isReturning = true;
        returnPath.clear();
        smartPath.clear();

        Cell start = grid[robotX][robotY];
        Cell target = grid[baseX][baseY];

        Queue<Cell> queue = new LinkedList<>();
        Map<Cell, Cell> parent = new HashMap<>();
        Set<Cell> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Cell curr = queue.poll();
            if (curr == target) break;
            for (Direction d : Direction.values()) {
                Cell neighbor = getCell(curr.x + d.dx, curr.y + d.dy);
                if (neighbor != null && neighbor.type != CellType.FURNITURE && !visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parent.put(neighbor, curr);
                    queue.add(neighbor);
                }
            }
        }

        if (parent.containsKey(target)) {
            List<Cell> path = new ArrayList<>();
            Cell c = target;
            while (c != start) {
                path.add(c);
                c = parent.get(c);
            }
            Collections.reverse(path);
            returnPath.addAll(path);
        }
    }

    public void reset() {
        for (int i = 0; i < COLS; i++)
            for (int j = 0; j < ROWS; j++) {
                grid[i][j].type = CellType.EMPTY;
                grid[i][j].dirt = DirtType.NONE;
                grid[i][j].isCleaned = false;
            }
        grid[baseX][baseY].type = CellType.BASE;
        grid[baseX][baseY].isCleaned = true;
        robotX = baseX;
        robotY = baseY;
        robotDir = Direction.EAST;
        battery = 100.0;
        isRunning = false;
        isReturning = false;
        cleaningTimer = 0;
        elapsedMillis = 0;
        spiralSteps = 1; stepsTaken = 0; turns = 0;
        returnPath.clear();
        smartPath.clear();
        clearPathHistory();
        calculateAreas();
        updateObservables();
    }

    public void updateElapsedTime() {
        if (isRunning) elapsedMillis = System.currentTimeMillis() - startTimeMillis;
        elapsedTimeProperty.set(elapsedMillis);
    }
}