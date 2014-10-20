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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

    private int saveStepsToImage = 9;
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

    class MLine {
        public Point start = new Point();
        public Point end = new Point();
        public double angle;
        public double intersect;
        public boolean important;

        public MLine(float x1, float y1, float x2, float y2) {
            this((double) x1, (double) y1, (double) x2, (double) y2);
        }
        public MLine(double x1, double y1, double x2, double y2) {
            start.x = x1;
            start.y = y1;
            end.x = x2;
            end.y = y2;
            angle = (end.y - start.y) / (end.x - start.x);
            intersect = y1 - angle * x1;
            important = false;
        }

        public double getPerpAngle() {
            return (start.x - end.x) / (start.y - end.y);
        }
        public double distance(Point b) {
            return Math.abs((end.x - start.x) * (start.y - b.y) - (start.x - b.x) * (end.y - start.y)) /
                    Math.sqrt(sq(end.x - start.x) + sq(end.y - start.y));
        }

        public double sq(double a) {
            return a * a;
        }

        /**
         * The closest distance from the start and the end of the line.
         * If the point is in between, then the distance from the line.
         * @param b
         * @return
         */
        public double endDistance(Point b) {
            if (start.x < b.x && b.x < end.x && start.y < b.y && b.y < end.y) {
                return distance(b);
            }
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
                v += 180;
            }
            //Log.i(TAG, "arch: y: " + y + " x: " + x + " archtan: " + v);
            return v;
        }

        public Point getStep() {
            Point step = new Point();
            if (Math.abs(start.x - end.x) > Math.abs(start.y-end.y)) {
                step.x = Math.signum(end.x - start.x);
                step.y = angle;
            } else {
                step.y = Math.signum(end.y - start.y);
                step.x = step.y * getPerpAngle();
            }
            return step;
        }
        public int getStepLength() {
            if (Math.abs(start.x - end.x) > Math.abs(start.y-end.y)) {
                return (int)Math.abs(start.x-end.x);
            } else {
                return (int)Math.abs(start.y - end.y);
            }
        }
    }


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
        //saveImageToDisk(matOriginalPhoto, "step0-orig", "doki", this, Imgproc.COLOR_RGBA2RGB);

        Mat matCanny = new Mat();
        Imgproc.Canny(matOriginalPhoto, matCanny, paramCanny1, paramCanny2);
        Log.i(TAG, "Canny processed");

        // do harris instead of canny
//        Mat harrisResult = Mat.zeros( matOriginalPhoto.size(), CvType.CV_8UC3);
//        doHarrisProc(matOriginalPhoto, harrisResult);
//        saveImageToDisk(harrisResult, "step0-harris.jpg", "doki", this, -1);
//        Imgproc.cvtColor(harrisResult, matCanny, Imgproc.COLOR_RGBA2GRAY);
//        saveImageToDisk(matCanny, "step0-harris2.jpg", "doki", this, -1);

        Mat matLines = new Mat();
        Imgproc.HoughLinesP(matCanny, matLines, 1, Math.PI/180, paramHLthreshold, paramHLminLineSize, paramHLlineGap);
        Log.i(TAG, "HoughLinesP processed");

        Imgproc.cvtColor(matCanny, matCanny, Imgproc.COLOR_GRAY2BGRA, 4);
        //saveImageToDisk(matCanny, "step1-canny", "doki", this, Imgproc.COLOR_RGBA2RGB);
        matCanny.release();

// -------------------------------------------------
        // osszes vonal sargaval, es fontosak meghatarozasa
        Log.i(TAG, "HL vonalak rajzolasa");
        Mat matOriginalCopy = matOriginalPhoto.clone();
        ArrayList<MLine> goodLines = findGoodLines(matLines, matOriginalCopy);
        saveImageToDisk(matOriginalCopy, "step2-vonalak", "doki", this, Imgproc.COLOR_RGBA2RGB, 2);
        //matLines.release();

// --------------------------------------------------------------------
        // a fontos vonalak metszespontjait szamoljuk
        Log.i(TAG, "HL fonos vonalak metszespontja");
        Point pointBull = findBullFromGoodLines(goodLines, matOriginalCopy);
        saveImageToDisk(matOriginalCopy, "step3-fontosVonalak", "doki", this, Imgproc.COLOR_RGBA2RGB, 8);

// --------------------------------------------------------------------
        // vegig megyunk az osszes vonalon, es a bullon atmenoket megkeressuk
        // eltaroljuk az igy talalt egyeneseket
        // es a minxy es maxy pontokat, abban van a tabla
        Log.i(TAG, "bullon atmeno vonalak keresese");
        ArrayList<MLine> bullLines = findBullLines(allLines, pointBull, matOriginalCopy);
        bullLines.size();
        saveImageToDisk(matOriginalCopy, "step7-bull", "doki", this, Imgproc.COLOR_RGBA2RGB, 8);

// -------------------------------------------------
        Log.i(TAG, "Harris");
        Mat harrisResult = Mat.zeros(matOriginalPhoto.size(), CvType.CV_8UC3);
        doHarrisProc(matOriginalPhoto, harrisResult);
        saveImageToDisk(harrisResult, "step8-cornerHarrisDstNormScaled-d", "doki", this, Imgproc.COLOR_RGBA2RGB,8);
// -------------------------------------------------
        // végig megyünk a bull-ba menő vonalakonés megnézzük, hogy a harris kimeneten hol vannak leágazásaok
        findXing(bullLines, harrisResult);
        saveImageToDisk(harrisResult, "step9-harris-es-bull-lines", "doki", this, Imgproc.COLOR_RGBA2RGB, 9);

// -------------------------------------------------
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
        saveImageToDisk(src_gray, "step7-gray.jpg", "doki", this, -1);

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

        Log.i(TAG, "XXX blockSize:" + blockSize + ":apertureSize:" + apertureSize + ":k: " + k + ":min:" + minH + ":max:" + maxH);
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
                    if (true || l.endDistance(pointBull) < 10) {
                        double angle = l.getAngeInDegree();
                        //Log.i(TAG, "Angle: " + angle + " degree: " + (angle * (180 / Math.PI)));
                        if (firstAngle == Double.MIN_VALUE) {
                            firstAngle = angle;
                        }
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
                if (Math.abs(l.getAngeInDegree() - mLine.getAngeInDegree()) < 1 || Math.abs(l.getAngeInDegree() - mLine.getAngeInDegree()) > 180 - 1) {
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
                        Log.i(TAG, "XXX blockSize:" + blockSize + ":apertureSize:" + apertureSize + ":k: " + k + ":min:" + minH + ":max:" + maxH);
                    }catch(Exception e) {
                        Log.i(TAG, "XXX blockSize:" + blockSize + ":apertureSize:" + apertureSize + ":k:" + k + ":Kikattant...." + e.getMessage());
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
