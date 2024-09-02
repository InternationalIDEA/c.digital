package com.rezapps.cdigital;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.rezapps.cplano.CPlanoPage;
import com.rezapps.cplano.ElectionType;
import com.rezapps.cplano.FieldName;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class CPlanoReaderAgent implements CPlanoReaderOld {

    public static String TAG = "CDigital";
    public static String CREADER_HOST = "http://creader.ddns.net/";
    public static String ACTION_PATH = "api";

    public static String RESULT_DATA = "Data";
    public static String SUBFIELD_NILAI_AKHIR = "NilaiAkhir";

    Context context;

    public CPlanoReaderAgent(Context context) {
        this.context = context;
    }

    @Override
    public CPlanoPage read(Bitmap photo, String fileName, ElectionType electType, int dapil, int pageNum)
            throws IOException {

        Log.i(TAG, "Read CPlano " + electType + " page=" + pageNum + ", dapil=" + dapil);

        String userAgent = "com.rezapps.CDigital ver: " + BuildConfig.VERSION_CODE;
        MultipartUploader uploader  =
                new MultipartUploader(CREADER_HOST+ACTION_PATH, userAgent);
        uploader.addFormField("sender", Util.getDeviceName());
        uploader.addFormField("pemilihan", "" + electType.ordinal());
        uploader.addFormField("dapil", "" + dapil);
        uploader.addFormField("halaman", "" + pageNum);
        uploader.addFormField("uploadTS", ""+ System.currentTimeMillis());

        byte[] photoBA = Util.preprocessPhoto(photo);
        uploader.addFilePart("cHasil", fileName, photoBA);

        Log.i(TAG,"Post data to " + uploader.url);
        uploader.finish();

        int respCode = uploader.getResponseCode();
        if (respCode != 200) {
            Log.e(TAG, "HTTP error response: " + respCode);
        }

        CPlanoPage page = fromJson(uploader.getResponseBody(), electType, pageNum, fileName);

        return page;
    }

    private CPlanoPage fromJson(String responseStr, ElectionType electType, int pageNum, String fileName)
            throws IOException {

        Log.i(TAG, "Response: " + responseStr);

        CPlanoPage page = new CPlanoPage(electType, pageNum);
        //page.photoFile = fileName;


        Gson gson = new Gson();
        JsonObject responseJson = gson.fromJson(responseStr, JsonObject.class);

        String message = responseJson.get("Message").getAsString();

        if (message != null && message.length() > 0) {
            Log.w(TAG, "Error message: " + message);
            throw new IOException(message);
        }

        JsonObject resultJson = responseJson.getAsJsonObject("Result");
        //page.alignedPhotoURL = CREADER_HOST + responseJson.get("AlignedURL").getAsString();

        JsonObject data = resultJson.getAsJsonObject(RESULT_DATA);
        for (String key : data.keySet()) {
            if (key.equals(FieldName.PASLON) || key.equals(FieldName.CALON)) {
                JsonArray votes = data.getAsJsonArray(key);
                page.votes = new int[votes.size()];
                page.verifiedVotes = new int[votes.size()];
                for (int i=0; i<votes.size(); i++) {
                    page.votes[i] = parseField(votes.get(i).getAsJsonObject());
                    page.verifiedVotes[i] = page.votes[i];
                }
            } else {
                page.data.put(key, parseField(data.getAsJsonObject(key)));
            }
        }
        page.verifiedData.putAll(page.data);
        return page;

    }

//    public void downloadImage(String url, String fileName) throws IOException {
//
//        Log.i(TAG,"Download image from " + url);
//
//        if (!fileName.endsWith(".jpg"))
//            fileName = fileName + ".jpg";
//
//        File file = Util.getPhotoFile(context, fileName);
//        InputStream in = new URL(url).openStream();
//
//        copyInputStreamToFile(in, file);
//        in.close();
//
//    }

    private static void copyInputStreamToFile(InputStream inputStream, File file)
            throws IOException {

        // append = false
        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            int read;
            byte[] bytes = new byte[8192];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }

    }

    private int parseField(JsonObject fieldJson) {
        return fieldJson.get(SUBFIELD_NILAI_AKHIR).getAsInt();


    }
}
