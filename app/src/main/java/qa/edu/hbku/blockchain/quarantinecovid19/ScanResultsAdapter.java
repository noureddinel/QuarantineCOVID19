package qa.edu.hbku.blockchain.quarantinecovid19;


import android.content.Context;
import android.graphics.Typeface;
import android.net.wifi.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.json.JSONException;

public class ScanResultsAdapter extends BaseAdapter {

    private final Context context;
    private ScanResults results;

    /**
     * @param context
     * @param results Wifi scan results list.
     */
    public ScanResultsAdapter(Context context, ScanResults results) {
        this.context = context;
        this.results =results;
    }

    @Override
    public int getCount() {
        return results != null ? results.getCount(): 0;
    }

    @Override
    public Object getItem(int position) { //One scan
        return results.getRecord(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String ID = "";
        int MinRSSI = 0;
        int MaxRSSI = 0;
        String ID2 = "";
        if(convertView==null) {
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.wificellslists, null);
        }

        try {
            ID = results.getRecord(position);
            MinRSSI = results.getMinRssi(ID);
            MaxRSSI = results.getMaxRssi(ID);
            ID2 = results.getID2(ID);
            //System.out.println(ID+": "+RSSI+", "+position);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Get textview fields
        TextView txtSSID = (TextView)convertView.findViewById(R.id.ID);
        TextView txtID2 = (TextView)convertView.findViewById(R.id.ID2);
        TextView txtMinRssi = (TextView)convertView.findViewById(R.id.min_rssi);
        TextView txtMaxRssi = (TextView)convertView.findViewById(R.id.max_rssi);

        txtSSID.setText( ID);
        txtID2.setText( ID2);
        txtMinRssi.setText( Integer.toString(MinRSSI));
        txtMaxRssi.setText(Integer.toString(MaxRSSI));
        return convertView;
    }

}