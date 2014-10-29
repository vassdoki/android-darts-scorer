package org.opencv.samples.imagemanipulations;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This object holds the original image and provides access to the pixels
 *
 * Created by vassdoki on 10/29/14.
 */
public class ColorImage {
    private int width;
    private int height;
    private Bitmap bmp32;
    private Mat mat;

    private static final float RED_RATIO = (float)2;
    private static final float RED_MIN = 60;
    private static final float GREEN_RATIO = (float)1.6;
    private static final float GREEN_MIN = 20;
    private static final int PIXEL_BLACK_LIMIT = 3 * 25;

    private HashMap<String, Canvas> debugCanvas = new HashMap<String, Canvas>();
    private HashMap<String, Bitmap> debugBitmap = new HashMap<String, Bitmap>();

    private static final String TAG = "OCVSample::DokiPicture";

    public ColorImage(String mCurrentPhotoPath) {
        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        width = bmOptions.outWidth;
        height = bmOptions.outHeight;

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
    }

    /**
     * @return The image as a Mat object
     */
    public Mat getMat() {
        if (mat == null) {
            mat = new Mat();
            Utils.bitmapToMat(bmp32, mat);
        }
        return mat.clone();
    }

    /**
     * Returns a pixel as an RGB array from the original image.
     * @param x x coordinate
     * @param y y coordinate
     * @return [R, G, B]
     */
    public int[] getPx(int x, int y) {
        int color = bmp32.getPixel(x,y);
        return new int[]{
                (color & 0xff0000) >> 16,
                (color & 0x00ff00) >> 8,
                (color & 0x0000ff)
        };
    }

