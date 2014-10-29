package org.opencv.samples.imagemanipulations;

import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;

import java.util.ArrayList;

/**
 * This object holds colors on a given line.
 * Created by vassdoki on 10/29/14.
 */
public class LineColors {
    private static final String TAG = "OCVSample::DokiPicture";

    private ColorImage colorImage;
    private ArrayList<Byte> colors = new ArrayList<Byte>();
    private int[] numOfColors = new int[4];

    public LineColors(ColorImage colorImage) {
        this.colorImage = colorImage;
    }
    public void update(MLinea l) {
        colors.clear();
        numOfColors = new int[4];
        Point currP = l.getStart();
        double[] step = l.getStep();

        int[] colors = new int[4];
        colors[0] = Color.argb(255, 50, 150, 150);
        colors[1] = Color.argb(255, 255,200,150);
        colors[2] = Color.argb(255, 255,0,0);
        colors[3] = Color.argb(255, 0,255,0);

        int steps = l.getStepLength();
        byte currColor=0;
        for(int i = 0; i < steps; i++) {
            currP.x = (int) (l.start.x + i * step[0]);
            currP.y = (int) (l.start.y + i * step[1]);
            currColor = colorImage.getPxColor(currP.x, currP.y);
            //Log.i(TAG, "lineColor: " + currP.x+","+currP.y+" val: " + currColor);
            this.colors.add(currColor);
            numOfColors[currColor]++;
            colorImage.debugPoint("getColoredPixelNum", currP, colors[currColor]);
        }
    }

    public float getColorRatio() {
        return (float)(numOfColors[2] + numOfColors[3]) / colors.size();
    }

    public Integer getBlackWhiteError() {
        int half = colors.size() / 2;
        int sumLeft=0, sumRight=0;
        for(int j = 0; j < colors.size(); j++) {
            if (j < half) {
                sumLeft += colors.get(j) % 2;
            }
            // direkt nem adjuk hozza a kozepet
            if (j > half) {
                sumRight += colors.get(j) % 2;
            }
        }
        int colorLeft, colorRight;
        if (sumLeft > sumRight) {
            colorLeft = 1;
            colorRight = 0;
        } else {
            colorLeft = 0;
            colorRight = 1;
        }
        // megszamoljuk a kozeptol rossz helyen levoket, de amit jot talalunk a tobbi mar nem erdekel
        int badLeft = 0;
        int badRight = 0;
        boolean continueLeft = true;
        boolean continueRight = true;
        for(int j = 1; j < half; j++) {
            if (colors.get(half - j) % 2 != colorLeft && continueLeft) {
                badLeft ++;
            } else {
                continueLeft = false;
            }
            if (colors.get(half + j) % 2 != colorRight && continueRight) {
                badRight ++;
            } else {
                continueRight = false;
            }
        }
        //Log.i(TAG, "xxx;balance;"+debugArrayList(cs) + ";result left;"+badLeft+";resultRight;"+badRight+";result;"+(badRight-badLeft));
        return badRight - badLeft;

    }

    public String colorNumString() {
        return "Bl:"+numOfColors[0] + " Wh:" + numOfColors[1] + " Re:" + numOfColors[2] + " Gr:" + numOfColors[3];
    }
}
