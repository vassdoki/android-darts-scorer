package org.opencv.samples.imagemanipulations;

import android.graphics.Point;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;

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

    private static final String TAG = "OCVSample::DokiPicture";


    public DartsTable(ColorImage colorImage) {
        this.colorImage = colorImage;
    }

    public void setOrigBull(Point origBull) {
        this.origBull = origBull;
    }



    private String debugArrayList(ArrayList a) {
        StringBuilder sb = new StringBuilder();
        for(Object o : a) {
            sb.append(o.toString()).append(";");
        }
        return sb.toString();
    }

    public void setTransPoints(Point[] ps) {
        this.transPoints = ps.clone();
    }
    public void setInvTrans(double[][] invTrans) {
        this.invTrans = invTrans.clone();
    }
    public void setTrans(double[][] trans) {
        this.trans = trans.clone();
    }
    private Point getOrigPoint(Point p) {
        double m1 = p.x*invTrans[0][0] + p.y * invTrans[0][1] + invTrans[0][2];
        double m2 = p.x*invTrans[1][0] + p.y * invTrans[1][1] + invTrans[1][2];
        double m3 = p.x*invTrans[2][0] + p.y * invTrans[2][1] + invTrans[2][2];
        return new Point((int)(m1/m3), (int)(m2/m3));
    }

}
