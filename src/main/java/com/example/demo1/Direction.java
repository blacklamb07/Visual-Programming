package com.example.demo1;

public enum Direction {
    NORTH(0, -1, "Kuzey"),
    EAST(1, 0, "Doğu"),
    SOUTH(0, 1, "Güney"),
    WEST(-1, 0, "Batı");

    public final int dx, dy;
    public final String name;

    Direction(int dx, int dy, String name) {
        this.dx = dx;
        this.dy = dy;
        this.name = name;
    }

    public Direction turnRight() { return values()[(this.ordinal() + 1) % 4]; }
    public Direction turnLeft()  { return values()[(this.ordinal() + 3) % 4]; }
}