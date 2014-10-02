package org.opencv.samples.imagemanipulations;

import android.app.Activity;
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
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Mat imgMAT = new Mat();
        Utils.bitmapToMat(bmp32, imgMAT);

        Mat temp = new Mat();
        int canny1 = 180;
        int canny2 = 190;
        Imgproc.Canny(imgMAT, temp, canny1, canny2);

        Mat lines = new Mat();
        int threshold = 80;
        int minLineSize = 20;
        int lineGap = 20;
        Imgproc.HoughLinesP(temp, lines, 1, Math.PI/180, threshold, minLineSize, lineGap);

        Imgproc.cvtColor(temp, imgMAT, Imgproc.COLOR_GRAY2BGRA, 4);
        temp.release();

        ArrayList<Double[]> fontosVonalak = new ArrayList<Double[]>();
        minx = 1000000;
        miny = 1000000;
        maxx = -1000000;
        maxy = -1000000;
        // --------------------------------------------------------------------
        // osszes vonal sargaval, es fontosak meghatarozasa
        boolean fontos = true;
        for (int i = 0; i < lines.cols(); i++) {
            double[] vec = lines.get(0, i);
            double x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];
            double meredek = ((y2 - y1) / (double) (x2 - x1));
            Point start = new Point(x1, y1);
            Point end = new Point(x2, y2);
            Core.line(imgMAT, start, end, new Scalar(155, 155, 0), 4);

            fontos = true;

            // vegig nezzuk az osszes fontos vonalat, es ha ennek a meredeksege elter, akkor lesz fontos csak
            for (Double[] f : fontosVonalak) {
                if (Math.abs(f[4] - meredek) < 0.5) {
                    fontos = false;
                }
            }
            if (fontos) {
                Double[] v = new Double[5];
                v[0] = x1;
                v[1] = y1;
                v[2] = x2;
                v[3] = y2;
                v[4] = meredek;
                fontosVonalak.add(v);
            }
        }

        // --------------------------------------------------------------------
        // a fontos vonalak metszespontjait szamoljuk
        HashMap<Point, Integer> metszespontok = new HashMap<Point, Integer>();

        for (int i = 0; i < fontosVonalak.size() - 1; i++) {
            Double[] vec1 = fontosVonalak.get(i);
            double x1 = vec1[0],
                    y1 = vec1[1],
                    x2 = vec1[2],
                    y2 = vec1[3];
            Point start = new Point(x1, y1);
            Point end = new Point(x2, y2);

            Core.line(imgMAT, start, end, new Scalar(255, 0, 0), 4);


            for (int j = i + 1; j < fontosVonalak.size(); j++) {
                Double[] vec2 = fontosVonalak.get(j);

                double a1 = (vec1[1] - vec1[3]) / (double) (vec1[0] - vec1[2]);
                double b1 = vec1[1] - a1 * vec1[0];

                double a2 = (vec2[1] - vec2[3]) / (double) (vec2[0] - vec2[2]);
                double b2 = vec2[1] - a2 * vec2[0];

                double x = (b2 - b1) / (a1 - a2);
                double y = a1 * x + b1;
                Point p = new Point(x, y);
                if (p.x > 0 && p.x < 5000 & p.y > 0 && p.y < 5000) {
                    // benne van a kepben TODO: kene tudni a kep felbontasat...
                    boolean megvan = false;
                    for (Map.Entry<Point, Integer> entry : metszespontok.entrySet()) {
                        Point key = entry.getKey();
                        Integer value = entry.getValue();
                        if (Math.abs(key.x - x) < 50 && Math.abs(key.y - y) < 50) {
                            metszespontok.put(key, new Integer(value + 1));
                            megvan = true;
                            //Log.d(TAG, "Metszespont megvan: " + p.x + "," + p.y + " szam: " + (value + 1));
                        }
                    }
                    if (!megvan) {
                        //Log.d(TAG, "Metszespont uj: " + p.x + "," + p.y);
                        metszespontok.put(p, 1);
                    }
                } else {
                    //Log.d(TAG, "Metszespont nincs a kepen: " + p.x + "," + p.y);
                }
                //Core.line(rgbaInnerWindow, p, new Point(vec1[0], vec1[1]), new Scalar(0, 255, 0), 1);
                //Core.line(rgbaInnerWindow, p, new Point(vec2[0], vec2[1]), new Scalar(0, 255, 0), 1);
            }
        }

        // --------------------------------------------------------------------
        // megkeressuk a legnagyobb metszespontot
        int max = 0;
        Point maxPoint = null;
        //Log.d(TAG, "Metszespont eredmeny: =============================================================");
        for (Map.Entry<Point, Integer> entry : metszespontok.entrySet()) {
            Point key = entry.getKey();
            Integer value = entry.getValue();
            //Log.d(TAG, "M;" + key.x + ";" + key.y + ";darab;" + value);
            if (value > max) {
                maxPoint = key;
                max = value;
            }
        }
        Point bull = null;
        if (maxPoint != null) {
            bull = maxPoint;
        }

        // --------------------------------------------------------------------
        // vegig megyunk az osszes vonalon, es a bullon atmenoket megkeressuk
        // eltaroljuk az igy talalt egyeneseket
        // es a minxy es maxy pontokat, abban van a tabla
        ArrayList<Integer> bullVonalak = new ArrayList<Integer>();
        ArrayList<Double> szogek = new ArrayList<Double>();
        double firstAngle = 0;
        if (bull != null) {
            for (int i = 0; i < lines.cols(); i++) {
                double[] vec = lines.get(0, i);
                double x1 = vec[0],
                        y1 = vec[1],
                        x2 = vec[2],
                        y2 = vec[3];



                double x0 = bull.x;
                double y0 = bull.y;
                double vonalTavolsag = Math.abs((x2 - x1) * (y1 - y0) - (x1 - x0) * (y2 - y1)) /
                        Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
                if (vonalTavolsag < 20) {
                    // ha az egyenes kozel van, attol meg maga a szakasz lehet tavol
                    double p1Tav = Math.sqrt((x0 - x1)*(x0 - x1) + (y0 - y1)*(y0 - y1));
                    double p2Tav = Math.sqrt((x0 - x2)*(x0 - x2) + (y0 - y2)*(y0 - y2));
                    if (p1Tav < 20 || p2Tav < 20) {
                        double angle = Math.atan2(y1 - y2, x1 - x2);
                        Log.i(TAG, "Angle: " + angle + " degree: " + (angle * (180 / Math.PI)));
                        if (i == 0) {
                            firstAngle = angle;
                        }

                        Point start = new Point(x1, y1);
                        Point end = new Point(x2, y2);
                        Core.line(imgMAT, start, end, new Scalar(0, 0, 255), 4);
                        updateMinMax(x1,y1);
                        updateMinMax(x2,y2);
                        bullVonalak.add(i);
                    }
                }
            }
            Core.line(imgMAT, new Point(minx, miny), new Point(minx, maxy), new Scalar(0, 0, 255), 4);
            Core.line(imgMAT, new Point(maxx, miny), new Point(maxx, maxy), new Scalar(0, 0, 255), 4);
            Core.line(imgMAT, new Point(minx, miny), new Point(maxx, miny), new Scalar(0, 0, 255), 4);
            Core.line(imgMAT, new Point(minx, maxy), new Point(maxx, maxy), new Scalar(0, 0, 255), 4);
        }

        if (bull != null) {
            Core.circle(imgMAT, bull, 50, new Scalar(255, 0, 255), 7);
        }
        //bitmap = Bitmap.createBitmap(imgMAT.cols(), imgMAT.rows(), Bitmap.Config.ARGB_8888);
        // kivagunk egy akkorat, mint amekkora a bitmap a bull-lal kozepen

        int h = mImageView.getHeight();
        int w = mImageView.getWidth();


        int rowStart = Math.max(0, (int) (bull.y - h / 2));
        int colStart = Math.max(0, (int) (bull.x - w / 2));
        int rowEnd = rowStart + h;
        int colEnd = colStart + w;
        int maxRows = imgMAT.rows();
        int maxCols = imgMAT.cols();
        if (rowEnd > maxRows) {
            rowStart -= (rowEnd - maxRows);
            rowEnd -= (rowEnd - maxRows);
        }
        if (colEnd > maxCols) {
            colStart -= (colEnd - maxCols);
            colEnd -= (colEnd - maxCols);
        }
        
        imgMAT = imgMAT.submat(rowStart, rowEnd, colStart, colEnd);

        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imgMAT, bitmap);
        imgMAT.release();

        mImageView.setImageBitmap(bitmap);
    }

    private void updateMinMax(double x, double y) {
        minx = Math.min(minx, x);
        maxx = Math.max(maxx, x);
        miny = Math.min(miny, y);
        maxy = Math.max(maxy, y);
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
