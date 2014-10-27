package org.opencv.samples.imagemanipulations;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.opencv.core.Core.convertScaleAbs;
import static org.opencv.core.Core.normalize;

/**
 * Created by vassdoki on 10/2/14.
 */
public class DokiPicture extends Activity {
    private static final String TAG = "OCVSample::DokiPicture";
    static final int REQUEST_IMAGE_CAPTURE = 1;

    private Button button, button2;
    private ImageView mImageView;

    // a terulet hataroloja, ahol a tabla van a kepen
    double minx=1000000, miny=1000000, maxx=-1000000, maxy=-1000000;

    private int saveStepsToImage = 12;
    // imageView's dimensions
    private int mImageViewW;
    private int mImageViewH;
    // photo dimensions
    private int photoW;
    private int photoH;
    // Canny parameters
    private int paramCanny1 = 180;
    private int paramCanny2 = 190;
    // Hough Lines parameters
    private int paramHLthreshold = 80;
    private int paramHLminLineSize = 20;
    private int paramHLlineGap = 20;

    private ArrayList<MLine> allLines;
    private float[] harrisResultVals;
    //    private ArrayList<MLine> goodLines;
//    private Point pointBull;



    public DokiPicture() {
        Log.i(TAG, "Activity created");
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.doki_picture_layout);

        mImageView = (ImageView)findViewById(R.id.imageView);

