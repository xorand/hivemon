package com.example.ph.hivemon;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import org.json.JSONObject;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void setText(final String value){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView t = (TextView) findViewById(R.id.hello);
                t.setText(value);
            }
        });
    }

    public void Refresh() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    String sSecretKey = prefs.getString("pref_secret_key", "");
                    String sPublicKey = prefs.getString("pref_public_key", "");
                    String sRigGroup = prefs.getString("pref_rig_group", "");
                    TextView t = (TextView) findViewById(R.id.hello);
                    HiveAPI api = new HiveAPI(sSecretKey, sPublicKey);
                    JSONObject stats = api.getCurrentStats();
                    JSONObject summary = (JSONObject)stats.get("summary");
                    String sText = "rig group: " + sRigGroup + "\n";
                    sText += "workers: " + summary.get("workers_online") + "\t";
                    sText += "gpus: " + summary.get("gpus_online") + "\t";
                    sText += "pwr: " + summary.get("power") + "\n";
                    JSONObject rigs = (JSONObject)stats.get("rigs");
                    for(int i = 0; i<rigs.names().length(); i++){
                        JSONObject rig = (JSONObject)rigs.get(rigs.names().getString(i));
                        sText += " rig: " + rig.get("name") + " id: " + rig.get("id_rig");
                        sText += "\n";
                    }
                    setText(sText);
                } catch (Exception e) {
                    setText("HiveAPI Failed !");
                }
            }
        });
        thread.start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_refresh:
                Refresh();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
