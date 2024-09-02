package com.rezapps.cdigital;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * This utility class provides an abstraction layer for sending multipart HTTP
 * POST requests to a web server.
 * @author www.codejava.net
 *
 */
public class MultipartUploader {
    private final String boundary;
    private static final String LINE_FEED = "\r\n";
    private HttpURLConnection httpConn;
    private String charset = "UTF-8";
    private OutputStream outputStream;
    private PrintWriter writer;
    private int responseCode;
    private String responseBody = "";
    public URL url;
    //	private UploadProgressListener progressListener;
    int progress = 0;

    /**
     * This constructor initializes a new HTTP POST request with content type
     * is set to multipart/form-data
     * @param requestURL
     * @param userAgent
     * @throws IOException
     */
    public MultipartUploader(String requestURL, String userAgent) throws IOException {

//    	this.progressListener = progressListener;

        // creates a unique boundary based on time stamp
        boundary = "----------" + System.currentTimeMillis();

        url = new URL(requestURL);
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("POST");
        httpConn.setUseCaches(false);
        httpConn.setDoOutput(true); // indicates POST method
        httpConn.setDoInput(true);
        httpConn.setRequestProperty("Content-Type",
                "multipart/form-data; boundary=" + boundary);
        httpConn.setRequestProperty("User-Agent", userAgent);
        httpConn.setRequestProperty("Test", "Bonjour");
        outputStream = httpConn.getOutputStream();
        writer = new PrintWriter(new OutputStreamWriter(outputStream, charset),
                true);
    }

    /**
     * Adds a form field to the request
     * @param name field name
     * @param value field value
     */
    public void addFormField(String name, String value) {
        writer.append("--" + boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"" + name + "\"")
                .append(LINE_FEED);
        writer.append("Content-Type: text/plain; charset=" + charset).append(
                LINE_FEED);
        writer.append(LINE_FEED);
        writer.append(value).append(LINE_FEED);
        writer.flush();
    }

    /**
     * Adds a upload file section to the request
     * @param fieldName name attribute in <input type="file" name="..." />
     * @param uploadFile a File to be uploaded
     * @throws IOException
     */
    public int addFilePart(String fieldName, String fileName, byte[] data)
            throws IOException {

        writeFilePartHeader(fieldName, fileName);
//
//        FileInputStream inputStream = new FileInputStream(uploadFile);
//        byte[] buffer = new byte[4096];
//        int bytesRead = -1;
//        while ((bytesRead = inputStream.read(buffer)) != -1) {
//            outputStream.write(buffer, 0, bytesRead);
//        }
//        outputStream.flush();
//        inputStream.close();

//        int offset = 0;
//        while (offset < data.length) {
//        	int length = 4096;
//        	if (offset + length > data.length)
//        		length = data.length - offset;
//            outputStream.write(data, offset, length);
//        	offset += 4096;
//
//            outputStream.flush();
//
//        	if (progressListener != null) {
//        		progress += length;
//        		progressListener.onProgress(progress);
//        	}
//        }


        outputStream.write(data);
        outputStream.flush();
        writer.append(LINE_FEED);
        writer.flush();

        return data.length;
    }

    public int addFilePart(String fieldName, String fileName, File file)
            throws IOException {

        writeFilePartHeader(fieldName, fileName);

        FileInputStream inputStream = new FileInputStream(file);
        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        int fileSize = 0;
        while ((bytesRead = inputStream.read(buffer, 0, buffer.length)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
            fileSize += bytesRead;

//        	if (progressListener != null) {
//        		progress += bytesRead;
//        		progressListener.onProgress(bytesRead);
//        	}

        }

        outputStream.flush();
        inputStream.close();

        writer.append(LINE_FEED);
        writer.flush();

        return fileSize;
    }

    public int addFilePart(String fieldName, String fileName, InputStream inputStream)
            throws IOException {

        writeFilePartHeader(fieldName, fileName);

        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        int fileSize = 0;
        while ((bytesRead = inputStream.read(buffer, 0, buffer.length)) != -1) {
            outputStream.write(buffer, 0, bytesRead);

            fileSize += bytesRead;

        }

        outputStream.flush();
        inputStream.close();

        writer.append(LINE_FEED);
        writer.flush();

        return fileSize;
    }

    private void writeFilePartHeader(String fieldName, String fileName) {
        writer.append("--" + boundary).append(LINE_FEED);
        writer.append(
                        "Content-Disposition: form-data; name=\"" + fieldName
                                + "\"; filename=\"" + fileName + "\"")
                .append(LINE_FEED);
        writer.append(
                        "Content-Type: "
                                + URLConnection.guessContentTypeFromName(fileName))
                .append(LINE_FEED);
        writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();
    }

    /**
     * Adds a header field to the request.
     * @param name - name of the header field
     * @param value - value of the header field
     */
    public void addHeaderField(String name, String value) {
        writer.append(name + ": " + value).append(LINE_FEED);
        writer.flush();
    }

    /**
     * Completes the request and receives response from the server.
     * @return a list of Strings as response in case the server returned
     * status OK, otherwise an exception is thrown.
     * @throws IOException
     */
    public void finish() throws IOException {

        writer.append(LINE_FEED).append("--" + boundary + "--").append(LINE_FEED);
        writer.flush();
        writer.close();

        // checks server's status code first
        responseCode = httpConn.getResponseCode();
        BufferedReader reader = null;
        if (responseCode == HttpURLConnection.HTTP_OK) {
            reader = new BufferedReader(new InputStreamReader(
                    httpConn.getInputStream()));

        } else {
            reader = new BufferedReader(new InputStreamReader(
                    httpConn.getErrorStream()));
        }
        String line = null;
        while ((line = reader.readLine()) != null) {
            responseBody += line + "\n";
        }
        reader.close();
        httpConn.disconnect();

    }

    public String getResponseBody() {
        return responseBody;
    }

    public int getResponseCode() {
        return responseCode;
    }
//
//	public interface UploadProgressListener {
//		public void onProgress(int size);
//	}


}