package org.opencv.samples.imagemanipulations;

import android.graphics.Point;
import android.util.Log;

import java.util.ArrayList;

/**
 * This class provides access to the transformed original image.
 * Created by vassdoki on 10/29/14.
 */
public class DartsTable {
    /**
     * The original color image.
     */
    private final ColorImage colorImage;
    /**
     * The 4 + 4 point, that generates the transformation matrix
     */
    Point[] transPoints;
    /**
     * Transformation matrix from the original image to the transformed image.
     */
    double[][] trans = new double[3][3];
    /**
     * Transformation matrix from the transformed image to the original.
     */
    double[][] invTrans = new double[3][3];
    /**
     * Bull on the original image.
     */
    private Point origBull;
    private Point bull;

    LineError[] lineErrors = new LineError[20];

    public static int estBullSize = 50;
    public static int estTrippleDistance = 310;
    public static int estDoubleDistance = 470;
    public static int estColoredWith = 30;

    private static final String TAG = "OCVSample::DokiPicture";
    /**
     * This gives how longer a line will be after transformation
     */
    public double transRatio;


    public LineError[] getLineErrors() {
        return getLineErrors(1);
    }
    public LineError[] getLineErrors(int step) {
        for(int i = 0; i < 20; i += step) {
            int degree = 9 + i * 18;
            LineError le = new LineError(colorImage, this, bull, degree);
            lineErrors[i] = le;
            Log.i(TAG, "Line error (" + degree + ") B/W diff: " + le.getAverageBWError() + " tripple: " + le.getRegion(0) +"," + le.getRegion(1) + " double: " + le.getRegion(2) +","+le.getRegion(3));
        }
        return lineErrors;
    }

    public int getCircleErrorSum() {
        int sum = 0;
        for (LineError le : lineErrors) {
            if (le != null) {
                sum += Math.abs(estDoubleDistance - le.getRegion(2));
                //Log.i(TAG, "SUM ERROR: " + sum + " akt: " + Math.abs(estDoubleDistance - le.getRegion(2)) + " value: " + le.getRegion(2) + " avg: " + estDoubleDistance);
            }
        }
        return sum;
    }
    public Point[] getTransToFitCircle() {
        // kivlasztjuk a legnagyobb eltérést és módosítjuk az oda tartozó pontot.
        float maxDiff = -1;
        int maxId = -1;
        float maxDiffValue = 0;
        int ii = 0;
        for (LineError le : lineErrors) {
            if (le != null) {
                float currDiff = estDoubleDistance - le.getRegion(2);
                if (Math.abs(currDiff) > maxDiff) {
                    maxDiffValue = estDoubleDistance - le.getRegion(2);
                    maxDiff = Math.abs(maxDiffValue);
                    maxId = ii;
                }
                ii++;
            }
        }
        Point[] newTrans = new Point[8];
        System.arraycopy(transPoints, 0, newTrans, 0, transPoints.length);

        int[][] transEdit = new int[4][2];
        transEdit[0][0] = 0; // x
        transEdit[0][1] = -1; // y
        transEdit[1][0] = -1; // x
        transEdit[1][1] = 0; // y
        transEdit[2][0] = 0; // x
        transEdit[2][1] = 1; // y
        transEdit[3][0] = 1; // x
        transEdit[3][1] = 0; // y

//        Log.i(TAG, "Ezt a pontot szeretnem athelyezni (front): " + transPoints[maxId + 4].x +","+transPoints[maxId + 4].y);
//        Point change = invTransform(new Point((int)(transPoints[maxId + 4].x + transEdit[maxId][0] * maxDiffValue), (int)(transPoints[maxId + 4].y + transEdit[maxId][1] * maxDiffValue)));
//        newTrans[maxId].x = change.x;
//        newTrans[maxId].y = change.y;
//        Log.i(TAG, "new transformation points, id: " + maxId + " diffValue: " + maxDiffValue + " change: " + (change.x) + "," + (change.y));

        double changeX = transEdit[maxId][0] * Math.signum(maxDiffValue) * 2;
        double changeY = transEdit[maxId][1] * Math.signum(maxDiffValue) * 2;
//        double changeX = transEdit[maxId][0] * maxDiffValue / (transRatio * 6);
//        double changeY = transEdit[maxId][1] * maxDiffValue / (transRatio * 6);
        newTrans[maxId].x += changeX;
        newTrans[maxId].y += changeY;
        Log.i(TAG, ">>> new transformation points, id: " + maxId + " diffValue: " + maxDiffValue + " change: " + (changeX) + "," +
                "" + (changeY) + " new trans points: " + debugTransRes(newTrans));

        return newTrans;
    }




    public DartsTable(ColorImage colorImage) {
        this.colorImage = colorImage;
    }

    public void setOrigBull(Point origBull) {
        this.origBull = origBull;
    }

    public String debugArrayList(ArrayList a) {
        StringBuilder sb = new StringBuilder();
        for(Object o : a) {
            sb.append(o.toString()).append(";");
        }
        return sb.toString();
    }

    private String debugTransRes(Point[] transRes2) {
        StringBuilder sb = new StringBuilder();
        if (transRes2 == null) {
            return "";
        }
        for(android.graphics.Point p : transRes2) {
            sb.append(p.x+","+p.y+" ");
        }
        return sb.toString();
    }

    public void setTransPoints(Point[] ps) {
        this.transPoints = ps.clone();
    }
    public void setInvTrans(double[][] invTrans) {
        this.invTrans = new double[3][3];
        System.arraycopy(invTrans, 0, this.invTrans, 0, 3);
    }
    public void setTrans(double[][] trans) {
        this.trans = trans.clone();
        if (this.origBull != null) {
            this.bull = transform(origBull);
        }
        // based on the transformation, we calculate the ratio of the original image size / transformed image size
        Point p1 = new Point(100, 100);
        Point p2 = MLinea.rotatePoint(p1, 45, 100);
        // distance = 500
        double dist1 = MLinea.distanceOfTwoPoints(p1, p2);
        Point p1t = transform(p1);
        Point p2t = transform(p2);
        double dist2 = MLinea.distanceOfTwoPoints(p1t, p2t);

        this.transRatio = dist2 / dist1;
        Log.i(TAG, "Trans ratio: " + transRatio + " l1: " + dist1 + " l2: " + dist2);
    }
    public Point transform(Point p) {
        double m1 = p.x*trans[0][0] + p.y * trans[0][1] + trans[0][2];
        double m2 = p.x*trans[1][0] + p.y * trans[1][1] + trans[1][2];
        double m3 = p.x*trans[2][0] + p.y * trans[2][1] + trans[2][2];
        return new Point((int)(m1/m3), (int)(m2/m3));
    }
    public Point invTransform(Point p) {
        double m1 = p.x*invTrans[0][0] + p.y * invTrans[0][1] + invTrans[0][2];
        double m2 = p.x*invTrans[1][0] + p.y * invTrans[1][1] + invTrans[1][2];
        double m3 = p.x*invTrans[2][0] + p.y * invTrans[2][1] + invTrans[2][2];
        return new Point((int)(m1/m3), (int)(m2/m3));
    }

}