        button = (Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "button on click");
                dispatchTakePictureIntent();
            }
        });
        button2 = (Button)findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startOpenCvActivity();
            }
        });
    }

    private void startOpenCvActivity() {
        Intent intent = new Intent(this, ImageManipulationsActivity.class);
        startActivity(intent);
    }

    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        /*
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                //...
            }
            // Continue only if the File was successfully created
            Log.i(TAG, "Photo file: " + photoFile);
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
        */

        /*
        Intent pickIntent = new Intent();
        pickIntent.setType("image/*");
        pickIntent.setAction(Intent.ACTION_GET_CONTENT);

        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        String pickTitle = "Select or take a new Picture"; // Or get from strings.xml
        Intent chooserIntent = Intent.createChooser(pickIntent, pickTitle);
        chooserIntent.putExtra                (
                        Intent.EXTRA_INITIAL_INTENTS,
                        new Intent[] { takePhotoIntent }
                );

        startActivityForResult(chooserIntent, REQUEST_IMAGE_CAPTURE);
        */

        startActivityForResult(new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI), REQUEST_IMAGE_CAPTURE);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            /*
            Uri _uri = data.getData();

            //User had pick an image.
            Cursor cursor = getContentResolver().query(_uri, new String[] { android.provider.MediaStore.Images.ImageColumns.DATA }, null, null, null);
            cursor.moveToFirst();

            //Link to the image
            mCurrentPhotoPath = cursor.getString(0);
            Log.i(TAG, "file: " + mCurrentPhotoPath);
            cursor.close();
            */

            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(selectedImage,filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            mCurrentPhotoPath = cursor.getString(columnIndex);
            cursor.close();
            //mImageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));

            setPic();
            //Bundle extras = data.getExtras();
            //Bitmap imageBitmap = (Bitmap) extras.get("data");
            //mImageView.setImageBitmap(imageBitmap);
        }
    }

    private void setPic() {
        // Get the dimensions of the View
        Log.i(TAG, ">>>> setPic start");
        mImageViewW = mImageView.getWidth();
        mImageViewH = mImageView.getHeight();
        Mat matOriginalPhoto = getOriginalPhotoMat();
        Log.i(TAG, "Photo loaded");
        //saveImageToDisk(matOriginalPhoto, "step00-0-orig", "doki", this, Imgproc.COLOR_RGBA2RGB);

        Mat matCannyGray = new Mat();
        Imgproc.Canny(matOriginalPhoto, matCannyGray, paramCanny1, paramCanny2);
        Log.i(TAG, "Canny processed");

        // do harris instead of canny
//        Mat harrisResult = Mat.zeros( matOriginalPhoto.size(), CvType.CV_8UC3);
//        doHarrisProc(matOriginalPhoto, harrisResult);
//        saveImageToDisk(harrisResult, "step00-1-harris.jpg", "doki", this, -1);
//        Imgproc.cvtColor(harrisResult, matCanny, Imgproc.COLOR_RGBA2GRAY);
//        saveImageToDisk(matCanny, "step00-2-harris2.jpg", "doki", this, -1);

        Mat matLines = new Mat();
        Imgproc.HoughLinesP(matCannyGray, matLines, 1, Math.PI/180, paramHLthreshold, paramHLminLineSize, paramHLlineGap);
        Log.i(TAG, "HoughLinesP processed");

        Mat matCannyRgba = new Mat();
        Imgproc.cvtColor(matCannyGray, matCannyRgba, Imgproc.COLOR_GRAY2BGRA, 4);
        //saveImageToDisk(matCanny, "step01-canny", "doki", this, Imgproc.COLOR_RGBA2RGB);
        //matCannyRgba.release();

//  -------------------------------------------------
        // osszes vonal sargaval, es fontosak meghatarozasa
        Log.i(TAG, "HL vonalak rajzolasa");
        Mat matOriginalCopy = matOriginalPhoto.clone();
        ArrayList<MLine> goodLines = findGoodLines(matLines, matOriginalCopy);
        saveImageToDisk(matOriginalCopy, "step02-vonalak", "doki", this, Imgproc.COLOR_RGBA2RGB, 2);
        //matLines.release();

//  --------------------------------------------------------------------
        // a fontos vonalak metszespontjait szamoljuk
        Log.i(TAG, "HL fonos vonalak metszespontja");
        Point pointBull = findBullFromGoodLines(goodLines, matOriginalCopy);
        saveImageToDisk(matOriginalCopy, "step03-fontosVonalak", "doki", this, Imgproc.COLOR_RGBA2RGB, 8);

//  --------------------------------------------------------------------
        // vegig megyunk az osszes vonalon, es a bullon atmenoket megkeressuk
        // eltaroljuk az igy talalt egyeneseket
        // es a minxy es maxy pontokat, abban van a tabla
//        Log.i(TAG, "bullon atmeno vonalak keresese");
//        ArrayList<MLine> bullLines = findBullLines(allLines, pointBull, matOriginalCopy);
//        bullLines.size();
//        saveImageToDisk(matOriginalCopy, "step07-bull", "doki", this, Imgproc.COLOR_RGBA2RGB, 9);
//  --------------------------------------------------------------------


        Mat matOriginalBeforeTrans = matOriginalPhoto.clone();
        // this is not exact, but gives +-2 degree all the segments
        ArrayList<Integer> segments = findColorSegments(pointBull, matOriginalBeforeTrans);
        Point[] transRes = getPointsForTransFromSegments(pointBull, matOriginalBeforeTrans, segments);
        Mat transformed3 = doTransform(transRes, matOriginalBeforeTrans);
        double trippleDistance = MLine.distanceOfTwoPoints(pointBull, transRes[0]) * 3;
        double tableSideDistance = 700 * trippleDistance / 290;
        debugImage(transformed3, tableSideDistance, trippleDistance);
        // transformed3.submat((int)(pointBull.x - tableSideDistance), (int)(pointBull.x + tableSideDistance), (int)(pointBull.y - tableSideDistance), (int)(pointBull.y + tableSideDistance))
        saveImageToDisk(
                transformed3,
                "step12-3", "doki", this, Imgproc.COLOR_RGBA2RGB, 12);

//  -------------------------------------------------
//        boolean doHarris = false;
//        if (doHarris) {
//            Log.i(TAG, "Harris");
//            Mat harrisResult = Mat.zeros(matOriginalPhoto.size(), CvType.CV_8UC3);
//            doHarrisProc(matOriginalPhoto, harrisResult);
//            saveImageToDisk(harrisResult, "step8-cornerHarrisDstNormScaled-d", "doki", this, Imgproc.COLOR_RGBA2RGB, 8);
////  -------------------------------------------------
//            // végig megyünk a bull-ba menő vonalakonés megnézzük, hogy a harris kimeneten hol vannak leágazásaok
//            findXing(bullLines, harrisResult);
//            saveImageToDisk(harrisResult, "step9-harris-es-bull-lines", "doki", this, Imgproc.COLOR_RGBA2RGB, 9);
//        }
//
//  -------------------------------------------------
        int h = mImageView.getHeight();
        int w = mImageView.getWidth();


        int rowStart = Math.max(0, (int) (pointBull.y - h / 2));
        int colStart = Math.max(0, (int) (pointBull.x - w / 2));
        int rowEnd = rowStart + h;
        int colEnd = colStart + w;
        int maxRows = matOriginalCopy.rows();
        int maxCols = matOriginalCopy.cols();
        if (rowEnd > maxRows) {
            rowStart -= (rowEnd - maxRows);
            rowEnd -= (rowEnd - maxRows);
        }
        if (colEnd > maxCols) {
            colStart -= (colEnd - maxCols);
            colEnd -= (colEnd - maxCols);
        }

        rowEnd += h - (rowEnd - rowStart); // ha nem pixel pontos, akkor itt javitjuk
        colEnd += w - (colEnd - colStart);
        matOriginalCopy = matOriginalCopy.submat(rowStart, rowEnd, colStart, colEnd);
        //saveImageToDisk(matOriginalCopy, "step5-submat", "doki", this, Imgproc.COLOR_RGBA2RGB);

        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matOriginalCopy, bitmap);
        matOriginalCopy.release();

        Log.i(TAG, ">>>> setPic end");
        mImageView.setImageBitmap(bitmap);
    }

    private Point[] getPointsForTransFromSegments(Point pointBull, Mat matOriginalBeforeTrans, ArrayList<Integer> segments) {
        int s1a=0, s1b=0, s2a=0, s2b=0;
        int s1i=0;
        int s2i=0;
        int max1 = -1, max2 = -1;

        Point p, p2;
        Scalar color = new Scalar(255,255,255);
        for(int i = 0; i < segments.size() - 1; i++) {
            Integer degree = segments.get(i);
            int size;
            int sb = segments.get(i).intValue();
            int sa;
            if (i == 0) {
                sa = 360 - segments.get(segments.size() - 1).intValue();
                size = sb + sa;
            } else {
                sa = segments.get(i - 1).intValue();
                size = sb - sa;
            }

            if (size >= max1) {
                s2i = s1i;
                max2 = max1;
                s2a = s1a;
                s2b = s1b;
                max1 = size;
                s1i = i;
                s1a = sa;
                s1b = sb;
            }
            p = MLine.rotatePoint(pointBull, degree.intValue(), 200);
            p2 = MLine.rotatePoint(pointBull, degree.intValue(), 130);
            Core.line(matOriginalBeforeTrans, p2, p, color);
            p = MLine.rotatePoint(pointBull, degree.intValue(), 210);
            Core.putText(matOriginalBeforeTrans, ""+sb, p, 1, 0.75, color);
            p = MLine.rotatePoint(pointBull, (sa + sb)/2, 130);
            Core.putText(matOriginalBeforeTrans, ""+size, p, 1, 0.75, color);
        }


        Point[] points1 = findCorners(s1a, s1b, pointBull, matOriginalBeforeTrans);
        Core.circle(matOriginalBeforeTrans, points1[0], 3, color);
        Core.circle(matOriginalBeforeTrans, points1[1], 3, color);
        Point[] points2 = findCorners(s2a, s2b, pointBull, matOriginalBeforeTrans);
        Core.circle(matOriginalBeforeTrans, points2[0], 3, color);
        Core.circle(matOriginalBeforeTrans, points2[1], 3, color);
        saveImageToDisk(matOriginalBeforeTrans, "step11-01", "doki", this, Imgproc.COLOR_RGBA2RGB, 1101);

        // ezek a pontok a szemből nézett táblán itt vannak:
        double ratio = 0.5;
        Point frontBull = new Point(photoW * ratio, photoH * ratio);
        double radius = 0.4 * photoH  * ratio;
        Point ta1 = MLine.rotatePoint(frontBull, 9 + (s1i -1) * 18, radius);
        Point ta2 = MLine.rotatePoint(frontBull, 9 + (s1i +0) * 18, radius);
        Point tb1 = MLine.rotatePoint(frontBull, 9 + (s2i -1) * 18, radius);
        Point tb2 = MLine.rotatePoint(frontBull, 9 + (s2i +0) * 18, radius);

        return new Point[]{points1[0], points1[1], points2[0], points2[1], ta1, ta2, tb1, tb2};
    }

    private void debugImage(Mat transformed3, double tableSideDistance, double trippleDistance) {
        Point nBull = new Point(photoW/2, photoH/2);
        Point px = new Point();
        Scalar colorx = new Scalar(250, 200, 10);
        for (int i = 9; i < 360; i += 18) {
            px = MLine.rotatePoint(nBull, i, tableSideDistance);
            Core.line(transformed3, nBull, px, colorx);
        }
        Core.circle(transformed3, nBull, (int)trippleDistance, colorx);
        Core.circle(transformed3, nBull, (int)tableSideDistance, colorx);
    }

    /**
     * a és b szög között kell megtalálni azt az egyenes vonalat, ami X%-ban zöld vagy piros vonalat tartalmaz.
     * Ha ez megvan, akkor még szélesebb vonalban a szín kezdeténél megkeressük a sarkot.
     * @param a
     * @param b
     * @param matOriginalCopy
     * @return
     */
    private Point[] findCorners(int a, int b, Point bull, Mat matOriginalCopy) {
        double distanceStart = 20 - 1;
        boolean found = false;
        double currDistance = distanceStart;
        int currentSegmentColor = -1;
        Point p;
        int[] segmentColorCount = new int[4];
        int color=0;
        while(!found) {
            currDistance += 1;
            // TODO: mi van ha 360-at atlepjuk?
            int count = 0;
            int colorCount = 0;
            for(int i = a + 1; i < b; i++) {
                count++;
                p = MLine.rotatePoint(bull, i, currDistance);
                try {
                    color = PVec.getColor(matOriginalCopy.get((int) p.y, (int) p.x));
                }catch (Exception e) {
                    Log.i(TAG, "EXCEDPTION currDistance: " + currDistance + " x: " + p.x + " y: " + p.y + " colorCount: " + colorCount + " count: " + count);
                    found = true;
                }
                segmentColorCount[color]++;
                if (color > 1) {
                    colorCount++;
                }
            }
            if ((float)colorCount / count > 0.4) {
                found = true;
            } else {
                Log.i(TAG, "currDistance: " + currDistance + " colorCount: " + colorCount + " count: " + count);
            }
        }

        // currDistance a távolságot tartalmazza, ahol 75%-a a pontoknak színes.
        // A szegmens színe
        if (segmentColorCount[0] + segmentColorCount[2] > segmentColorCount[1] + segmentColorCount[3]) {
            currentSegmentColor = 0;
            Core.line(matOriginalCopy, MLine.rotatePoint(bull, a, currDistance), MLine.rotatePoint(bull, b, currDistance), new Scalar(200,0,0));
        } else {
            currentSegmentColor = 1;
            Core.line(matOriginalCopy, MLine.rotatePoint(bull, a, currDistance), MLine.rotatePoint(bull, b, currDistance), new Scalar(0,200,0));
        }
        Log.i(TAG, "Segment color: " + segmentColorCount[0] + "," + segmentColorCount[1] + "," + segmentColorCount[2] + "," + segmentColorCount[3] + " res: " + currentSegmentColor);
        // megkeressuk mindket iranyba a szin valtast:
        Point[] res = new Point[2];
        res[0] = findCorners2(currentSegmentColor, currDistance, -1, a, b, bull, matOriginalCopy);
        res[1] = findCorners2(currentSegmentColor, currDistance, 1, a, b, bull, matOriginalCopy);
        return res;
    }

    private Point findCorners2(int currentSegmentColor, double currDistance, int direction, int a, int b, Point bull, Mat matOriginalCopy) {
        int edge = -1;
        Point p = null;
        Point prevPoint = null;
        Point toPrevPoint = null;
        int center = (a + b) / 2;
        double d = 1;
        Scalar[] colors = new Scalar[4];
        colors[0] = new Scalar(50,50,50);
        colors[1] = new Scalar(255,255,255);
        colors[2] = new Scalar(255,0,0);
        colors[3] = new Scalar(0,255,0);

        while(edge == -1) {
            int count = 0;
            int colorCount = 0;
            if (edge == -1) {
                for (int i = -10; i < 11; i++) {
                    p = MLine.rotatePoint(bull, center + (direction * d), currDistance + i);
                    if (i == 0) {
                        toPrevPoint = p;
                    }
                    int color = PVec.getColor(matOriginalCopy.get((int) p.y, (int) p.x));

                    //Core.line(matOriginalCopy, p, p, colors[color]);

                    if (color % 2 != currentSegmentColor) {
                        colorCount++;
                    }
                    count++;
                }
                if ((float) colorCount / count > 0.75) {
                    Log.i(TAG, "findCorners2 FOUND: currentScolor: " + currentSegmentColor + " currDistance: " + currDistance + " direction: " + direction + " a: " + a + " b: " + b + " colorCount: " + colorCount + " count: " + count);
                    if (prevPoint == null) {
                        return p;
                    } else {
                        return prevPoint;
                    }
                } else {
                    Log.i(TAG, "findCorners2: currentScolor: " + currentSegmentColor + " currDistance: " + currDistance + " direction: " + direction + " a: " + a + " b: " + b + " colorCount: " + colorCount + " count: " + count);
                }
            }
            d += 0.5;
            prevPoint = toPrevPoint;
        }
        return p;
    }

    private ArrayList<Integer> findColorSegments(Point bull, Mat matOriginalCopy) {
        // egyre nagyobb körökben nézzük meg a pontok színét addig, amíg találunk olyat, ami pontosan 20 felé osztja a kört.
        // nem baj, ha nem pontos, mert nem ezt fogjuk majd használni. Ez csak arra kell, hogy ha találunk 4 pontos pontot, akkor
        // meg tudjuk mondani, hogy a nem elforgatott táblán hova esnek azok a pontok.

        double minDegreeDiff = 6;
        double degreeStep = 1;
        double distanceStart = 20;
        double distanceStep = 5;
        // TODO: A Mat objektumból olvassuk a pontokat, ami sokkal lassab, mintha kikérnénk egyszer az egészet
        // plusz nem kell az egész Mat, csak egy része is elég lenne
        Point p;
        boolean found = false;
        double currDistance = distanceStart;
        ArrayList<ArrayList<Integer>> colorChanges = new ArrayList<ArrayList<Integer>>();
        while(!found) {
            int prevColor = -1;
            double prevStart = -1;
            ArrayList<Integer> colorChange = new ArrayList<Integer>();
            for (double i = 0; i < 360; i += degreeStep) {
                p = MLine.rotatePoint(bull, i, currDistance);
                if (p.y > matOriginalCopy.rows() || p.x > matOriginalCopy.cols()) {
                    found = true;
                    continue;
                }
                int color = PVec.getColor(matOriginalCopy.get((int) p.y, (int) p.x)) % 2;
                if (prevColor == -1) {
                    prevColor = color;
                    prevStart = i;
                }
                if (prevColor != color) {
                    colorChange.add(Integer.valueOf((int)i));
                    prevColor = color;
                }
            }
            for(int i = 0; i < colorChange.size()-1; i++) {
                if (colorChange.get(i+1) - colorChange.get(i) < minDegreeDiff) {
                    colorChange.remove(i+1);
                    colorChange.remove(i);
                    i--;
                }
            }
            if (colorChange.size() == 20) {
                Log.i(TAG, "XXFCX;cella;"+colorChange.size()+";distance;"+currDistance+";cellak;"+debugArrayList(colorChange));
                colorChanges.add(colorChange);
            }
            if (colorChanges.size() > 10) {
                found = true;
            }
            currDistance += distanceStep;
        }

        ArrayList<ArrayList<Integer>> colorChanges2 = new ArrayList<ArrayList<Integer>>();
        for(int i = 0; i < 20; i++) {
            colorChanges2.add(new ArrayList<Integer>());
        }
        for(ArrayList<Integer> x: colorChanges) {
            for(int i = 0; i < 20; i++) {
                colorChanges2.get(i).add(x.get(i));
            }
        }
        ArrayList<Integer> finalFields = new ArrayList<Integer>();
        for(int i = 0; i < 20; i++) {
            ArrayList<Integer> ai = colorChanges2.get(i);
            Collections.sort(ai);
            int sum = 0;
            int count = 0;
            for(int j = ai.size()/4; j <= 3 * ai.size() /4; j++) {
                count++;
                sum += ai.get(j);
            }
            finalFields.add(Integer.valueOf(sum/count));
        }
        Log.i(TAG, "Final fields: " + debugArrayList(finalFields));
        return finalFields;
    }

    private Point[] findCircle(Point bull, ArrayList<MLine> bullLines, Mat matCannyGray, Mat matCannyRgba, Mat matOriginalCopy) {
        // megkeressük a leghosszabb bull vonalat, ami nem megy át a bullon
        double maxBullLineLengh = -1;
        MLine maxLine = null;
        for(MLine l : bullLines) {
            if (l.endDistance(bull) < 20) {
                if (l.length() > maxBullLineLengh) {
                    maxBullLineLengh = l.length();
                    maxLine = l;
                }
            }
        }
        assert(maxBullLineLengh > -1);
        double maxLength = maxBullLineLengh * 1.4;

        int dstChannels = matCannyGray.channels();
        int pixelNum = (int)matCannyGray.total() * dstChannels;
        int type = matCannyGray.type();
        Log.i(TAG, "type: " + CvType.typeToString(type));
        byte[] cannyVals = new byte[pixelNum];
        matCannyGray.get(0,0, cannyVals);


        Point p;
        Scalar lightBlue = new Scalar(0, 255, 255);
        double degreeStep = 0.5;
        MLine l = new MLine();
        ArrayList<ArrayList<Double>> points = new ArrayList<ArrayList<Double>>();
        ArrayList<double[]> pointNumbers = new ArrayList<double[]>();
        ArrayList<PVec> pVecs = new ArrayList<PVec>();
        for(double i = 0; i < 360; i+= degreeStep) {
            // TODO: itt elég lenne egy viszonylag szűk sávban körbe menni. A bullhoz közeli és nagyon
            // távoli rész inkább csak ront a pontosságon.
            ArrayList<Double> currPoints = new ArrayList<Double>();
            currPoints.add(Double.valueOf((double) i));
            p = maxLine.rotatePoint(bull, i, maxLength);
            //Core.circle(matCannyRgba, p, 3, lightBlue);
            //Core.line(matCannyRgba, bull, p, lightBlue);
            l.reset(bull, p);
            //currPoints.addAll(deteckPointsOnLine(cannyVals, matCannyRgba, l));
            PVec pv = detecktColorPoints(i, l, matOriginalCopy);
            pVecs.add(pv);
            //pv.logStat(TAG);
            points.add(currPoints);
            double[] pn = new double[2];
            pn[0] = i;
            pn[1] = sumPoints(currPoints, maxLength);
            pointNumbers.add(pn);
        }
        boolean isPrevBlack = false;
        Scalar cWhite = new Scalar(255, 255, 255);
        Scalar[] colors = new Scalar[4];
        colors[0] = new Scalar(0,0,0);
        colors[1] = new Scalar(255,255,255);
        colors[2] = new Scalar(255,0,0);
        colors[3] = new Scalar(0,255,0);
        for(int i = 0; i < pVecs.size(); i++) {
            PVec pv = pVecs.get(i);
            if (i==0) isPrevBlack = pv.isBlackField;
            if (isPrevBlack != pv.isBlackField) {
                Point point = MLine.rotatePoint(pv.start, pv.degree, maxLength);
                Core.line(matOriginalCopy, pv.start, point, cWhite);
                Core.putText(matOriginalCopy, ""+pv.degree, point, 1, 0.75, cWhite);
                isPrevBlack = pv.isBlackField;
            }
            Point point1 = new Point();
            for(int j=0; j < pv.pixelColors.size(); j++) {
                point1.x = 10 + MLine.distanceOfTwoPoints(pv.start, pv.points.get(j)) * 2;
                point1.y = i * 2 + 10;
                Core.line(matOriginalCopy, point1, point1, colors[pv.pixelColors.get(j).intValue()], 2);
            }
        }
        saveImageToDisk(matOriginalCopy, "step11-00", "doki", this, Imgproc.COLOR_RGBA2RGB, 111);

        Core.line(matCannyRgba, new Point(bull.x-5, bull.y), new Point(bull.x+5, bull.y), new Scalar(255, 0, 255), 1);
        Core.line(matCannyRgba, new Point(bull.x, bull.y-5), new Point(bull.x, bull.y+5), new Scalar(255, 0, 255), 1);
        //saveImageToDisk(matCannyRgba, "step11-00", "doki", this, Imgproc.COLOR_RGBA2RGB, 111);

        ArrayList<double[]> linesFromBull = findLocaleMaximas(pointNumbers);
        Scalar c = new Scalar(0, 255, 255);
        double max1 = -1, max2 = -1;
        int lineNum1 = -1, lineNum2 = -1;
        double[] max1d = new double[2];
        double[] max2d = new double[2];
        double prevD = -1;
        double d[];
        //for(double[] d: linesFromBull) {
        for(int i = 0; i < linesFromBull.size(); i++ ){
            d = linesFromBull.get(i);
            if (i == 0) {
                prevD = 360 - linesFromBull.get(linesFromBull.size()-1)[0];
            }
            double diff = Math.abs(d[0] - prevD);
            if (diff > max1) {
                max2 = max1;
                max2d[0] = max1d[0];
                max2d[1] = max1d[1];
                lineNum2 = lineNum1;
                max1 = diff;
                max1d[0] = prevD;
                max1d[1] = d[0];
                lineNum1 = i - 1;
            }
            p = maxLine.rotatePoint(bull, d[0], maxLength);
            Core.line(matCannyRgba, bull, p, c);
            Core.putText(matCannyRgba, ""+d[0], p, 1, 0.75, c);
            Core.line(matOriginalCopy, bull, p, c);
            Core.putText(matOriginalCopy, ""+d[0], p, 1, 0.75, c);
            prevD = d[0];
        }
        saveImageToDisk(matCannyRgba, "step11-2", "doki", this, Imgproc.COLOR_RGBA2RGB, 11);

        // megkeressük a két legnagyobb szöget (max1, max2), ezek egymással szemben lesznek, és a köztük levő tripla
        // sáv merőleges lesz a bull-ra (nagyon remélem mindig igaz ez)

        // összeadjuk a pontokat a 2 szög között, de csak a bull-tól 1/3-nál messzebb levőket, mert közel túl zajos
        // ezekben az összegekben az első lokális maximumot keressük, ott lesz a beső tripla
        double distanceFromBull1 = findTripple(max1d[0], max1d[1], (int)(maxBullLineLengh * 0.33), (int)maxBullLineLengh, points);
        double distanceFromBull2 = findTripple(max2d[0], max2d[1], (int)(maxBullLineLengh * 0.33), (int)maxBullLineLengh, points);
        Point a1 = maxLine.rotatePoint(bull, max1d[0], distanceFromBull1);
        Point a2 = maxLine.rotatePoint(bull, max1d[1], distanceFromBull1);
        Point b1 = maxLine.rotatePoint(bull, max2d[0], distanceFromBull2);
        Point b2 = maxLine.rotatePoint(bull, max2d[1], distanceFromBull2);
        c = new Scalar(80, 250, 50);
        Core.circle(matCannyRgba, a1, 5, c, 2);
        Core.circle(matCannyRgba, a2, 5, c, 2);
        Core.circle(matCannyRgba, b1, 5, c, 2);
        Core.circle(matCannyRgba, b2, 5, c, 2);
        saveImageToDisk(matCannyRgba, "step11-3", "doki", this, Imgproc.COLOR_RGBA2RGB, 11);

        // ezek a pontok a szemből nézett táblán itt vannak:
        Point frontBull = new Point(photoW/2, photoH/2);
        double radius = 0.4 * photoH / 2;
        Point ta1 = maxLine.rotatePoint(frontBull, 9 + (lineNum1 +0) * 18, radius);
        Point ta2 = maxLine.rotatePoint(frontBull, 9 + (lineNum1 +1) * 18, radius);
        Point tb1 = maxLine.rotatePoint(frontBull, 9 + (lineNum2 +0) * 18, radius);
        Point tb2 = maxLine.rotatePoint(frontBull, 9 + (lineNum2 +1) * 18, radius);

        return new Point[]{a1, a2, b1, b2, ta1, ta2, tb1, tb2};
    }

    /**
     * points első értéke a szög, ahol a pontok vannak .utána minden érték egy távolságot jelent, ahol van pont.
     * @param d1
     * @param d2
     * @param minDist
     * @param points
     * @return
     */
    private int findTripple(double d1, double d2, int minDist, int maxDist, ArrayList<ArrayList<Double>> points) {
        int size = maxDist - minDist;
        int[] vals = new int[size];
        int maxValue = 0;
        for(ArrayList<Double> ps: points) {
            if (isBetweenDegree(d1, d2, ps.get(0))) {
                maxValue++;
                int idx;
                for(int i = 1; i < ps.size(); i++) {
                    idx = Math.min(ps.get(i).intValue() - minDist, size - 1);
                    if (idx < 0) continue;
                    vals[idx]++;
                }
            }
        }
        Log.i(TAG, "d1: " + d1 + " d2: " + d2 + " minDist: " + minDist + " maxDist: " + maxDist);
        debugIntArray(vals);
        // a vals-ban van minDist-től maxDist-ig az oszlopok ban levő pixelek darabszáma.
        // Ezekből keressük az első lokális maximumot
        // minimum 60%-os értéketet keresünk, ami 20% alatti értékekkel van körülvéve
        int pos = -1;
        int posMax = -1;
        int currPos = 0;
        boolean foundFirst = false;
        int filterMinValue = (int)(maxValue * 0.2);
        int filterMaxValue = (int)(maxValue * 0.6);
        Log.i(TAG, "filterMinValue: " + filterMinValue + " filterMaxValue: " + filterMaxValue);
        for(int value: vals) {
            if (!foundFirst && value > filterMinValue) {
                continue;
            } else {
                foundFirst = true;
            }
            if (value > filterMaxValue && value > posMax) {
                posMax = value;
                pos = currPos;
            }
            if (pos > -1 && value < filterMinValue) {
                // end of the locale intervall
                break;
            }
            currPos++;
        }
        Log.i(TAG, "pos: " + pos +  " posMax: " + posMax);
        if (pos > -1) {
            return pos + minDist;
        } else {
            return -1;
        }
    }

    private String debugArrayList(ArrayList a) {
        StringBuilder sb = new StringBuilder();
        for(Object o : a) {
            sb.append(o.toString() + ";");
        }
        return sb.toString();
    }
    private void debugIntArray(int[] vals) {
        StringBuilder sb = new StringBuilder();
        for(int x : vals) {
            sb.append(x + ";");
        }
        Log.i(TAG, "Int array;" + sb.toString());
    }

    private boolean isBetweenDegree(double d1, double d2, Double degree) {
        if (d2 > d1) {
            return d1 < degree && degree < d2;
        } else {
            // e.g.: d1 = 350; d2 = 10
            return d1 < degree || degree < d2;
        }
    }

    private Mat doTransform(Point[] trn, Mat img) {
        Mat perspectiveTransform = getTransformationMatrix(trn);
        Mat out = img.clone();
        Imgproc.warpPerspective(img, out, perspectiveTransform, img.size(), Imgproc.INTER_CUBIC); // Imgproc.INTER_LINEAR + Imgproc.CV_WARP_FILL_OUTLIERS
        return out;
    }

    private Mat getTransformationMatrix(Point[] trn) {
        List<Point> src_pnt = new ArrayList<Point>();
        for(int i = 0; i < trn.length/2; i++) {
            src_pnt.add(trn[i]);
        }
        Mat startM = Converters.vector_Point2f_to_Mat(src_pnt);

        List<Point> dst_pnt = new ArrayList<Point>();
        for(int i = trn.length/2; i < trn.length; i++) {
            dst_pnt.add(trn[i]);
        }
        Mat endM = Converters.vector_Point2f_to_Mat(dst_pnt);

        return Imgproc.getPerspectiveTransform(startM, endM);
    }

    private ArrayList<double[]> findLocaleMaximas(ArrayList<double[]> pointNumbers) {
        // pointNumbers [szog, darab] -ban megkeressuk a lokalis maximumokat.
        // 6 fokonként csináljuk a lokális maximumokat
        // addig mindeképpen lépegetünk előre, amíg a következő nagyobb, mint az aktuális.
        // lefele maximum 3 fokot haladunk (3 fokon át kisebbet olvastunk, mint a megtalált maximum)
        ArrayList<double[]> result = new ArrayList<double[]>();
        Log.i(TAG, "Lokalis maximum kereses kezdo ertek: ");
        debugArrayDouble(pointNumbers);
        double localeWidth = 3; // A keresési intervellum fele
        double degree;
        double value;
        double prevValue = 0;
        double maxValue = 0;
        double maxValueDegree = 0;
        boolean isFirst = true;
        for(double[] d: pointNumbers) {
            degree = d[0];
            value = d[1];
            if (isFirst) {
                maxValue = value;
                maxValueDegree = degree;
                isFirst = false;
                continue;
            }
            if (value >= maxValue) {
                // we go up, until we can
                maxValue = value;
                maxValueDegree = degree;
            } else {
                if (prevValue >= value) {
                    // we go down until we can
                } else {
                    if (degree - maxValueDegree > localeWidth) {
                        // we have find a locale maximum
                        double[] res = new double[2];
                        res[0] = maxValueDegree;
                        res[1] = maxValue;
                        result.add(res);
                        maxValue = value;
                        maxValueDegree = degree;
                    }
                }
            }
            prevValue = value;
        }
        Log.i(TAG, "Elso cilkus utan eredmeny: ");
        debugArrayDouble(result);

        // rendezzuk csokkeno sorrendben
        Collections.sort(result, new Comparator<double[]>() {
            public int compare(double[] d1, double[] d2) {
                return -1 * Double.compare(d1[1], d2[1]);
            }
        });
        Log.i(TAG, "Rendezve: ");
        debugArrayDouble(result);

        // kivesszuk a 20 felettieket
        for(int i = result.size()-1; i > 19; i--) {
            result.remove(i);
        }

        // ujra rendezzuk szog szerint
        Collections.sort(result, new Comparator<double[]>() {
            public int compare(double[] d1, double[] d2) {
                return Double.compare(d1[0], d2[0]);
            }
        });
        Log.i(TAG, "A keresett szogek: ");
        debugArrayDouble(result);

        return result;
    }

    private void debugArrayDouble(ArrayList<double[]> a) {
        StringBuffer sb1 = new StringBuffer("yyy1;");
        StringBuffer sb2 = new StringBuffer("yyy2;");
        for(double[] x : a) {
            sb1.append(x[0] + ";");
            sb2.append(x[1] + ";");
        }
        Log.i(TAG, sb1.toString());
        Log.i(TAG, sb2.toString());
    }


    private double sumPoints(ArrayList<Double> p, double maxLength) {
        StringBuilder sb = new StringBuilder();
        int x = 1;
        int[] vals = new int[(int)(maxLength * 2)];
        double sum = 0;
        boolean skipForst = true;
        for(double d: p) {
            if (skipForst) {
                skipForst=false;
                continue;
            }
            // TODO: nem nézzük az értéket, csak 0 vagy 1-et használunk. Meg kell nézni, hogy az érték többet mond-e
            sum ++;
            vals[(int)(d/x)]++;
        }
        sb.append("xxx;" + p.get(0).toString() + ";");
        for(int i: vals) {
            if (i==0) {
                sb.append(";");
            } else {
                sb.append(i + ";");
            }
        }
        //Log.i(TAG, sb.toString());
        return sum;
    }

    private PVec detecktColorPoints(double degree, MLine l, Mat matOriginalCopy) {
        Point currP = l.start.clone();
        Point step = l.getStep();
        int stepLength = l.getStepLength();
        int i = 0;
        int x,y;
        Point p = new Point();
        int stepSkip = 1;
        double[] val;
        PVec pv = new PVec(3);
        pv.setDegree(l.start, degree);
        while(i < stepLength/stepSkip) {
            currP.x += step.x * stepSkip;
            currP.y += step.y * stepSkip;
            i+=stepSkip;
            pv.add(currP.clone(), matOriginalCopy.get((int) currP.y, (int) currP.x));
        }
        pv.fillStat();
        for(Point[] r: pv.redRegions) {
            Core.line(matOriginalCopy, r[0], r[1],new Scalar(255,80,0));
        }
        for(Point[] r: pv.greenRegions) {
            Core.line(matOriginalCopy, r[0], r[1],new Scalar(60,250,20));
        }
        return pv;
    }

    private ArrayList<Double> deteckPointsOnLine(byte[] cannyVals, Mat matCannyRgba, MLine l) {
        ArrayList<Double> result = new ArrayList<Double>();
        // végig megyünk a vonal pontjain
        Point currP = l.start.clone();
        Point step = l.getStep();
        int stepLength = l.getStepLength();
        int i = 0;
        int x,y;
        Scalar colorGray = new Scalar(155, 155, 155);
        Scalar colorLight = new Scalar(200, 50, 155);
        //sampleSize = 10;
        Point p = new Point();
        int stepSkip = 1;
        while(i < stepLength/stepSkip) {
            currP.x += step.x * stepSkip;
            currP.y += step.y * stepSkip;
            i+=stepSkip;
            float currValue = 0;
            for(int m = -1; m < 2; m++) {
                for(int n = -1; n < 2; n++) {
                    x = (int)currP.x + m;
                    y = (int)currP.y + n;
                    currValue += cannyVals[y * photoW + x];
                    p.x = x;
                    p.y = y;
                    //Core.line(matCannyRgba, p, p, colorGray);
                }
            }
            if (Math.abs(currValue) > 0) {
                //Core.line(matCannyRgba, currP, currP, colorLight);
                double distance = l.distanceOfTwoPoints(l.start, currP);
                result.add(distance);
            }
        }
        return result;
    }

    private void findPerspective(Mat img, Point bull, ArrayList<MLine> bullLines) {
        // kivágjuk a minxy és maxxy közötti részt, csak azon próbálkozunk, hogy gyorsabb legyen
        Log.i(TAG, "Submat: " + minx + " " + maxx + " y: " + miny + " " + maxy);
        Mat subMat = img.submat((int)miny, (int)maxy, (int)minx, (int)maxx);
        saveImageToDisk(subMat, "step10-01", "doki", this, Imgproc.COLOR_RGBA2RGB, 10);
        Log.i(TAG, "Bull: " + bull.x + ",'" + bull.y + " Bull lines: ");
        int i = 0;
        // kiszedjük az azonos vonalakat, ahol 2 foknál kisebb a különbség
        HashMap<Double, MLine> lines = new HashMap<Double, MLine>();
        for(MLine l : bullLines) {
            Log.i(TAG, "i: " + i + " start: " + l.start.x + "," + l.end.y + " -> " + l.end.x + "," + l.end.y + " angle: " + l.getAngeInDegree() + " length: " + l.length());
            i++;
            boolean inTheSet = false;
            for(Double d: lines.keySet()) {
                if (Math.abs(l.getAngeInDegree() - d) < 2 || Math.abs(l.getAngeInDegree() - d) > 360 - 2) {
                    inTheSet = true;
                    // ha ez hosszabb, akkor a rovidebbet kidobjuk
                    if (((MLine)lines.get(d)).length() < l.length()) {
                        lines.put(d, l);
                        Log.i(TAG, "This is longer, replace the previous");
                    }
                }
            }
            if (!inTheSet) {
                lines.put(l.getAngeInDegree(), l);
                Log.i(TAG, "First with this degree");
            }
        }
        // megkeressük a legkisebb szöget bezárót, reménykedünk benne, hogy ők egymás mellett levő vonalak
        MLine a = null, b = null;
        double minDegree = 400;
        Set<Double> skeys = lines.keySet();
        Double[] keys = skeys.toArray(new Double[skeys.size()]);
        for(int n = 0; n < keys.length - 1; n++ ) {
            for(int m = n+1; m < keys.length; m++ ) {
                if (Math.abs(keys[n] - keys[m]) < minDegree) {
                    // TODO: itt nem nezzük, hogy ha 180-hoz közeli a különbségük...
                    minDegree = Math.abs(keys[n] - keys[m]);
                    a = lines.get(keys[n]);
                    b = lines.get(keys[m]);
                    Log.i(TAG, "a: " + a.start + " b" + b.start);
                }
            }
        }
        if (a == null) {
            return;
        }
        Scalar colorA = new Scalar(255, 0, 0);
        Scalar colorB = new Scalar(0, 0, 255);
        Core.line(img, a.start, a.end, colorA);
        Core.line(img, b.start, b.end, colorB);
        Core.line(img, new Point(bull.x-5, bull.y), new Point(bull.x+5, bull.y), new Scalar(255, 0, 255), 1);
        Core.line(img, new Point(bull.x, bull.y-5), new Point(bull.x, bull.y+5), new Scalar(255, 0, 255), 1);
        subMat = img.submat((int)(miny*0.8), (int)(maxy*1.2), (int)(minx*0.8), (int)(maxx*1.2));
        //saveImageToDisk(subMat, "step-10-02", "doki", this, Imgproc.COLOR_RGBA2RGB, 10);

        // kijelölünk 4 pontot. a két szakasz kezdő pontja illetve ezek tükrözve a bull-ra
        Point a1 = a.start;
        Point b1 = b.start;
        Point a2 = a.mirrorPoint(a.start, bull);
        Point b2 = b.mirrorPoint(b.start, bull);
        Core.circle(img, a1, 5, colorA);
        Core.circle(img, a2, 5, colorA);
        Core.circle(img, b1, 5, colorB);
        Core.circle(img, b2, 5, colorB);


        // hova transzformáljuk ezt a 4 pontot?
        // a1-ről feltételezem, hogy jó helyen van
        double radius = a.distanceOfTwoPoints(bull, a1);
        double degree = a.getTwoPointAngle(bull, a1);
        Log.i(TAG, " a vonal eredeti szoget: " + a.getAngeInDegree() + " bullbol szamitott: " + degree);
        Point ta1 = a.rotatePoint(bull, degree, radius);
        Point tb1 = a.rotatePoint(bull, degree + 18, radius);
        Point ta2 = a.rotatePoint(bull, degree + 180, radius);
        Point tb2 = a.rotatePoint(bull, degree + 180 + 18, radius);
        Core.circle(img, ta1, 5, new Scalar(255,0,255)); // purple (bull szine)
        Core.circle(img, tb1, 5, new Scalar(255,255,0)); // sarga
        Core.circle(img, ta2, 5, new Scalar(0,255,255)); // vilagos keke
        Core.circle(img, tb2, 5, new Scalar(255,255,255)); // feher

        Core.line(img, a1, ta1, new Scalar(255,255,255));
        Core.line(img, a2, ta2, new Scalar(255,255,255));
        Core.line(img, b1, tb1, new Scalar(255,255,255));
        Core.line(img, b2, tb2, new Scalar(255,255,255));

        subMat = img.submat((int)(miny*0.8), (int)(maxy*1.2), (int)(minx*0.8), (int)(maxx*1.2));
        saveImageToDisk(subMat, "step10-03", "doki", this, Imgproc.COLOR_RGBA2RGB, 10);

        List<Point> src_pnt = new ArrayList<Point>();
        src_pnt.add(a1);
        src_pnt.add(a2);
        src_pnt.add(b1);
        src_pnt.add(b2);
        Mat startM = Converters.vector_Point2f_to_Mat(src_pnt);

        List<Point> dst_pnt = new ArrayList<Point>();
        dst_pnt.add(ta1);
        dst_pnt.add(ta2);
        dst_pnt.add(tb1);
        dst_pnt.add(tb2);
        Mat endM = Converters.vector_Point2f_to_Mat(dst_pnt);

        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(startM, endM);

        Mat out = img.clone();
        Imgproc.warpPerspective(img, out, perspectiveTransform, img.size(), Imgproc.INTER_CUBIC); // Imgproc.INTER_LINEAR + Imgproc.CV_WARP_FILL_OUTLIERS
        //saveImageToDisk(img, "step-10-04-0", "doki", this, Imgproc.COLOR_RGBA2RGB, 10);
        saveImageToDisk(out, "step10-04", "doki", this, Imgproc.COLOR_RGBA2RGB, 10);
        //saveImageToDisk(out, "step-10-04.jpg", "doki", this, -1, 10);

    }

    private void findXing(ArrayList<MLine> bullLines, Mat harrisResult) {
        for(MLine l:bullLines) {
            // végig megyünk a vonal pontjain
            Point currP = l.start;
            Point step = l.getStep();
            int stepLength = l.getStepLength();
            int i = 0;
            // kiszamoljuk a meroleges iranyt
            double perpAngle = l.getPerpAngle();
            Scalar color = new Scalar(255, 255, 0);
            if (step.x == 1) {
                color = new Scalar(255, 255, 255);
            }
            while(i < stepLength) {
                currP.x += step.x;
                currP.y += step.y;
                i++;
                Core.line(harrisResult, currP, currP, color);
            }
        }
    }


    private void doHarrisProc(Mat matOriginalPhoto, Mat matResult) {
        Mat src_gray = new Mat();
        Imgproc.cvtColor(matOriginalPhoto, src_gray, Imgproc.COLOR_RGBA2GRAY);
        saveImageToDisk(src_gray, "step07-gray.jpg", "doki", this, -1);

        Mat dst;
        dst = Mat.zeros( matOriginalPhoto.size(), CvType.CV_32FC1 );

        /// Detector parameters
        int blockSize = 2;
        int apertureSize = 5;
        double k = 0.05; // 0.04
        Imgproc.cornerHarris(src_gray, dst, blockSize, apertureSize, k, Imgproc.BORDER_DEFAULT);

        float minH = 999;
        float maxH = -999;

        Log.i(TAG, "Ciklus elott blockSize:" + blockSize + ":apertureSize:" + apertureSize + ":k: " + k + " rows: " + dst.rows() + " cols: " + dst.cols());
        int dstChannels = dst.channels();
        int pixelNum = (int)dst.total() * dstChannels;
        int type = dst.type();
        harrisResultVals = new float[pixelNum];
        dst.get(0,0, harrisResultVals);
        for (int j = 0; j < pixelNum; j+=dstChannels) {
            minH = Math.min(harrisResultVals[j], minH);
            maxH = Math.max(harrisResultVals[j], maxH);
        }

        Log.i(TAG, "XhXX blockSize:" + blockSize + ":apertureSize:" + apertureSize + ":k: " + k + ":min:" + minH + ":max:" + maxH);
        //Mat harrisResult = matOriginalPhoto.clone();
        int nullak = 0;
        double val;
        Scalar color;
        double v0;
        int i,j;
        int cols = dst.cols();
        Point start, end;
        for (int di = 0; di < pixelNum; di+=dstChannels) {
            i = di % cols;
            j = di / cols;
            val = harrisResultVals[di];
            if (Math.abs(val) > 0.0001) {
                if (val < 0) {
                    v0 = 127 + 128 * val / minH;
                    color = new Scalar(v0, 0, 0);
                    //Core.circle(matCanny, new Point(i, j), 1, color, 1);
                    start = new Point(i, j);
                    end = new Point(i, j);
                    Core.line(matResult, start, end, color, 1);
                } else {
                    v0 = 127 + 128 * val / maxH;
                    color = new Scalar(0, v0, 0);
                    //Core.circle(matCanny, new Point(i, j), 1, color, 1);
                    start = new Point(i, j);
                    end = new Point(i, j);
                    Core.line(matResult, start, end, color, 1);
                }
            } else {
                nullak++;
            }
        }
        Log.i(TAG, "nullak: " + nullak);
    }

    private ArrayList<MLine> findBullLines(ArrayList<MLine> allLines, Point pointBull, Mat matOriginalCopy) {
        minx = 1000000;
        miny = 1000000;
        maxx = -1000000;
        maxy = -1000000;

        ArrayList<MLine> bullLines = new ArrayList<MLine>();
        double firstAngle = Double.MIN_VALUE;
        if (pointBull != null) {
            for(MLine l: allLines) {
                double distance = l.distance(pointBull);
                // TODO: esetleg itt a clusterRadius-szal osszefuggesben kereshenenk a pontokat.
                if (distance < 3) {
                    // ha az egyenes kozel van, attol meg maga a szakasz lehet tavol
                    if (l.endDistanceOrDistance(pointBull) < 10) {
                        //double angle = l.getAngeInDegree();
                        //Log.i(TAG, "Angle: " + angle + " degree: " + (angle * (180 / Math.PI)));
//                        if (firstAngle == Double.MIN_VALUE) {
//                            firstAngle = angle;
//                        }
                        Core.line(matOriginalCopy, l.start, l.end, new Scalar(0, 0, 255), 1);
                        updateMinMax(l.start);
                        updateMinMax(l.end);
                        bullLines.add(l);
                    } else {
                        // az iranya jo, de a vege tul tavol van
                        Core.line(matOriginalCopy, l.start, l.end, new Scalar(36, 171, 52), 1);
                    }
                } else {
                    // az iranya sem jo
                    //Core.line(matOriginalCopy, l.start, l.end, new Scalar(240, 38, 255), 1);
                }
            }
            Core.line(matOriginalCopy, new Point(minx, miny), new Point(minx, maxy), new Scalar(0, 0, 255), 1);
            Core.line(matOriginalCopy, new Point(maxx, miny), new Point(maxx, maxy), new Scalar(0, 0, 255), 1);
            Core.line(matOriginalCopy, new Point(minx, miny), new Point(maxx, miny), new Scalar(0, 0, 255), 1);
            Core.line(matOriginalCopy, new Point(minx, maxy), new Point(maxx, maxy), new Scalar(0, 0, 255), 1);
        }

        return bullLines;
    }

    private Point findBullFromGoodLines(ArrayList<MLine> goodLines, Mat matOriginalCopy) {
        HashMap<String, Integer> goodLinesXing = new HashMap<String, Integer>();
        for (int i = 0; i < goodLines.size(); i++) {
            MLine l1 = goodLines.get(i);
            Core.line(matOriginalCopy, l1.start, l1.end, new Scalar(255, 0, 0), 1);


            for (int j = i + 1; j < goodLines.size(); j++) {
                MLine l2 = goodLines.get(j);

                double x = (l2.intersect - l1.intersect) / (l1.angle - l2.angle);
                double y = l1.angle * x + l1.intersect;
                Point p = new Point(x, y);
                String key = "" + (int)x + "," + (int)y;
                Integer value = new Integer(1);
                if (goodLinesXing.containsKey(key)) {
                    value = new Integer(goodLinesXing.get(key) + 1);
                }
                goodLinesXing.put(key, value);
                Core.line(matOriginalCopy, p, p, new Scalar(255, 0, 255), ((int)value /10)+1);
            }
        }

        // --------------------------------------------------------------------
        // megkeressuk a legnagyobb metszespontot

        // Ha nincs olyan pont, ahol egynel tobb lenne, ezert kicsit klaszterezni kell oket
        // a szomszedos n ponttan osszevonjuk oket
        Point maxPoint = null;
        int max = 0;
        for(int clasterRadius = 2; clasterRadius < 1000 && max < 2; clasterRadius+=5) {
            Log.d(TAG, "Metszespont eredmeny: =============================================================");
            for (Map.Entry<String, Integer> entry : goodLinesXing.entrySet()) {
                String key = entry.getKey();
                Integer value = entry.getValue();
                //Log.d(TAG, "M;" + key + ";darab;" + value);
                if (value > max) {
                    String[] x = key.split(",");
                    maxPoint = new Point(Double.parseDouble(x[0]), Double.parseDouble(x[1]));
                    max = value;
                }
            }
            Log.d(TAG, "Metszespont sulya(max): " + max + " clasterRadius: " + clasterRadius);
            if (max == 1) {
                HashMap<String, Integer> newGoodLinesXing = new HashMap<String, Integer>();
                for (Map.Entry<String, Integer> entry : goodLinesXing.entrySet()) {
                    // TODO: ez nincs tesztelve!!!
                    String key = entry.getKey();
                    int value = entry.getValue();
                    if (value != 0) {
                        String[] k = key.split(",");
                        int x, y;
                        x = Integer.parseInt(k[0]);
                        y = Integer.parseInt(k[1]);
                        for (int i = -1 * clasterRadius; i <= clasterRadius; i++) {
                            for (int j = -1 * clasterRadius; j <= clasterRadius; j++) {
                                if (i == 0 && j == 0) continue;
                                String key2 = "" + (x + i) + "," + (y + j);
                                if (goodLinesXing.containsKey(key2)) {
                                    value += newGoodLinesXing.get(key2);
                                    goodLinesXing.put(key2, Integer.valueOf(0));
                                }
                            }
                        }
                        newGoodLinesXing.put(key, Integer.valueOf(value));
                    }
                }
                goodLinesXing = (HashMap<String, Integer>) newGoodLinesXing.clone();
            }
        }

        if (maxPoint != null) {
            if (maxPoint != null) {
                //Core.circle(matOriginalCopy, pointBull, 50, new Scalar(255, 0, 255), 7);
                Core.line(matOriginalCopy, new Point(maxPoint.x-3, maxPoint.y), new Point(maxPoint.x+3, maxPoint.y), new Scalar(255, 0, 255), 2);
                Core.line(matOriginalCopy, new Point(maxPoint.x, maxPoint.y-3), new Point(maxPoint.x, maxPoint.y+3), new Scalar(255, 0, 255), 2);
            }

            return maxPoint;
        } else {
            return null;
        }
    }

    private ArrayList<MLine> findGoodLines(Mat matLines, Mat matOriginalCopy) {
        int linesChannels = matLines.channels();
        assert linesChannels == 4 : "Unknown HoughLinesP output";
        int linesLength = (int)matLines.total() * linesChannels;
        int linesType = matLines.type();
        Log.i(TAG, "Type: " + CvType.typeToString(linesType) + " channels: " + CvType.channels(linesType));
        int[] linesArr = new int[linesLength];
        matLines.get(0, 0, linesArr);

        allLines = new ArrayList<MLine>();
        ArrayList<MLine> goodLines = new ArrayList<MLine>();
        boolean isGood = true;
        for (int i = 0; i < linesArr.length; i+=linesChannels) {
            MLine mLine = new MLine(linesArr[i + 0], linesArr[i + 1], linesArr[i + 2], linesArr[i + 3]);
            allLines.add(mLine);
            Core.line(matOriginalCopy, mLine.start, mLine.end, new Scalar(155, 155, 0), 1);

            isGood = true;

            // vegig nezzuk az osszes fontos vonalat, es ha ennek a meredeksege elter, akkor lesz fontos csak
            for(MLine l: goodLines) {
                //if (Math.abs(l.angle - mLine.angle) < 0.5) {
                //Log.i(TAG, "l degree: " + l.getAngeInDegree() + " mLine degree: " + mLine.getAngeInDegree() + " diff: " + Math.abs(l.getAngeInDegree() - mLine.getAngeInDegree()));
                if (Math.abs(l.getAngeInDegree() - mLine.getAngeInDegree()) < 1 || Math.abs(l.getAngeInDegree() - mLine.getAngeInDegree()) > 360 - 1) {
                    isGood = false;
                }
            }
            if (isGood) {
                goodLines.add(mLine);
            }
        }
        return goodLines;
    }

    private void testHarrisParameters(Mat src_gray) {
        Mat dst = new Mat();
        for(int blockSize = 2; blockSize <= 3; blockSize++) {
            for (int apertureSize = 3; apertureSize <= 5; apertureSize++) {
                for (double k = 0.03; k <= 0.06; k += 0.005) {
                    //Log.i(TAG, "blockSize: " + blockSize + " apertureSize: " + apertureSize + " k: " + k + " ");
                    try {
                        Imgproc.cornerHarris(src_gray, dst, blockSize, apertureSize, k, Imgproc.BORDER_DEFAULT);
                        double minH = 999;
                        double maxH = -999;
                        Log.i(TAG, "Ciklus elott blockSize:" + blockSize + ":apertureSize:" + apertureSize + ":k: " + k + " rows: " + dst.rows() + " cols: " + dst.cols());
                        for (int j = 0; j < (dst.cols() ); j++) {
                            for (int i = 0; i < (dst.rows() ); i++) {
                                Double p = dst.get(i, j)[0];
                                minH = Math.min(p, minH);
                                maxH = Math.max(p, maxH);
                            }
                        }
                        Log.i(TAG, "XhXX blockSize:" + blockSize + ":apertureSize:" + apertureSize + ":k: " + k + ":min:" + minH + ":max:" + maxH);
                    }catch(Exception e) {
                        Log.i(TAG, "XhXX blockSize:" + blockSize + ":apertureSize:" + apertureSize + ":k:" + k + ":Kikattant...." + e.getMessage());
                    }
                }
            }
        }

    }

    private Mat getOriginalPhotoMat() {
        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        photoW = bmOptions.outWidth;
        photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / mImageViewW, photoH / mImageViewH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        // ez nem tudom kell-e!!
        //bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

        Mat imgMAT = new Mat();
        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, imgMAT);
        return imgMAT;
    }

    private void updateMinMax(double x, double y) {
        minx = Math.min(minx, x);
        maxx = Math.max(maxx, x);
        miny = Math.min(miny, y);
        maxy = Math.max(maxy, y);
    }
    private void updateMinMax(Point p) {
        minx = Math.min(minx, p.x);
        maxx = Math.max(maxx, p.x);
        miny = Math.min(miny, p.y);
        maxy = Math.max(maxy, p.y);
    }

    public void saveImageToDisk(Mat source, String filename, String directoryName, Context ctx, int colorConversion) {
        saveImageToDisk(source, filename, directoryName, ctx, colorConversion, 0);
    }
    public void saveImageToDisk(Mat source, String filename, String directoryName, Context ctx, int colorConversion, int level){
        if (saveStepsToImage == level || saveStepsToImage < 0) {
            Mat mat = source.clone();
            if (colorConversion != -1)
                Imgproc.cvtColor(mat, mat, colorConversion, 4);

            Bitmap bmpOut = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat, bmpOut);
            if (bmpOut != null) {

                mat.release();
                OutputStream fout = null;
                String root = Environment.getExternalStorageDirectory().getAbsolutePath();
                String dir = root + "/" + ctx.getResources().getString(R.string.app_name) + "/" + directoryName;
                String fileName = filename;
                if (!filename.contains(".jpg")) {
                    fileName = filename + ".png";
                }

                File file = new File(dir);
                Log.i(TAG, "DIR: " + dir + " file: " + fileName);
                file.mkdirs();
                file = new File(dir, fileName);

                try {
                    fout = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(fout);
                    if (filename.contains(".jpg")) {
                        bmpOut.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                    } else {
                        bmpOut.compress(Bitmap.CompressFormat.PNG, 100, bos);
                    }
                    bos.flush();
                    bos.close();
                    bmpOut.recycle();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            bmpOut.recycle();
        }
    }
    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    //mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


}
