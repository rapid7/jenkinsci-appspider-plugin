package com.rapid7.jenkinspider;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.rapid7.appspider.*;

import static java.lang.Thread.sleep;

/**
 * Created by nbugash on 20/07/15.
 */
public class PostBuildScan extends Publisher {

    private final int SLEEPTIME = 90; //seconds
    private final String SCAN_DONE_REGEX = "Completed|Stopped";

    private final String scanConfig;
    private final String scanFilename;
    private final Boolean enableScan;
    private final Boolean generateReport;


    @DataBoundConstructor
    public PostBuildScan(String scanConfig, String scanFilename,
                         Boolean enableScan, Boolean generateReport) {
        this.scanConfig = scanConfig;
        this.scanFilename = scanFilename;
        this.enableScan = enableScan;
        this.generateReport = generateReport;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /*
    *  This will be used from the config.jelly
    * */
    public String getScanConfig() {
        return scanConfig;
    }

    public String getScanFilename() {
        return scanFilename;
    }

    public Boolean getEnableScan() {
        return enableScan;
    }

    public Boolean getReport() {
        return generateReport;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        PrintStream log = listener.getLogger();
        String ntoEntUrl = getDescriptor().getNtoEntUrl();
        String ntoEntApiKey = getDescriptor().getNtoEntApiKey();
        String ntoLogin = getDescriptor().getNtoLogin();
        String ntoPassword = getDescriptor().getNtoPassword();

        log.println("Value of NTOEnterprise Server Url: " + ntoEntUrl);
        log.println("Value of NTOEnterprise Server API Key: [FILTERED]");
        log.println("Value of NTOEnterprise Login: " + ntoLogin);
        log.println("Value of NTOEnterprise Password: [FILTERED]");
        log.println("Value of Scan Configuration name: " + scanConfig);
        log.println("Value of Scan Filename: " + scanFilename);


        // Don't perform a scan
        if (!enableScan) {
            log.println("Scan is not enabled. Continuing the build without scanning.");
            return false;
        }

        /*
        * Check if we need an authentication token
        * */
        if (ntoEntApiKey.isEmpty()) {
            // We need to get the authToken
            ntoEntApiKey = Authentication.authenticate(ntoEntUrl, ntoLogin, ntoPassword);
        }

        /*
        * (1) Execute the scan
        * (2) Obtain the response from the NTOEnterprise Server
        * */
        JSONObject scanResponse = ScanManagement.runScanByConfigName(ntoEntUrl, ntoEntApiKey, scanConfig);

        /*
        * Check if a malformed response was received from the server
        * */
        if (scanResponse.equals(null)) {
            log.println("Error: Check the JSON response from the NTOEnterprise Server");
            return false;
        }

        /*
        * Response received. Check if the request was successful.
        * */
        if (!scanResponse.getBoolean("IsSuccess")) {
            log.println("Error: Response from " + ntoEntUrl + " came back not successful");
            return false;
        }

        /*
        * If user opted out from the monitoring the scan, continue with the build process
        * */
        if (!generateReport) {
            log.println("Continuing the build without generating the report.");
            return true;
        }

        /* In a regular interval perform a check if the scan is done */
        String scanId = scanResponse.getJSONObject("Scan").getString("Id");
        String scanStatus = ScanManagement.getScanStatus(ntoEntUrl, ntoEntApiKey, scanId).getString("Status");
        log.println("Waiting for scan to complete");
        while (!scanStatus.matches(SCAN_DONE_REGEX)) {
            log.println("Still waiting for scan to complete");
            try {
                // Sleep for SLEEPTIME seconds
                TimeUnit.SECONDS.sleep(SLEEPTIME);
                ntoEntApiKey = Authentication.authenticate(ntoEntUrl, ntoLogin, ntoPassword);
                scanStatus = ScanManagement.getScanStatus(ntoEntUrl, ntoEntApiKey, scanId).getString("Status");
                log.println("Scan status is: " + scanStatus);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        /* Scan finished */
        scanResponse = ScanManagement.hasReport(ntoEntUrl, ntoEntApiKey, scanId);
        if (!scanResponse.getBoolean("Result")) {
            log.println("No reports for this scan: " + scanId);
            return false;
        }

        log.println("Scan completed!");

        FilePath filePath = build.getWorkspace();
        log.println("Generating xml report to:" + filePath.getBaseName());
        String xmlFile = ReportManagement.getVulnerabilitiesSummaryXml(ntoEntUrl, ntoEntApiKey, scanId);

        /* Saving the Report*/
        SaveToFile(filePath.getParent() + "/" + filePath.getBaseName() + "/" + scanFilename + "_" +
                new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(new Date()) + ".xml", xmlFile);
        log.println("Generating report done.");
        return true;
    }

    private static void SaveToFile(String filename, String data) {
        File file = new File(filename);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsolutePath()));
            bw.write(data);
            bw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImp getDescriptor() {
        return (DescriptorImp) super.getDescriptor();
    }

    /**
     * Descriptor for {@link PostBuildScan}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     * <p>
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension
    public static final class DescriptorImp extends Descriptor<Publisher> {

        private String ntoEntUrl;
        private String ntoEntApiKey;
        private String ntoLogin;
        private String ntoPassword;
        private String[] ntoConfigNames;

        /* Advance Jira Options */

        private String jiraRestUrl;
        private String jiraLogin;
        private String jiraPassword;

        public DescriptorImp() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the browser.
         * <p>
         * Note that returning {@link FormValidation#error(String)} does not
         * prevent the form from being saved. It just means that a message
         * will be displayed to the user.
         */
        public FormValidation doCheckNtoEntUrl(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a value");
            if (value.length() < 4)
                return FormValidation.warning("Isn't the value too short?");
            return FormValidation.ok();
        }

        /**
         * @param aClass
         * @return boolean
         */
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * @return String
         */
        @Override
        public String getDisplayName() {
            return "Publish Scan to NTOEnterprise";
        }

        /**
         * @return
         */
        public String getNtoEntUrl() {
            return ntoEntUrl;
        }

        public String getNtoEntApiKey() {
            return ntoEntApiKey;
        }

        public String getNtoLogin() {
            return ntoLogin;
        }

        public String getNtoPassword() {
            return ntoPassword;
        }

        public String[] getNtoConfigNames() {
            return ntoConfigNames;
        }

        public String getJiraRestUrl() { return jiraRestUrl; }

        public String getJiraLogin() { return jiraLogin; }

        public String getJiraPassword() { return jiraPassword; }

        @Override
        public boolean configure(StaplerRequest req, net.sf.json.JSONObject formData) throws FormException {
            this.ntoEntUrl = formData.getString("ntoEntUrl");
            this.ntoEntApiKey = formData.getString("ntoEntApiKey");
            this.ntoLogin = formData.getString("ntoLogin");
            this.ntoPassword = formData.getString("ntoPassword");
            this.jiraRestUrl = formData.getString("jiraRestUrl");
            this.jiraLogin = formData.getString("jiraLogin");
            this.jiraPassword = formData.getString("jiraPassword");
            save();
            return super.configure(req, net.sf.json.JSONObject.fromObject(formData));
        }

        /*
        * Method for populating the dropdown menu with
        * all the available scan configs
        * */
        public ListBoxModel doFillScanConfigItems() {
            ntoConfigNames = getConfigNames();
            ListBoxModel items = new ListBoxModel();
            for (int i = 0; i < ntoConfigNames.length; i++) {
                items.add(ntoConfigNames[i]);
            }
            return items;
        }
        /*
        * @param ntoRestUrl ntoLogin ntoPassword
        *
        * */
        public FormValidation doTestCredentials(@QueryParameter("ntoEntUrl") final String ntoRestUrl,
                                                @QueryParameter("ntoLogin") final String ntoLogin,
                                                @QueryParameter("ntoPassword") final String ntoPassword) {
            try {
                String authToken = Authentication.authenticate(ntoRestUrl, ntoLogin, ntoPassword);
                if (authToken.equals(null)) {
                    return FormValidation.error("Invalid username / password combination");
                } else {
                    return FormValidation.ok("Connected Successfully.");
                }
            } catch (NullPointerException e) {
                return FormValidation.error("Invalid username / password combination");
            }

        }

        /*
        * @param jiraRestUrl jiraLogin jiraPassword
        * */
        public FormValidation doTestJiraCredentials(@QueryParameter("jiraRestUrl") final String jiraRestUrl,
                                                    @QueryParameter("jiraLogin") final String jiraLogin,
                                                    @QueryParameter("jiraPassword") final String jiraPassword) {
            try {
                return FormValidation.error("Not yet implemented");
            } catch (NullPointerException e) {
                return FormValidation.error("Invalid username / password combination");
            }

        }

        /**
         * @return
         */
        private String[] getConfigNames() {
            if (ntoEntApiKey.isEmpty()) {
                this.ntoEntApiKey = Authentication.authenticate(ntoEntUrl, ntoLogin, ntoPassword);
            }
            String[] configNames = ScanConfiguration.getConfigNames(ntoEntUrl, ntoEntApiKey);
            return configNames;
        }

    }

}
