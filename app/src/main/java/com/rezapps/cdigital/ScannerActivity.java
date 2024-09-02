package com.rezapps.cdigital;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.google.zxing.Result;
import com.rezapps.cplano.CPlanoInfo;
import com.rezapps.cplano.ElectionType;
import kotlin.text.Regex;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ScannerActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    private static final String TAG = "CDigital";

    ZXingScannerView mScannerView;
    String qrCode;
    ElectionType electType;
    // int pageIdx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        setTitle("Scan Kode QR");

        electType = ElectionType.valueOf(getIntent().getExtras().getString(Param.ELECTION_TYPE));


        initScannerView();
    }

    @Override
    protected void onStart() {

        super.onStart();
        mScannerView.startCamera();
        doRequestPermission();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }


    private void initScannerView() {
        Log.i(TAG, "initScannerView");

        mScannerView = new ZXingScannerView(this);
        mScannerView.setAutoFocus(true);
        mScannerView.setResultHandler(this);

        ((FrameLayout)findViewById(R.id.frame_layout_camera)).addView(mScannerView);
        mScannerView.startCamera();
    }

    @Override
    public void handleResult(Result result) {
        if (result != null) {
            qrCode = result.getText();
            CPlanoInfo cpInfo = new CPlanoInfo(qrCode);
            Log.i(TAG, "KodeQR=" + qrCode );
            Regex pattern = new Regex("^\\d{6}.\\d{2}$");
            if (!pattern.matches(qrCode)) {
                Log.w(TAG, "KodeQR=" + qrCode + " \ntidak dikenali");
                showMessageBox("KodeQR Tidak Dikenal!", "Format kodeQR (" + qrCode + ") \ntidak dikenali");
                return;
            }

            // Verify election Type
            if (cpInfo.getElectionType() != electType) {
                Log.w(TAG, "Kode pemilihan pada kodeQR (" + cpInfo.getElectionType() + ") tidak sesuai");
                showMessageBox("KodeQR Tidak Sesuai",
                        "KodeQR menunjukkan bahwa ini adalah formulir untuk " + cpInfo.getElectionType()  );
                return;
            }

            // Derive pageIdx from page Nnumber
            int pageNumber = Integer.parseInt(qrCode.substring(7,9));
            CDigitalApplication app = (CDigitalApplication)getApplication();
            int pageIdx = pageNumber - 1;
            if (electType.dpr && pageNumber > app.getParties(electType).length + 1 ) {
                pageIdx = app.getParties(electType).length + 1;
            }


            Toast.makeText(this, "KodeQR=" + qrCode, Toast.LENGTH_LONG).show();

            Uri photoUri = getIntent().getParcelableExtra(Param.PHOTO_URI);
            Log.d(TAG, "photoUri: " + photoUri);

            Intent intent = new Intent(ScannerActivity.this, PageActivity.class);
            intent.putExtra(Param.PAGE_INDEX, pageIdx);
            intent.putExtra(Param.PAGE_NUMBER, cpInfo.getPageNum());
            intent.putExtra(Param.ELECTION_TYPE, cpInfo.getElectionType().name());
            intent.putExtra(Param.TEMPLATE_CODE, cpInfo.getTemplateCode());
            intent.putExtra(Param.PHOTO_URI, photoUri);
            startActivity(intent);
            finish();
            //readCPlano(photo, photoFileName, pageNumber);

        }
    }



    private void showMessageBox(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }


    private void doRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                Log.i(TAG, "doRequestPermission");
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {

            Log.i(TAG, "onRequestPermissionsResult");
            initScannerView();
        }
    }
}