    /**
     * Returns the color of a pixel from the original image.
     * @param x x coordinate
     * @param y y coordinate
     * @return 0: black, 1: white, 2: red, 3: green
     */
    public int getPxColor(int x, int y) {
        int[] pi = getPx(x, y);
        if (pi[0] > RED_MIN && pi[0] > RED_RATIO * pi[1]  && pi[0] > RED_RATIO * pi[2]) {
            return 2;
        }
        if (pi[1] > GREEN_RATIO * pi[0]  && pi[1] > GREEN_RATIO * pi[2]) {
            return 3;
        }
        int pSumVals = 0;
        for(int i = 0; i < 3; i++) {
            pSumVals+=pi[i];
        }
        if (pSumVals > PIXEL_BLACK_LIMIT) {
            return 1;
        }
        return 0;
    }
    public int getPxColor(Point p) {
        return getPxColor(p.x, p.y);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Find the lines on the table from the bull. This is not exact, only for starting the
     * processing.
     * See the colors in growing circles around the bull.
     * If the number of change in black and white is 20, then record the degree of the changes.
     * If we have 10 radius where the number of changes is 20, then:
     * - sort the arrays
     * - cut the 1/4 of both sides
     * - take the average of the remaining.
     * This will be the estimate for the 20 lines.
     *
     * Körülbelüli meghatározása a bull-ból kinduló vonalak szögének 0 foktól (0 lefele van a 3-as irányába)
     * egyre nagyobb körökben nézzük meg a pontok színét addig, amíg találunk olyat, ami pontosan 20 felé osztja a kört.
     * nem baj, ha nem pontos, mert nem ezt fogjuk majd használni. Ez csak arra kell, hogy ha találunk 4 pontos pontot, akkor
     * meg tudjuk mondani, hogy a nem elforgatott táblán hova esnek azok a pontok.
     * @return List of degrees where the black and white segments change
     */
    public ArrayList<Integer> estimateColorSegments(Point bull) {
        if (bull == null) {
            Log.e(TAG, "Bull is NULL, can't continue");
            return null;
        }
        // this will hold the 20 degree where the color turns
        ArrayList<ArrayList<Integer>> colorChanges = new ArrayList<ArrayList<Integer>>();
        for(int i = 0; i < 20; i++) {
            colorChanges.add(new ArrayList<Integer>());
        }

        double minDegreeDiff = 6;
        double degreeStep = 1;
        double distanceStart = 20;
        double distanceStep = 5;
        double currDistance = distanceStart;
        // Egyre nagyobb körökben degreeStep fokonként veszünk egy pontot és megnézzük milyen színű.
        // Ha a körben pont 20 szín váltást találunk, akkor feljegyezzük a váltások helyét a
        // colorChanges-be
        while(colorChanges.get(0).size() < 10) {
            ArrayList<Integer> colorChangesAroundBull = getColorChangesAroundBull(bull, minDegreeDiff, degreeStep, currDistance);
            Log.i(TAG, "XXFCX;cella;" + colorChangesAroundBull.size() + ";distance;" + currDistance + ";cellak;" + debugArrayList(colorChangesAroundBull));
            if (colorChangesAroundBull.size() == 20) {
                for(int i = 0; i < 20; i++) {
                    colorChanges.get(i).add(colorChangesAroundBull.get(i));
                }
            }
            currDistance += distanceStep;
        }

        ArrayList<Integer> finalFields = new ArrayList<Integer>();
        // az azonos pozícióba eső szín váltásokat a szög szerint rendezzük.
        // Az első és az utolsó negyedet elhagyjuk
        // a maradék átlagát vesszük, ez lesz a végleges becslés az adott szögre.
        // order the degree of the changes by every position
        for(int i = 0; i < 20; i++) {
            ArrayList<Integer> ai = colorChanges.get(i);
            Collections.sort(ai);
            int sum = 0;
            int count = 0;
            for(int j = ai.size()/4; j <= 3 * ai.size() /4; j++) {
                count++;
                sum += ai.get(j);
            }
            finalFields.add(sum / count);
            Point p1 = MLine.rotatePoint(bull, sum/count, 100);
            debugLine("est_col_seg", bull, p1, 255, 0, 0);
        }
        //debugSaveCanvas("est_col_seg");
        Log.i(TAG, "Final fields: " + debugArrayList(finalFields));
        return finalFields;
    }

    /**
     * Go a circle around the bull with radius
     * @param minDegreeDiff if the color change is closer to this, than that is not a change
     * @param degreeStep go by this step on the circle
     * @param radius the radius around the bull
     * @return
     */
    private ArrayList<Integer> getColorChangesAroundBull(Point bull, double minDegreeDiff, double degreeStep, double radius) {
        Point p;
        ArrayList<Integer> colorChange = new ArrayList<Integer>();
        int prevColor = -1;
        for (double i = 0; i < 360; i += degreeStep) {
            p = MLine.rotatePoint(bull, i, radius);
            if (p.y > getHeight() || p.x > getWidth()) {
                break;
            }
            int color = getPxColor(p);
            if (prevColor == -1) {
                prevColor = color;
            }
            if (prevColor != color) {
                colorChange.add((int) i);
                prevColor = color;
            }
        }
        // remove the color changes if the distance is too small
        // that must be wrong recognition of the color
        for(int i = 0; i < colorChange.size()-1; i++) {
            if (colorChange.get(i+1) - colorChange.get(i) < minDegreeDiff) {
                colorChange.remove(i+1);
                colorChange.remove(i);
                i--;
            }
        }
        return colorChange;
    }

    /**
     * 4 derekszogu vonal alapjan 4 pont meghatarozasa (0, 5, 10, 15 -os pont)
     * A körülbelüli vonal felosztás alapján a 4 derékszögű irányban szintén kb meghatározzuk,
     * hogy hol kezdődhet a trippla vonal.
     * @param segments A kb szög felosztása a bullból menő vonalaknak.
     * @return Transzformációs mátrix
     */
    public Point[] estimateTransformation(Point bull, ArrayList<Integer> segments) {
        Point[] res = new Point[8];
        for(int i = 0; i < 4; i++) {
            double degree = segments.get(i * 5).doubleValue();
            Point point = findCornerOnLine(bull, degree);
            res[i] = point;
            debugCircle("est_trans", point, 5, 255,255,0);
        }

        // ezek a pontok a szemből nézett táblán itt vannak:
        double ratio = 0.5;
        Point frontBull = new Point((int)(getWidth() * ratio), (int)(getHeight() * ratio));
        double radius = 0.4 * getHeight()  * ratio;
        for(int i = 0; i < 4; i++) {
            Point point = MLine.rotatePoint(frontBull, 9 + (i * 5) * 18, radius);
            debugCircle("est_trans", point, 5, 0,255,0);
            res[4+i] = point;
        }
        //debugSaveCanvas("est_trans");
        return res;
    }

    /**
     * Megkeressük az első potenciális trippla kereszteződést a szög mentén haladva.
     * @param bull a bull
     * @param degree a szög, amin haladunk
     * @return a pont, ahol valószínűleg a trippla kezdődik a vonalon
     */
    private Point findCornerOnLine(Point bull, double degree) {
        double distanceStart = 20 - 1;
        int searchWidth = 5;
        boolean found = false;
        double currDistance = distanceStart;
        Point p;
        Point pSave = null;
        int color=0;
        while(!found) {
            currDistance += 1;
            int count = 0;
            int colorCount = 0;
            for(int i = -1 * searchWidth; i <= searchWidth; i++) {
                count++;
                p = MLine.rotatePoint(bull, degree + i, currDistance);
                if (i == 0) {
                    pSave = p;
                }
                try {
                    color = getPxColor(p);
                }catch (Exception e) {
                    Log.i(TAG, "EXCEDPTION currDistance: " + currDistance + " x: " + p.x + " y: " + p.y + " colorCount: " + colorCount + " count: " + count);
                    found = true;
                }
                if (color > 1) {
                    colorCount++;
                }
            }
            if ((float)colorCount / count > 0.4) {
                Log.i(TAG, "currDistance(FOUND): degree: " + degree + " currDist: " + currDistance + " colorCount: " + colorCount + " count: " + count);
                found = true;
            } else {
                //Log.i(TAG, "currDistance: " + currDistance + " colorCount: " + colorCount + " count: " + count);
            }
        }
        return pSave;
    }

    private String debugArrayList(ArrayList a) {
        StringBuilder sb = new StringBuilder();
        for(Object o : a) {
            sb.append(o.toString()).append(";");
        }
        return sb.toString();
    }

    public void debugLine(String key, Point p1, Point p2, int r, int g, int b) {
        Canvas c;
        if (debugCanvas.containsKey(key)) {
            c = debugCanvas.get(key);
        } else {
            Bitmap bmp = Bitmap.createBitmap(bmp32);
            debugBitmap.put(key, bmp);
            c = new Canvas(bmp);
            debugCanvas.put(key, c);
        }
        Paint paint = new Paint();
        paint.setARGB(255, r, g, b);
        c.drawLine(p1.x, p1.y, p2.x, p2.y, paint);
    }

    public void debugCircle(String key, Point p1, float radius, int r, int g, int b) {
        Canvas c;
        if (debugCanvas.containsKey(key)) {
            c = debugCanvas.get(key);
        } else {
            Bitmap bmp = Bitmap.createBitmap(bmp32);
            debugBitmap.put(key, bmp);
            c = new Canvas(bmp);
            debugCanvas.put(key, c);
        }
        Paint paint = new Paint();
        paint.setARGB(255, r, g, b);
        paint.setStyle(Paint.Style.STROKE);
        c.drawCircle(p1.x, p1.y, radius, paint);
    }

    public void debugText(String key, Point p1, String text, int r, int g, int b) {
        Canvas c;
        if (debugCanvas.containsKey(key)) {
            c = debugCanvas.get(key);
        } else {
            Bitmap bmp = Bitmap.createBitmap(bmp32);
            debugBitmap.put(key, bmp);
            c = new Canvas(bmp);
            debugCanvas.put(key, c);
        }
        Paint paint = new Paint();
        paint.setARGB(255, r, g, b);
        c.drawText(text, p1.x, p1.y, paint);
    }

    public void debugSaveCanvas(String key) {
        Bitmap bmp;
        if (debugBitmap.containsKey(key)) {
            bmp = debugBitmap.get(key);
        } else {
            return;
        }
        try {
            String dir = Environment.getExternalStorageDirectory().toString() + "/doki";

            File f = new File(dir, key + ".png");
            f.createNewFile();
            System.out.println("file created " + f.toString());
            FileOutputStream out = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            deleteDebugCanvas(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteDebugCanvas(String key) {
        if (debugCanvas.containsKey(key)) {
            debugCanvas.remove(key);
            debugBitmap.get(key).recycle();
            debugBitmap.remove(key);
        }
    }

}
