package qa.edu.hbku.blockchain.quarantinecovid19;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "QuarantineCOVID19";
    private static final String GEO_FILE_NAME = "Geo_res.txt";
    private static final int SCAN_FREQUENCY = 3000; //3 Seconds
    private  static final int INIT_SCAN = 0;
    private  static final int MONITOR_SCAN = 1;

    private WifiManager wifiManager;
    private List<ScanResult> wifiScanResults;

    private TelephonyManager telephonyManager;
    private List<CellInfo> neighboringCellInfoList;

    private Button scanBtn;
    private Button confirmBtn;
    private ListView resList;

    final Handler handler = new Handler();
    Timer timer;
    TimerTask timerTask;
    int num_records;

    private ScanResultsAdapter resAdapter;

    SharedPreferences geoFencingPref;
    SharedPreferences.Editor geoFencingEditor;
    ScanResults myScanResults;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);

        geoFencingPref = getApplicationContext().getSharedPreferences(GEO_FILE_NAME,  0);
        geoFencingEditor = geoFencingPref.edit();


        scanBtn = findViewById(R.id.scanBtn);
        confirmBtn = findViewById(R.id.confirmBtn);
        resList = findViewById(R.id.listRes);
        scanBtn.setTag(1);
        myScanResults = new ScanResults(getApplicationContext(), GEO_FILE_NAME);

        scanBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final int status =(Integer) v.getTag();
                if(status == 1) {
                    try {
                        scanNetworks(INIT_SCAN);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    scanBtn.setText("Stop Scanning");
                    timer = new Timer();
                    num_records = 0;
                    clearSharedReferences(geoFencingEditor);
                    timerTask = new doAsynchronousScan();
                    timer.schedule(timerTask, 0, SCAN_FREQUENCY);
                    scanBtn.setBackgroundColor(Color.RED);
                    confirmBtn.setVisibility(View.INVISIBLE);
                    v.setTag(0); //pause
                } else {
                    timer.cancel();
                    num_records = 0;
                    scanBtn.setText("Scan My Quarantine Area");
                    scanBtn.setBackgroundColor(Color.rgb(0,87,75));
                    confirmBtn.setEnabled(true);
                    confirmBtn.setVisibility(View.VISIBLE);
                    v.setTag(1); //pause
                    resList = resList==null ? (ListView)findViewById(R.id.listRes) : resList;
                    resAdapter = new ScanResultsAdapter(getApplicationContext(), myScanResults );
                    resList.setAdapter(resAdapter);
                }
            }
        });
        enableWifi();

        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i;
                i = new Intent(getApplicationContext(), Monitor.class);
                startActivity(i);
                scanBtn.setEnabled(false);
                scanBtn.setVisibility(View.INVISIBLE);
                num_records = 0;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableWifi();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void scanNetworks(int type) throws JSONException {
        String location = Manifest.permission.ACCESS_COARSE_LOCATION;
        String newLocation = Manifest.permission.ACCESS_FINE_LOCATION;
        String[] permissions = new String[] { location, newLocation };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions , 1);
        }
        //myScanResults.addRecord("0000000", 0, "000000");
        System.out.println("myScanResults ++>: "+ myScanResults.getCount());
        boolean scan = wifiManager.startScan();
        if (scan) {
            wifiScanResults = wifiManager.getScanResults();
            System.out.println("Scanning size -->: "+ wifiScanResults.size());
            storeWifiRecords(wifiScanResults);
        }
        neighboringCellInfoList = telephonyManager.getAllCellInfo();
        storeCellsRecords (neighboringCellInfoList);
        //System.out.println(neighboringCellInfoList);
    }

    public boolean enableWifi(){
        if(!wifiManager.isWifiEnabled()){
            try{
                wifiManager.setWifiEnabled(true);
                Toast.makeText(this, "Enabling Wifi...", Toast.LENGTH_LONG).show();
                return true;
            }catch (Exception ex){
                Log.e(TAG, "onEnablingWifi: "+ex.getMessage());
            }
        }
        return true;
    }

    public void storeWifiRecords(List<ScanResult> res) throws JSONException {
        for (int i=0; i<res.size(); i++){
            myScanResults.addRecord(res.get(i).BSSID, res.get(i).level, res.get(i).SSID);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)

    public void storeCellsRecords(List<CellInfo> res) throws JSONException {
        for (final CellInfo info : res) {
            if (info instanceof CellInfoWcdma) {
                String ID = Integer.toString(((CellInfoWcdma) info).getCellIdentity().getCid());
                if (((CellInfoWcdma) info).getCellIdentity().getCid() != 2147483647){
                    myScanResults.addRecord(ID, ((CellInfoWcdma) info).getCellSignalStrength().getDbm(), ID);
                }
            }
            if (info instanceof CellInfoCdma) {
                String ID = Integer.toString(((CellInfoCdma) info).getCellIdentity().getBasestationId());
                if (ID != Integer.toString(65535)){
                    myScanResults.addRecord(ID, ((CellInfoCdma) info).getCellSignalStrength().getDbm(), ID);
                }
            }
            if (info instanceof CellInfoLte) {
                String ID = Integer.toString(((CellInfoLte) info).getCellIdentity().getCi());
                if (((CellInfoLte) info).getCellIdentity().getCi() != 2147483647){
                    myScanResults.addRecord(ID, ((CellInfoLte) info).getCellSignalStrength().getDbm(), ID);
                }
            }
        }
    }


    public void clearSharedReferences(SharedPreferences.Editor _editor){
        _editor.clear();
        _editor.apply();
    }

    private class doAsynchronousScan extends TimerTask {
        @Override
        public void run() {
            handler.post(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
                @SuppressWarnings("unchecked")
                public void run() {
                    try {
                        scanNetworks(INIT_SCAN);
                        num_records ++;
                    }
                    catch (Exception e) {
                        // TODO Auto-generated catch block
                    }
                }
            });
        }
    }

}
