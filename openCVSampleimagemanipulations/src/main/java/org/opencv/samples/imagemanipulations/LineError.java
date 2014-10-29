package org.opencv.samples.imagemanipulations;

import android.graphics.Point;

import java.util.ArrayList;

/**
 * Created by vassdoki on 10/29/14.
 */
public class LineError {
    private ColorImage colorImage;
    private int degree;
    private DartsTable dartsTable;
    private static final String TAG = "OCVSample::DokiPicture";
    private ArrayList<Integer> doubleAndTrippleDistance;
    private ArrayList<Integer> blackWhiteDiffs;

    /**
     * Find paralell lines from the bull in one direction.
     * Measure the distance to the double and the tripple line.
     * Measure the black and white ratio in the two sides.
     * @param degree degree of the line to analyze
     */
    public LineError(ColorImage colorImage, DartsTable dartsTable, Point bull, int degree) {
        this.colorImage = colorImage;
        this.degree = degree;
        this.dartsTable = dartsTable;

        double currDistance = DartsTable.estBullSize * 2 * dartsTable.transRatio;
        MLinea lineToCheck = new MLinea();
        doubleAndTrippleDistance = new ArrayList<Integer>();
        blackWhiteDiffs = new ArrayList<Integer>();
        int minRegionLength = (int) (DartsTable.estColoredWith * 0.3);
        int colorStatus = 0;
        int lastStatusPos = 0;

        LineColors lc = new LineColors(colorImage);
        while(doubleAndTrippleDistance.size() < 4) {
            // TODO: nem értem itt miért kell ez a *4, de különben azonos pontokat is megnéz egymás után
            currDistance += dartsTable.transRatio * 4;
            Point centerPoint = MLinea.rotatePoint(bull, degree, currDistance);
            Point start = MLinea.rotatePoint(centerPoint, (degree + 90)%360, -35);
            Point end = MLinea.rotatePoint(centerPoint, (degree + 90)%360, +35);
            colorImage.debugLine("getColoredPixelNum", start, end, 90, 90, 255);
            lineToCheck.reset(dartsTable.invTransform(start), dartsTable.invTransform(end));
            colorImage.debugLine("getColoredPixelNum", dartsTable.transform(lineToCheck.start), dartsTable.transform(lineToCheck.end), 255, 90, 255);
            lc.update(lineToCheck);

            //Log.i(TAG, "Line to check, front (degree "+degree+" dist: " + currDistance +"): colorNum: " + lc.colorNumString() + " " + start.x + "," + start.y + " - " + end.x +","+end.y+" transformed: " + lineToCheck.start.x + "," + lineToCheck.start.y + " - " + lineToCheck.end.x +","+lineToCheck.end.y);
            if (currDistance > DartsTable.estDoubleDistance * 1.6) {
                break;
            }
            if (lc.getColorRatio() < 0.1) {
                blackWhiteDiffs.add(lc.getBlackWhiteError());
            }
            if (lc.getColorRatio() > 0.15 && colorStatus == 0) {
                // megvan az elso szines
                colorStatus = 1;
                lastStatusPos = (int) currDistance;
            }
            if (lc.getColorRatio() < 0.1 && colorStatus == 1) {
                if (currDistance - lastStatusPos > minRegionLength) {
                    // lezarult az elso szines es eleg vastag
                    colorStatus = 0;
                    doubleAndTrippleDistance.add(lastStatusPos);
                    doubleAndTrippleDistance.add((int) currDistance);
                } else {
                    // nem eleg vastag a szines csik, ezert eldobjuk
                    lastStatusPos = (int) currDistance;
                    colorStatus = 0;
                }
            }
        }
        //Log.i(TAG, "szinek listaja: degree(" + degree + "): " + dartsTable.debugArrayList(doubleAndTrippleDistance));
        //saveImageToDisk(matOriginal, "step-dd", this, Imgproc.COLOR_RGBA2RGB, 901);
    }

    public float getAverageBWError() {
        float res = 0;
        for(Integer i: blackWhiteDiffs) {
            res += i;
        }
        return res / blackWhiteDiffs.size();
    }
    public int getRegion(int i) {
        if (doubleAndTrippleDistance.size() <= i) {
            return 9999;
        } else {
            return doubleAndTrippleDistance.get(i);
        }
    }

}
