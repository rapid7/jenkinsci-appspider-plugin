package com.rapid7.appspider;
import java.util.HashMap;
import java.util.Map;
import java.io.InputStream; 

/**
 * Created by nbugash on 09/07/15.
 */
public class ReportManagement extends Base {

    public final static String IMPORTSTANDARDREPORT = "/Report/ImportStandardReport";
    public final static String IMPORTCHECKMARKREPORT = "/Report/ImportCheckmarkReport";
    public final static String GETREPORTALLFILES = "/Report/GetReportAllFiles";
    public final static String GETVULNERABILITIESSUMMARY = "/Report/GetVulnerabilitiesSummaryXml";
    public final static String GETCRAWLEDLINKS = "/Report/GetCrawledLinksXml";
    public final static String GETREPORTZIP = "/Report/GetReportZip";

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
    }

    /**
     * @param restUrl
     * @param authToken
     * @param scanId
     * @return byte array representing zipped report, ideally this should stream to a file rather than
     *         one big array this is just an initial attempt
     */
    public static InputStream getReportZip(String restUrl, String authToken, String scanId) {
        String apiCall = restUrl + GETREPORTZIP;
        Map<String, String> params = new HashMap<String, String>();
        params.put("scanId", scanId);

        return getInputStreamReader(apiCall, authToken, params);
    }
}