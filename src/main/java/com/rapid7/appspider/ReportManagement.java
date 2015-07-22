package com.rapid7.appspider;

import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import javax.swing.text.html.parser.Entity;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nbugash on 09/07/15.
 */
public class ReportManagement extends Base {

    public final static String IMPORTSTANDARDREPORT = "/Report/ImportStandardReport";
    public final static String IMPORTCHECKMARKREPORT = "/Report/ImportCheckmarkReport";
    public final static String GETREPORTALLFILES = "/Report/GetReportAllFiles";
    public final static String GETVULNERABILITIESSUMMARY = "/Report/GetVulnerabilitiesSummaryXml";
    public final static String GETCRAWLEDLINKS = "/Report/GetCrawledLinksXml";

    /**
     * @param restUrl
     * @param authToken
     * @param scanId
     * @return
     */
    public static String getVulnerabilitiesSummaryXml(String restUrl, String authToken, String scanId) {
        String apiCall = restUrl + GETVULNERABILITIESSUMMARY;
        Map<String, String> params = new HashMap<String, String>();
        params.put("scanId", scanId);
        String response = (String) get(apiCall, authToken, params);
        return response;
//        return SaveToFile(scanId + (new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date())) + ".xml", response);
    }

    private static File SaveToFile(String filename, String data) {
        File file = new File(filename);
        try {
            if (!file.exists()) file.createNewFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
            bw.write(data);
            bw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
}