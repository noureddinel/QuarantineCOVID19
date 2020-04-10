package qa.edu.hbku.blockchain.quarantinecovid19;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class Monitor extends AppCompatActivity {
    private static final int MONITOR_FREQUENCY = 2000;//1 min
    private static final String GEO_FILE_NAME = "Geo_res.txt";


    private WifiManager wifiManager;
    private List<ScanResult> wifiScanResultsNow;

    private TelephonyManager telephonyManager;
    private List<CellInfo> cellsInfoListNow;


    TextView isInTextView;
    TextView isOutTextView;

    ScanResults myScanResults;

    private float similarityIndex = 0;
    private float noSimilarityIndex = 0;


    Timer timer;
    TimerTask timerTask;
    final Handler handler = new Handler();


    private ProgressBar progBar;
    private ProgressBar progBar2;

    private int mProgressStatus=0;
    private int mProgressStatus2=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);

        isInTextView = findViewById(R.id.isInText);
        isOutTextView = findViewById(R.id.isOutText);

        myScanResults = new ScanResults(getApplicationContext(), GEO_FILE_NAME);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);

        timer = new Timer();
        timerTask = new doAsynchronousMonitor();
        timer.schedule(timerTask, 0, MONITOR_FREQUENCY);

        progBar= (ProgressBar)findViewById(R.id.progressBar);
        progBar2= (ProgressBar)findViewById(R.id.progressBar2);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void startMonitor()throws JSONException {
        String location = Manifest.permission.ACCESS_COARSE_LOCATION;
        String newLocation = Manifest.permission.ACCESS_FINE_LOCATION;
        String[] permissions = new String[] { location, newLocation };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions , 1);
        }
        similarityIndex = 0;
        noSimilarityIndex = 0;
        boolean scan = wifiManager.startScan();
        if (scan) {
            wifiScanResultsNow = wifiManager.getScanResults();
            storeWifiRecords(wifiScanResultsNow);
        }
        cellsInfoListNow = telephonyManager.getAllCellInfo();
        storeCellsRecords(cellsInfoListNow);
        IamWithinQuarantine();
    }

    public void storeWifiRecords(List<ScanResult> res) throws JSONException {
        for (int i=0; i<res.size(); i++){
             if (myScanResults.contains(res.get(i).BSSID)){
                similarityIndex ++;
             } else{
                 noSimilarityIndex ++;
             }
        }

    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)

    public void storeCellsRecords(List<CellInfo> res) throws JSONException {
        String ID;
        for (final CellInfo info : res) {
            JSONObject obj = new JSONObject();
            if (info instanceof CellInfoWcdma) {
                ID = Integer.toString(((CellInfoWcdma) info).getCellIdentity().getCid());
                if (!ID.equals(Integer.toString(2147483647))) {
                    if (myScanResults.contains(ID)) {
                        similarityIndex++;
                    } else noSimilarityIndex++;
                }
            }
            if (info instanceof CellInfoCdma) {
                ID = Integer.toString(((CellInfoCdma) info).getCellIdentity().getBasestationId());
                if (!ID.equals(Integer.toString(65535))) {
                    if (myScanResults.contains(ID)) {
                        similarityIndex++;
                    } else noSimilarityIndex++;
                }
            }
            if (info instanceof CellInfoLte) {
                ID = Integer.toString(((CellInfoLte) info).getCellIdentity().getCi());
                if (((CellInfoLte) info).getCellIdentity().getCi() != 2147483647) {
                    if (myScanResults.contains(ID)) {
                        similarityIndex++;
                    } else noSimilarityIndex++;
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void IamWithinQuarantine() {
        //isInTextView.setText(Float.toString(similarityIndex/myScanResults.getCount()));
        //isOutTextView.setText(Float.toString(noSimilarityIndex/myScanResults.getCount()));
        mProgressStatus = (int)(similarityIndex/myScanResults.getCount()*100);
        mProgressStatus2 = (int)(noSimilarityIndex/myScanResults.getCount()*100);
        progBar.setProgress(mProgressStatus);
        isInTextView.setText(""+mProgressStatus+"%");
        progBar2.setProgress(mProgressStatus2);
        isOutTextView.setText(""+mProgressStatus2+"%");

        System.out.println("Similarity Index:" + similarityIndex);
    }

    private class doAsynchronousMonitor extends TimerTask {
        @Override
        public void run() {
            handler.post(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
                @SuppressWarnings("unchecked")
                public void run() {
                    try {
                        startMonitor();
                    }
                    catch (Exception e) {
                        // TODO Auto-generated catch block
                    }
                }
            });
        }
    }
}