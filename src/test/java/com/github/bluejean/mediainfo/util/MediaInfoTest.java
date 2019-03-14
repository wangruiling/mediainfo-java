package com.github.bluejean.mediainfo.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class MediaInfoTest {
    @BeforeAll
    static void initAll() {
        System.setProperty("jna.debug_load", "true");
    }

    @Test
    public void simple() {
        System.out.println("开始执行...");
        MediaInfo MI = new MediaInfo();

        String FileName = "D:/tmp/video/4K/bjm05960001.mov";
        if (MI.open(FileName)>0){
            System.out.println("文件打开成功");
            //获得视频的宽和高
            String width = MI.get(MediaInfo.StreamKind.Video, 0, "Width");
            String height = MI.get(MediaInfo.StreamKind.Video, 0, "Height");

            System.out.println(MI.inform());
            System.out.println(width + " " + height);
        }
        MI.close();
        System.out.println("执行成功!");
    }

    @Test
    public void simple2() {
        System.out.println("开始执行...");
        MediaInfo MI = new MediaInfo();

        String FileName = "D:/tmp/video/4K/bjm05960001.mov";
        if (MI.open(FileName)>0){
            System.out.println("文件打开成功");
            //获得视频的宽和高
            String width = MI.get(MediaInfo.StreamKind.Video, 0, "Width");
            String height = MI.get(MediaInfo.StreamKind.Video, 0, "Height");
            //帧率(单位：帧/秒)
            String frameRate = MI.get(MediaInfo.StreamKind.Video, 0, "FrameRate");
            //时长(单位：毫秒)：26360
            String duration = MI.get(MediaInfo.StreamKind.General, 0, "Duration");
            //文件大小(单位：Byte)
            String fileSize = MI.get(MediaInfo.StreamKind.General, 0, "FileSize");

            System.out.println(MI.inform());
            System.out.println(width + " " + height);
            System.out.println("帧率:" + frameRate);
            System.out.println("时长:" + duration);
            System.out.println("文件大小:" + fileSize);
        }
        MI.close();
        System.out.println("执行成功!");
    }

    @Test
    public void info() {
        System.out.println("开始执行...");
        MediaInfo MI = new MediaInfo();

        String FileName = "D:/tmp/video/4K/bjm05960001.mov";
        if (MI.open(FileName)>0){
            System.out.println("文件打开成功");
//            String m = MI.get(MediaInfo.StreamKind.General, 0, "OverallBitRate");
//            System.out.println("综合码率(音视频结合码率)："+m);
            MI.option("Complete", "1");
            System.out.println("文件所有详细信息："+ MI.inform());
        }
        MI.close();
        System.out.println("执行成功!");
    }

    @Test
    public void HowToUse_Dll() throws Exception {
        String FileName = "D:/tmp/video/4K/bjm05960001.mov";

        //Comment this line and uncomment the next one if you would like to test the "by buffer" interface
        //if (true) {
        if (false) {
            if (FileName.startsWith("http://") || FileName.startsWith("https://")) {
                ByBuffer_URL(FileName);
            } else {
                ByBuffer_LocalFile(FileName);
            }
        } else {
            ByFileName(FileName);
        }
    }

    private void ByFileName(String FileName) throws Exception {
        String To_Display = "";

        //Info about the library

        To_Display += MediaInfo.optionStatic("Info_Version");

        To_Display += "\r\n\r\nInfo_Parameters\r\n";
        To_Display += MediaInfo.optionStatic("Info_Parameters");

        To_Display += "\r\n\r\nInfo_Capacities\r\n";
        To_Display += MediaInfo.optionStatic("Info_Capacities");

        To_Display += "\r\n\r\nInfo_Codecs\r\n";
        To_Display += MediaInfo.optionStatic("Info_Codecs");

        //An example of how to use the library

        MediaInfo MI = new MediaInfo();

        To_Display += "\r\n\r\nOpen\r\n";
        if (MI.open(FileName) > 0)
            To_Display += "is OK\r\n";
        else
            To_Display += "has a problem\r\n";

        To_Display += "\r\n\r\ninform with Complete=false\r\n";
        MI.option("Complete", "");
        To_Display += MI.inform();

        To_Display += "\r\n\r\ninform with Complete=true\r\n";
        MI.option("Complete", "1");
        To_Display += MI.inform();

        To_Display += "\r\n\r\nCustom inform\r\n";
        MI.option("inform", "General;Example : FileSize=%FileSize%");
        To_Display += MI.inform();

        To_Display += "\r\n\r\nGetI with Stream=General and Parameter=2\r\n";
        To_Display += MI.get(MediaInfo.StreamKind.General, 0, 2, MediaInfo.InfoKind.Text);

        To_Display += "\r\n\r\ncountGet with StreamKind=Stream_Audio\r\n";
        To_Display += MI.countGet(MediaInfo.StreamKind.Audio, -1);

        To_Display += "\r\n\r\nget with Stream=General and Parameter=\"AudioCount\"\r\n";
        To_Display += MI.get(MediaInfo.StreamKind.General, 0, "AudioCount", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);

        To_Display += "\r\n\r\nget with Stream=Audio and Parameter=\"StreamCount\"\r\n";
        To_Display += MI.get(MediaInfo.StreamKind.Audio, 0, "StreamCount", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);

        To_Display += "\r\n\r\nget with Stream=General and Parameter=\"FileSize\"\r\n";
        To_Display += MI.get(MediaInfo.StreamKind.General, 0, "FileSize", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);

        To_Display += "\r\n\r\nClose\r\n";
        MI.close();

        System.out.println(To_Display);
    }

    private void ByBuffer_LocalFile(String FileName) throws Exception {
        FileName = FileName.replace(" ", "%20");

        //Initializing MediaInfo
        MediaInfo MI = new MediaInfo();

        //From: preparing an example file for reading
        RandomAccessFile From = new RandomAccessFile(FileName, "r"); //From file

        //From: preparing a memory buffer for reading
        byte[] From_Buffer = new byte[64 * 1024];
        int From_Buffer_Size; //The size of the read file buffer

        //Preparing to fill MediaInfo with a buffer
        MI.openBufferInit(From.length(), 0);

        //The parsing loop
        do {
            //Reading data somewhere, do what you want for this.
            From_Buffer_Size = From.read(From_Buffer);

            //Sending the buffer to MediaInfo
            int Result = MI.openBufferContinue(From_Buffer, From_Buffer_Size);
            if ((Result & 8) == 8) // Status.Finalized
                break;

            //Testing if MediaInfo request to go elsewhere
            if (MI.openBufferContinueGotoGet() != -1) {
                long newPos = MI.openBufferContinueGotoGet();
                From.seek(newPos); //Position the file
                MI.openBufferInit(From.length(), newPos); //Informing MediaInfo we have seek
            }
        }
        while (From_Buffer_Size > 0);

        //Finalizing
        MI.openBufferFinalize(); //This is the end of the stream, MediaInfo must finnish some work

        //get() example
        String To_Display = new String();
        To_Display += "get with Stream=General and Parameter=\"Format\": ";
        To_Display += MI.get(MediaInfo.StreamKind.General, 0, "Format", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
        To_Display += "\r\n";
        To_Display += "get with Stream=Video and Parameter=\"Format_Settings_GOP\": ";
        To_Display += MI.get(MediaInfo.StreamKind.Video, 0, "Format_Settings_GOP", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
        System.out.println(To_Display);
    }

    private void ByBuffer_URL(String FileName) throws Exception {
        String Amazon_S3_Date = new String(); // Amazon only
        String Amazon_S3_Authorization = new String(); // Amazon only

        // Parsing user input
        FileName = FileName.replace(" ", "%20");
        int FileName_Amazon_S3_End = FileName.indexOf("@s3.amazonaws.com/"); //Testing if Amazon is used and a AccessKeyId/SecretAccessKey is provided, TODO: use a less ugly method
        if (FileName_Amazon_S3_End != -1) {
            // Amazon only, computing the Authorization value
            // See http://docs.aws.amazon.com/AmazonS3/latest/dev/RESTAuthentication.html
            int FileName_Amazon_S3_Begin = FileName.indexOf("://") + 3;
            int FileName_Amazon_S3_Middle = FileName.indexOf(":", FileName_Amazon_S3_Begin);
            String Amazon_S3_AWSAccessKeyId = FileName.substring(FileName_Amazon_S3_Begin, FileName_Amazon_S3_Middle);
            String Amazon_S3_AWSSecretAccessKey = FileName.substring(FileName_Amazon_S3_Middle + 1, FileName_Amazon_S3_End);
            FileName = FileName.replace(FileName.substring(FileName_Amazon_S3_Begin, FileName_Amazon_S3_End + 1), new String());
            Amazon_S3_Date = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).format(new Date());
            String Amazon_S3_ToSign = "GET\n\n\n" + Amazon_S3_Date + "\n" + new URL(FileName).getPath();
            Amazon_S3_Authorization = "AWS " + Amazon_S3_AWSAccessKeyId + ":" + calculateHmacSHA1(Amazon_S3_ToSign, Amazon_S3_AWSSecretAccessKey);
        }
        URL FileUrl = new URL(FileName);

        //From: preparing an example file for reading
        HttpURLConnection HttpFrom = (HttpURLConnection) FileUrl.openConnection();
        if (!Amazon_S3_Authorization.isEmpty()) {
            HttpFrom.setRequestProperty("Date", Amazon_S3_Date); // Amazon only
            HttpFrom.setRequestProperty("Authorization", Amazon_S3_Authorization); // Amazon only
        }
        HttpFrom.connect();
        if (HttpFrom.getResponseCode() != 200) {
            java.util.Scanner S = new java.util.Scanner(HttpFrom.getErrorStream());
            S.useDelimiter("\\Z");
            System.out.println(S.next());
            return;
        }
        InputStream From = HttpFrom.getInputStream();
        long From_Length = Long.parseLong(HttpFrom.getHeaderField("Content-Length")); //.getContentLengthLong(), for 2GB+ file support is available only in Java 7 and lot of distros have Java 6

        //From: preparing a memory buffer for reading
        byte[] From_Buffer = new byte[64 * 1024];
        int From_Buffer_Size; //The size of the read file buffer

        //Initializing MediaInfo
        MediaInfo MI = new MediaInfo();

        //Preparing to fill MediaInfo with a buffer
        MI.openBufferInit(From_Length, 0);

        //The parsing loop
        do {
            //Reading data somewhere, do what you want for this.
            From_Buffer_Size = From.read(From_Buffer);

            //Sending the buffer to MediaInfo
            int Result = MI.openBufferContinue(From_Buffer, From_Buffer_Size);
            if ((Result & 8) == 8) // Status.Finalized
                break;

            //Testing if MediaInfo request to go elsewhere
            if (MI.openBufferContinueGotoGet() != -1) {
                long newPos = MI.openBufferContinueGotoGet();
                From.close();
                HttpFrom.disconnect();
                HttpFrom = (HttpURLConnection) FileUrl.openConnection();
                if (!Amazon_S3_Authorization.isEmpty()) {
                    HttpFrom.setRequestProperty("Date", Amazon_S3_Date); // Amazon only
                    HttpFrom.setRequestProperty("Authorization", Amazon_S3_Authorization); // Amazon only
                }
                HttpFrom.setRequestProperty("Range", "bytes=" + newPos + "-");
                HttpFrom.connect();
                if (HttpFrom.getResponseCode() != 206) {
                    java.util.Scanner S = new java.util.Scanner(HttpFrom.getErrorStream());
                    S.useDelimiter("\\Z");
                    System.out.println(S.next());
                    return;
                }
                From = HttpFrom.getInputStream();
                MI.openBufferInit(From_Length, newPos); //Informing MediaInfo we have seek, TODO: take newPos from HTTP Content-Range value
            }
        }
        while (From_Buffer_Size > 0);

        //Finalizing
        MI.openBufferFinalize(); //This is the end of the stream, MediaInfo must finish some work

        //get() example
        String To_Display = new String();
        To_Display += "get with Stream=General and Parameter=\"Format\": ";
        To_Display += MI.get(MediaInfo.StreamKind.General, 0, "Format", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
        To_Display += "\r\n";
        To_Display += "get with Stream=Video and Parameter=\"Format_Settings_GOP\": ";
        To_Display += MI.get(MediaInfo.StreamKind.Video, 0, "Format_Settings_GOP", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
        System.out.println(To_Display);
    }

    /**
     * Computes HMAC signature.
     *
     * @param data      The data to be signed.
     * @param secretKey The signing secret key.
     * @return The Base64-encoded HMAC signature.
     * @throws SignatureException when signature generation fails
     */
    private static String calculateHmacSHA1(String data, String secretKey)
            throws SignatureException {
        String Result;

        try {
            Mac mac = Mac.getInstance("HmacSHA1"); // Create an HMAC instance
            mac.init(new SecretKeySpec(secretKey.getBytes(), "HmacSHA1")); // Initialize the HMAC instance with the secret key
            Result = DatatypeConverter.printBase64Binary(mac.doFinal(data.getBytes()));
        } catch (Exception e) {
            throw new SignatureException(e.getMessage());
        }
        return Result;
    }
}