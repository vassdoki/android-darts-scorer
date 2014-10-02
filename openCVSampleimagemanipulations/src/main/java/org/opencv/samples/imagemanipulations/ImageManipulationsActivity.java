package org.opencv.samples.imagemanipulations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

public class ImageManipulationsActivity extends Activity implements CvCameraViewListener2 {
    private static final String  TAG                 = "OCVSample::Activity";

    public static final int      VIEW_MODE_RGBA      = 0;
    public static final int      VIEW_MODE_HIST      = 1;
    public static final int      VIEW_MODE_CANNY     = 2;
    public static final int      VIEW_MODE_DOKI      = 4;
    public static final int      VIEW_MODE_ZOOM      = 5;

    private MenuItem             mItemPreviewRGBA;
    private MenuItem             mItemPreviewHist;
    private MenuItem             mItemPreviewCanny;
    private MenuItem             mItemPreviewDoki;
    private MenuItem mItemDokiPicture;

    private CameraBridgeViewBase mOpenCvCameraView;
    private SeekBar              seek1, seek2, seek3;
    private int canny1 = 190;
    private int canny2 = 200;
    private int line1tre = 80; // a felismert minimalis vonalhossz
    private int line2tre = 20;
    private int line3tre = 20;
    private TextView t1, t2;
    private TextView tRight;
    private long prevTime;
    private Point bull = null;


    private Size                 mSize0;

    private Mat                  mIntermediateMat;
    private Mat                  mMat0;
    private MatOfInt             mChannels[];
    private MatOfInt             mHistSize;
    private int                  mHistSizeNum = 25;
    private MatOfFloat           mRanges;
    private Scalar               mColorsRGB[];
    private Scalar               mColorsHue[];
    private Scalar               mWhilte;
    private Point                mP1;
    private Point                mP2;
    private float                mBuff[];

    public static int           viewMode = VIEW_MODE_RGBA;
    private static int          prevViewMode;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    private Size numOfLines;
    private Handler mHandler = new Handler();
    private TextView t3;
    private String debugStr;


    public ImageManipulationsActivity() {
        Log.i(TAG, "Instantiated new ImageManipulationsActivity");
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.image_manipulations_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.image_manipulations_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

        seek1 = (SeekBar)findViewById(R.id.seek1);
        seek2 = (SeekBar)findViewById(R.id.seek2);
        seek3 = (SeekBar)findViewById(R.id.seek3);
        seek1.setProgress(line1tre);
        seek2.setProgress(line2tre);
        seek3.setProgress(line3tre);
        t1 = (TextView)findViewById(R.id.textView1); t1.setText("" + line1tre);
        t2 = (TextView)findViewById(R.id.textView2); t2.setText("" + line2tre);
        t3 = (TextView)findViewById(R.id.textView3); t3.setText("" + line3tre);
        tRight = (TextView)findViewById(R.id.textViewRight);
        prevViewMode = viewMode;

        seek1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                switch (ImageManipulationsActivity.viewMode) {
                    case ImageManipulationsActivity.VIEW_MODE_CANNY:
                        canny1 = progresValue;
                        break;
                    case ImageManipulationsActivity.VIEW_MODE_DOKI:
                        line1tre = progresValue;
                        break;
                }

                }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        seek2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                switch (ImageManipulationsActivity.viewMode) {
                    case ImageManipulationsActivity.VIEW_MODE_CANNY:
                        canny2 = progresValue;
                        break;
                    case ImageManipulationsActivity.VIEW_MODE_DOKI:
                        line2tre = progresValue;
                        break;
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        seek3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                switch (ImageManipulationsActivity.viewMode) {
                    case ImageManipulationsActivity.VIEW_MODE_CANNY:
                        break;
                    case ImageManipulationsActivity.VIEW_MODE_DOKI:
                        line3tre = progresValue;
                        break;
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemPreviewRGBA  = menu.add("Preview RGBA");
        mItemPreviewHist  = menu.add("Histograms");
        mItemPreviewCanny = menu.add("Canny");
        mItemPreviewDoki = menu.add("Doki");
        mItemDokiPicture = menu.add("Photo");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemPreviewRGBA)
            viewMode = VIEW_MODE_RGBA;
        if (item == mItemPreviewHist)
            viewMode = VIEW_MODE_HIST;
        else if (item == mItemPreviewCanny)
            viewMode = VIEW_MODE_CANNY;
        else if (item == mItemPreviewDoki)
            viewMode = VIEW_MODE_DOKI;
        else if (item == mItemDokiPicture) {
            Intent intent = new Intent(this, DokiPicture.class);
            startActivity(intent);
        }
        return true;
    }

