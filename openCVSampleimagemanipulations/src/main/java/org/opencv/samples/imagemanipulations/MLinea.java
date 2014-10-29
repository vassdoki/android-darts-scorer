package org.opencv.samples.imagemanipulations;

import android.graphics.Point;

/**
 * Created by vassdoki on 10/23/14.
 */
class MLinea {
    public Point start = new Point();
    public Point end = new Point();
    public double angle;
    public double intersect;
    public boolean important;

    public MLinea() {
    }
    public MLinea(float x1, float y1, float x2, float y2) {
        this((int) x1, (int) y1, (int) x2, (int) y2);
    }
    public MLinea(int x1, int y1, int x2, int y2) {
        start.x = x1;
        start.y = y1;
        end.x = x2;
        end.y = y2;
        angle = (end.y - start.y) / (end.x - start.x);
        intersect = y1 - angle * x1;
        important = false;
    }

    public MLinea(Point pStart, Point pEnd) {
        this(pStart.x, pStart.y, pEnd.x, pEnd.y);
    }

    public MLinea reset(Point start, Point end) {
        this.start.x = start.x;
        this.start.y = start.y;
        this.end.x = end.x;
        this.end.y = end.y;
        if (end.x - start.x == 0) {
            angle = 9999;
        } else {
            angle = (end.y - start.y) / (end.x - start.x);
        }
        intersect = start.y - angle * start.x;
        important = false;
        return this;
    }

    public Point getStart() {
        return new Point(start.x, start.y);
    }
    public Point getEnd() {
        return new Point(end.x, end.y);
    }

    public double getPerpAngle() {
        return (start.x - end.x) / (start.y - end.y);
    }

    public double length() {
        return Math.sqrt(sq(start.x - end.x) + sq(start.y - end.y));
    }

    public double distance(Point b) {
        return Math.abs((end.x - start.x) * (start.y - b.y) - (start.x - b.x) * (end.y - start.y)) /
                Math.sqrt(sq(end.x - start.x) + sq(end.y - start.y));
    }

    public static double sq(double a) {
        return a * a;
    }
    public static double sq(int a) {
        return (double)a * a;
    }

    /**
     * The closest distance from the start and the end of the line.
     * If the point is in between, then the distance from the line.
     * @param b
     * @return
     */
    public double endDistanceOrDistance(Point b) {
        if (start.x < b.x && b.x < end.x && start.y < b.y && b.y < end.y) {
            return distance(b);
        }
        double dstFromStart = Math.sqrt(sq(b.x - start.x) + sq(b.y - start.y));
        double dstFromEnd = Math.sqrt(sq(b.x - end.x) + sq(b.y - end.y));
        return Math.min(dstFromStart, dstFromEnd);


    }
    /**
     * The closest distance from the start and the end of the line.
     * @param b
     * @return
     */
    public double endDistance(Point b) {
        double dstFromStart = Math.sqrt(sq(b.x - start.x) + sq(b.y - start.y));
        double dstFromEnd = Math.sqrt(sq(b.x - end.x) + sq(b.y - end.y));
        return Math.min(dstFromStart, dstFromEnd);


    }

    public double getAngeInDegree() {
        double y = start.y - end.y;
        double x = start.x - end.x;
        double v = 180 * Math.atan2(y, x) / Math.PI;
        if (v > 180) {
            v = 180;
        }
        if (v < -180) {
            v = -180;
        }
        if (v < 0) {
            v += 360;
        }
        //Log.i(TAG, "arch: y: " + y + " x: " + x + " archtan: " + v);
        return v;
    }

    public double[] getStep() {
        double[] step = new double[2];
        double length;
        if (Math.abs(start.x - end.x) > Math.abs(start.y-end.y)) {
            length = Math.abs(start.x - end.x);
        } else {
            length = Math.abs(start.y - end.y);
        }
        step[0] = (double)(end.x - start.x) / length;
        step[1] = (double)(end.y - start.y) / length;
        return step;
    }
    public int getStepLength() {
        if (Math.abs(start.x - end.x) > Math.abs(start.y-end.y)) {
            return (int)Math.abs(start.x-end.x);
        } else {
            return (int)Math.abs(start.y - end.y);
        }
    }

    public static Point mirrorPoint(Point p, Point c) {
        Point r = new Point();
        r.x = p.x + 2 * (c.x - p.x);
        r.y = p.y + 2 * (c.y - p.y);
        return r;
    }
    public static double distanceOfTwoPoints(Point a, Point b) {
        return Math.sqrt(sq(a.x - b.x) + sq(a.y - b.y));
    }

    public static Point rotatePoint(Point c, double degree, double radius) {
        Point p = new Point();
        double sin = Math.sin(Math.PI * degree / 180);
        p.x = (int) (c.x + sin * radius);
        double cos = Math.cos(Math.PI * degree / 180);
        p.y = (int) (c.y + cos * radius);
        //Log.i(TAG, "rotatePoint, bull;" + c.x +";"+ c.y + ";radius;" + radius + ";degree;" + degree + ";sin;" + sin + ";cos;" + cos + ";p;" + p.x +";"+p.y);
        return p;
    }

    public static double getTwoPointAngle(Point bull, Point a) {
        double y = a.y - bull.y;
        double x = a.x - bull.x;
        double v = 180 * Math.atan2(y, x) / Math.PI;
        return v;
    }

    public Point getIntersection(MLinea l2) {
        double a1 = (start.y - end.y) / (double)(start.x - end.x);
        double b1 = start.y - a1 * start.x;

        double a2 = (l2.start.y - l2.end.y) / (double)(l2.start.x - l2.end.x);
        double b2 = l2.start.y - a2 * l2.start.x;

        if (Math.abs(a1 - a2) < 0.00001) return null;

        int x = (int) ((b2 - b1) / (a1 - a2));
        int y = (int) (a1 * x + b1);
        return new Point(x, y);
    }

}
