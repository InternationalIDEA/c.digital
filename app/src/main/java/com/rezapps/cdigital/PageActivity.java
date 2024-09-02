package com.rezapps.cdigital;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.rezapps.cplano.CPlano;
import com.rezapps.cplano.CPlanoPage;
import com.rezapps.cplano.CPlanoReader;
import com.rezapps.cplano.ElectionType;
import com.rezapps.cplano.FieldName;
import com.rezapps.ocrform.FieldEntry;
import com.rezapps.ocrform.FieldTemplate;
import com.rezapps.ocrform.FormTemplate;
import com.rezapps.ocrform.SubField;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PageActivity extends AppCompatActivity implements CPlanoReader.ReadListener {

    public static final String TAG = "CDigital";
    private static final float BLURRY_THRESHOLD = 10f;

    ElectionType electType;
    int pageIdx;
    CPlano cPlano;
    CPlanoPage page;
    Menu menu;

    Map<String, EditText> dataFields = new HashMap<>();
    List<EditText> voteFields = new ArrayList<>();

    View progressBar;
    ImageView imagePhoto;
    LinearLayout dataLayout;

    Button buttonOK;
    TextView textInstruction;

    boolean editing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page);

        electType = ElectionType.valueOf(getIntent().getStringExtra(Param.ELECTION_TYPE));
        pageIdx = getIntent().getIntExtra(Param.PAGE_INDEX, 0);

        setTitle(electType.desc);
        Log.i(TAG, "Open PageActivity for " + electType + " page " + pageIdx );

        cPlano = CPlano.fromPreference(getSharedPreferences(Param.C_PLANO, MODE_PRIVATE), electType);

        dataLayout = findViewById(R.id.layout_data);
        progressBar = findViewById(R.id.progressBar2);
        imagePhoto = findViewById(R.id.image_page_photo);
        buttonOK = findViewById(R.id.button_ok);
        textInstruction = findViewById(R.id.text_instruction);
        page = cPlano.pages[pageIdx];

        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editing) {
                    SharedPreferences pref = getSharedPreferences(Param.C_PLANO, MODE_PRIVATE);
                    saveData(pref);
                    invalidateOptionsMenu();
                    editing = false;
                    buttonOK.setText("OK");
                } else {
                    finish();
                }
            }
        });

        // adjust dataLayout width
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        if (dpWidth > 352) {
            Log.d(TAG, "Adjusting layout width");
            dataLayout.getLayoutParams().width = (int)(352 * displayMetrics.density);
            dataLayout.requestLayout();

            View tableHeader = findViewById(R.id.segment_table_header);
            tableHeader.getLayoutParams().width = (int)(352 * displayMetrics.density);
            tableHeader.requestLayout();
        }

        if (getIntent().hasExtra(Param.PHOTO_URI)) {
            progressBar.setVisibility(View.VISIBLE);

            // load photo to Bitmap
            Uri photoUri = getIntent().getParcelableExtra(Param.PHOTO_URI);
            Log.d(TAG, "photoUri: " + photoUri);

            //Bitmap pagePhoto = loadBitmapFromUri(photoUri);
            imagePhoto.setImageURI(photoUri);

            new Thread() {
                public void run() {
                    readCPlano(photoUri);
                }
            }.start();

        } else if (page != null) {
            loadPageData();

        } else {
            finish();
        }
    }

    private void loadPageData() {

        String pageStr = "Halaman " + page.getPageNum();
        ((TextView)findViewById(R.id.text_page_num)).setText(pageStr);
        loadPagePhoto(page.alignedPhotoFile);

        dataLayout.removeAllViews();
        dataFields.clear();
        voteFields.clear();

        // layout data fields
        if (page.getType() == CPlanoPage.PageType.DATA_PPHP) {
            layoutDataPPHP();
        } else if (page.getType() == CPlanoPage.PageType.SUARA_PWP) {
            layoutSuaraPWP();
        } else if (page.getType() == CPlanoPage.PageType.SUARA_DPRX) {
            try {
                layoutSuaraDPRX();
            } catch (Exception e) {
            }
        } else if (page.getType() == CPlanoPage.PageType.SUARA_DPD) {
            layoutSuaraDPD(page.getPageNum());
        } else if (page.getType() == CPlanoPage.PageType.SUARA_TOTAL) {
            if (electType == ElectionType.DPD_RI)
                layoutSuaraTotalDPD();
            else
                layoutSuaraTotalDPRx();
        }

        textInstruction.setVisibility(View.VISIBLE);
        buttonOK.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    private void readCPlano(Uri photoUri) {

        Log.d(TAG, "Start readCPlano");

        CDigitalApplication application = (CDigitalApplication)getApplication();
        CPlanoReader creader = new CPlanoReader(PageActivity.this, this);

        Log.d(TAG, "CPlanoReader initialized");

        // Check orientation
        Bitmap photo = Util.loadBitmapFromUri(this, photoUri);
        if (photo.getWidth() > photo.getHeight()) {
            showErrorMessage("Error", "Foto salah orientasi. \nPastikan foto berorientasi portrait.");
            return;
        }

        Log.d(TAG, "Photo loaded");

        // check sharpness
        float sharpness = creader.calculateSharpness(photo);
        if (sharpness < BLURRY_THRESHOLD) {
            showErrorMessage("Error", "Foto kurang fokus.");
            return;
        }

        Log.d(TAG, "Sharpness Checked: " + sharpness);

        // Determine template
        int pageNum = getIntent().getIntExtra(Param.PAGE_NUMBER, pageIdx + 1);
        char templateCode = getIntent().getCharExtra(Param.TEMPLATE_CODE, '-');
        FormTemplate template = application.templates.get(templateCode);
        if (template == null)
            template = application.determineTemplate(electType, pageNum);

        // Preprocess: Align
        Bitmap aligned = creader.align(photo, template);

        if (aligned == null) {
            Log.e(TAG, "Unable to align photo");
            showErrorMessage("Error","Gagal mengenali foto lembar C1-Plano");
            return;
        }

        Log.d(TAG, "Alignment completed ");

        // Aligned succesful, save a brightened version page and display it
        Bitmap brighten = creader.brighten(aligned);
        SharedPreferences pref = getSharedPreferences(Param.TPS, MODE_PRIVATE);
        String alignedFileName = pref.getString(Param.TPS_ID, Param.TPS_ID_DEFAULT) + "_" +
                electType.abbrev + "_" + pageIdx + ".a.jpg";
        saveBitmap(brighten, alignedFileName);
        runOnUiThread(() -> loadPagePhoto(alignedFileName));

        Log.d(TAG, "Photo brightened and saved");

        // delete original photo to save space
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File photoFile = new File(dir, Util.getFileNameFromUri(this, photoUri));
        boolean deleted = photoFile.delete();
        Log.d(TAG, "Delete original photo?? " + deleted);

        // More preparation: cropping
        CPlanoReader.CroppedForm cropped = creader.crop(aligned, template);
        if (cropped == null)
            showErrorMessage("Error","Gagal mengolah foto lembar C1-Plano");

        Log.i(TAG, "Cropping completed");

        // read CPlano
        page = creader.readCPlano(cropped, template, pageNum, alignedFileName, cPlano);

        if (page != null) {
            cPlano.pages[pageIdx] = page;
            SharedPreferences pref2 = getSharedPreferences(Param.C_PLANO, MODE_PRIVATE);
            cPlano.store(pref2);
            runOnUiThread(this::loadPageData);

            Log.i(TAG, "CPlano reading completed");
        } else {
            showErrorMessage("Error", "Gagal membaca CPlano");
        }

    }

    private void showErrorMessage(String title, String message) {

        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(PageActivity.this);
            builder.setTitle(title);
            builder.setMessage(message);
            builder.setPositiveButton("OK", (dialog, id) -> finish());
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.show();
        });
    }


    private void loadPagePhoto(String fileName) {
        Uri photoUri = Util.generatePhotoUri(this, fileName);

        Bitmap photo;
        try {
            photo = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);

            imagePhoto.setScaleType(ImageView.ScaleType.FIT_XY);
            imagePhoto.setImageBitmap(photo);
            imagePhoto.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(photoUri, "image/*");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        if (!cPlano.hasDigitalCopy()) {
            getMenuInflater().inflate(R.menu.menu_page, menu);
            this.menu = menu;

            menu.findItem(R.id.action_edit).setVisible(true);
            menu.findItem(R.id.action_save).setVisible(false);
            menu.findItem(R.id.action_undo).setVisible(false);
        }


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        SharedPreferences pref = getSharedPreferences(Param.C_PLANO, MODE_PRIVATE);
        if (item.getItemId() == R.id.action_edit) {
            editing = true;
            item.setVisible(false);
            menu.findItem(R.id.action_save).setVisible(true);
            menu.findItem(R.id.action_undo).setVisible(true);
            buttonOK.setText("Save");

            // Enable all Edit Text boxes
            for (String fieldName : dataFields.keySet()) {
                dataFields.get(fieldName).setEnabled(true);
            }

            for (EditText field : voteFields) {
                field.setEnabled(true);
            }
            return true;

        } else if (item.getItemId() == R.id.action_save) {
            editing = false;
            item.setVisible(false);
            menu.findItem(R.id.action_edit).setVisible(true);
            menu.findItem(R.id.action_undo).setVisible(false);
            buttonOK.setText("OK");
            return saveData(pref);

        } else if (item.getItemId() == R.id.action_undo) {
            editing = false;
            item.setVisible(false);
            menu.findItem(R.id.action_edit).setVisible(true);
            menu.findItem(R.id.action_save).setVisible(false);
            buttonOK.setText("OK");

                // Revert EditText values with data from verifiedData/Votes
                for (String fieldName : dataFields.keySet()) {
                    dataFields.get(fieldName).setText(String.valueOf(page.verifiedData.get(fieldName)));
                    dataFields.get(fieldName).setEnabled(false);
                }
                for (int i=0; i<voteFields.size(); i++) {
                    voteFields.get(i).setText(String.valueOf(page.verifiedVotes[i]));
                    voteFields.get(i).setEnabled(false);
                }
                loadPageData();
                return true;

        } else if (item.getItemId() == R.id.action_delete) {

            // Show dialog asking delete confirmation
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle("Konfirmasi penghapusan");
            alertDialogBuilder.setPositiveButton("Hapus", (dialog, which) -> {

                int pageNum = page.getPageNum();
                cPlano.pages[pageIdx] = null;
                cPlano.store(pref);
                finish();
                Toast.makeText(PageActivity.this,
                        "Data halaman " + pageNum + " telah dihapus.", Toast.LENGTH_LONG).show();

            });
            alertDialogBuilder.setNegativeButton("Batal", (dialog, which) -> {
                // do nothing
            });
            alertDialogBuilder.create().show();
            return true;

        }

        return super.onOptionsItemSelected(item);

    }

    private boolean saveData(SharedPreferences pref) {

        // Save data from EditTexts into verifiedData/Votes
        for (String fieldName : dataFields.keySet()) {
            try {
                page.verifiedData.put(fieldName,
                        Integer.parseInt(dataFields.get(fieldName).getText().toString()));
            } catch (Exception e) {
                page.verifiedData.put(fieldName, 0);
            }
            dataFields.get(fieldName).setEnabled(false);
        }
        for (int i=0; i<voteFields.size(); i++) {
            try {
                page.verifiedVotes[i] =
                    Integer.parseInt(voteFields.get(i).getText().toString());
            } catch (Exception e) {
                page.verifiedVotes[i] = 0;
            }
            voteFields.get(i).setEnabled(false);
        }

        cPlano.store(pref);
        loadPageData();
        Toast.makeText(this, "Perubahan data telah disimpan.", Toast.LENGTH_LONG).show();
        return true;
    }


    private void layoutDataPPHP() {

        findViewById(R.id.segment_table_header).setVisibility(View.VISIBLE);

        addSectionTitle(getResources().getString(R.string.section_1));
        addSectionTitle(getResources().getString(R.string.section_1A));
        addDataPemilihRow(getResources().getString(R.string.field_DP1), FieldName.DP1L, FieldName.DP1P, FieldName.DP1T);
        addDataPemilihRow(getResources().getString(R.string.field_DP2), FieldName.DP2L, FieldName.DP2P, FieldName.DP2T);
        addDataPemilihRow(getResources().getString(R.string.field_DP3), FieldName.DP3L, FieldName.DP3P, FieldName.DP3T);
        addDataPemilihRow(getResources().getString(R.string.field_DPJ), FieldName.DP4L, FieldName.DP4P, FieldName.DP4T);
        
        addSectionTitle(getResources().getString(R.string.section_1B));
        addDataPemilihRow(getResources().getString(R.string.field_PHP1), FieldName.PHP1L, FieldName.PHP1P, FieldName.PHP1T);
        addDataPemilihRow(getResources().getString(R.string.field_PHP2), FieldName.PHP2L, FieldName.PHP2P, FieldName.PHP2T);
        addDataPemilihRow(getResources().getString(R.string.field_PHP3), FieldName.PHP3L, FieldName.PHP3P, FieldName.PHP3T);
        addDataPemilihRow(getResources().getString(R.string.field_PHPJ), FieldName.PHP4L, FieldName.PHP4P, FieldName.PHP4T);

        addSectionTitle(getResources().getString(R.string.section_2));
        View fieldPSS1= createDataRow(getResources().getString(R.string.field_DPSS1), FieldName.PSS1);
        dataLayout.addView(fieldPSS1);

        // Check Jumlah Surat Suara Digunakan
        if (!page.verifiedData.get(FieldName.PSS1).equals(page.verifiedData.get(FieldName.PHP4T)) ) {
            Log.i(TAG, "PSS1=" + page.verifiedData.get(FieldName.PSS1) +
                    ", PHP4T=" + page.verifiedData.get(FieldName.PHP4T));
            TextView textWarning = fieldPSS1.findViewById(R.id.text_warning);
            textWarning.setVisibility(View.VISIBLE);
            textWarning.setText("Jumlah Surat Suara Digunakan berbeda dengan Jumlah Pengguna Hak Pilih");
            fieldPSS1.findViewById(R.id.text_field_value3).setBackgroundColor(
                    getResources().getColor(R.color.light_red));
        }

        dataLayout.addView(createDataRow(getResources().getString(R.string.field_DPSS2), FieldName.PSS2));
        dataLayout.addView(createDataRow(getResources().getString(R.string.field_DPSS3), FieldName.PSS3));

        View fieldPSS4= createDataRow(getResources().getString(R.string.field_DPSS4), FieldName.PSS4);
        dataLayout.addView(fieldPSS4);

        // Check Jumlah Surat Suara Diterima
        if (!page.verifiedData.get(FieldName.PSS4).equals(
                 page.verifiedData.get(FieldName.PSS1) +
                 page.verifiedData.get(FieldName.PSS2) +
                 page.verifiedData.get(FieldName.PSS3)) ) {
            TextView textWarning = fieldPSS4.findViewById(R.id.text_warning);
            textWarning.setVisibility(View.VISIBLE);
            textWarning.setText("Jumlah Surat Suara Diterima berbeda dengan II.1 + II.2 + II.3");
            fieldPSS4.findViewById(R.id.text_field_value3).setBackgroundColor(
                    getResources().getColor(R.color.light_red));
        }

        addSectionTitle(getResources().getString(R.string.section_3));
        addDataPemilihRow(getResources().getString(R.string.field_DPD1), FieldName.DPD1L, FieldName.DPD1P, FieldName.DPD1T);
        addDataPemilihRow(getResources().getString(R.string.field_DPD2), FieldName.DPD2L, FieldName.DPD2P, FieldName.DPD2T);
    }

    private void addDataPemilihRow(String desc, String l, String p, String total) {
        View dataRow = createDataRow(desc, l, p, total);
        dataLayout.addView(dataRow);

        if (page.verifiedData.get(total) != page.verifiedData.get(l) + page.verifiedData.get(p)) {
            TextView textWarning = dataRow.findViewById(R.id.text_warning);
            textWarning.setVisibility(View.VISIBLE);
            textWarning.setText("Jumlah total berbeda dengan laki-laki + perempuan");
            dataRow.findViewById(R.id.text_field_value1).setBackgroundColor(
                    getResources().getColor(R.color.light_red));
            dataRow.findViewById(R.id.text_field_value2).setBackgroundColor(
                    getResources().getColor(R.color.light_red));
            dataRow.findViewById(R.id.text_field_value3).setBackgroundColor(
                    getResources().getColor(R.color.light_red));
        }

    }


    private void layoutSuaraPWP() {

        findViewById(R.id.segment_table_header).setVisibility(View.GONE);

        addSectionTitle(getResources().getString(R.string.section_4_PWP));
        String[] candidates = ((CDigitalApplication)getApplication()).getCandidates(electType);

        int i=0;
        while (i<candidates.length && i<page.verifiedVotes.length) {
            int noUrut = i + 1;
            dataLayout.addView(createDataRow("" + noUrut + ". " + candidates[i], i));
            i++;
        }

        addSectionTitle(getResources().getString(R.string.section_5));
        View fieldSah = createDataRow(getResources().getString(R.string.field_SAH), FieldName.SAH);
        dataLayout.addView(fieldSah);

        // Check Field Sah
        int sah = page.verifiedData.get(FieldName.SAH);
        int votes = 0;
        for (int v : page.verifiedVotes) {
            votes += v;
        }
        if (votes != sah) {
            TextView textWarning = fieldSah.findViewById(R.id.text_warning);
            textWarning.setVisibility(View.VISIBLE);
            textWarning.setText("Jumlah Suara Sah berbeda dengan total Suara Paslon");
            fieldSah.findViewById(R.id.text_field_value3).setBackgroundColor(
                    getResources().getColor(R.color.light_red));
        }

        dataLayout.addView(createDataRow(getResources().getString(R.string.field_TIDAK_SAH), FieldName.TIDAK_SAH));

        View fieldTotal = createDataRow(getResources().getString(R.string.field_TOTAL), FieldName.TOTAL);
        dataLayout.addView(fieldTotal);

        // Check Field Total
        int total = page.verifiedData.get(FieldName.TOTAL);
        int sahTidakSah = sah + page.verifiedData.get(FieldName.TIDAK_SAH);;
        if (total != sahTidakSah) {
            TextView textWarning = fieldTotal.findViewById(R.id.text_warning);
            textWarning.setVisibility(View.VISIBLE);
            textWarning.setText("Jumlah Suara berbeda dengan Suara Sah + Suara Tidak Sah");
            fieldTotal.findViewById(R.id.text_field_value3).setBackgroundColor(
                    getResources().getColor(R.color.light_red));
        }

    }


    private void layoutSuaraDPRX() {

        CDigitalApplication app = (CDigitalApplication)getApplication();

        findViewById(R.id.segment_table_header).setVisibility(View.GONE);

        addSectionTitle(getResources().getString(R.string.section_4_DPR));
        addSectionTitle(getResources().getString(R.string.section_4A_DPR));
        int partyIdx = pageIdx - 1;
        dataLayout.addView(createDataRow(app.getParties(electType)[partyIdx], FieldName.PARTAI));

        addSectionTitle(getResources().getString(R.string.section_4B_DPR));
        String[] candidates = app.getPartyCandidates(electType, partyIdx);

        int i=0;
        while (i<candidates.length && i<page.verifiedVotes.length) {
            int noUrut = i + 1;
            dataLayout.addView(createDataRow("" + noUrut + ". " + candidates[i], i));
            i++;
        }

        addSectionTitle(getResources().getString(R.string.section_4C_DPR));
        //dataLayout.addView(createDataRow("Jumlah Suara", FieldName.TOTAL_PARTAI));

        View fieldTotal = createDataRow("Jumlah Suara", FieldName.TOTAL_PARTAI);
        dataLayout.addView(fieldTotal);

        // Check Field Total
        int total = page.verifiedData.get(FieldName.TOTAL_PARTAI);
        int partai = page.verifiedData.get(FieldName.PARTAI);
        int votes = 0;
        for (int v : page.verifiedVotes) {
            votes += v;
        }
        if (total != (partai + votes)) {
            TextView textWarning = fieldTotal.findViewById(R.id.text_warning);
            textWarning.setVisibility(View.VISIBLE);
            textWarning.setText("Jumlah berbeda dengan Total Suara Partai dan seluruh Suara Calon");
            fieldTotal.findViewById(R.id.text_field_value3).setBackgroundColor(
                    getResources().getColor(R.color.light_red));
        }

    }


    private void layoutSuaraDPD(int pageNum) {

        CDigitalApplication app = (CDigitalApplication)getApplication();
        String[] candidates = app.getCandidates(electType);

        findViewById(R.id.segment_table_header).setVisibility(View.GONE);
        addSectionTitle(getResources().getString(R.string.section_4_DPD));

        int offset = (pageIdx - 1) * CDigitalApplication.DPD_CANDIDATES_PER_PAGE;
        int i=0;
        while (i+offset<candidates.length && i<page.verifiedVotes.length) {
            int noUrut = offset + i + 21;
            dataLayout.addView(createDataRow("" + noUrut + ". " + candidates[i+offset], i));
            i++;
        }

    }


    private void layoutSuaraTotalDPD() {

        findViewById(R.id.segment_table_header).setVisibility(View.GONE);

        addSectionTitle(getResources().getString(R.string.section_5));
        View fieldSah = createDataRow(getResources().getString(R.string.field_SAH), FieldName.SAH);
        dataLayout.addView(fieldSah);

        // Check Field Sah
        if (cPlano.isCompleted() && cPlano.getTotalCandidateVotes() != page.verifiedData.get(FieldName.SAH)) {
            TextView textWarning = fieldSah.findViewById(R.id.text_warning);
            textWarning.setVisibility(View.VISIBLE);
            textWarning.setText("Jumlah Suara Sah berbeda dengan total Suara Calon");
            fieldSah.findViewById(R.id.text_field_value3).setBackgroundColor(
                    getResources().getColor(R.color.light_red));
        }

        dataLayout.addView(createDataRow(getResources().getString(R.string.field_TIDAK_SAH), FieldName.TIDAK_SAH));

        View fieldTotal = createDataRow(getResources().getString(R.string.field_TOTAL), FieldName.TOTAL);
        dataLayout.addView(fieldTotal);

        // Check Field Total
        int total = page.verifiedData.get(FieldName.TOTAL);
        int sahTidakSah = page.verifiedData.get(FieldName.SAH) + page.verifiedData.get(FieldName.TIDAK_SAH);
        if (total != sahTidakSah) {
            TextView textWarning = fieldTotal.findViewById(R.id.text_warning);
            textWarning.setVisibility(View.VISIBLE);
            textWarning.setText("Jumlah Suara berbeda dengan Suara Sah + Suara Tidak Sah");
            fieldTotal.findViewById(R.id.text_field_value3).setBackgroundColor(
                    getResources().getColor(R.color.light_red));
        }
    }

    private void layoutSuaraTotalDPRx() {

        findViewById(R.id.segment_table_header).setVisibility(View.GONE);

        addSectionTitle(getResources().getString(R.string.section_5));
        View fieldSah = createDataRow(getResources().getString(R.string.field_SAH), FieldName.SAH);
        dataLayout.addView(fieldSah);

        // Check Field Sah
        if (cPlano.isCompleted()) {
            int totalPartai = 0;
            for (CPlanoPage page : cPlano.pages) {
                if (page.verifiedData.containsKey(FieldName.TOTAL_PARTAI))
                    totalPartai += page.verifiedData.get(FieldName.TOTAL_PARTAI);
            }
            if (totalPartai != page.verifiedData.get(FieldName.SAH)) {
                Log.i(TAG, "TOTAL_PARTAI=" + totalPartai +
                        ", SAH=" + page.verifiedData.get(FieldName.SAH));

                TextView textWarning = fieldSah.findViewById(R.id.text_warning);
                textWarning.setVisibility(View.VISIBLE);
                textWarning.setText("Jumlah Suara Sah berbeda dengan total Suara Partai + Calon");
                fieldSah.findViewById(R.id.text_field_value3).setBackgroundColor(
                        getResources().getColor(R.color.light_red));
            }
        }

        dataLayout.addView(createDataRow(getResources().getString(R.string.field_TIDAK_SAH), FieldName.TIDAK_SAH));

        View fieldTotal = createDataRow(getResources().getString(R.string.field_TOTAL), FieldName.TOTAL);
        dataLayout.addView(fieldTotal);

        // Check Field Total
        int total = page.verifiedData.get(FieldName.TOTAL);
        int sahTidakSah = page.verifiedData.get(FieldName.SAH) + page.verifiedData.get(FieldName.TIDAK_SAH);
        if (total != sahTidakSah) {
            TextView textWarning = fieldTotal.findViewById(R.id.text_warning);
            textWarning.setVisibility(View.VISIBLE);
            textWarning.setText("Jumlah Suara berbeda dengan Suara Sah + Suara Tidak Sah");
            fieldTotal.findViewById(R.id.text_field_value3).setBackgroundColor(
                    getResources().getColor(R.color.light_red));
        }
    }

    private void addSectionTitle(String string) {
        TextView txt = new TextView(this);
        txt.setTypeface(txt.getTypeface(), Typeface.BOLD);
        txt.setText(string);
        dataLayout.addView(txt);
    }

    private View createDataRow(String desc, String field) {
        return createDataRow(desc, null, null, field);
    }

    private View createDataRow(String desc, String field1, String field2, String field3) {
        LinearLayout dataSegment = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.segment_data_row, null);
        ((TextView)dataSegment.findViewById(R.id.text_field_desc)).setText(desc);

        EditText txtField1 = (EditText)dataSegment.findViewById(R.id.text_field_value1);
        if (field1 != null) {
            txtField1.setVisibility(View.VISIBLE);
            txtField1.setText(String.valueOf(page.verifiedData.get(field1)));
            dataFields.put(field1, txtField1);
        } else {
            txtField1.setVisibility(View.GONE);
        }

        EditText txtField2 = (EditText)dataSegment.findViewById(R.id.text_field_value2);
        if (field2 != null) {
            txtField2.setVisibility(View.VISIBLE);
            txtField2.setText(String.valueOf(page.verifiedData.get(field2)));
            dataFields.put(field2, txtField2);
        } else {
            txtField2.setVisibility(View.GONE);
        }

        EditText txtField3 = (EditText)dataSegment.findViewById(R.id.text_field_value3);
        txtField3.setText(String.valueOf(page.verifiedData.get(field3)));
        dataFields.put(field3, txtField3);

        return(dataSegment);
        //dataLayout.addView(dataSegment);
    }


    private View createDataRow(String desc, int idx) {
        LinearLayout dataSegment = (LinearLayout) getLayoutInflater()
                .inflate(R.layout.segment_data_row, null);
        ((TextView)dataSegment.findViewById(R.id.text_field_desc)).setText(desc);

        dataSegment.findViewById(R.id.text_field_value1).setVisibility(View.GONE);
        dataSegment.findViewById(R.id.text_field_value2).setVisibility(View.GONE);

        EditText txtField = (EditText)dataSegment.findViewById(R.id.text_field_value3);
        txtField.setText(String.valueOf(page.verifiedVotes[idx]));
        voteFields.add(txtField);

        return(dataSegment);
        //dataLayout.addView(dataSegment);
    }


    private void saveBitmap(Bitmap bitmap, String fileName) {

        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File file = new File(dir, fileName);

        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEntryRead(FieldTemplate field, int idx, FieldEntry entry) {
        String fieldName;
        if (field.isMultiple())
            fieldName = field.name() + "_" + (idx + 1) ;
        else
            fieldName = field.name();

        Log.i(TAG, String.format("%-20s : %4d \n", fieldName, entry.getCombinedValue()));
    }

    @Override
    public void onSubFieldRead(FieldTemplate field, int idx, SubField subField, Integer value) {
        String fieldName;
        if (field.isMultiple())
            fieldName = field.name() + "_" + (idx + 1) + "." + subField.name();
        else
            fieldName = field.name() + "." + subField.name();

        Log.d(TAG, String.format("%-20s : %4d \n", fieldName, value));
    }
//
//    private Bitmap loadBitmapFromUri(Uri photoUri) {
//        Bitmap photo = null;
//        try {
//            photo = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
//            ExifInterface exif = new ExifInterface(getContentResolver().openInputStream(photoUri));
//            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
//
//            switch (orientation) {
//                case ExifInterface.ORIENTATION_ROTATE_270:
//                    photo = rotate(photo, 270);
//                    break;
//                case ExifInterface.ORIENTATION_ROTATE_180:
//                    photo = rotate(photo, 180);
//                    break;
//                case ExifInterface.ORIENTATION_ROTATE_90:
//                    photo = rotate(photo, 90);
//                    break;
//                default:
//            }
//        } catch (IOException e) {
//            Log.e(TAG, "Unable to load photo: " + e.getMessage(), e);
//            showErrorMessage("Unable to load photo: " + e.getMessage());
//        }
//
//        return photo;
//    }
//
//    public Bitmap rotate(Bitmap source, float angle)
//    {
//        Matrix matrix = new Matrix();
//        matrix.postRotate(angle);
//        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
//    }
}