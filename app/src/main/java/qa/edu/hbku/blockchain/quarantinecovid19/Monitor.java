package qa.edu.hbku.blockchain.quarantinecovid19;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class Monitor extends AppCompatActivity {
    private static final int MONITOR_FREQUENCY = 10000;//10 sec min
    private static final String GEO_FILE_NAME = "Geo_res.txt";
    private static final String MON_FILE_NAME = "Mon_res.txt";



    private WifiManager wifiManager;
    private List<ScanResult> wifiScanResultsNow;

    private TelephonyManager telephonyManager;
    private List<CellInfo> cellsInfoListNow;


    TextView isInTextView;
    TextView isOutTextView;

    TextView inOutTextView;

    ScanResults myScanResults;

    private float similarityIndex = 0;
    private float noSimilarityIndex = 0;

    SharedPreferences monPref;
    SharedPreferences.Editor monEditor;
    ScanResults monScanResults;


    Timer timer;
    TimerTask timerTask;
    final Handler handler = new Handler();

    int nb_scan = 0;

    private ProgressBar progBar;
    private ProgressBar progBar2;

    private int similartyRatio=0;
    private int noSimilartyRatio=0;

    int nb_successive_scan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);
        nb_successive_scan = 0;

        monPref = getApplicationContext().getSharedPreferences(MON_FILE_NAME,  0);
        monEditor = monPref.edit();
        monScanResults = new ScanResults(getApplicationContext(), MON_FILE_NAME);

        isInTextView = findViewById(R.id.isInText);
        isOutTextView = findViewById(R.id.isOutText);
        inOutTextView = findViewById(R.id.in_out_text);

        myScanResults = new ScanResults(getApplicationContext(), GEO_FILE_NAME);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);

        timer = new Timer();
        timerTask = new doAsynchronousMonitor();
        timer.schedule(timerTask, 0, MONITOR_FREQUENCY);

        progBar= (ProgressBar)findViewById(R.id.progressBar);
        progBar2= (ProgressBar)findViewById(R.id.progressBar2);
        clearSharedReferences(monEditor);
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
            System.out.println("Mon Scanning size -->: "+ wifiScanResultsNow.size());
            storeWifiRecords(wifiScanResultsNow);
        }
        cellsInfoListNow = telephonyManager.getAllCellInfo();
        storeCellsRecords(cellsInfoListNow);
        //checkRecords(wifiScanResultsNow);
        //IamWithinQuarantine();
        if (nb_scan == 5) {
            checkRecords(wifiScanResultsNow);
            IamWithinQuarantine();
        } else {
            nb_scan ++;
            startMonitor();
        }
        //clearSharedReferences(monEditor);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)

    public void checkRecords(List<ScanResult> res) throws JSONException {
        System.out.println("HERE : Size MonRes  " + monScanResults.getCount());
        for (int i=0; i < monScanResults.getCount(); i++){
            String ID = monScanResults.getID(i);
            if (myScanResults.contains(ID)){
                similarityIndex ++;
            } else{
                noSimilarityIndex ++;
            }
        }
    }

    public void storeWifiRecords(List<ScanResult> res) throws JSONException {
        for (int i=0; i<res.size(); i++){
            monScanResults.addRecord(res.get(i).BSSID, res.get(i).level, res.get(i).SSID);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void storeCellsRecords(List<CellInfo> res) throws JSONException {
        for (final CellInfo info : res) {
            JSONObject obj = new JSONObject();
            if (info instanceof CellInfoWcdma) {
                String ID = Integer.toString(((CellInfoWcdma) info).getCellIdentity().getCid());
                if (((CellInfoWcdma) info).getCellIdentity().getCid() != 2147483647){
                    monScanResults.addRecord(ID, ((CellInfoWcdma) info).getCellSignalStrength().getDbm(), ID);
                }
            }
            if (info instanceof CellInfoCdma) {
                String ID = Integer.toString(((CellInfoCdma) info).getCellIdentity().getBasestationId());
                if (ID != Integer.toString(65535)){
                    monScanResults.addRecord(ID, ((CellInfoCdma) info).getCellSignalStrength().getDbm(), ID);
                }
            }
            if (info instanceof CellInfoLte) {
                String ID = Integer.toString(((CellInfoLte) info).getCellIdentity().getCi());
                if (((CellInfoLte) info).getCellIdentity().getCi() != 2147483647){
                    monScanResults.addRecord(ID, ((CellInfoLte) info).getCellSignalStrength().getDbm(), ID);
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void IamWithinQuarantine() {
        //isInTextView.setText(Float.toString(similarityIndex/myScanResults.getCount()));
        //isOutTextView.setText(Float.toString(noSimilarityIndex/myScanResults.getCount()));
        similartyRatio = (int)(similarityIndex/(similarityIndex+noSimilarityIndex)*100);
        noSimilartyRatio = (int)(noSimilarityIndex/(similarityIndex+noSimilarityIndex)*100);
        progBar.setProgress(similartyRatio);
        isInTextView.setText(""+similartyRatio+"%");
        progBar2.setProgress(noSimilartyRatio);
        isOutTextView.setText(""+noSimilartyRatio+"%");
        if (noSimilartyRatio > 50) {
            inOutTextView.setText("Out house (warning!)");
            inOutTextView.setTextColor(Color.RED);
        }
        if (similartyRatio == 100) {
            inOutTextView.setText("In house (safe)");
            inOutTextView.setTextColor(Color.GREEN);
        }
        if ( similartyRatio > 50 && similartyRatio < 100) {
            inOutTextView.setText("Maybe out house (warning!)");
            inOutTextView.setTextColor(Color.BLUE);
        }

        System.out.println("Similarity Index:" + similarityIndex);
    }

    public void clearSharedReferences(SharedPreferences.Editor editor){
        editor.clear();
        editor.apply();
    }

    private class doAsynchronousMonitor extends TimerTask {
        @Override
        public void run() {
            handler.post(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
                @SuppressWarnings("unchecked")
                public void run() {
                    try {
                        clearSharedReferences(monEditor);
                        nb_scan = 0;
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
