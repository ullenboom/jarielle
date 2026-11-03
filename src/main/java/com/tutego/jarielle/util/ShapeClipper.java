package com.tutego.jarielle.util;

import com.tutego.jarielle.model.Point;
import com.tutego.jarielle.model.PositionedNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of src/shape-clipping.ts — clips orthogonal edge endpoints to diamond boundaries.
 */
public final class ShapeClipper {

    private ShapeClipper() {}

    public static List<Point> clipEdgeToShape(List<Point> points, PositionedNode node, boolean isStart) {
        if (points.size() < 2) return points;
        if (node.shape().isRectangular()) return points;

        if (node.shape() == com.tutego.jarielle.model.NodeShape.DIAMOND) {
            List<Point> result = new ArrayList<>(points);
            if (isStart) {
                result.set(0, clipToDiamond(points.get(0), points.get(1), node));
            } else {
                int last = points.size() - 1;
                result.set(last, clipToDiamond(points.get(last), points.get(last - 1), node));
            }
            return List.copyOf(result);
        }
        return points;
    }

    private static Point clipToDiamond(Point endpoint, Point adjacent, PositionedNode node) {
        double cx = node.x() + node.width() / 2.0;
        double cy = node.y() + node.height() / 2.0;

        Point top    = new Point(cx, node.y());
        Point right  = new Point(node.x() + node.width(), cy);
        Point bottom = new Point(cx, node.y() + node.height());
        Point left   = new Point(node.x(), cy);

        double dx = endpoint.x() - adjacent.x();
        double dy = endpoint.y() - adjacent.y();
        boolean isVertical = Math.abs(dx) < Math.abs(dy);

        if (isVertical) {
            double rayX = endpoint.x();
            if (dy > 0) {
                if (rayX <= cx) {
                    Point p = intersectVertical(rayX, left, top);
                    return p != null ? p : top;
                } else {
                    Point p = intersectVertical(rayX, top, right);
                    return p != null ? p : top;
                }
            } else {
                if (rayX <= cx) {
                    Point p = intersectVertical(rayX, bottom, left);
                    return p != null ? p : bottom;
                } else {
                    Point p = intersectVertical(rayX, right, bottom);
                    return p != null ? p : bottom;
                }
            }
        } else {
            double rayY = endpoint.y();
            if (dx > 0) {
                if (rayY <= cy) {
                    Point p = intersectHorizontal(rayY, top, left);
                    return p != null ? p : left;
                } else {
                    Point p = intersectHorizontal(rayY, left, bottom);
                    return p != null ? p : left;
                }
            } else {
                if (rayY <= cy) {
                    Point p = intersectHorizontal(rayY, top, right);
                    return p != null ? p : right;
                } else {
                    Point p = intersectHorizontal(rayY, right, bottom);
                    return p != null ? p : right;
                }
            }
        }
    }

    private static Point intersectHorizontal(double rayY, Point p1, Point p2) {
        double dy = p2.y() - p1.y();
        if (Math.abs(dy) < 1e-3) return null;
        double t = (rayY - p1.y()) / dy;
        if (t < 0 || t > 1) return null;
        double x = p1.x() + t * (p2.x() - p1.x());
        return new Point(x, rayY);
    }

    private static Point intersectVertical(double rayX, Point p1, Point p2) {
        double dx = p2.x() - p1.x();
        if (Math.abs(dx) < 1e-3) return null;
        double t = (rayX - p1.x()) / dx;
        if (t < 0 || t > 1) return null;
        double y = p1.y() + t * (p2.y() - p1.y());
        return new Point(rayX, y);
    }
}
