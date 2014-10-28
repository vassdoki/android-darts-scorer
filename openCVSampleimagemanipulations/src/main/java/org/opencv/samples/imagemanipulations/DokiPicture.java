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
 * DartsCam
 * Created by vassdoki on 10/2/14.
 */
public class DokiPicture extends Activity {
    private static final String TAG = "OCVSample::DokiPicture";
    static final int REQUEST_IMAGE_CAPTURE = 1;

    private ImageView mImageView;

    // a terulet hataroloja, ahol a tabla van a kepen
    double minx=1000000, miny=1000000, maxx=-1000000, maxy=-1000000;

    private int saveStepsToImage = 9915;
    // imageView's dimensions
    private int mImageViewW;
    private int mImageViewH;
    // photo dimensions
    private int photoW;
    private int photoH;
    // Canny parameters
    private static final  int paramCanny1 = 180;
    private static final  int paramCanny2 = 190;
    // Hough Lines parameters
    private static final int paramHLthreshold = 80;
    private static final  int paramHLminLineSize = 20;
    private static final  int paramHLlineGap = 20;

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

        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "button on click");
                dispatchTakePictureIntent();
            }
        });
        Button button2 = (Button) findViewById(R.id.button2);
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

        Mat matOriginalBeforeTrans = matOriginalPhoto.clone();
        // this is not exact, but gives +-2 degree all the segments
        ArrayList<Integer> segments = findColorSegments(pointBull, matOriginalBeforeTrans);
        Point[] transRes = getPointsForTransFromSegments2(pointBull, matOriginalBeforeTrans, segments);
        transRes = finalizeTransformationBase(matOriginalPhoto.clone(), pointBull, transRes);
        {
            Mat transformed3 = doTransform(transRes, matOriginalPhoto.clone());
            double trippleDistance = MLine.distanceOfTwoPoints(pointBull, transRes[0]) * 3;
            double tableSideDistance = 700 * trippleDistance / 290;
            debugImage(transformed3, tableSideDistance, trippleDistance);
            saveImageToDisk(transformed3, "step15-1", "doki", this, Imgproc.COLOR_RGBA2RGB, 15);
            transformed3.release();
        }

        matOriginalBeforeTrans = matOriginalPhoto.clone();
        transRes = checkLineBalance(transRes, pointBull, matOriginalBeforeTrans);
        matOriginalBeforeTrans.release();
        {
            Mat transformed3 = doTransform(transRes, matOriginalPhoto.clone());
            double trippleDistance = MLine.distanceOfTwoPoints(pointBull, transRes[0]) * 3;
            double tableSideDistance = 700 * trippleDistance / 290;
            debugImage(transformed3, tableSideDistance, trippleDistance);
            saveImageToDisk(transformed3, "step15-2", "doki", this, Imgproc.COLOR_RGBA2RGB, 15);
            transformed3.release();
        }
        transRes = finalizeTransformationBase(matOriginalPhoto.clone(), pointBull, transRes);
        {
            Mat transformed3 = doTransform(transRes, matOriginalPhoto.clone());
            double trippleDistance = MLine.distanceOfTwoPoints(pointBull, transRes[0]) * 3;
            double tableSideDistance = 700 * trippleDistance / 290;
            debugImage(transformed3, tableSideDistance, trippleDistance);
            saveImageToDisk(transformed3, "step15-3", "doki", this, Imgproc.COLOR_RGBA2RGB, 15);
            transformed3.release();
        }
        matOriginalBeforeTrans = matOriginalPhoto.clone();
        transRes = checkLineBalance(transRes, pointBull, matOriginalBeforeTrans);
        {
            Mat transformed3 = doTransform(transRes, matOriginalPhoto.clone());
            double trippleDistance = MLine.distanceOfTwoPoints(pointBull, transRes[0]) * 3;
            double tableSideDistance = 700 * trippleDistance / 290;
            debugImage(transformed3, tableSideDistance, trippleDistance);
            saveImageToDisk(transformed3, "step15-4", "doki", this, Imgproc.COLOR_RGBA2RGB, 15);
            transformed3.release();
        }
        transRes = finalizeTransformationBase(matOriginalPhoto.clone(), pointBull, transRes);
        {
            Mat transformed3 = doTransform(transRes, matOriginalPhoto.clone());
            double trippleDistance = MLine.distanceOfTwoPoints(pointBull, transRes[0]) * 3;
            double tableSideDistance = 700 * trippleDistance / 290;
            debugImage(transformed3, tableSideDistance, trippleDistance);
            saveImageToDisk(transformed3, "step15-5", "doki", this, Imgproc.COLOR_RGBA2RGB, 15);
            transformed3.release();
        }
        matOriginalBeforeTrans = matOriginalPhoto.clone();
        transRes = checkLineBalance(transRes, pointBull, matOriginalBeforeTrans);

        // az utolsot megtartjuk, ez lesz a kimenet
        Mat transformed3 = doTransform(transRes, matOriginalPhoto.clone());
        double trippleDistance = MLine.distanceOfTwoPoints(pointBull, transRes[0]) * 3;
        double tableSideDistance = 700 * trippleDistance / 290;
        debugImage(transformed3, tableSideDistance, trippleDistance);
        saveImageToDisk(transformed3, "step15-6", "doki", this, Imgproc.COLOR_RGBA2RGB, 15);

