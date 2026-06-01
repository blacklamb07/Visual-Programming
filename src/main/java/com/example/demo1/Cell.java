package com.example.demo1;

public class Cell {
    public final int x, y;
    public CellType type = CellType.EMPTY;
    public DirtType dirt = DirtType.NONE;
    public boolean isCleaned = false;

    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
    }
}