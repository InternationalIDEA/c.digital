package com.rezapps.cdigital;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;

public class SettingsActivity extends AppCompatActivity {

    public static String TAG = "CDigital";
    private static final int BROWSE_CERTIFICATE = 1;

    SharedPreferences pref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        pref = this.getSharedPreferences(Param.TPS, MODE_PRIVATE);

        ((ImageButton)findViewById(R.id.button_browse)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "browseCertificate" );
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .setType("application/x-pkcs12")
                        .setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                startActivityForResult(Intent.createChooser(intent, "Select PKCS12 file"), BROWSE_CERTIFICATE);
            }
        });

        findViewById(R.id.button_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        EditText textProv = (EditText)findViewById(R.id.text_prov);
        textProv.setText(pref.getString(Param.PROVINSI, Param.PROVINSI_DEFAULT));
        textProv.addTextChangedListener(new ParamSaver(Param.PROVINSI));

        EditText textKab = (EditText)findViewById(R.id.text_kab);
        textKab.setText(pref.getString(Param.KABUPATEN, Param.KABUPATEN_DEFAULT));
        textKab.addTextChangedListener(new ParamSaver(Param.KABUPATEN));

        EditText textKec = (EditText)findViewById(R.id.text_kec);
        textKec.setText(pref.getString(Param.KECAMATAN, Param.KECAMATAN_DEFAULT));
        textKec.addTextChangedListener(new ParamSaver(Param.KECAMATAN));

        EditText textKel = (EditText)findViewById(R.id.text_kel);
        textKel.setText(pref.getString(Param.KELURAHAN, Param.KELURAHAN_DEFAULT));
        textKel.addTextChangedListener(new ParamSaver(Param.KELURAHAN));

        EditText textTPS = (EditText)findViewById(R.id.text_tps);
        textTPS.setText(pref.getString(Param.TPS_NO, Param.TPS_NO_DEFAULT));
        textTPS.addTextChangedListener(new ParamSaver(Param.TPS_NO));

        EditText textNama = (EditText)findViewById(R.id.text_nama);
        textNama.setText(pref.getString(Param.NAMA, Param.NAMA_DEFAULT));
        textNama.addTextChangedListener(new ParamSaver(Param.NAMA));

        EditText textNIK = (EditText)findViewById(R.id.text_nik);
        textNIK.setText(pref.getString(Param.NIK, Param.NIK_DEFAULT));
        textNIK.addTextChangedListener(new ParamSaver(Param.NIK));

        String certFile = pref.getString(Param.CERT_FILE, null);
        if (certFile != null) {
            ((TextView)findViewById(R.id.text_cert)).setText(certFile);

        }


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {

            Uri fileUri = data.getData(); //The uri with the location of the file
            Log.i(TAG, "File Uri: " + fileUri);

            if (requestCode == BROWSE_CERTIFICATE) {
                inputPIN(fileUri);
            }
        }
    }


    private void inputPIN(Uri certUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Password for Digital ID");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String pin = input.getText().toString();
                KeyStore keystore = null;

                // Try loading the keystore
                try {
                    InputStream is = getContentResolver().openInputStream(certUri);
                    KeyStore keystoreTmp = KeyStore.getInstance("PKCS12");
                    keystoreTmp.load(is, pin.toCharArray());
                    is.close();
                    keystore = keystoreTmp;
                } catch (Exception e) {
                    Toast.makeText(SettingsActivity.this,"Error loading Digital ID: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error loading keystore: " + e.getMessage(), e);
                    return;
                }

                // Save cert file to internal dir
                String certFileName = getFileName(certUri);
                File certFile = new File(getFilesDir(), certFileName);
                try {
                    OutputStream os = new FileOutputStream(certFile);
                    keystore.store(os, pin.toCharArray());
                    Toast.makeText(SettingsActivity.this,
                            "Sertifikat digital " + certFileName + " telah disimpan",
                            Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    Log.e(TAG, "Error saving keystore: " + e.getMessage(), e);
                    return;
                }

                SharedPreferences.Editor editor = pref.edit();
                editor.putString(Param.CERT_FILE, certFileName);
                editor.putString(Param.CERT_PIN, pin);
                editor.commit();
                ((TextView)findViewById(R.id.text_cert)).setText(certFileName);


                Log.i(TAG, "Digital certificate saved: " + certFileName + ", " + pin);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.cancel();
            }
        });

        builder.show();
    }




    @SuppressLint("Range")
    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private class ParamSaver implements TextWatcher {

        String paramName;

        ParamSaver(String paramName) {
            this.paramName = paramName;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            SharedPreferences.Editor editor = pref.edit();
            editor.putString(paramName, s.toString().trim());
            editor.apply();

        }
    }
}