    public void onCameraViewStarted(int width, int height) {
        mIntermediateMat = new Mat();
        mSize0 = new Size();
        mChannels = new MatOfInt[] { new MatOfInt(0), new MatOfInt(1), new MatOfInt(2) };
        mBuff = new float[mHistSizeNum];
        mHistSize = new MatOfInt(mHistSizeNum);
        mRanges = new MatOfFloat(0f, 256f);
        mMat0  = new Mat();
        mColorsRGB = new Scalar[] { new Scalar(200, 0, 0, 255), new Scalar(0, 200, 0, 255), new Scalar(0, 0, 200, 255) };
        mColorsHue = new Scalar[] {
                new Scalar(255, 0, 0, 255),   new Scalar(255, 60, 0, 255),  new Scalar(255, 120, 0, 255), new Scalar(255, 180, 0, 255), new Scalar(255, 240, 0, 255),
                new Scalar(215, 213, 0, 255), new Scalar(150, 255, 0, 255), new Scalar(85, 255, 0, 255),  new Scalar(20, 255, 0, 255),  new Scalar(0, 255, 30, 255),
                new Scalar(0, 255, 85, 255),  new Scalar(0, 255, 150, 255), new Scalar(0, 255, 215, 255), new Scalar(0, 234, 255, 255), new Scalar(0, 170, 255, 255),
                new Scalar(0, 120, 255, 255), new Scalar(0, 60, 255, 255),  new Scalar(0, 0, 255, 255),   new Scalar(64, 0, 255, 255),  new Scalar(120, 0, 255, 255),
                new Scalar(180, 0, 255, 255), new Scalar(255, 0, 255, 255), new Scalar(255, 0, 215, 255), new Scalar(255, 0, 85, 255),  new Scalar(255, 0, 0, 255)
        };
        mWhilte = Scalar.all(255);
        mP1 = new Point();
        mP2 = new Point();

    }

