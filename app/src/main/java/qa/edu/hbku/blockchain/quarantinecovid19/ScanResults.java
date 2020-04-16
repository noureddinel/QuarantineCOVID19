package qa.edu.hbku.blockchain.quarantinecovid19;


import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class ScanResults {
    private String fileName;
    private final Context context;

    SharedPreferences scanResPref;
    SharedPreferences.Editor scanResEditor;

    public ScanResults( Context context, String fileName) {
        this.context = context;
        this.fileName = fileName;
        scanResPref   = this.context.getSharedPreferences(this.fileName, 0);
        scanResEditor  = scanResPref.edit( );
    }

    public void addRecord(String ID, int RSSI, String ID2) throws JSONException {
        if (scanResPref.contains(ID)){
            JSONObject obj= new JSONObject(scanResPref.getString(ID, ""));
            if (RSSI < obj.getInt("MIN_RSSI")){
                obj.put("MIN_RSSI", RSSI);
                obj.put("MAX_RSSI", obj.getInt("MAX_RSSI"));
                scanResEditor.putString(ID, obj.toString());
            } else {
                if (RSSI > obj.getInt("MAX_RSSI")){
                    obj.put("MIN_RSSI", obj.getInt("MIN_RSSI"));
                    obj.put("MAX_RSSI", RSSI);
                    scanResEditor.putString(ID, obj.toString());
                }
            }
        }else{
            JSONObject obj = new JSONObject();
            obj.put("MIN_RSSI", RSSI);
            obj.put("MAX_RSSI", RSSI);
            obj.put("ID2", ID2);
            scanResEditor.putString(ID, obj.toString());
        }
        //System.out.println(scanResPref.getAll());
        scanResEditor.apply();
    }

    public int getCount(){
        return scanResPref.getAll().size();
    }

    public Boolean contains(String ID) throws JSONException {
        return scanResPref.contains(ID);
    }

    public int getMinRssi(String ID) throws JSONException {
        if (scanResPref.contains(ID)){
            JSONObject obj= new JSONObject(scanResPref.getString(ID, ""));
            return obj.getInt("MIN_RSSI");
        }
        return 0;
    }

    public int getMaxRssi(String ID) throws JSONException {
        if (scanResPref.contains(ID)){
            JSONObject obj= new JSONObject(scanResPref.getString(ID, ""));
            return obj.getInt("MAX_RSSI");
        }
        return 0;
    }

    public String getID2(String ID) throws JSONException {
        if (scanResPref.contains(ID)){
            JSONObject obj= new JSONObject(scanResPref.getString(ID, ""));
            return obj.getString("ID2");
        }
        return null;
    }

    public String getID(int pos) {
        if (scanResPref.getAll().size() > pos) {
            int i = 0;
            for (Map.Entry<String, ?> entry : scanResPref.getAll().entrySet()){
                if (i == pos) return entry.getKey();
                i ++;
            }
        }
        return null;
    }
}
