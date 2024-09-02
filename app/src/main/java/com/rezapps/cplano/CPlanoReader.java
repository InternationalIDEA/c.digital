package com.rezapps.cplano;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.rezapps.ocrform.FieldEntry;
import com.rezapps.ocrform.Form;
import com.rezapps.ocrform.FormReader;
import com.rezapps.ocrform.FormTemplate;
import com.rezapps.ocrform.PreProcessor;
import com.rezapps.ocrform.SubField;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.objdetect.QRCodeDetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CPlanoReader {

    private static final String TAG = "CPlano" ;


    static String[][] DP_MATRIX = {
            {FieldName.DP1L, FieldName.DP1P, FieldName.DP1T},
            {FieldName.DP2L, FieldName.DP2P, FieldName.DP2T},
            {FieldName.DP3L, FieldName.DP3P, FieldName.DP3T},
            {FieldName.DP4L, FieldName.DP4P, FieldName.DP4T}
    };

    static String[][] PHP_MATRIX = {
            {FieldName.PHP1L, FieldName.PHP1P, FieldName.PHP1T},
            {FieldName.PHP2L, FieldName.PHP2P, FieldName.PHP2T},
            {FieldName.PHP3L, FieldName.PHP3P, FieldName.PHP3T},
            {FieldName.PHP4L, FieldName.PHP4P, FieldName.PHP4T}
    };

    PreProcessor prepper;
    FormReader freader;
    Context context;


    public CPlanoReader(Context context, ReadListener listener) {
        this.context = context;
        this.prepper = new PreProcessor(context, null);
        this.freader = new FormReader(context, listener);

    }

    /**
     * Perform alignment process on a C.Plano-PWP  form photo.
     * May return null if the algorithm fail to perform the alignment process.
     *
     * @param  photoUri  an Android Uri that represents the location of the original C.Plano-PWP photo file
     * @param  template  C.Plano template specification
     * @return      the aligned photo of the C.Plano-PWP  form
     */
    public Bitmap align(Uri photoUri, FormTemplate template) {
        Bitmap photo = loadBitmapFromUri(photoUri);

        return prepper.alignForm(photo, template);
    }
    /**
     * Perform alignment process on a C.Plano-PWP  form photo.
     * May return null if the algorithm fail to perform the alignment process.
     *
     * @param  photo  Bitmap of C.Plano-PWP photo file
     * @param  template  C.Plano template specification
     * @return      the aligned photo of the C.Plano-PWP  form
     */
    public Bitmap align(Bitmap photo, FormTemplate template) {
        return prepper.alignForm(photo, template);
    }

    /**
     * Crop the aligned C.Plano-PWP form photo into a set of cells. Each cell represents
     * a single written digit/letter.
     *
     * @param  aligned  a Bitmap object containing aligned photo of C.Plano-PWP form
     * @param  template  C.Plano template specification
     * @return      an object containing a collection of cropped form cells
     */
    public CroppedForm crop(Bitmap aligned, FormTemplate template) {
        Bitmap bw = prepper.binarize(aligned, true);
        return new CroppedForm(prepper.cropForm(bw, template));
    }

    public Bitmap brighten(Bitmap image) {
        return prepper.brighten(image);
    }

    public CPlanoPage readCPlano(CroppedForm cropped, FormTemplate template,
                                 int pageNum, String alignedPhotoFile) {
        return readCPlano(cropped, template, pageNum, alignedPhotoFile, null);
    }

    public CPlanoPage readCPlano(CroppedForm cropped, FormTemplate template,
                                 int pageNum, String alignedPhotoFile, CPlano cPlano) {

        Log.i(TAG, "readCPlano " + template.name + " page=" + pageNum);
        long ts1 = System.currentTimeMillis();

        Form form = freader.readForm(template, cropped.cells);

        if (form.templateName.equals(Templates.DPPHP)) {
            Log.i(TAG, "Perform Error Check DP");
            performErrorCorrection_DPPHP(form, DP_MATRIX);
            Log.i(TAG, "Perform Error Check PHP");
            performErrorCorrection_DPPHP(form, PHP_MATRIX);
            Log.i(TAG, "Perform Error Check JSSD");
            performErrorCorrection_JSSD(form);

        } else if (form.templateName.equals(Templates.PWP3)) {
            Log.i(TAG, "Perform Error Check PWP");
            performErrorCorrection_PWP(form);

        } else if (form.templateName.equals(Templates.DPRX8) || form.templateName.equals(Templates.DPRX10)
                || form.templateName.equals(Templates.DPRX12) || form.templateName.equals(Templates.DPRX14)) {
            Log.i(TAG, "Perform Error Check DPRx");
            performErrorCorrection_DPRX(form);
        } else if (form.templateName.equals(Templates.DPX_J)) {
            Log.i(TAG, "Perform Error Check DPX_J");
            if (cPlano == null)
                performErrorCorrection_DPX_J(form);
            else
                performErrorCorrection_DPX_J(form, cPlano);

        }

        long ts2 = System.currentTimeMillis();
        Log.i(TAG, "readCPlano duration " + (ts2-ts1) + "ms");

        return new CPlanoPage(form, pageNum, alignedPhotoFile);
    }



    public float calculateSharpness(Bitmap bitmap) {
        return prepper.calculateSharpness(bitmap);
    }


    private void performErrorCorrection_DPPHP(Form form, String[][] fieldMatrix) {

        // Count rows/cols with invalid checksum
        List<Integer> invalidRows = new ArrayList<>();
        List<Integer> invalidCols = new ArrayList<>();

        for (int r=0; r < 4; r++) {
            if (form.getFieldValue(fieldMatrix[r][0]) + form.getFieldValue(fieldMatrix[r][1]) !=
                    form.getFieldValue(fieldMatrix[r][2])) {
                invalidRows.add(r);
            }
        }
        for (int c=0; c < 3; c++) {
            if (form.getFieldValue(fieldMatrix[0][c]) + form.getFieldValue(fieldMatrix[1][c]) +
                    form.getFieldValue(fieldMatrix[2][c]) != form.getFieldValue(fieldMatrix[3][c])) {
                invalidCols.add(c);
            }
        }

        // Corrections can be performed if error occurs on single column or single row.
        if (invalidCols.size() == 1) {
            int c = invalidCols.get(0);
            for (int r : invalidRows) {
                int corrected;
                if (c == 0)
                    corrected = form.getFieldValue(fieldMatrix[r][2]) - form.getFieldValue(fieldMatrix[r][1]);
                else if (c == 1)
                    corrected = form.getFieldValue(fieldMatrix[r][2]) - form.getFieldValue(fieldMatrix[r][0]);
                else
                    corrected = form.getFieldValue(fieldMatrix[r][0]) + form.getFieldValue(fieldMatrix[r][1]);

                Log.i(TAG, "Error correction " + fieldMatrix[r][c] + ": " +
                        form.getFieldValue(fieldMatrix[r][c]) +  " => " + corrected);
                form.getFieldEntry(fieldMatrix[r][c], 0).setCombinedValue(corrected);
            }

        } else if (invalidRows.size() == 1) {
            int r = invalidRows.get(0);
            for (int c : invalidCols) {
                int corrected;
                if (r == 0)
                    corrected = form.getFieldValue(fieldMatrix[3][c]) - form.getFieldValue(fieldMatrix[1][c]) - form.getFieldValue(fieldMatrix[2][c]);
                else if (r == 1)
                    corrected = form.getFieldValue(fieldMatrix[3][c]) - form.getFieldValue(fieldMatrix[0][c]) - form.getFieldValue(fieldMatrix[2][c]);
                else if (r == 2)
                    corrected = form.getFieldValue(fieldMatrix[3][c]) - form.getFieldValue(fieldMatrix[0][c]) - form.getFieldValue(fieldMatrix[1][c]);
                else
                    corrected = form.getFieldValue(fieldMatrix[0][c]) + form.getFieldValue(fieldMatrix[1][c]) + form.getFieldValue(fieldMatrix[2][c]);

                Log.i(TAG, "Error correction " + fieldMatrix[r][c] + ": " +
                        form.getFieldValue(fieldMatrix[r][c]) +  " => " + corrected);
                form.getFieldEntry(fieldMatrix[r][c], 0).setCombinedValue(corrected);
            }
        } else if (invalidCols.size() > 1 && invalidRows.size() > 1) {
            Log.w(TAG, "Multiple error, unable to correct");
        }
    }

    private void performErrorCorrection_JSSD(Form form) {

        FieldEntry fieldPSS1 = form.getFieldEntry(FieldName.PSS1);
        if (fieldPSS1.getCombinedValue() != form.getFieldValue(FieldName.PHP4T)) {
            int currValue = fieldPSS1.getCombinedValue();
            int checksum = form.getFieldValue(FieldName.PSS4) - form.getFieldValue(FieldName.PSS3)
                    - form.getFieldValue(FieldName.PSS2);

            fieldPSS1.putSubFieldValue(SubField.CHECKSUM, checksum);
            fieldPSS1.putSubFieldValue(SubField.CHECKSUM2, form.getFieldValue(FieldName.PHP4T));

            int newValue = fieldPSS1.combineValues();
            if (newValue != currValue) {
                Log.i(TAG, "Error Correction: " + FieldName.PSS1 + " " + currValue + " => " + newValue);
            }
        }
    }

    private void performErrorCorrection_PWP(Form form) {

        // Check field SAH
        FieldEntry fieldSah = form.getFieldEntry(FieldName.SAH);
        if (fieldSah.getSubFieldValue(SubField.SPELLED) != null) {
            int currValue = fieldSah.getCombinedValue();
            int sahChecksum = form.getFieldValue(FieldName.PASLON);
            fieldSah.putSubFieldValue(SubField.CHECKSUM, sahChecksum);
            int newValue = fieldSah.combineValues();
            if (newValue != currValue) {
                Log.i(TAG, "Error Correction: " + FieldName.SAH + " " + currValue + " => " + newValue);
            }
        }

        // Check field TOTAL
        FieldEntry fieldTotal = form.getFieldEntry(FieldName.TOTAL);
        if (fieldTotal.getSubFieldValue(SubField.SPELLED) != null) {
            int currValue = fieldTotal.getCombinedValue();
            int totalChecksum = form.getFieldValue(FieldName.SAH) + form.getFieldValue(FieldName.TIDAK_SAH);
            fieldTotal.putSubFieldValue(SubField.CHECKSUM, totalChecksum);
            int newValue = fieldTotal.combineValues();
            if (newValue != currValue) {
                Log.i(TAG, "Error Correction: " + FieldName.TOTAL + " " + currValue + " => " + newValue);
            }
        }
    }

    private void performErrorCorrection_DPRX(Form form) {

        // Check field TOTAL
        FieldEntry total = form.getFieldEntry(FieldName.TOTAL_PARTAI);
        if (total.getSubFieldValue(SubField.SPELLED) != null) {
            int currValue = total.getCombinedValue();
            int totalChecksum = form.getFieldValue(FieldName.CALON) + form.getFieldValue(FieldName.PARTAI);
            total.putSubFieldValue(SubField.CHECKSUM, totalChecksum);
            int newValue = total.combineValues();
            if (newValue != currValue) {
                Log.i(TAG, "Error Correction: " + FieldName.TOTAL_PARTAI + " " + currValue + " => " + newValue);
            }
        }
    }

    private void performErrorCorrection_DPX_J(Form form) {
        performErrorCorrection_DPX_J(form, null);
    }
    private void performErrorCorrection_DPX_J(Form form, CPlano cPlano) {

        // Check field SAH
        FieldEntry fieldSah = form.getFieldEntry(FieldName.SAH);
        if (fieldSah.getSubFieldValue(SubField.SPELLED) != null && cPlano != null) {
            int currValue = fieldSah.getCombinedValue();

            int scannedCandidatePages = 0;
            for (int i=1; i < cPlano.pages.length - 1; i++) {
                if (cPlano.pages[i] != null)
                    scannedCandidatePages++;
            }

            int validChecksum = 0;
            if (scannedCandidatePages == cPlano.pages.length - 2) {
                // Check valid votes
                if (cPlano.electionType == ElectionType.DPD_RI) {
                    validChecksum = cPlano.getTotalCandidateVotes();
                } else {
                    for (CPlanoPage page : cPlano.pages) {
                        if (page != null && page.verifiedData.containsKey(FieldName.TOTAL_PARTAI))
                            validChecksum += page.verifiedData.get(FieldName.TOTAL_PARTAI);
                    }
                }
            } else {
                validChecksum = form.getFieldValue(FieldName.TOTAL) - form.getFieldValue(FieldName.TIDAK_SAH);
            }

            fieldSah.putSubFieldValue(SubField.CHECKSUM, validChecksum);
            int newValue = fieldSah.combineValues();
            if (newValue != currValue) {
                Log.i(TAG, "Error Correction: " + FieldName.SAH + " " + currValue + " => " + newValue);
            }
        }

        // Check field TOTAL
        FieldEntry fieldTotal = form.getFieldEntry(FieldName.TOTAL);
        if (fieldTotal.getSubFieldValue(SubField.SPELLED) != null) {
            int currValue = fieldTotal.getCombinedValue();
            int totalChecksum = form.getFieldValue(FieldName.SAH) + form.getFieldValue(FieldName.TIDAK_SAH);
            fieldTotal.putSubFieldValue(SubField.CHECKSUM, totalChecksum);
            int newValue = fieldTotal.combineValues();
            if (newValue != currValue) {
                Log.i(TAG, "Error Correction: " + FieldName.TOTAL + " " + currValue + " => " + newValue);
            }
        }
    }


    public CPlanoInfo scanQRCode(Uri photoUri) {
        Bitmap photo = loadBitmapFromUri(photoUri);
        int tw = photo.getWidth() / 3;
        int th = photo.getHeight() / 4;
        Bitmap upperRight = Bitmap.createBitmap(photo, photo.getWidth() - tw, 0, tw, th);

        QRCodeDetector detector = new QRCodeDetector();
        Mat mat = new Mat();
        Utils.bitmapToMat(upperRight, mat);
        String qrCode = detector.detectAndDecode(mat);
        if (qrCode == null || qrCode.trim().length() == 0)
            return null;
        else {
            CPlanoInfo cpInfo = new CPlanoInfo(qrCode);
            return cpInfo;
        }
    }



    private Bitmap loadBitmapFromUri(Uri photoUri) {
        Bitmap photo = null;
        try {
            photo = MediaStore.Images.Media.getBitmap(context.getContentResolver(), photoUri);
            ExifInterface exif = new ExifInterface(context.getContentResolver().openInputStream(photoUri));
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    photo = rotate(photo, 270);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    photo = rotate(photo, 180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    photo = rotate(photo, 90);
                    break;
                default:
            }
        } catch (IOException e) {
            Log.e("CPlanoPWPReader", "Unable to load photo: " + e.getMessage(), e);
            e.printStackTrace();
        }

        return photo;
    }

    private Bitmap rotate(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }


    public class CroppedForm {
        public Map<String, List<Map<SubField, Mat[]>>> cells;
        public CroppedForm(Map<String, List<Map<SubField, Mat[]>>> cells) {
            this.cells = cells;
        }
    }

    public interface ReadListener extends FormReader.ReadListener {}
}
