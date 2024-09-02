package com.rezapps.cdigital;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.rezapps.cplano.CPlano;
import com.rezapps.cplano.CPlanoPage;
import com.rezapps.cplano.ElectionType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

public class ElectionActivity extends AppCompatActivity {

    public static String TAG = "CDigital";
    public static int TAKE_PHOTO = 1;
    public static int PICK_FROM_GALLERY = 2;

    CPlano cPlano;
    ElectionType electType;
    List<FrameLayout> pageThumbs;
    // FloatingActionButton fab;
    Button buttonExport;
    Button buttonShare;
    Button buttonShow;
    ImageButton buttonDelete;

    //String currentPhotoFile;
    int currentPageIndex = -1;

    boolean firstTime;
    PDFGenerator pdfGen;
    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_election);

        firstTime = true;
        pageThumbs = new ArrayList<>();
        electType = ElectionType.valueOf(getIntent().getExtras().getString(Param.ELECTION_TYPE));
        this.setTitle(electType.desc);
        Log.i(TAG, "Open ElectionActivity for " + electType);

        SharedPreferences pref = this.getSharedPreferences(Param.C_PLANO, MODE_PRIVATE);
        cPlano = CPlano.fromPreference(pref, electType);
        int rowCount = (int)Math.ceil((float)cPlano.pages.length / 2);

        ViewGroup pageGrid = (ViewGroup) findViewById(R.id.layout_pages);
        LayoutInflater inflater = this.getLayoutInflater();

        // Generate page rows
        for (int row=0; row < rowCount; row++) {
            inflater.inflate(R.layout.segment_page_row, pageGrid, true);
        }

        // Generate page thumbnails
        for (int i=0; i < cPlano.pages.length; i++) {
            int row = i / 2;
            Log.i(TAG, "generate thumb page " + i + ", row " + row );
            LinearLayout pageRow = (LinearLayout) pageGrid.getChildAt(row);
            FrameLayout pageThumb = generatePageThumb(inflater, pageRow, i);
            pageThumbs.add(pageThumb);
        }

        // fab = findViewById(R.id.fab);
        // fab.setOnClickListener(v -> takePhoto());


        buttonExport = findViewById(R.id.button_export);
        buttonExport.setOnClickListener(v -> {
            pdfGen = new PDFGenerator(ElectionActivity.this);
            generateAndSignPDF();
        });

        buttonShow = findViewById(R.id.button_show);
        buttonShow.setOnClickListener(v -> {
//            File pdfFile = new File(getExternalMediaDirs()[0], cPlano.digitalCopyFile);
            File dir = getExternalFilesDir(null);
            File pdfFile = new File(dir, cPlano.digitalCopyFile);
            Uri pdfUri = FileProvider.getUriForFile(ElectionActivity.this,
                    BuildConfig.APPLICATION_ID + ".provider", pdfFile);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        });

        buttonShare = findViewById(R.id.button_share);
        buttonShare.setOnClickListener(v -> {
            File dir = getExternalFilesDir(null);
            File pdfFile = new File(dir, cPlano.digitalCopyFile);
            Uri pdfUri = FileProvider.getUriForFile(ElectionActivity.this,
                    BuildConfig.APPLICATION_ID + ".provider", pdfFile);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        });

        buttonDelete = findViewById(R.id.button_delete);
        buttonDelete.setOnClickListener(v -> {
            cPlano.digitalCopyFile = null;
            cPlano.store(pref);

            // reset ElectionActivity display
            runOnUiThread(this::onStart);
        });

    }

    @NonNull
    private FrameLayout generatePageThumb(LayoutInflater inflater, LinearLayout pageRow, int pageIdx) {

        ViewGroup pageCell = (ViewGroup) pageRow.getChildAt(pageIdx % 2);
        FrameLayout pageThumb = (FrameLayout) inflater.inflate(R.layout.segment_page_thumb, pageCell, true);

        TextView textPageNum = (TextView)pageThumb.findViewById(R.id.text_page_num);
        String pageStr = "Hal. " + (pageIdx +1);
        textPageNum.setText(pageStr);

        pageThumb.setOnClickListener(v -> {
            if (cPlano.pages[pageIdx] == null) {
                takePhoto(pageIdx);

            } else {
                Log.i(TAG, "Open page (index) " + pageIdx);
                Intent intent = new Intent(ElectionActivity.this, PageActivity.class);
                intent.putExtra(Param.PAGE_INDEX, pageIdx);
                intent.putExtra(Param.ELECTION_TYPE, electType.name());
                startActivity(intent);
            }
        });

        // Setup Upload Button
        View uploadButton = pageThumb.findViewById(R.id.image_upload);
        //int pageIdx = i2;
        uploadButton.setOnClickListener(v -> {
            currentPageIndex = pageIdx;
            Log.i(TAG, "Provide options to take photo or pick from gallery");

            Dialog optionDialog = new Dialog(ElectionActivity.this);
            View dialogView = optionDialog.getLayoutInflater().inflate(R.layout.dialog_upload_options, null);
            optionDialog.setContentView(dialogView);

            optionDialog.findViewById(R.id.button_camera).setOnClickListener(v1 -> {
                takePhoto(pageIdx);
                optionDialog.dismiss();
            });

            optionDialog.findViewById((R.id.button_gallery)).setOnClickListener(v2 -> {

                Log.i(TAG, "Pick from gallery for page " + pageIdx);
                optionDialog.dismiss();
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_FROM_GALLERY);
            });

            optionDialog.show();
        });
        return pageThumb;
    }

    private void takePhoto(int pageIdx) {
        Log.i(TAG, "Take photo");

        if (firstTime) {
            firstTime = false;

            // show Hint dialog
            Dialog dialog = new Dialog(this);
            View dialogView = dialog.getLayoutInflater().inflate(R.layout.dialog_photo_hint, null);
            dialog.setContentView(dialogView);
            dialog.setCancelable(true);
            dialogView.findViewById(R.id.button_ok).setOnClickListener(v -> {
                takePhoto(pageIdx);
                dialog.dismiss();
            });
            dialog.show();

        } else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            String photoFileName = "IMG." + System.currentTimeMillis() + ".jpg";
            photoUri = Util.generatePhotoUri(ElectionActivity.this, photoFileName);
            if (photoUri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                intent.putExtra(Param.PAGE_INDEX, pageIdx);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Log.d(TAG, "photoUri: " + photoUri);
                startActivityForResult(intent, TAKE_PHOTO);
            } else {
                Log.e(TAG, "Unable to prepare photoUri");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        doRequestPermission();

        Log.i(TAG, "Refresh ElectionActivity display");
        // Refresh cPlano data from Preferences
        SharedPreferences pref = this.getSharedPreferences(Param.C_PLANO, MODE_PRIVATE);
        cPlano = CPlano.fromPreference(pref, electType);

        for (int i=0; i < cPlano.pages.length; i++) {
            FrameLayout pageThumb = pageThumbs.get(i);
            ImageView thumbImage = (ImageView)pageThumb.findViewById(R.id.image_thumb);
            CPlanoPage page = cPlano.pages[i];
            if (page != null) {

                pageThumb.findViewById(R.id.image_upload).setVisibility(View.GONE);
            } else {
                thumbImage.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.no_image));
                pageThumb.findViewById(R.id.image_upload).setVisibility(View.VISIBLE);
            }
        }

        new Thread() {
            public void run() {

                for (int i=0; i < cPlano.pages.length; i++) {
                    FrameLayout pageThumb = pageThumbs.get(i);
                    ImageView thumbImage = (ImageView)pageThumb.findViewById(R.id.image_thumb);
                    CPlanoPage page = cPlano.pages[i];
                    if (page != null) {

                        Log.d(TAG, "PageNum=" + page.getPageNum() + ", PageType=" + page.getType() +
                                ", AlignedPhoto=" + page.alignedPhotoFile);

                        try {
                            Bitmap photo = MediaStore.Images.Media.getBitmap(getContentResolver(),
                                        Util.generatePhotoUri(getApplicationContext(), page.alignedPhotoFile));

                            Bitmap scaled = Bitmap.createScaledBitmap(photo, 592, 880, true);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    thumbImage.setImageBitmap(scaled);
                                }
                            });
                        } catch (IOException e) {
                            Log.e(TAG, "Image not found", e);
                            //cPlano.pages[i] = null;
                        }

                    }
                }
            }
        }.start();

        // Determine panel & FAB visibility
        FrameLayout digitalCopyPanel = (FrameLayout) findViewById(R.id.panel_salinan_digital);
        if (cPlano.isCompleted()) {
            // fab.setVisibility(View.GONE);
            digitalCopyPanel.setVisibility(View.VISIBLE);
            if (cPlano.hasDigitalCopy()) {
                findViewById(R.id.button_export).setVisibility(View.GONE);
                findViewById(R.id.button_show).setVisibility(View.VISIBLE);
                findViewById(R.id.button_share).setVisibility(View.VISIBLE);
                findViewById(R.id.button_delete).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.button_export).setVisibility(View.VISIBLE);
                findViewById(R.id.button_show).setVisibility(View.GONE);
                findViewById(R.id.button_share).setVisibility(View.GONE);
                findViewById(R.id.button_delete).setVisibility(View.GONE);
            }
        } else {
            // fab.setVisibility(View.VISIBLE);
            digitalCopyPanel.setVisibility(View.GONE);
        }

        findViewById(R.id.progressBar3).setVisibility(View.GONE);
    }

    private void doRequestPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == TAKE_PHOTO) {
            if (resultCode == Activity.RESULT_OK) {
                // Open QRCode scanner
                Intent intent = new Intent(this, ScannerActivity.class);
                intent.putExtra(Param.PHOTO_URI, photoUri);
                intent.putExtra(Param.PAGE_INDEX, currentPageIndex);
                intent.putExtra(Param.ELECTION_TYPE, electType.name());
                startActivity(intent);
            }
        } else if (requestCode == PICK_FROM_GALLERY) {
            if (resultCode == Activity.RESULT_OK) {
                Intent intent = new Intent(this, PageActivity.class);
                intent.putExtra(Param.PHOTO_URI, data.getData());
                intent.putExtra(Param.PAGE_INDEX, currentPageIndex);
                intent.putExtra(Param.ELECTION_TYPE, electType.name());
                startActivity(intent);

            }
        }
    }



    private void generateAndSignPDF() {

        buttonExport.setVisibility(View.GONE);
        findViewById(R.id.progressBar3).setVisibility(View.VISIBLE);


        new Thread() {
            public void run() {
                SharedPreferences tpsPref = getSharedPreferences(Param.TPS, MODE_PRIVATE);

                String pdfFileName = "CDigital_" + electType.abbrev + "_" + Param.TPS_ID_DEFAULT + ".pdf";
                Log.i(TAG, "Generate CDigital PDF file: " + pdfFileName);

                // File dir = getExternalMediaDirs()[0];
                File dir = getExternalFilesDir(null);
                File pdfFile = new File(dir, pdfFileName);
                pdfGen.generatePDF(cPlano, pdfFile);
                cPlano.digitalCopyFile = pdfFileName;

                Log.i(TAG, "CDigital PDF completed");

                // If Certificate available, sign it
                //if (tpsPref.contains(Param.CERT_FILE)) {
                try {
                    // Load Keystore
                    InputStream keystoreIS;
                    String pin;
                    if (tpsPref.contains(Param.CERT_FILE)) {

                        File certFile = new File(getFilesDir(), tpsPref.getString(Param.CERT_FILE, "xxx"));
                        keystoreIS = new FileInputStream(certFile);
                        pin = tpsPref.getString(Param.CERT_PIN, "xxx");
                    } else {

                        keystoreIS = getAssets().open("test.p12");
                        pin = "rahasia";
                    }

                    if (keystoreIS == null) {
                        Log.w(TAG, "KeystoreIS is null");
                    }

                    KeyStore keystore = KeyStore.getInstance("PKCS12");
                    keystore.load(keystoreIS, pin.toCharArray());

                    if (keystore == null) {
                        Log.w(TAG, "Keystore is null");
                    }

                    String signedPdfFileName = "CDigital_" + electType.abbrev + "_" + Param.TPS_ID_DEFAULT + "_signed.pdf";
                    File signedPdfFile = new File(dir, signedPdfFileName);

                    pdfGen.signPDF(pdfFile, signedPdfFile, keystore, pin);

                    cPlano.digitalCopyFile = signedPdfFileName;
                    pdfFile = signedPdfFile;

                } catch (Exception e) {
                        Log.e(TAG, "Error Signing: " + e.getMessage(), e);
                }
                //}

                Uri pdfUri = FileProvider.getUriForFile(ElectionActivity.this,
                        BuildConfig.APPLICATION_ID + ".provider", pdfFile);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(pdfUri, "application/pdf");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);

                // save CPlano
                SharedPreferences pref = getSharedPreferences(Param.C_PLANO, MODE_PRIVATE);
                cPlano.store(pref);

                // reset ElectionActivity display
                runOnUiThread(() -> onStart());

            }

        }.start();

    }


}