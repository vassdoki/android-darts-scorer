package org.opencv.samples.imagemanipulations;

import android.util.Log;

import org.opencv.core.Point;

import java.util.ArrayList;

/**
 * Ebben az osztályban a bullból kiindulva egy szöghöz tartozó képpontokat tároljuk
 * a bulltól egyre távolodva. A képpontókból következtetünk a tábla elemeinek a pozíciójára.
 * Created by vassdoki on 10/24/14.
 */
public class PVec {
    private static final String TAG = "OCVSample::DokiPicture";
    private static final int PIXEL_BLACK_LIMIT = 3 * 25;
    private static final float FIELD_BLACK_RATIO_LIMIT = (float) 0.69;
    private static final float RED_RATIO = (float)2;
    private static final float RED_MIN = 60;
    private static final float GREEN_RATIO = (float)1.6;
    private static final float GREEN_MIN = 20;
    /** [0, 360) */
    public Point start;
    public double degree;
    public ArrayList<int[]> pixels = new ArrayList<int[]>();
    public ArrayList<Point> points = new ArrayList<Point>();
    public ArrayList<Point[]> redRegions = new ArrayList<Point[]>();
    public ArrayList<Point[]> greenRegions = new ArrayList<Point[]>();
    public ArrayList<Integer> pixelColors = new ArrayList<Integer>(); // 0: black; 1: white; 2: red; 3: green
    /** A képponthoz tartozó értékek száma (grayscale: 1, rgb: 3, rgba: 4) */
    public int channels;
    public int length;
    public boolean isBlackField;
    public boolean isStatFilled = false;
    private int numOfBlack;
    private int numOfNotBlack;

    PVec(int channels) {
        this.channels = channels;
    }

    public void fillStat() {
        if (isStatFilled) {
            return;
        }
        length = pixels.size();
        numOfBlack = 0;
        numOfNotBlack = 0;

        boolean isPrevRedRegion = false;
        Point redRegionStart = null;
        boolean isPrevgreenRegion = false;
        Point greenRegionStart = null;

        int pSumVals = 0;

        int[] pi;
        Point po;
        Point prevPoint = null;
        int pointColor;
        for(int i = 0; i < pixels.size(); i++) {
            pi = pixels.get(i);
            po = points.get(i);
            // RGB
            pointColor = -1;
            if (pi[0] > RED_MIN && pi[0] > RED_RATIO * pi[1]  && pi[0] > RED_RATIO * pi[2]) {
                pointColor = 2;
                // we are in a red region
                if (!isPrevRedRegion) {
                    isPrevRedRegion = true;
                    redRegionStart = po;
                }
            } else {
                // this is not a red region
                if (isPrevRedRegion) {
                    Point[] rr = {redRegionStart, prevPoint};
                    redRegions.add(rr);
                    isPrevRedRegion = false;
                }
            }
            if (pi[1] > GREEN_RATIO * pi[0]  && pi[1] > GREEN_RATIO * pi[2]) {
                pointColor = 3;
                // we are in a green region
                if (!isPrevgreenRegion) {
                    isPrevgreenRegion = true;
                    greenRegionStart = po;
                }
            } else {
                // this is not a green region
                if (isPrevgreenRegion) {
                    Point[] rr = {greenRegionStart, prevPoint};
                    greenRegions.add(rr);
                    isPrevgreenRegion = false;
                }
            }
            if (!isPrevgreenRegion && !isPrevRedRegion) {
                pSumVals = 0;
                for (int x : pi) pSumVals += x;
                if (pSumVals < PIXEL_BLACK_LIMIT) {
                    pointColor = 0;
                } else {
                    pointColor = 1;
                }
                if ((numOfBlack + numOfNotBlack) < 40) {
                    if (pointColor == 0) {
                        numOfBlack++;
                    } else {
                        numOfNotBlack++;
                    }
                }
            }
            prevPoint = po;
            this.pixelColors.add(Integer.valueOf(pointColor));
        }

//        if (greenRegions.size() < redRegions.size()) {
//            isBlackField = true;
//        } else {
//            isBlackField = false;
//        }
        if (numOfBlack + redRegions.size() > numOfNotBlack + greenRegions.size()) {
            isBlackField = true;
        } else {
            isBlackField = false;
        }
        isStatFilled = true;
    }

    public void add(Point p, int[] pixel) {
        if (pixel.length == channels) {
            doAdd(p, pixel);
            return;
        }
        if (pixel.length > channels) {
            int[] t = new int[channels];
            System.arraycopy(pixel, 0, t, 0, channels);
            doAdd(p, t);
            return;
        }
        int[] t = new int[channels];
        for(int i = 0; i < channels; i++) {
            if (i < pixel.length) {
                t[i] = pixel[i];
            } else {
                t[0] = 0;
            }
        }
        doAdd(p, t);
    }
    public void add(Point p, double[] pixel) {
        int[] t = new int[channels];
        if (pixel.length >= channels) {
            for(int i = 0; i < channels; i++) {
                t[i] = (int)pixel[i];
            }
            doAdd(p, t);
        } else {
            for(int i = 0; i < channels; i++) {
                if (i < pixel.length) {
                    t[i] = (int)pixel[i];
                } else {
                    t[0] = 0;
                }
            }
        }
        doAdd(p, t);
    }
    private void doAdd(Point p, int[] pixel) {
        pixels.add(pixel);
        points.add(p);
    }

    public void logStat(String tag) {
        fillStat();
        StringBuilder sb = new StringBuilder();
        int prev = 1;
        for(Point[] r: redRegions) {
            sb.append(r[0] + ";"+r[1]);
        }
        Log.i(tag, "XXPVX;Degree;"+degree+";length;"+length+";IsBlack;"+isBlackField+";numOfBlack;"+numOfBlack+";numOfNotBlack;"+numOfNotBlack+";redRegionCount;"+redRegions.size()+";redRegions;"+sb.toString());
    }

    public void setDegree(Point start, double degree) {
        this.start = start;
        this.degree = degree;
    }
}