//  -------------------------------------------------
        int h = mImageView.getHeight();
        int w = mImageView.getWidth();
        matOriginalCopy = transformed3;

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

    /**
     * Az összes vonal közül szelektálás, hogy csak néhány fontos vonal maradjon.
     * Végig megy az összes vonalon (matLines) és csak egyet tart meg minden olyanból, amik
     * 1 foknál kisebb szöget zárnak be. Vagyis maximum 180 vonalat tart meg az összesből.
     * @param matLines Az összes vonal amit a hugh lines talált.
     * @param matOriginalCopy A színes kép, amire a feldolgozás grafikusan megjeleníthető.
     * @return Azok a vonalak, amik fontosnak lettek ítélve.
     */
    private ArrayList<MLine> findGoodLines(Mat matLines, Mat matOriginalCopy) {
        int linesChannels = matLines.channels();
        int linesLength = (int)matLines.total() * linesChannels;
        int linesType = matLines.type();
        Log.i(TAG, "Type: " + CvType.typeToString(linesType) + " channels: " + CvType.channels(linesType));
        int[] linesArr = new int[linesLength];
        matLines.get(0, 0, linesArr);

        ArrayList<MLine> goodLines = new ArrayList<MLine>();
        boolean isGood;
        for (int i = 0; i < linesArr.length; i+=linesChannels) {
            MLine mLine = new MLine(linesArr[i + 0], linesArr[i + 1], linesArr[i + 2], linesArr[i + 3]);
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

    /**
     * Bull meghatározása a goodLines alapján.
     * @param goodLines A vonalak, amik legalább 1 fokot zárnak be a képen.
     * @param matOriginalCopy Debug kép.
     * @return A bull
     */
    private Point findBullFromGoodLines(ArrayList<MLine> goodLines, Mat matOriginalCopy) {
        HashMap<String, Integer> goodLinesXing = new HashMap<String, Integer>();
        // Az összes jó vonal pár metszéspontjának megkeresése.
        // Feljegyezzük a metszéspontokat és az egybe eső metszéspontok számát.
        for (int i = 0; i < goodLines.size(); i++) {
            MLine l1 = goodLines.get(i);
            Core.line(matOriginalCopy, l1.start, l1.end, new Scalar(255, 0, 0), 1);

            for (int j = i + 1; j < goodLines.size(); j++) {
                MLine l2 = goodLines.get(j);

                double x = (l2.intersect - l1.intersect) / (l1.angle - l2.angle);
                double y = l1.angle * x + l1.intersect;
                Point p = new Point(x, y);
                String key = "" + (int)x + "," + (int)y;
                Integer value = 1;
                if (goodLinesXing.containsKey(key)) {
                    value = goodLinesXing.get(key) + 1;
                }
                goodLinesXing.put(key, value);
                Core.line(matOriginalCopy, p, p, new Scalar(255, 0, 255), ((int)value /10)+1);
            }
        }

        // --------------------------------------------------------------------
        // megkeressuk a legnagyobb metszespontot
        // Ha az első lépésben nincs olyan metszéspont, aminek 1-nél több tagja lenne
        // TODO: akkor a közelieket összevonjuk, de ez nincs tesztelve

        // Ha nincs olyan pont, ahol egynel tobb lenne, ezert kicsit klaszterezni kell őket
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
                                    goodLinesXing.put(key2, 0);
                                }
                            }
                        }
                        newGoodLinesXing.put(key, value);
                    }
                }
                goodLinesXing = (HashMap<String, Integer>) newGoodLinesXing.clone();
            }
        }

        if (maxPoint != null) {
            //Core.circle(matOriginalCopy, pointBull, 50, new Scalar(255, 0, 255), 7);
            Core.line(matOriginalCopy, new Point(maxPoint.x-3, maxPoint.y), new Point(maxPoint.x+3, maxPoint.y), new Scalar(255, 0, 255), 2);
            Core.line(matOriginalCopy, new Point(maxPoint.x, maxPoint.y-3), new Point(maxPoint.x, maxPoint.y+3), new Scalar(255, 0, 255), 2);
            return maxPoint;
        } else {
            return null;
        }
    }

    /**
     * Körülbelüli meghatározása a bull-ból kinduló vonalak szögének 0 foktól (0 lefele van a 3-as irányába)
     * egyre nagyobb körökben nézzük meg a pontok színét addig, amíg találunk olyat, ami pontosan 20 felé osztja a kört.
     * nem baj, ha nem pontos, mert nem ezt fogjuk majd használni. Ez csak arra kell, hogy ha találunk 4 pontos pontot, akkor
     * meg tudjuk mondani, hogy a nem elforgatott táblán hova esnek azok a pontok.
     * @param bull A bull
     * @param matOriginalCopy A kép, amin keressük a színes pontokat.
     * @return Szög lista, ahol kb a fekete/fehér szín váltások vannak
     */
    private ArrayList<Integer> findColorSegments(Point bull, Mat matOriginalCopy) {
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
        // Egyre nagyobb körökben degreeStep fokonként veszünk egy pontot és megnézzük milyen színű.
        // Ha a körben pont 20 szín váltást találunk, akkor feljegyezzük a váltások helyét a
        // colorChanges-be
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
                    colorChange.add((int) i);
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

        // TODO: átolvassuk az adatokat egy másik tömbbe (valószínűleg feleslegesen)
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
        // az azonos pozícióba eső szín váltásokat a szög szerint rendezzük.
        // Az első és az utolsó negyedet elhagyjuk
        // a maradék átlagát vesszük, ez lesz a végleges becslés az adott szögre.
        for(int i = 0; i < 20; i++) {
            ArrayList<Integer> ai = colorChanges2.get(i);
            Collections.sort(ai);
            int sum = 0;
            int count = 0;
            for(int j = ai.size()/4; j <= 3 * ai.size() /4; j++) {
                count++;
                sum += ai.get(j);
            }
            finalFields.add(sum / count);
        }
        Log.i(TAG, "Final fields: " + debugArrayList(finalFields));
        return finalFields;
    }

    /**
     * 4 derekszogu vonal alapjan 4 pont meghatarozasa (0, 5, 10, 15 -os pont)
     * A körülbelüli vonal felosztás alapján a 4 derékszögű irányban szintén kb meghatározzuk,
     * hogy hol kezdődhet a trippla vonal.
     * @param pointBull A bull
     * @param matOriginalBeforeTrans Színes fotó
     * @param segments A kb szög felosztása a bullból menő vonalaknak.
     * @return Transzformációs mátrix
     */
    private Point[] getPointsForTransFromSegments2(Point pointBull, Mat matOriginalBeforeTrans, ArrayList<Integer> segments) {
        Point[] res = new Point[8];
        for(int i = 0; i < 4; i++) {
            double degree = segments.get(i * 5).doubleValue();
            res[i] = findCornerOnLine(pointBull, degree, matOriginalBeforeTrans);
            Core.circle(matOriginalBeforeTrans, res[i], 4, new Scalar(255,255, 0));
        }

        // ezek a pontok a szemből nézett táblán itt vannak:
        double ratio = 0.5;
        Point frontBull = new Point(photoW * ratio, photoH * ratio);
        double radius = 0.4 * photoH  * ratio;
        for(int i = 0; i < 4; i++) {
            res[4+i] = MLine.rotatePoint(frontBull, 9 + (i*5) * 18, radius);
        }

        return res;
    }

    /**
     * Transzformációs mátrix módosítása, hogy a kör pontosabb kör legyen.
     * A 4 irányban megkeressük hol kezdődnek a színes körök (trippla, dupla).
     * Összeadjuk az összes eltérést, addig finomítunk, amíg az összes eltérés csökken.
     * Minden körben a legnagyobb eltérést finomítjuk úgy, hogy az oda tartozó transzformációs
     * pontot arra mozgatjuk, hogy az eltérés csökkenjen.
     *
     * @param matOriginalPhoto Színes kép
     * @param pointBull bull
     * @param transRes transzformációs mátrix
     * @return módosított transzformációs mátrix
     */
    private Point[] finalizeTransformationBase(Mat matOriginalPhoto, Point pointBull, Point[] transRes) {
        int prevSumError = 99999;
        Point[] prevTransRes = null;
        int stepNum = 0;
        boolean cont = true;
        while(cont) {
            stepNum++;
            Mat matOrig1 = matOriginalPhoto.clone();
            ArrayList<ArrayList<Integer>> colorLineDistances = findColoredCirclesDistance(transRes, pointBull, matOrig1);
            saveImageToDisk(matOrig1, "step13-" + stepNum, "doki", this, Imgproc.COLOR_RGBA2RGB, 13);

            // megpróbáljuk a színes kört egy igazi körre hozni
            // belso korok atlaga
            int sum = 0;
            for (ArrayList<Integer> i : colorLineDistances) {
                sum += i.get(2);
            }
            float average = (float) sum / 4;
            // kivlasztjuk a legnagyobb eltérést és módosítjuk az oda tartozó pontot.
            float maxDiff = -1;
            int maxId = -1;
            float maxDiffValue = 0;
            int ii = 0;
            int sumDiff = 0;
            for (ArrayList<Integer> i : colorLineDistances) {
                float currDiff = average - i.get(2);
                sumDiff += Math.abs(currDiff);
                if (Math.abs(currDiff) > maxDiff) {
                    maxDiffValue = average - i.get(2);
                    maxDiff = Math.abs(maxDiffValue);
                    maxId = ii;
                }
                ii++;
            }
            Log.i(TAG, "finalizeTransformationBase, step: " + stepNum + " currError: " + sumDiff);
            if (sumDiff >= prevSumError) {
                cont = false;
                transRes = prevTransRes;
            } else {
                prevSumError = sumDiff;
                prevTransRes = transRes.clone();

                int[][] transEdit = new int[4][2];
                transEdit[0][0] = 0; // x
                transEdit[0][1] = -1; // y
                transEdit[1][0] = -1; // x
                transEdit[1][1] = 0; // y
                transEdit[2][0] = 0; // x
                transEdit[2][1] = 1; // y
                transEdit[3][0] = 1; // x
                transEdit[3][1] = 0; // y

                Mat invMat = invertTransMatrix(transRes);
                Point change = transformPoint(new Point(transRes[maxId + 4].x + transEdit[maxId][0] * maxDiffValue, transRes[maxId + 4].y + transEdit[maxId][1] * maxDiffValue), invMat);

                transRes[maxId].x = change.x;
                transRes[maxId].y = change.y;
            }
        }
        return transRes;
    }

    /**
     * Transzformációs mátrix finomítása, hogy a 4 alap vonal jobb és bal oldalán a fehér/fekete arány
     * jó legyen.
     * A 4 fő vonal mellett megnézzük, hogy mennyire lóg át a fehér vagy a fekete.
     * A legnagyobb eltérést próbáljuk javítani a transzformációs pont mozgatásával.
     * Addig csináljuk amíg az összes eltérés összege csökken.
     * @param transRes Transzformációs mátrix
     * @param pointBull Bull
     * @param pmat szines kep
     * @return transzformacios matrix
     */
    private Point[] checkLineBalance(Point[] transRes, Point pointBull, Mat pmat) {
        ArrayList<ArrayList<Integer>> colorLineDistances = findColoredCirclesDistance(transRes, pointBull, pmat);
        int[] coloredRegions = new int[4];
        for(ArrayList<Integer> x:colorLineDistances) {
            for(int i = 0; i < 4; i++) {
                coloredRegions[i] += x.get(i);
            }
        }
        for(int i = 0; i < 4; i++) {
            coloredRegions[i] /= 4;
        }
        int startDistance = 100;
        int step = 0;
        Point frontBull = new Point(photoW/2, photoH/2);
        Mat invMat;
        ArrayList<Float> result;
        float prevSumError = 999999;
        Point[] prevTransRes = null;
        boolean next = true;
        Mat mat;

        while(next) {
            mat = pmat.clone();
            step++;
            invMat = invertTransMatrix(transRes);
            result = getLineBalanceValues(mat, coloredRegions[3], startDistance, frontBull, invMat);
            float sumSumError = 0;
            float maxError = 0;
            int maxId = -1;
            for (int j = 0; j < 4; j++) {
                if (Math.abs(result.get(j)) > Math.abs(maxError)) {
                    maxError = result.get(j);
                    maxId = j;
                }
                sumSumError += Math.abs(result.get(j));
            }
            Log.i(TAG, "lineBalance result, sum error: " + sumSumError + " values: " + result.get(0) + "," + result.get(1) + "," + result.get(2) + "," + result.get(3));
            saveImageToDisk(mat, "step14-"+step, "doki", this, Imgproc.COLOR_RGBA2RGB, 14);
            if (sumSumError > prevSumError || maxId == -1) {
                next = false;
                transRes = prevTransRes;
            } else {
                prevSumError = sumSumError;
                prevTransRes = transRes.clone();
                int[][] transEdit = new int[4][2];
                transEdit[0][0] = 1; // x
                transEdit[0][1] = 0; // y
                transEdit[1][0] = 0; // x
                transEdit[1][1] = -1; // y
                transEdit[2][0] = -1; // x
                transEdit[2][1] = 0; // y
                transEdit[3][0] = 0; // x
                transEdit[3][1] = 1; // y

                invMat = invertTransMatrix(transRes);
                Point change = transformPoint(new Point(transRes[maxId + 4].x + transEdit[maxId][0] * maxError, transRes[maxId + 4].y + transEdit[maxId][1] * maxError), invMat);
                transRes[maxId].x = change.x;
                transRes[maxId].y = change.y;
            }
        }
        return transRes;
    }

    /**
     * Van egy transzformációs mátrix.
     * Megcsináljuk az inverzét, hogy az eredeti képen tudjunk pontot keresni.
     * Azt tudjuk, hogy a transRes a 0, 5, 10 ,15 vonalakból indul ki.
     * Kiszámoljuk, hogy az aktuális transzformáció mennyire jó.
     * Csinálunk kis módosítást a transzformációs mátrixon.
     * Újra kiszámoljuk.
     * A mátrixokat elmentjük a jóságukkal együtt.
     * Bizonyos lépés után vesszük a legjobb mátrixot.
     *
     * A jóság mérőszáma:
     * A vonalak vonalakra esnek.
     * A vonalak által bezárt szög stimmel.
     * A dupla és a tripla körökkel bezárható, nem lógnak ki.
     * @param transRes az aktualis transzformacios matrix
     * @param bull a bull
     * @param matOriginal szines kep
     * @return a negy iranyban a szines savok szele 4 szamkent
     */
    private ArrayList<ArrayList<Integer>> findColoredCirclesDistance(Point[] transRes, Point bull, Mat matOriginal) {
        Mat invMat = invertTransMatrix(transRes);
        Point frontBull = new Point(photoW/2, photoH/2);
        /*
        Scalar colorx = new Scalar(250, 200, 10);
        int tableSideDistance = 400;

        MLine l1 = new MLine(transRes[0].x, transRes[0].y, transRes[2].x, transRes[2].y);
        MLine l2 = new MLine(transRes[1].x, transRes[1].y, transRes[3].x, transRes[3].y);
        Point tBull = l1.getIntersection(l2);
        Point p;
        for (int i = 9; i < 360; i += 18) {
            p = MLine.rotatePoint(frontBull, i, tableSideDistance);
            Core.line(matOriginal, tBull, transformPoint(p, invMat), colorx);
        }
        for (int i = 0; i < 4; i++) {
            Core.circle(matOriginal, transformPoint(transRes[4+i], invMat), 3, colorx);
        }
        */

        // megkeressük a dupla és a tripla vonalat a 4 kitüntetett vonal mentén
        ArrayList<ArrayList<Integer>> colorLineDistances = new ArrayList<ArrayList<Integer>>();
        colorLineDistances.add(findColorLinesDistance(frontBull, invMat, 9, matOriginal));
        colorLineDistances.add(findColorLinesDistance(frontBull, invMat, 9 + 5*18, matOriginal));
        colorLineDistances.add(findColorLinesDistance(frontBull, invMat, 9 + 10*18, matOriginal));
        colorLineDistances.add(findColorLinesDistance(frontBull, invMat, 9 + 15*18, matOriginal));
        return colorLineDistances;

    }

    /**
     * A frontBull-bol (szemboli nezetbol) derékszögű vonalak mentén megkeressük a két
     * keresztező színes vonalat. Visszaadjuk a 2 vonal elejét és végét.
     * @param frontBull bull szembol nezve
     * @param invMat inverz transzformacios matrix
     * @param degree fok
     * @param matOriginal szines kep
     * @return Trippla belső távolsága, trippla külső távolsága, dupla belső távolsága, dupla külső távolsága0
     */
    private ArrayList<Integer> findColorLinesDistance(Point frontBull, Mat invMat, int degree, Mat matOriginal) {
        int startDistance = 100;
        int currDistance = startDistance - 1;
        MLine lineToCheck = new MLine();
        ArrayList<Integer> regions = new ArrayList<Integer>();
        int minRegionLength = 15;
        int colorStatus = 0;
        int lastStatusPos = 0;
        while(regions.size() < 4) {
            currDistance++;
            Point centerPoint = MLine.rotatePoint(frontBull, degree, currDistance);
            Point start = MLine.rotatePoint(centerPoint, (degree + 90)%360, -50);
            Point end = MLine.rotatePoint(centerPoint, (degree + 90)%360, +50);
            lineToCheck.reset(transformPoint(start, invMat), transformPoint(end, invMat));
            //Log.i(TAG, "Line to check, front ("+degree+"): " + start.x + "," + start.y + " - " + end.x +","+end.y+" transformed: " + lineToCheck.start.x + "," + lineToCheck.start.y + " - " + lineToCheck.end.x +","+lineToCheck.end.y);
            int colorNum = getColoredPixelNumOnLine(lineToCheck, matOriginal);
            if (currDistance > 600) {
                break;
            }
            if (colorNum > 10 && colorStatus == 0) {
                // megvan az elso szines
                colorStatus = 1;
                lastStatusPos = currDistance;
            }
            if (colorNum == 0 && colorStatus == 1) {
                if (currDistance - lastStatusPos > minRegionLength) {
                    // lezarult az elso szines es eleg vastag
                    colorStatus = 0;
                    regions.add(lastStatusPos);
                    regions.add(currDistance);
                } else {
                    // nem eleg vastag a szines csik, ezert eldobjuk
                    lastStatusPos = currDistance;
                    colorStatus = 0;
                }
            }
        }
        Log.i(TAG, "szinek listaja: degree(" + degree + "): " + debugArrayList(regions));

        return regions;
    }

    /**
     * A paraméterül kapott szakasz pontjain végig megyünk és megszámoljuk a színes pontokat.
     * @param l line to check
     * @param mat image
     * @return a szines pontok szama a vonalon
     */
    private int getColoredPixelNumOnLine(MLine l, Mat mat) {
        Point currP = l.start.clone();
        Point step = l.getStep();
        int stepLength = l.getStepLength();
        int i = 0;
        int color = -1;
        int coloredPixel = 0;
        Scalar[] colors = new Scalar[4];
        colors[0] = new Scalar(50,150,150);
        colors[1] = new Scalar(255,200,150);
        colors[2] = new Scalar(255,0,0);
        colors[3] = new Scalar(0,255,0);
        while(i < stepLength) {
            currP.x += step.x;
            currP.y += step.y;
            i+=1;
            color = PVec.getColor(mat.get((int) currP.y, (int) currP.x));
            if (color > 1) {
                coloredPixel++;
            }
            //Core.line(mat, currP, currP, colors[color]);
        }
        return coloredPixel;
    }

    /**
     * A 4 derékszögű vonalon megnézzük, hogy átlagosan a jobb és bal oldalon a fehér/fekete
     * színek rendben vannak-e. Az eredmény 4 szám a 4 derékszögű vonalra jellemzően, hogy
     * átlagosan hány pixel rossz színű. Ha pozitív a szám, akkor az óramutatóval ellenkező
     * irányban van az eltérés.
     *
     * @param mat színes kép
     * @param coloredRegion a trippla vonal kezdete (eddig nézzük a vonalak mentén a színeket)
     * @param startDistance ettől nézzük (bull külső kör távolsága)
     * @param frontBull szemből a bull helye
     * @param invMat inverz transzformacios matrix
     * @return a 4 derékszögű vonal mellett levő fekete/fehér eltolódása
     */
    private ArrayList<Float> getLineBalanceValues(Mat mat, int coloredRegion, int startDistance, Point frontBull, Mat invMat) {
        ArrayList<Float> result = new ArrayList<Float>();
        MLine lineToCheck = new MLine();
        for(int degree = 9; degree < 360; degree += 5 * 18) {
            //Log.i(TAG, "Degree: " + degree + "--------------------------");
            int currDistance = startDistance - 1;
            int sumDiffNum = 0;
            int diffCount = 0;
            while (currDistance < coloredRegion) {
                currDistance+=5;
                Point centerPoint = MLine.rotatePoint(frontBull, degree, currDistance);
                Point start = MLine.rotatePoint(centerPoint, (degree + 90) % 360, -49);
                Point end = MLine.rotatePoint(centerPoint, (degree + 90) % 360, +49);
                lineToCheck.reset(transformPoint(start, invMat), transformPoint(end, invMat));
                sumDiffNum += getColorBalanceOnLine(lineToCheck, mat);
                diffCount ++;
            }
            Core.line(mat, transformPoint(frontBull, invMat), transformPoint(MLine.rotatePoint(frontBull, degree, currDistance), invMat), new Scalar(255,0,0));
            //Log.i(TAG, "Color balance, degree: " + degree + " value: " + (float)sumDiffNum / diffCount + " errorSum: " + sumDiffNum);
            result.add((float)sumDiffNum/diffCount);
        }
        return result;
    }

    /**
     * A paraméterül kapott szakasz pontjain végig megyünk és megmondjuk melyik irányban van
     * több rossz színű pont. Az első jó színű pont után abbahagyjuk a számolást.
     * @param l line to check
     * @param mat image
     * @return pozitiv szam eseten a vonal vege fele tobb rossz helyen levo szin van, mint az eleje fele
     */
    private int getColorBalanceOnLine(MLine l, Mat mat) {
        Point currP = l.start.clone();
        Point step = l.getStep();
        int stepLength = l.getStepLength();
        int i = 0;
        int color;
        Scalar[] colors = new Scalar[4];
        colors[0] = new Scalar(0,0,255);
        colors[1] = new Scalar(255,255,255);
        colors[2] = new Scalar(255,0,0);
        colors[3] = new Scalar(0,255,0);
        ArrayList<Integer> cs = new ArrayList<Integer>();
        while(i < stepLength) {
            currP.x += step.x;
            currP.y += step.y;
            i+=1;
            color = PVec.getColor(mat.get((int) currP.y, (int) currP.x));
            cs.add(color % 2);
            Core.line(mat, currP, currP, colors[color%2]);
        }
        // megnézzük, hogy a közepétől a széle felé haladva mennyi van a rossz oldalon
        // ha már van egy jó, akkor a többi nem érdekel
        int half = cs.size() / 2;
        int sumLeft=0, sumRight=0;
        for(int j = 0; j < cs.size(); j++) {
            if (j < half) {
                sumLeft += cs.get(j);
            }
            // direkt nem adjuk hozza a kozepet
            if (j > half) {
                sumRight += cs.get(j);
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
            if (cs.get(half - j) != colorLeft && continueLeft) {
                badLeft ++;
            } else {
                continueLeft = false;
            }
            if (cs.get(half + j) != colorRight && continueRight) {
                badRight ++;
            } else {
                continueRight = false;
            }
        }
        //Log.i(TAG, "xxx;balance;"+debugArrayList(cs) + ";result left;"+badLeft+";resultRight;"+badRight+";result;"+(badRight-badLeft));
        return badRight - badLeft;
    }

    private Mat invertTransMatrix(Point[] t) {
        Point[] invRes = new Point[]{t[4], t[5], t[6], t[7], t[0], t[1], t[2], t[3]};
        return getTransformationMatrix(invRes);
    }
    private Point transformPoint(Point p, Mat m) {
        double m1 = p.x*m.get(0,0)[0] + p.y * m.get(0,1)[0] + m.get(0,2)[0];
        double m2 = p.x*m.get(1,0)[0] + p.y * m.get(1,1)[0] + m.get(1,2)[0];
        double m3 = p.x*m.get(2,0)[0] + p.y * m.get(2,1)[0] + m.get(2,2)[0];
        return new Point(m1/m3, m2/m3);
    }

    /**
     * Megkeressük az első potenciális trippla kereszteződést a szög mentén haladva.
     * @param bull a bull
     * @param degree a szög, amin haladunk
     * @param matColor színes kép
     * @return a pont, ahol valószínűleg a trippla kezdődik a vonalon
     */
    private Point findCornerOnLine(Point bull, double degree, Mat matColor) {
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
                    color = PVec.getColor(matColor.get((int) p.y, (int) p.x));
                }catch (Exception e) {
                    Log.i(TAG, "EXCEDPTION currDistance: " + currDistance + " x: " + p.x + " y: " + p.y + " colorCount: " + colorCount + " count: " + count);
                    found = true;
                }
                if (color > 1) {
                    colorCount++;
                }
            }
            if ((float)colorCount / count > 0.4) {
                Log.i(TAG, "currDistance(FOUND): " + currDistance + " colorCount: " + colorCount + " count: " + count);
                found = true;
            } else {
                //Log.i(TAG, "currDistance: " + currDistance + " colorCount: " + colorCount + " count: " + count);
            }
        }
        return pSave;
    }

    private void debugImage(Mat transformed3, double tableSideDistance, double trippleDistance) {
        Point nBull = new Point(photoW/2, photoH/2);
        Point px;
        Scalar colorx = new Scalar(250, 200, 10);
        for (int i = 9; i < 360; i += 18) {
            px = MLine.rotatePoint(nBull, i, tableSideDistance);
            Core.line(transformed3, nBull, px, colorx);
        }
        Core.circle(transformed3, nBull, (int)trippleDistance, colorx);
        Core.circle(transformed3, nBull, (int)tableSideDistance, colorx);
    }


    private String debugArrayList(ArrayList a) {
        StringBuilder sb = new StringBuilder();
        for(Object o : a) {
            sb.append(o.toString()).append(";");
        }
        return sb.toString();
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

    /**
     * mCurrentPhotoPath-ból a fájl betöltése egy Mat objektumba
     * @return a színes eredeti fotó Mat-ként
     */
    private Mat getOriginalPhotoMat() {
        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        photoW = bmOptions.outWidth;
        photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        //int scaleFactor = Math.min(photoW / mImageViewW, photoH / mImageViewH);

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

    public void saveImageToDisk(Mat source, String filename, String directoryName, Context ctx, int colorConversion, int level){
        if (saveStepsToImage == level || saveStepsToImage < 0) {
            Mat mat = source.clone();
            if (colorConversion != -1)
                Imgproc.cvtColor(mat, mat, colorConversion, 4);

            Bitmap bmpOut = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat, bmpOut);
            if (bmpOut != null) {

                mat.release();
                OutputStream fout;
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
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    bmpOut.recycle();
                }
            }
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
