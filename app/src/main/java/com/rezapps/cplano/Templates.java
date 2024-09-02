package com.rezapps.cplano;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.rezapps.ocrform.FieldTemplate;
import com.rezapps.ocrform.FormTemplate;
import com.rezapps.ocrform.RefMarker;
import com.rezapps.ocrform.SubField;
import com.rezapps.ocrform.SubFieldTemplate;
import com.rezapps.ocrform.Util;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Templates {

    private static final String TAG = "CPlano" ;

    public static String DPPHP = "DPPHP";
    public static String PWP3 = "PWP3";
    public static String DPRX8 = "DPRX8";
    public static String DPRX10 = "DPRX10";
    public static String DPRX12 = "DPRX12";
    public static String DPRX14 = "DPRX14";
    public static String DPX_J = "DPX_J";
    public static String DPD = "DPD";


    private final Map<String, FormTemplate> templates = new TreeMap<>();

    Mat markSheetC;
    Mat markSheetH;
    Mat markSheetV;

    Mat markTableTL;
    Mat markTableTC;
    Mat markTableTR;
    Mat markTableBL;
    Mat markTableBC;
    Mat markTableBR;

    Mat markRowL;
    Mat markRowR;


    public Templates(Context context) {

        if (!OpenCVLoader.initDebug()) {
            Log.w(TAG, "Internal OpenCV library not found. Using System.loadLibrary");
            System.loadLibrary("opencv_java4");
        }
        // Load markers

        try {
            markSheetC = Util.bmpToMat(BitmapFactory.decodeStream(context.getAssets().open("marker_sheet_c.png")));
            markSheetH = Util.bmpToMat(BitmapFactory.decodeStream(context.getAssets().open("marker_sheet_h.png")));
            markSheetV = Util.bmpToMat(BitmapFactory.decodeStream(context.getAssets().open("marker_sheet_v.png")));

            markTableTL = Util.bmpToMat(BitmapFactory.decodeStream(context.getAssets().open("marker_table_tl.png")));
            markTableTC = Util.bmpToMat(BitmapFactory.decodeStream(context.getAssets().open("marker_table_tc.png")));
            markTableTR = Util.bmpToMat(BitmapFactory.decodeStream(context.getAssets().open("marker_table_tr.png")));
            markTableBL = Util.bmpToMat(BitmapFactory.decodeStream(context.getAssets().open("marker_table_bl.png")));
            markTableBC = Util.bmpToMat(BitmapFactory.decodeStream(context.getAssets().open("marker_table_bc.png")));
            markTableBR = Util.bmpToMat(BitmapFactory.decodeStream(context.getAssets().open("marker_table_br.png")));

            markRowL = Util.bmpToMat(BitmapFactory.decodeStream(context.getAssets().open("marker_l_i.png")));
            markRowR = Util.bmpToMat(BitmapFactory.decodeStream(context.getAssets().open("marker_r_i.png")));


        } catch (IOException e) {
            e.printStackTrace();
        }

        prepareTemplatePWP3();
        prepareTemplateDPRX8();
        prepareTemplateDPRX10();
        prepareTemplateDPRX14();
        prepareTemplateDPD();
        prepareTemplateDPXJ();
        prepareTemplateDPPHP();


    }


    private void prepareTemplateDPD() {

        // Lembar Suara PWP3
        int linesumW = 36;
        int numsumW = 60;
        int numsumH = 72;
        double numsumGap = 18.5;

        int tallyL = 38;
        int linesumL = 824;
        int numsumL = 938;
        int spellsumT = 225;

        int cellH = 36;
        int tallyW = 58;
        int spellsumW = 28;
        int spellsumH = 39;
        int searchMargin = 80;

        Map<SubField, SubFieldTemplate> detailSubFields = new TreeMap<>();
        detailSubFields.put(SubField.TALLY,
                new SubFieldTemplate(-10, tallyL, tallyW,cellH,12,tallyW+4.7,5,cellH+4.1));
        detailSubFields.put(SubField.LINESUM,
                new SubFieldTemplate(-10, linesumL, linesumW, cellH,2,linesumW+4,5,cellH+4.1));
        detailSubFields.put(SubField.SEGMEN7,
                new SubFieldTemplate(-9, numsumL, numsumW, numsumH,3,numsumW+numsumGap));
        detailSubFields.put(SubField.SPELLED,
                new SubFieldTemplate(spellsumT, tallyL, spellsumW, spellsumH,35,spellsumW+4.1));

        List<FieldTemplate> fields = new ArrayList<>();
        fields.add(new FieldTemplate(FieldName.CALON, detailSubFields, 457, 484, 263, 1200,
                searchMargin, markRowL, markRowR, 6, 322.8));

        RefMarker[][] refMarkerSets = {
                {new RefMarker(markTableTL,  530, 332), new RefMarker(markSheetC,   81,  66), new RefMarker(markSheetH,  900,  55)},
                {new RefMarker(markTableTR, 1633, 332), new RefMarker(markSheetC, 1720,  66), new RefMarker(markSheetV, 1733,1404)},
                {new RefMarker(markTableBL,  530,2473), new RefMarker(markSheetC,   81,2742), new RefMarker(markTableBC,1082,2472)},
                {new RefMarker(markTableBR, 1633,2473), new RefMarker(markSheetC, 1720,2742), new RefMarker(markSheetV,   70,1404)}
        };

        templates.put(DPD, new FormTemplate(DPD, fields, refMarkerSets));
        // End of Lembar Suara DPD
    }

    private void prepareTemplatePWP3() {

        // Lembar Suara PWP3
        int linesumW = 36;
        int numsumW = 60;
        int numsumH = 72;
        double numsumGap = 18.5;

        int tallyL = 38;
        int linesumL = 824;
        int numsumL = 938;
        int spellsumT = 225;

        int cellH = 36;
        int tallyW = 58;
        int spellsumW = 28;
        int spellsumH = 39;
        int searchMargin = 80;

        Map<SubField, SubFieldTemplate> detailSubFields = new TreeMap<>();
        detailSubFields.put(SubField.TALLY,
                new SubFieldTemplate(-10, tallyL, tallyW,cellH,12,tallyW+4.7,5,cellH+4.1));
        detailSubFields.put(SubField.LINESUM,
                new SubFieldTemplate(-10, linesumL, linesumW, cellH,2,linesumW+4,5,cellH+4.1));
        detailSubFields.put(SubField.SEGMEN7,
                new SubFieldTemplate(-9, numsumL, numsumW, numsumH,3,numsumW+numsumGap));
        detailSubFields.put(SubField.SPELLED,
                new SubFieldTemplate(spellsumT, tallyL, spellsumW, spellsumH,35,spellsumW+4.1));

        Map<SubField, SubFieldTemplate> sumSubFields = new TreeMap<>();
        sumSubFields.put(SubField.SEGMEN7,
                new SubFieldTemplate(-3, numsumL, numsumW, numsumH,3,numsumW+numsumGap));
        sumSubFields.put(SubField.SPELLED,
                new SubFieldTemplate(-9, tallyL, spellsumW, spellsumH,27,spellsumW+4.1, 2, spellsumH+7));

        List<FieldTemplate> fields = new ArrayList<>();
        fields.add(new FieldTemplate(FieldName.PASLON, detailSubFields, 457, 484, 263, 1200, searchMargin, markRowL, markRowR, 3, 322.8));
        fields.add(new FieldTemplate(FieldName.SAH, sumSubFields, 1526, 484, 75, 1200, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.TIDAK_SAH, detailSubFields, 1753, 484, 263, 1200, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.TOTAL, sumSubFields, 2109, 484, 76, 1200, searchMargin, markRowL, markRowR));

        RefMarker[][] refMarkerSets = {
                {new RefMarker(markTableTL,  530, 332), new RefMarker(markSheetH,  900,  55), new RefMarker(markSheetC,   81, 66)},
                {new RefMarker(markTableTR, 1633, 332), new RefMarker(markSheetC, 1720,  66), new RefMarker(markTableTC, 1082,333)},
                {new RefMarker(markTableBL,  530,2236), new RefMarker(markSheetH,  900,2755), new RefMarker(markSheetC,   81,2742)},
                {new RefMarker(markTableBR, 1633,2236), new RefMarker(markSheetC, 1720,2742), new RefMarker(markTableTC, 1082,2236)}
        };

        templates.put(PWP3, new FormTemplate(PWP3, fields, refMarkerSets));
        // End of Lembar Suara PWP3
    }

    private void prepareTemplateDPRX8() {

        int linesumW = 36;
        int numsumW = 60;
        double numsumGap = 18.5;

        // DPRX8
        int tallyL = 26;
        int linesumL = 824;
        int numsumL = 938;
        int spellSumL = 34;

        int cellH = 36;
        int tallyW = 56;
        int spellsumW = 28;
        int spellsumH =39;
        int numsumH = 70;
        int searchMargin = 70;

        Map<SubField, SubFieldTemplate> detailSubFields = new TreeMap<>();
        detailSubFields.put(SubField.TALLY,
                new SubFieldTemplate(-18, tallyL, tallyW, cellH,12,tallyW+9.3,5,cellH+4.8));
        detailSubFields.put(SubField.LINESUM,
                new SubFieldTemplate(-18, linesumL, linesumW, cellH,2,linesumW+4,5,cellH+4.8));
        detailSubFields.put(SubField.SEGMEN7,
                new SubFieldTemplate(-17, numsumL, numsumW, numsumH,3,numsumW+numsumGap));

        Map<SubField, SubFieldTemplate> sumSubFields = new TreeMap<>();
        sumSubFields.put(SubField.SEGMEN7,
                new SubFieldTemplate(-5, numsumL, numsumW, numsumH,3,numsumW+numsumGap));
        sumSubFields.put(SubField.SPELLED,
                new SubFieldTemplate(-11, spellSumL, spellsumW, spellsumH,27,spellsumW+4.1, 2, spellsumH+7));

        List<FieldTemplate> fields = new ArrayList<>();
        fields.add(new FieldTemplate(FieldName.PARTAI, detailSubFields, 450, 484, 181, 1200, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.CALON, detailSubFields, 682, 484, 181, 1200, searchMargin, markRowL, markRowR, 8, 208.4));
        fields.add(new FieldTemplate(FieldName.TOTAL_PARTAI, sumSubFields, 2344, 484, 85, 1200, searchMargin, markRowL, markRowR));

        RefMarker[][] refMarkerSets = new RefMarker[][]{
                {new RefMarker(markTableTL,  530, 332), new RefMarker(markSheetC,   81,  66), new RefMarker(markSheetH,  900,  55)},
                {new RefMarker(markTableTR, 1633, 332), new RefMarker(markSheetC, 1720,  66), new RefMarker(markSheetV, 1733,1404)},
                {new RefMarker(markTableBL,  530,2473), new RefMarker(markSheetC,   81,2742), new RefMarker(markTableBC,1082,2472)},
                {new RefMarker(markTableBR, 1633,2473), new RefMarker(markSheetC, 1720,2742), new RefMarker(markSheetV,   70,1404)}
        };

        templates.put(DPRX8, new FormTemplate(DPRX8, fields, refMarkerSets));
        // End of DPRX8


    }


    private void prepareTemplateDPRX10() {

        int linesumW = 36;
        int numsumW = 60;
        double numsumGap = 18.5;

        // DPRX10
        int tallyL = 25;
        int linesumL = 823;
        int numsumL = 938;
        int spellSumL = 34;

        int cellH = 29;
        int tallyW = 56;
        int spellsumW = 28;
        int spellsumH = 39;
        int numsumH = 70;
        int searchMargin = 60;

        Map<SubField, SubFieldTemplate> detailSubFields = new TreeMap<>();
        detailSubFields.put(SubField.TALLY,
                new SubFieldTemplate(-18, tallyL, tallyW, cellH,12,tallyW+9.3,5,cellH+3.5));
        detailSubFields.put(SubField.LINESUM,
                new SubFieldTemplate(-18, linesumL, linesumW, cellH,2,linesumW+4,5,cellH+3.5));
        detailSubFields.put(SubField.SEGMEN7,
                new SubFieldTemplate(-17, numsumL, numsumW, numsumH,3,numsumW+numsumGap));

        Map<SubField, SubFieldTemplate> sumSubFields = new TreeMap<>();
        sumSubFields.put(SubField.SEGMEN7,
                new SubFieldTemplate(-9, numsumL, numsumW, numsumH,3,numsumW+numsumGap));
        sumSubFields.put(SubField.SPELLED,
                new SubFieldTemplate(-15, spellSumL, spellsumW, spellsumH,27,spellsumW+4.1, 2, spellsumH+8));

        List<FieldTemplate> fields = new ArrayList<>();
        fields.add(new FieldTemplate(FieldName.PARTAI, detailSubFields, 450, 484, 145, 1200, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.CALON, detailSubFields, 646, 484, 145, 1200, searchMargin, markRowL, markRowR, 10, 172.3));
        fields.add(new FieldTemplate(FieldName.TOTAL_PARTAI, sumSubFields, 2369, 484, 70, 1200, searchMargin, markRowL, markRowR));

        RefMarker[][] refMarkerSets = new RefMarker[][]{
                {new RefMarker(markTableTL,  530, 332), new RefMarker(markSheetC,   81,  66), new RefMarker(markSheetH,  900,  55)},
                {new RefMarker(markTableTR, 1633, 332), new RefMarker(markSheetC, 1720,  66), new RefMarker(markSheetV, 1733,1404)},
                {new RefMarker(markTableBL,  530,2473), new RefMarker(markSheetC,   81,2742), new RefMarker(markTableBC,1082,2472)},
                {new RefMarker(markTableBR, 1633,2473), new RefMarker(markSheetC, 1720,2742), new RefMarker(markSheetV,   70,1404)}
        };

        templates.put(DPRX10, new FormTemplate(DPRX10, fields, refMarkerSets));
        // End of DPRX10
    }


    private void prepareTemplateDPRX14() {

        int linesumW = 36;
        int numsumW = 60;
        double numsumGap = 18.5;

        // DPRX14
        int tallyL = 26;
        int linesumL = 825;
        int numsumL = 938;
        int spellSumL = 34;

        int cellH = 27;
        int tallyW = 56;
        int numsumH = 70;
        int searchMargin = 42;
        int spellsumW = 28;
        int spellsumH = 38;

        Map<SubField, SubFieldTemplate> detailSubFields1 = new TreeMap<>();
        detailSubFields1.put(SubField.TALLY,
                new SubFieldTemplate(-18, tallyL, tallyW, cellH,12,tallyW+9.3,5,cellH+5.5));
        detailSubFields1.put(SubField.LINESUM,
                new SubFieldTemplate(-18, linesumL, linesumW, cellH,2,linesumW+4,5,cellH+5.5));
        detailSubFields1.put(SubField.SEGMEN7,
                new SubFieldTemplate(-18, numsumL, numsumW, numsumH,3,numsumW+numsumGap));

        Map<SubField, SubFieldTemplate> detailSubFields2 = new TreeMap<>();
        detailSubFields2.put(SubField.TALLY,
                new SubFieldTemplate(-18, tallyL, tallyW, cellH,12,tallyW+9.3,4,cellH+2));
        detailSubFields2.put(SubField.LINESUM,
                new SubFieldTemplate(-18, linesumL, linesumW, cellH,2,linesumW+4,4,cellH+2));
        detailSubFields2.put(SubField.SEGMEN7,
                new SubFieldTemplate(-17, numsumL, numsumW, numsumH,3,numsumW+numsumGap));

        Map<SubField, SubFieldTemplate> sumSubFields = new TreeMap<>();
        sumSubFields.put(SubField.SEGMEN7,
                new SubFieldTemplate(-4, numsumL, numsumW, numsumH,3,numsumW+numsumGap));
        sumSubFields.put(SubField.SPELLED,
                new SubFieldTemplate(-15, spellSumL, spellsumW, spellsumH,27,spellsumW+4.1,2,spellsumH+8));

        List<FieldTemplate> fields = new ArrayList<>();
        fields.add(new FieldTemplate(FieldName.PARTAI, detailSubFields1, 452, 483, 145, 1200, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.CALON, detailSubFields2, 646, 483, 98, 1200, searchMargin, markRowL, markRowR, 14, 123.2));
        fields.add(new FieldTemplate(FieldName.TOTAL_PARTAI, sumSubFields, 2369, 483, 70, 1200, searchMargin, markRowL, markRowR));

        RefMarker[][] refMarkerSets = new RefMarker[][]{
                {new RefMarker(markTableTL,  530, 332), new RefMarker(markSheetC,   81,  66), new RefMarker(markSheetH,  900,  55)},
                {new RefMarker(markTableTR, 1633, 332), new RefMarker(markSheetC, 1720,  66), new RefMarker(markSheetV, 1733,1404)},
                {new RefMarker(markTableBL,  530,2473), new RefMarker(markSheetC,   81,2742), new RefMarker(markTableBC,1082,2472)},
                {new RefMarker(markTableBR, 1633,2473), new RefMarker(markSheetC, 1720,2742), new RefMarker(markSheetV,   70,1404)}
        };

        templates.put(DPRX14, new FormTemplate(DPRX14, fields, refMarkerSets));
        // End of DPRX14

    }

    private void prepareTemplateDPXJ() {

        // DPRX_J - Lembar Suara Sah & Tidak Sah
        int linesumW = 36;
        int numsumW = 60;
        int numsumH = 72;
        double numsumGap = 18.5;

        int tallyL = 38;
        int linesumL = 824;
        int numsumL = 938;
        int spellSumT = 225;

        int cellH = 36;
        int tallyW = 58;
        int spellsumW = 28;
        int spellsumH =39;
        int searchMargin = 80;

        Map<SubField, SubFieldTemplate> detailSubFields = new TreeMap<>();
        detailSubFields.put(SubField.TALLY,
                new SubFieldTemplate(-10, tallyL, tallyW, cellH,12,tallyW+4.7,5,cellH+4.1));
        detailSubFields.put(SubField.LINESUM,
                new SubFieldTemplate(-10, linesumL, linesumW, cellH,2,linesumW+4,5,cellH+4.1));
        detailSubFields.put(SubField.SEGMEN7,
                new SubFieldTemplate(-9, numsumL, numsumW, numsumH,3,numsumW+numsumGap));
        detailSubFields.put(SubField.SPELLED,
                new SubFieldTemplate(spellSumT, tallyL, spellsumW, spellsumH,35,spellsumW+4.1));

        Map<SubField, SubFieldTemplate> sumSubFields = new TreeMap<>();
        sumSubFields.put(SubField.SEGMEN7,
                new SubFieldTemplate(-3, numsumL, numsumW, numsumH,3,numsumW+numsumGap));
        sumSubFields.put(SubField.SPELLED,
                new SubFieldTemplate(-9, tallyL, spellsumW, spellsumH,27,spellsumW+4.1, 2, spellsumH+7));

        List<FieldTemplate> fields = new ArrayList<>();
        fields.add(new FieldTemplate(FieldName.SAH, sumSubFields, 391, 484, 75, 1200, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.TIDAK_SAH, detailSubFields, 618, 484, 263, 1200, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.TOTAL, sumSubFields, 974, 484, 76, 1200, searchMargin, markRowL, markRowR));

        RefMarker[][] refMarkerSets = new RefMarker[][]{
                {new RefMarker(markTableTL,  530,  332), new RefMarker(markSheetH,  900,  55), new RefMarker(markSheetC,   81,  66)},
                {new RefMarker(markTableTR, 1633,  332), new RefMarker(markSheetC, 1720,  66)},
                {new RefMarker(markTableBL,  530, 1101), new RefMarker(markSheetV,   70,1404), new RefMarker(markSheetC,   81,2742)},
                {new RefMarker(markTableBR, 1633, 1101), new RefMarker(markSheetV, 1733,1404), new RefMarker(markSheetC, 1720,2742)}
        };

        templates.put(DPX_J, new FormTemplate(DPX_J, fields, refMarkerSets));


    }

    private void prepareTemplateDPPHP() {

        // DPPHP
        int numsumW = 55;
        int numsumH = 54;
        double numsumGap = 21.5;
        int searchMargin = 35;


        Map<SubField, SubFieldTemplate> sumSubFieldsL = new TreeMap<>();
        sumSubFieldsL.put(SubField.SEGMEN7, new SubFieldTemplate(-10, 40, numsumW, numsumH,3,numsumW+numsumGap));
        Map<SubField, SubFieldTemplate> sumSubFieldsP = new TreeMap<>();
        sumSubFieldsP.put(SubField.SEGMEN7, new SubFieldTemplate(-10, 290, numsumW, numsumH,3,numsumW+numsumGap));
        Map<SubField, SubFieldTemplate> sumSubFieldsJ = new TreeMap<>();
        sumSubFieldsJ.put(SubField.SEGMEN7, new SubFieldTemplate(-10, 541, numsumW, numsumH,3,numsumW+numsumGap));

        List<FieldTemplate> fields = new ArrayList<>();
        fields.add(new FieldTemplate(FieldName.DP1L, sumSubFieldsL,  943, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.DP1P, sumSubFieldsP,  943, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.DP1T, sumSubFieldsJ,  943, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.DP2L, sumSubFieldsL, 1020, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.DP2P, sumSubFieldsP, 1020, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.DP2T, sumSubFieldsJ, 1020, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.DP3L, sumSubFieldsL, 1097, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.DP3P, sumSubFieldsP, 1097, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.DP3T, sumSubFieldsJ, 1097, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.DP4L, sumSubFieldsL, 1174, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.DP4P, sumSubFieldsP, 1174, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.DP4T, sumSubFieldsJ, 1174, 889,  42, 795, searchMargin, markRowL, markRowR));

        fields.add(new FieldTemplate(FieldName.PHP1L, sumSubFieldsL, 1317, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.PHP1P, sumSubFieldsP, 1317, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.PHP1T, sumSubFieldsJ, 1317, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.PHP2L, sumSubFieldsL, 1394, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.PHP2P, sumSubFieldsP, 1394, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.PHP2T, sumSubFieldsJ, 1394, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.PHP3L, sumSubFieldsL, 1471, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.PHP3P, sumSubFieldsP, 1471, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.PHP3T, sumSubFieldsJ, 1471, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.PHP4L, sumSubFieldsL, 1548, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.PHP4P, sumSubFieldsP, 1548, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.PHP4T, sumSubFieldsJ, 1548, 889,  42, 795, searchMargin, markRowL, markRowR));

        fields.add(new FieldTemplate(FieldName.PSS1, sumSubFieldsL, 1792, 1390,  42, 294, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.PSS2, sumSubFieldsL, 1869, 1390,  42, 294, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.PSS3, sumSubFieldsL, 1946, 1390,  42, 294, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.PSS4, sumSubFieldsL, 2023, 1390,  42, 294, searchMargin, markRowL, markRowR));

        fields.add(new FieldTemplate(FieldName.DPD1L, sumSubFieldsL, 2259, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.DPD1P, sumSubFieldsP, 2259, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.DPD1T, sumSubFieldsJ, 2259, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.DPD2L, sumSubFieldsL, 2336, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.DPD2P, sumSubFieldsP, 2336, 889,  42, 795, searchMargin, markRowL, markRowR));
        fields.add(new FieldTemplate(FieldName.DPD2T, sumSubFieldsJ, 2336, 889,  42, 795, searchMargin, markRowL, markRowR));

        RefMarker[][] refMarkerSets = new RefMarker[][]{
                {new RefMarker(markTableTL,  934,  760), new RefMarker(markSheetH,  900,  55), new RefMarker(markSheetC,    81,   66)},
                {new RefMarker(markTableTR, 1633,  760), new RefMarker(markSheetV, 1733,1404), new RefMarker(markSheetC,  1720,   66)},
                {new RefMarker(markTableBL,  934, 2428), new RefMarker(markSheetH,  900,2755), new RefMarker(markSheetC,    81, 2742)},
                {new RefMarker(markTableBR, 1633, 2428), new RefMarker(markSheetC, 1720,2742), new RefMarker(markTableBC, 1284, 2428)}
        };

        templates.put(DPPHP, new FormTemplate(DPPHP, fields, refMarkerSets));
        // End of DPPHP
    }



    public FormTemplate get(String name) {
        return templates.get(name);
    }

    public FormTemplate get(char templateCode) {
        switch (templateCode) {
            case 'A':
                return templates.get(Templates.DPPHP);
            case 'B':
                return templates.get(Templates.PWP3);
            case 'C':
                return templates.get(Templates.DPRX8);
            case 'D':
                return templates.get(Templates.DPRX10);
            case 'E':
                return templates.get(Templates.DPRX12);
            case 'F':
                return templates.get(Templates.DPRX14);
            case 'G':
                return templates.get(Templates.DPD);
            case 'H':
                return templates.get(Templates.DPX_J);
            default:
                return null;
        }
    }
}
