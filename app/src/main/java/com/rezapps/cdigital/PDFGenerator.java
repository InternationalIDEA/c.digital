package com.rezapps.cdigital;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import com.rezapps.cplano.CPlano;
import com.rezapps.cplano.CPlanoPage;
import com.rezapps.cplano.ElectionType;
import com.rezapps.cplano.FieldName;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;

import org.apache.pdfbox.examples.signature.VisibleSignatureSigner;
import org.apache.pdfbox.examples.util.Rectangle2D;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PDFGenerator {

    private static final String TAG = "CDigital";

    public static int FONT_SIZE_TITLE = 14;
    public static int FONT_SIZE_BODY = 11;
    public static int FONT_SIZE_PAGE_HEADER = 9;
    public static int FONT_SIZE_TABLE_HEADER = 9;

    static String DOCUMENT_TITLE = "Salinan Digital Formulir C1-Plano-";

    static float MARGIN_L = 65;
    static float MARGIN_T = 800;
    static float MARGIN_B = 50;
    static float PAGE_NUM_L = 500;
    static float TITLE_T = 776;
    static float HEADER_TABLE_T = 710;
    static float HEADER_COL1_L = 65;
    static float HEADER_COL2_L = 160;
    static float HEADER_COL3_L = 325;
    static float HEADER_COL4_L = 420;

    static float BODY_T = 602;
    static float FIELD_VALUE_1_R = 385;
    static float FIELD_VALUE_2_R = 445;
    static float FIELD_VALUE_3_R = 505;

    static float FIELD_NAME_0_L = 80;
    static float FIELD_NAME_1_L = 356;
    static float FIELD_NAME_2_L = 411;
    static float FIELD_NAME_3_L = 480;

    static float PHOTO_B = 66;
    static float PHOTO_L = 65;
    static float PHOTO_W = 477;
    static float PHOTO_H = 709;

    static int SIGNATURE_T = 160;
    static int SIGNATURE_L = 325;
    static int SIGNATURE_W = 140;
    static int SIGNATURE_H = 44;

    static float DIGIT_WIDTH = 6.116f;
    static float LINE_SPACING = 15;

    static float PARTY_BLOCK_T = 730;
    static float PARTY_BLOCK_W = 250;
    static float PARTY_BLOCK_H = 315;
    static float PARTY_VOTE_L = 190;
    static float PARTY_VOTE_R = 210;

    float yPos;

    CDigitalApplication application;
    Activity context;
    CPlano cPlano;
    Map<String, Integer> mergedData = new HashMap<>();
    int pageCount;
    int currentPage = 0;
    String electionDesc1;
    String electionDesc2 = "";
    SharedPreferences tps;

    public PDFGenerator(Activity context) {
        this.context = context;
        application = (CDigitalApplication)context.getApplication();
    }

    public boolean generatePDF(CPlano cPlano, File pdfFile) {

        this.cPlano = cPlano;

        mergedData.putAll(cPlano.pages[0].verifiedData);
        mergedData.putAll(cPlano.pages[cPlano.pages.length-1].verifiedData);

        int votePageCount;
        if (cPlano.electionType == ElectionType.DPD_RI) {
            votePageCount = 1;
        } else if (cPlano.electionType.dpr) {
            String[] parties = application.getParties(cPlano.electionType);
            votePageCount = (int)Math.ceil(parties.length / 4.);
        } else {
            votePageCount = 0;
        }
        pageCount = cPlano.pages.length + 1 + votePageCount;
        tps = context.getSharedPreferences(Param.TPS, Context.MODE_PRIVATE);

        if (cPlano.electionType == ElectionType.DPRD_PROV) {
            electionDesc1 = "Pemilihan Anggota Dewan Perwakilan Rakyat Daerah";
            electionDesc2 = "Provinsi " + tps.getString(Param.PROVINSI, Param.PROVINSI_DEFAULT) + " ";
        } else if (cPlano.electionType == ElectionType.DPRD_KAB) {
            electionDesc1 = "Pemilihan Anggota Dewan Perwakilan Rakyat Daerah";
            electionDesc2 = tps.getString(Param.KABUPATEN, Param.KABUPATEN_DEFAULT) + " ";
        }  else {
            electionDesc1 = cPlano.electionType.fullDesc;
        }
        electionDesc2 += "Tahun 2024";

        PDFBoxResourceLoader.init(context);
        PDDocument pdf = new PDDocument();

        try {
            addCoverPage(pdf, votePageCount);
            if (cPlano.electionType == ElectionType.DPD_RI) {
                addDPDVoteResultPage(pdf)   ;
            } else {
                for (int i = 0; i < votePageCount; i++) {
                    addVoteResultPage(pdf, i);
                }
            }

            for (CPlanoPage page : cPlano.pages) {
                addPhotoPage(pdf, page);
            }


            pdf.save(pdfFile);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }


    private void addCoverPage(PDDocument pdf, int votePageCount) throws IOException {

        PDPage page = new PDPage(PDRectangle.A4);
        pdf.addPage(page);
        currentPage++;

        PDPageContentStream cs = new PDPageContentStream(pdf, page);

        cs.setLeading(LINE_SPACING);

        // Write Header
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, FONT_SIZE_PAGE_HEADER);
        cs.newLineAtOffset(PAGE_NUM_L, MARGIN_T);
        cs.showText("Hal. 1 dari " + pageCount);
        cs.endText();

        // Write Title
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_TITLE);
        cs.newLineAtOffset(MARGIN_L, TITLE_T);
        cs.showText(DOCUMENT_TITLE + cPlano.electionType.abbrev);
        cs.newLineAtOffset(0, -18);
        cs.showText(electionDesc1);
        cs.newLineAtOffset(0, -18);
        cs.showText(electionDesc2);
        cs.endText();

        // write TPS Info
        cs.setFont(PDType1Font.HELVETICA, FONT_SIZE_BODY);
        cs.beginText();
        cs.newLineAtOffset(HEADER_COL1_L, HEADER_TABLE_T);
        cs.showText("TPS ID:"); cs.newLine();
        cs.showText("TPS Nomor:"); cs.newLine();
        cs.showText("Kelurahan:"); cs.newLine();
        cs.showText("Kecamatan:"); cs.newLine();
        cs.showText("Kabupaten/Kota:"); cs.newLine();
        cs.showText("Provinsi:");
        cs.endText();

        cs.beginText();
        cs.newLineAtOffset(HEADER_COL2_L, HEADER_TABLE_T);
        cs.showText(tps.getString(Param.TPS_ID, Param.TPS_ID_DEFAULT)); cs.newLine();
        cs.showText(tps.getString(Param.TPS_NO, Param.TPS_NO_DEFAULT)); cs.newLine();
        cs.showText(tps.getString(Param.KELURAHAN, Param.KELURAHAN_DEFAULT)); cs.newLine();
        cs.showText(tps.getString(Param.KECAMATAN, Param.KECAMATAN_DEFAULT)); cs.newLine();
        cs.showText(tps.getString(Param.KABUPATEN, Param.KABUPATEN_DEFAULT)); cs.newLine();
        cs.showText(tps.getString(Param.PROVINSI, Param.PROVINSI_DEFAULT)); cs.newLine();
        cs.endText();

        cs.beginText();
        cs.newLineAtOffset(HEADER_COL3_L, HEADER_TABLE_T);
        cs.showText("Petugas KPPS:"); cs.newLine();
        cs.showText("NIK:"); cs.newLine();
        cs.endText();

        cs.beginText();
        cs.newLineAtOffset(HEADER_COL4_L, HEADER_TABLE_T);
        cs.showText(tps.getString(Param.NAMA, Param.NAMA_DEFAULT)); cs.newLine();
        cs.showText(tps.getString(Param.NIK, Param.NIK_DEFAULT)); cs.newLine();
        cs.endText();

        // Write Body
        Resources res = context.getResources();
        yPos = BODY_T;
        writeSectionTitle(cs, res.getString(R.string.section_1));
        writeTableHeader(cs, "Uraian", "Laki-laki", "Perempuan", "Jumlah");
        writeSectionTitle(cs, res.getString(R.string.section_1A));
        writeDataRow(cs, res.getString(R.string.field_DP1), FieldName.DP1L, FieldName.DP1P, FieldName.DP1T);
        writeDataRow(cs, res.getString(R.string.field_DP2), FieldName.DP2L, FieldName.DP2P, FieldName.DP2T);
        writeDataRow(cs, res.getString(R.string.field_DP3), FieldName.DP3L, FieldName.DP3P, FieldName.DP3T);
        writeDataRow(cs, res.getString(R.string.field_DPJ), FieldName.DP4L, FieldName.DP4P, FieldName.DP4T);

        writeSectionTitle(cs, res.getString(R.string.section_1B));
        writeDataRow(cs, res.getString(R.string.field_PHP1), FieldName.PHP1L, FieldName.PHP1P, FieldName.PHP1T);
        writeDataRow(cs, res.getString(R.string.field_PHP2), FieldName.PHP2L, FieldName.PHP2P, FieldName.PHP2T);
        writeDataRow(cs, res.getString(R.string.field_PHP3), FieldName.PHP3L, FieldName.PHP3P, FieldName.PHP3T);
        writeDataRow(cs, res.getString(R.string.field_PHPJ), FieldName.PHP4L, FieldName.PHP4P, FieldName.PHP4T);
        writeHorizontalLine(cs, yPos + 10);

        yPos -= LINE_SPACING;
        writeSectionTitle(cs, res.getString(R.string.section_2));
        writeTableHeader(cs, "Uraian", null, null, "Jumlah");
        writeDataRow(cs, res.getString(R.string.field_DPSS1), null, null, FieldName.PSS1);
        writeDataRow(cs, res.getString(R.string.field_DPSS2), null, null,  FieldName.PSS2);
        writeDataRow(cs, res.getString(R.string.field_DPSS3), null, null, FieldName.PSS3);
        writeDataRow(cs, res.getString(R.string.field_DPSS4), null, null, FieldName.PSS4);
        writeHorizontalLine(cs, yPos+10);

        yPos -= LINE_SPACING;
        writeSectionTitle(cs, res.getString(R.string.section_3));
        writeTableHeader(cs, "Uraian", "Laki-laki", "Perempuan", "Jumlah");
        writeDataRow(cs, res.getString(R.string.field_DPD1), FieldName.DPD1L, FieldName.DPD1P, FieldName.DPD1T);
        writeDataRow(cs, res.getString(R.string.field_DPD2), FieldName.DPD2L, FieldName.DPD2P, FieldName.DPD2T);
        writeHorizontalLine(cs, yPos+10);

        yPos -= LINE_SPACING;
        if (cPlano.electionType.dpr || cPlano.electionType == ElectionType.DPD_RI) {
            if (cPlano.electionType == ElectionType.DPD_RI)
                writeSectionTitle(cs, res.getString(R.string.section_4_DPD));
            else
                writeSectionTitle(cs, res.getString(R.string.section_4_DPR));

            cs.setFont(PDType1Font.HELVETICA, FONT_SIZE_TABLE_HEADER);
            cs.beginText();
            cs.newLineAtOffset(FIELD_NAME_0_L, yPos);
            if (votePageCount == 1)
                cs.showText("(Lihat halaman 2)");
            else
                cs.showText("(Lihat halaman 2 - " + (1+votePageCount) + ")");
            cs.endText();
            yPos -= LINE_SPACING;
            yPos -= LINE_SPACING;

        } else {
            writeSectionTitle(cs, res.getString(R.string.section_4_PWP));
            writeTableHeader(cs, "Nomor dan Nama Pasangan Calon", null, null, "Jumlah");
            String[] candidates = application.getCandidates(cPlano.electionType);

            int i=0;
            while (i<cPlano.pages[1].verifiedVotes.length && i<candidates.length) {
                writeVoteRow(cs, i);
                i++;
            }
            writeHorizontalLine(cs, yPos+10);
        }


        yPos -= LINE_SPACING;
        writeSectionTitle(cs, res.getString(R.string.section_5));
        writeTableHeader(cs, "Uraian", null, null, "Jumlah");
        writeDataRow(cs, res.getString(R.string.field_SAH), null, null, FieldName.SAH);
        writeDataRow(cs, res.getString(R.string.field_TIDAK_SAH), null, null,  FieldName.TIDAK_SAH);
        writeDataRow(cs, res.getString(R.string.field_TOTAL), null, null, FieldName.TOTAL);
        writeHorizontalLine(cs, yPos+10);

        writePageFooter(cs);
        cs.close();
    }


    private void addPhotoPage(PDDocument pdf, CPlanoPage page) throws IOException {

        PDPage pdPage = new PDPage(PDRectangle.A4);
        pdf.addPage(pdPage);
        currentPage++;

        PDPageContentStream cs = new PDPageContentStream(pdf, pdPage);
        writePageHeader(cs);

        // draw photo

        File dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File photoFile = new File(dir, page.alignedPhotoFile);
        Bitmap photoBitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
        photoBitmap = Bitmap.createScaledBitmap(photoBitmap, 1350, 2005, false);

        //PDImageXObject pdImage = PDImageXObject.createFromByteArray(pdf, photoBA, page.alignedPhotoFile);
        // PDImageXObject pdImage = PDImageXObject.createFromFileByContent(photoFile,pdf);
        PDImageXObject pdImage = JPEGFactory.createFromImage(pdf, photoBitmap);

        Log.i(TAG, "pdImage dimensions " + pdImage.getHeight() + "," + pdImage.getWidth());
        cs.drawImage(pdImage,PHOTO_L, PHOTO_B, PHOTO_W, PHOTO_H);

        // Draw photo border
        cs.setLineWidth(0.5f);
        cs.setStrokingColor(0.5f);
        cs.moveTo(PHOTO_L, PHOTO_B);cs.lineTo(PHOTO_L, PHOTO_B+PHOTO_H); cs.stroke();
        cs.moveTo(PHOTO_L, PHOTO_B+PHOTO_H); cs.lineTo(PHOTO_L+PHOTO_W, PHOTO_B+PHOTO_H); cs.stroke();
        cs.moveTo(PHOTO_L+PHOTO_W, PHOTO_B+PHOTO_H); cs.lineTo(PHOTO_L+PHOTO_W, PHOTO_B); cs.stroke();
        cs.moveTo(PHOTO_L+PHOTO_W, PHOTO_B); cs.lineTo(PHOTO_L, PHOTO_B); cs.stroke();

        writePageFooter(cs);
        cs.close();
    }

    private void writePageHeader(PDPageContentStream cs) throws IOException {
        // Write Header
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, FONT_SIZE_PAGE_HEADER);
        cs.newLineAtOffset(PAGE_NUM_L, MARGIN_T);
        cs.showText("Hal. " + currentPage + " dari " + pageCount);
        cs.endText();

        // Write Title
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_PAGE_HEADER);
        cs.newLineAtOffset(MARGIN_L, MARGIN_T);
        cs.showText(DOCUMENT_TITLE + cPlano.electionType.abbrev);
        cs.newLineAtOffset(0, -10);
        cs.showText(electionDesc1 + " " + electionDesc2);
        cs.endText();
    }

    private void writePageFooter(PDPageContentStream cs) throws IOException {
        // Write Header
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, FONT_SIZE_PAGE_HEADER);
        cs.newLineAtOffset(MARGIN_L, MARGIN_B);
        cs.showText("© 2023 - Reza Lesmana, NETGRIT, International IDEA");
        cs.endText();

    }


    private void addDPDVoteResultPage(PDDocument pdf) throws IOException {

        PDPage pdPage = new PDPage(PDRectangle.A4);
        pdf.addPage(pdPage);
        currentPage++;

        PDPageContentStream cs = new PDPageContentStream(pdf, pdPage);
        cs.setLeading(LINE_SPACING);
        writePageHeader(cs);

        yPos = PARTY_BLOCK_T+30;
        writeSectionTitle(cs, context.getResources().getString(R.string.section_4_DPD));

        String[] candidates = application.getCandidates(cPlano.electionType);
        yPos = PARTY_BLOCK_T;
        cs.setFont(PDType1Font.HELVETICA, FONT_SIZE_BODY);
        for (int i=0; i < candidates.length; i++) {

            cs.beginText();
            float xPos = MARGIN_L;
            cs.newLineAtOffset(xPos, yPos);
            String candidateStr = "" + (i+21) + ". " + candidates[i];
            cs.showText(candidateStr);

            int pageIdx = i / application.DPD_CANDIDATES_PER_PAGE;
            String value = "" + cPlano.pages[pageIdx+1].verifiedVotes[i - pageIdx * application.DPD_CANDIDATES_PER_PAGE];
            float valueWidth = value.length() * DIGIT_WIDTH;
            float xOffset = FIELD_VALUE_3_R - valueWidth - xPos;
            cs.newLineAtOffset(xOffset, 0);
            cs.showText(value);

            cs.endText();
            yPos -= LINE_SPACING;
        }

        writePageFooter(cs);
        cs.close();
    }


    private void addVoteResultPage(PDDocument pdf, int votePageIdx) throws IOException {

        PDPage pdPage = new PDPage(PDRectangle.A4);
        pdf.addPage(pdPage);
        currentPage++;

        PDPageContentStream cs = new PDPageContentStream(pdf, pdPage);
        cs.setLeading(LINE_SPACING);
        writePageHeader(cs);

        yPos = PARTY_BLOCK_T+30;
        String sectionTitle = context.getResources().getString(R.string.section_4_DPR);
        if (votePageIdx > 0)
            sectionTitle += " - lanjutan";
        writeSectionTitle(cs, sectionTitle);

        String[] parties = application.getParties(cPlano.electionType);
        int partyIdx = votePageIdx * 4;

        for (int r=0; r < 2; r++) {
            for (int c=0; c < 2; c++) {
                if (partyIdx >= parties.length)
                    break;

                float top = PARTY_BLOCK_T - PARTY_BLOCK_H * r;
                float left = MARGIN_L + PARTY_BLOCK_W * c;
                writePartyBlock(cs, top, left, partyIdx);
                partyIdx++;
            }
        }

        writePageFooter(cs);
        cs.close();
    }

    private void writePartyBlock(PDPageContentStream cs, float top, float left, int partyIdx) throws IOException {

        String partyNum = String.valueOf(partyIdx + 1);
        String partyName = application.getParties(cPlano.electionType)[partyIdx];
        String[] candidates = application.getPartyCandidates(cPlano.electionType, partyIdx);
        CPlanoPage partyPage = cPlano.pages[partyIdx+1];

        cs.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_BODY);
        cs.setLineWidth(0.5f);

        cs.beginText();
        cs.newLineAtOffset(left, top);
        cs.showText(partyNum + ". " + partyName);
        cs.endText();

        yPos = top - 20;
        cs.moveTo(left, yPos+14); cs.lineTo(left + PARTY_VOTE_R, yPos+14); cs.stroke();
        writePartyDataRow(cs, left, "A. Suara Partai", partyPage.verifiedData.get(FieldName.PARTAI));
        yPos -= 4;
        cs.moveTo(left, yPos+14); cs.lineTo(left + PARTY_VOTE_R, yPos+14); cs.stroke();
        writePartyDataRow(cs, left, "B. Suara Calon", null);

        int i=0;
        while (i < candidates.length && i < partyPage.verifiedVotes.length) {
            String txt = "" + (i + 1) + ". " + candidates[i];
            writePartyDataRow(cs, left, txt, partyPage.verifiedVotes[i]);
            i++;
        }

        yPos -= 4;
        cs.moveTo(left, yPos+14); cs.lineTo(left + PARTY_VOTE_R, yPos+14); cs.stroke();
        writePartyDataRow(cs, left, "C. Jumlah Suara Partai dan Calon",
                partyPage.verifiedData.get(FieldName.TOTAL_PARTAI));

        cs.moveTo(left, yPos+10); cs.lineTo(left + PARTY_VOTE_R, yPos+10); cs.stroke();
    }

    private void writePartyDataRow(PDPageContentStream cs, float left, String text, Integer value) throws IOException {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, FONT_SIZE_BODY);
        cs.newLineAtOffset(left, yPos);
        cs.showText(text);

        if (value != null) {
            cs.setFont(PDType1Font.COURIER, FONT_SIZE_BODY);
            cs.newLineAtOffset(PARTY_VOTE_L, 0);
            cs.showText(String.format(Locale.getDefault(), "%3d", value));
        }
        cs.endText();
        yPos -= LINE_SPACING;
    }

    private void writeHorizontalLine(PDPageContentStream cs, float y) throws IOException {
        cs.setLineWidth(0.5f);
        cs.moveTo(MARGIN_L, y);
        cs.lineTo(FIELD_VALUE_3_R+8, y);
        cs.stroke();
    }


    private void writeSectionTitle(PDPageContentStream cs, String title) throws IOException {
        cs.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE_BODY);
        cs.beginText();
        cs.newLineAtOffset(MARGIN_L, yPos);
        cs.showText(title);
        cs.endText();
        yPos -= LINE_SPACING;
    }

    private void writeTableHeader(PDPageContentStream cs, String field0, String field1, String field2, String field3)
            throws IOException {

        writeHorizontalLine(cs, yPos+10);
        cs.setFont(PDType1Font.HELVETICA, FONT_SIZE_TABLE_HEADER);
        cs.beginText();
        cs.newLineAtOffset(FIELD_NAME_0_L, yPos);
        cs.showText(field0);
        cs.newLineAtOffset(FIELD_NAME_1_L-FIELD_NAME_0_L, 0);
        if (field1 != null) cs.showText(field1);

        cs.newLineAtOffset(FIELD_NAME_2_L-FIELD_NAME_1_L, 0);
        if (field2 != null) cs.showText(field2);

        cs.newLineAtOffset(FIELD_NAME_3_L-FIELD_NAME_2_L, 0);
        if (field3 != null) cs.showText(field3);
        cs.endText();
        writeHorizontalLine(cs, yPos-3);
        yPos -= (LINE_SPACING+2);

    }


        private void writeDataRow(PDPageContentStream cs, String fieldDesc, String field1, String field2,
                              String field3) throws IOException {

        cs.setFont(PDType1Font.HELVETICA, FONT_SIZE_BODY);
        cs.beginText();

        float xPos = MARGIN_L;
        cs.newLineAtOffset(xPos, yPos);
        cs.showText(fieldDesc);

        if (field1 != null) {
            String value = "" + mergedData.get(field1);
            float valueWidth = value.length() * DIGIT_WIDTH;
            float xOffset = FIELD_VALUE_1_R - valueWidth - xPos;
            cs.newLineAtOffset(xOffset, 0);
            cs.showText(value);
            xPos += xOffset;
        }

        if (field2 != null) {
            String value = "" + mergedData.get(field2);
            float valueWidth = value.length() * DIGIT_WIDTH;
            float xOffset = FIELD_VALUE_2_R - valueWidth - xPos;
            cs.newLineAtOffset(xOffset, 0);
            cs.showText(value);
            xPos += xOffset;
        }

        String value;
        value = "" + mergedData.get(field3);
        float valueWidth = value.length() * DIGIT_WIDTH;
        float xOffset = FIELD_VALUE_3_R - valueWidth - xPos;
        cs.newLineAtOffset(xOffset, 0);
        cs.showText(value);

        cs.endText();
        yPos -= LINE_SPACING;

    }

    private void writeVoteRow(PDPageContentStream cs, int idx) throws IOException {

        String[] candidates = application.getCandidates(cPlano.electionType);
        String candidate = candidates[idx];
        cs.setFont(PDType1Font.HELVETICA, FONT_SIZE_BODY);
        cs.beginText();

        float xPos = MARGIN_L;
        cs.newLineAtOffset(xPos, yPos);
        cs.showText("" + (idx+1) + ". " + candidate);

        String value;
        value = "" + cPlano.pages[1].verifiedVotes[idx];
        float valueWidth = value.length() * DIGIT_WIDTH;
        float xOffset = FIELD_VALUE_3_R - valueWidth - xPos;
        cs.newLineAtOffset(xOffset, 0);
        cs.showText(value);

        cs.endText();
        yPos -= LINE_SPACING;
    }


    public boolean signPDF(File inputFile, File outputFile, KeyStore keystore, String pin) {

        Log.i(TAG, "Signing: " + inputFile.getPath());

        InputStream inputStream;
        try {
            inputStream = new FileInputStream(inputFile);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Error opening input file: " + e.getMessage(), e);
            return false;
        }

        // Output PDF
        OutputStream outputStream;
        try {
            outputStream = new FileOutputStream(outputFile);

        } catch (FileNotFoundException e) {
            Log.e(TAG, "Error opening output file: " + e.getMessage(), e);
            return false;
        }

        try {
            VisibleSignatureSigner signer = new VisibleSignatureSigner(keystore, pin.toCharArray());

            Rectangle2D signRect = new Rectangle2D(SIGNATURE_L, SIGNATURE_T, SIGNATURE_W, SIGNATURE_H);

            signer.signPDF(inputStream, outputStream, signRect, null, "Signature1");
            inputStream.close();
            outputStream.close();

            Log.i(TAG, "PDF Signed: " + outputFile.getPath()  );

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error Signing PDF: " + e.getMessage(), e);
            return false;
        }
    }



}
