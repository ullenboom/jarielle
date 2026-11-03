package com.tutego.jarielle.model;

/**
 * 2D point — immutable record, Java 25.
 */
public record Point(double x, double y) {

    public static Point of(double x, double y) {
        return new Point(x, y);
    }

    public double distance(Point other) {
        double dx = other.x - x;
        double dy = other.y - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public Point translate(double dx, double dy) {
        return new Point(x + dx, y + dy);
    }
}