    public void onCameraViewStopped() {
        // Explicitly deallocate Mats
        if (mIntermediateMat != null)
            mIntermediateMat.release();

        mIntermediateMat = null;
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        Size sizeRgba = rgba.size();

        Mat rgbaInnerWindow;

        int rows = (int) sizeRgba.height;
        int cols = (int) sizeRgba.width;

        int left = cols / 6;
        int top = rows / 8;

        int width = cols * 2 / 3;
        int height = rows * 3 / 4;

        switch (ImageManipulationsActivity.viewMode) {
        case ImageManipulationsActivity.VIEW_MODE_RGBA:
            break;

        case ImageManipulationsActivity.VIEW_MODE_HIST:
            Mat hist = new Mat();
            int thikness = (int) (sizeRgba.width / (mHistSizeNum + 10) / 5);
            if(thikness > 5) thikness = 5;
            int offset = (int) ((sizeRgba.width - (5*mHistSizeNum + 4*10)*thikness)/2);
            // RGB
            for(int c=0; c<3; c++) {
                Imgproc.calcHist(Arrays.asList(rgba), mChannels[c], mMat0, hist, mHistSize, mRanges);
                Core.normalize(hist, hist, sizeRgba.height/2, 0, Core.NORM_INF);
                hist.get(0, 0, mBuff);
                for(int h=0; h<mHistSizeNum; h++) {
                    mP1.x = mP2.x = offset + (c * (mHistSizeNum + 10) + h) * thikness;
                    mP1.y = sizeRgba.height-1;
                    mP2.y = mP1.y - 2 - (int)mBuff[h];
                    Core.line(rgba, mP1, mP2, mColorsRGB[c], thikness);
                }
            }
            // Value and Hue
            Imgproc.cvtColor(rgba, mIntermediateMat, Imgproc.COLOR_RGB2HSV_FULL);
            // Value
            Imgproc.calcHist(Arrays.asList(mIntermediateMat), mChannels[2], mMat0, hist, mHistSize, mRanges);
            Core.normalize(hist, hist, sizeRgba.height/2, 0, Core.NORM_INF);
            hist.get(0, 0, mBuff);
            for(int h=0; h<mHistSizeNum; h++) {
                mP1.x = mP2.x = offset + (3 * (mHistSizeNum + 10) + h) * thikness;
                mP1.y = sizeRgba.height-1;
                mP2.y = mP1.y - 2 - (int)mBuff[h];
                Core.line(rgba, mP1, mP2, mWhilte, thikness);
            }
            // Hue
            Imgproc.calcHist(Arrays.asList(mIntermediateMat), mChannels[0], mMat0, hist, mHistSize, mRanges);
            Core.normalize(hist, hist, sizeRgba.height/2, 0, Core.NORM_INF);
            hist.get(0, 0, mBuff);
            for(int h=0; h<mHistSizeNum; h++) {
                mP1.x = mP2.x = offset + (4 * (mHistSizeNum + 10) + h) * thikness;
                mP1.y = sizeRgba.height-1;
                mP2.y = mP1.y - 2 - (int)mBuff[h];
                Core.line(rgba, mP1, mP2, mColorsHue[h], thikness);
            }
            break;

        case ImageManipulationsActivity.VIEW_MODE_CANNY:
            rgbaInnerWindow = rgba.submat(top, top + height, left, left + width);
            Imgproc.Canny(rgbaInnerWindow, mIntermediateMat, canny1, canny2);
            Imgproc.cvtColor(mIntermediateMat, rgbaInnerWindow, Imgproc.COLOR_GRAY2BGRA, 4);
            rgbaInnerWindow.release();
            break;

        case ImageManipulationsActivity.VIEW_MODE_DOKI:
            rgbaInnerWindow = rgba.submat(top, top + height, left, left + width);
            Imgproc.Canny(rgbaInnerWindow, mIntermediateMat, canny1, canny2);

            // HoughLinesP futtatasa
            // http://docs.opencv.org/modules/imgproc/doc/feature_detection.html#houghlines
            // http://stackoverflow.com/questions/7925698/android-opencv-drawing-hough-lines
            Mat lines = new Mat();
            int threshold = line1tre;
            int minLineSize = line2tre;
            int lineGap = line3tre;
            Imgproc.HoughLinesP(mIntermediateMat, lines, 1, Math.PI/180, threshold, minLineSize, lineGap);
            numOfLines = lines.size();


            ArrayList<Double[]> fontosVonalak = new ArrayList<Double[]>();

            boolean bullOk = false;
            /*
            if (bull != null) {
                // azokat a vonalakat nézzük csak, amik közel vannak a bull-hoz:
                for (int i = 0; i < lines.cols(); i++) {
                    double[] vec = lines.get(0, i);
                    double x1 = vec[0],
                            y1 = vec[1],
                            x2 = vec[2],
                            y2 = vec[3];
                    double x0 = bull.x;
                    double y0 = bull.y;
                    double tavolsag = Math.abs((x2 - x1)*(y1- y0) - (x1 - x0)*(y2-y1)) /
                            Math.sqrt((x2 - x1)*(x2 - x1) + (y2 - y1)*(y2 - y1));
                    if (tavolsag < 50) {
                        Double[] v = new Double[5];
                        v[0] = x1;
                        v[1] = y1;
                        v[2] = x2;
                        v[3] = y2;
                        v[4] = 0.0;
                        fontosVonalak.add(v);

                        Core.line(rgbaInnerWindow, new Point(x1,y1), new Point(x2,y2), new Scalar(255, 255, 0), 5);
                    }
                }
                if (fontosVonalak.size() > 8) {
                    // van eleg...
                    bullOk = true;
                    debugStr = "Bull ok #" + fontosVonalak.size();
                    Core.circle(rgbaInnerWindow, bull, 50, new Scalar(255, 0, 255), 5);
                }
            }
            */

            if (!bullOk) {
                Imgproc.cvtColor(mIntermediateMat, rgbaInnerWindow, Imgproc.COLOR_GRAY2BGRA, 4);
                for (int i = 0; i < lines.cols(); i++) {
                    double[] vec = lines.get(0, i);
                    double x1 = vec[0],
                            y1 = vec[1],
                            x2 = vec[2],
                            y2 = vec[3];

                    // meredekseg:
                    double meredek = ((y2 - y1) / (double) (x2 - x1));
                    boolean fontos = true;

                    // ha van bull, akkor nem fontos az olyan vonal, ami nagyon messze van
                    if (bull != null) {
                        double x0 = bull.x;
                        double y0 = bull.y;
                        double tavolsag = Math.abs((x2 - x1) * (y1 - y0) - (x1 - x0) * (y2 - y1)) /
                                Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
                        if (tavolsag > 200) {
                            fontos = false;
                        }
                    }
                    /*
                    // ez tul sok, ha elveszti a bull-t akkor soha nem talal vissza
                    if (bull != null) {
                        double x0 = bull.x;
                        double y0 = bull.y;
                        double tavolsag = Math.abs((x2 - x1) * (y1 - y0) - (x1 - x0) * (y2 - y1)) /
                                Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
                        if (tavolsag > 60) {
                            fontos = false;
                        } else {
                            // ha kozel van az egyenes, attol meg a szakasz lehet messze.
                            // meg kell nezni a ket vegpont tavolsagat a bull-tol
                            // es ha nincs elojel valtas a ket vegpont es a bull kozott, akkor a kozelebbik pont tavolsaga alapjan
                            // ki lehet dobni
                            if (!((Math.min(x1,x2) < x0 && Math.max(x1,x2) > x0) || (Math.min(y1,y2) < y0 && Math.max(y1,y2) > y0))) {
                                // azonos terfelen van van minden vegpontja a vonalnak a bull-hoz kepest
                                double t1 = Math.sqrt((x1 - x0)*(x1 - x0) + (y1 - y0)*(y1 - y0));
                                double t2 = Math.sqrt((x2 - x0)*(x2 - x0) + (y2 - y0)*(y2 - y0));
                                if (Math.min(t1,t2) > 60) {
                                    fontos = false;
                                }
                            }
                        }
                    }
                    */
                    if (fontos) {
                        if (x2 - x1 < 0.00001) {
                            // fuggoleges vonalak nem erdekelnek
                            fontos = false;
                        } else {
                            for (Double[] f : fontosVonalak) {
                                if (Math.abs(f[4] - meredek) < 0.5) {
                                    fontos = false;
                                }
                            }
                        }
                    }
                    /*
                    if (bull != null && !fontos) {
                        // ha van bull es kozel halad a vonal, akkor visszarakjuk a fontos koze
                        double x0 = bull.x;
                        double y0 = bull.y;
                        double tavolsag = Math.abs((x2 - x1) * (y1 - y0) - (x1 - x0) * (y2 - y1)) /
                                Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
                        if (tavolsag < 40) {
                            fontos = true;
                        }
                    }
                    */
                    if (fontos) {
                        Double[] v = new Double[5];
                        v[0] = x1;
                        v[1] = y1;
                        v[2] = x2;
                        v[3] = y2;
                        v[4] = meredek;
                        fontosVonalak.add(v);

                        Point start = new Point(x1, y1);
                        Point end = new Point(x2, y2);
                        Core.circle(rgbaInnerWindow, start, 5, new Scalar(100, 255, 0));
                        Core.circle(rgbaInnerWindow, end, 5, new Scalar(100, 255, 0));
                        Core.line(rgbaInnerWindow, start, end, new Scalar(155, 155, 0), 4);
                    } else {
                        Point start = new Point(x1, y1);
                        Point end = new Point(x2, y2);
                        //Core.circle(rgbaInnerWindow, start, 5, new Scalar(0, 255, 0));
                        //Core.circle(rgbaInnerWindow, end, 5, new Scalar(0, 255, 0));
                        //Core.line(rgbaInnerWindow, start, end, new Scalar(255,0,0), 1);
                    }
                }
                debugStr = "Lines: " + numOfLines.width + " fontos: " + fontosVonalak.size();

                HashMap<Point, Integer> metszespontok = new HashMap<Point, Integer>();
                Log.d(TAG, "Metszespont: ============================================");
                // minden fontos vonalnak kiszámoljuk a metszéspntját, és ezt is rárajzoljuk
                for (int i = 0; i < fontosVonalak.size() - 1; i++) {
                    for (int j = i + 1; j < fontosVonalak.size(); j++) {
                        Double[] vec1 = fontosVonalak.get(i);
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
                        Core.circle(rgbaInnerWindow, p, 10, new Scalar(0, 0, 255));
                        //Core.line(rgbaInnerWindow, p, new Point(vec1[0], vec1[1]), new Scalar(0, 255, 0), 1);
                        //Core.line(rgbaInnerWindow, p, new Point(vec2[0], vec2[1]), new Scalar(0, 255, 0), 1);
                    }
                }
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
                if (maxPoint != null) {
                    debugStr = debugStr + " kozeppont megvan: " + (int)maxPoint.x + "X" + (int)maxPoint.y + " metszespont szam: " + max;
                    Core.circle(rgbaInnerWindow, maxPoint, 20, new Scalar(255, 0, 255), 5);
                    bull = maxPoint;
                }
            }

            // minden vonalnak minden vonallal kiszámoljuk a metszéspntját, és ezt is rárajzoljuk
            /*
            if (1 == 2) {
                for (int i = 0; i < lines.cols() - 1; i++) {
                    for (int j = i + 1; j < lines.cols(); j++) {
                        double[] vec1 = lines.get(0, i);
                        double[] vec2 = lines.get(0, j);

                        double a1 = (vec1[1] - vec1[3]) / (double) (vec1[0] - vec1[2]);
                        double b1 = vec1[1] - a1 * vec1[0];

                        double a2 = (vec2[1] - vec2[3]) / (double) (vec2[0] - vec2[2]);
                        double b2 = vec2[1] - a2 * vec2[0];

                        // parhuzamosak-e
                        if (Math.abs(a1 - a2) < 0.1) {
                            // elég párhuzamosak ahhoz, hogy ne érdekeljen
                        } else {
                            double x = (b2 - b1) / (a1 - a2);
                            double y = a1 * x + b1;
                            Point p = new Point(x, y);
                            Core.circle(rgbaInnerWindow, p, 10, new Scalar(0, 0, 255));
                            Core.line(rgbaInnerWindow, p, new Point(vec1[0], vec1[1]), new Scalar(0, 255, 0), 1);
                            Core.line(rgbaInnerWindow, p, new Point(vec2[0], vec2[1]), new Scalar(0, 255, 0), 1);

                        }
                    }
                }
            }
            */


            rgbaInnerWindow.release();
            break;

        case ImageManipulationsActivity.VIEW_MODE_ZOOM:
            Mat zoomCorner = rgba.submat(0, rows / 2 - rows / 10, 0, cols / 2 - cols / 10);
            Mat mZoomWindow = rgba.submat(rows / 2 - 9 * rows / 100, rows / 2 + 9 * rows / 100, cols / 2 - 9 * cols / 100, cols / 2 + 9 * cols / 100);
            Imgproc.resize(mZoomWindow, zoomCorner, zoomCorner.size());
            Size wsize = mZoomWindow.size();
            Core.rectangle(mZoomWindow, new Point(1, 1), new Point(wsize.width - 2, wsize.height - 2), new Scalar(255, 0, 0, 255), 2);
            zoomCorner.release();
            mZoomWindow.release();
            break;

        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                updateGui();
            }
        });



        return rgba;
    }

    private void updateGui() {
        long now = System.nanoTime();
        double lastFrame = 1000000000 / (now - prevTime);
        prevTime = now;
        tRight.setText(debugStr + " view: " + ImageManipulationsActivity.viewMode + " ?");

        if (prevViewMode != ImageManipulationsActivity.viewMode) {
            switch (ImageManipulationsActivity.viewMode) {
                case ImageManipulationsActivity.VIEW_MODE_CANNY:
                    seek1.setProgress(canny1);
                    seek2.setProgress(canny2);
                    break;
                case ImageManipulationsActivity.VIEW_MODE_DOKI:
                    seek1.setProgress(line1tre);
                    seek2.setProgress(line2tre);
                    break;
            }
            prevViewMode = ImageManipulationsActivity.viewMode;
        }

        switch (ImageManipulationsActivity.viewMode) {
            case ImageManipulationsActivity.VIEW_MODE_CANNY:
                tRight.setText(debugStr + " view: " + ImageManipulationsActivity.viewMode + " canny " + lastFrame + " fps");
                t1.setText("" + canny1);
                t2.setText("" + canny2);
                break;
            case ImageManipulationsActivity.VIEW_MODE_DOKI:
                tRight.setText(debugStr + " view: " + ImageManipulationsActivity.viewMode + " doki " + lastFrame + " fps");
                t1.setText("" + line1tre);
                t2.setText("" + line2tre);
                t3.setText("" + line3tre);
                break;
            default:
                tRight.setText("" + lastFrame + " fps");
        }
    }
}
