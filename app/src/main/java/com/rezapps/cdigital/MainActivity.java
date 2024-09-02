package com.rezapps.cdigital;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.rezapps.cplano.ElectionType;

public class MainActivity extends AppCompatActivity {

    public static String TAG = "CDigital";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name);


        findViewById(R.id.button_pres).setOnClickListener(v -> {
            Intent myIntent = new Intent(MainActivity.this, ElectionActivity.class);
            myIntent.putExtra(Param.ELECTION_TYPE, ElectionType.PRESIDEN.name());
            startActivity(myIntent);
        });

        findViewById(R.id.button_dpr).setOnClickListener(v -> {
            Intent myIntent = new Intent(MainActivity.this, ElectionActivity.class);
            myIntent.putExtra(Param.ELECTION_TYPE, ElectionType.DPR_RI.name());
            startActivity(myIntent);
        });

        findViewById(R.id.button_dprp).setOnClickListener(v -> {
            Intent myIntent = new Intent(MainActivity.this, ElectionActivity.class);
            myIntent.putExtra(Param.ELECTION_TYPE, ElectionType.DPRD_PROV.name());
            startActivity(myIntent);
        });

        findViewById(R.id.button_dprk).setOnClickListener(v -> {
            Intent myIntent = new Intent(MainActivity.this, ElectionActivity.class);
            myIntent.putExtra(Param.ELECTION_TYPE, ElectionType.DPRD_KAB.name());
            startActivity(myIntent);
        });

        findViewById(R.id.button_dpd).setOnClickListener(v -> {
            Intent myIntent = new Intent(MainActivity.this, ElectionActivity.class);
            myIntent.putExtra(Param.ELECTION_TYPE, ElectionType.DPD_RI.name());
            startActivity(myIntent);
        });

        findViewById(R.id.image_logo).setOnClickListener(v -> {
            showAboutDialog();
        });


        SharedPreferences tpsPref = this.getSharedPreferences(Param.TPS, MODE_PRIVATE);
        String name = tpsPref.getString(Param.NAMA, Param.NAMA_DEFAULT);
        if ("".equals(name)) {
            Intent myIntent = new Intent(this, SettingsActivity.class);
            this.startActivity(myIntent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_settings) {
            Intent myIntent = new Intent(this, SettingsActivity.class);
            this.startActivity(myIntent);
            return(true);

        } else if (item.getItemId() == R.id.action_about) {
            showAboutDialog();
            return(true);
        }
        return(super.onOptionsItemSelected(item));
    }

    private void showAboutDialog() {
        Dialog dialog = new Dialog(this);
        View dialogView = dialog.getLayoutInflater().inflate(R.layout.dialog_about, null);
        dialog.setContentView(dialogView);
        dialog.setCancelable(true);
        dialogView.findViewById(R.id.button_ok).setOnClickListener(v -> {
            dialog.dismiss();
        });
        TextView txtVersion = dialogView.findViewById(R.id.text_version);
        txtVersion.setText("versi " + BuildConfig.VERSION_NAME);
        dialog.show();
    }

}