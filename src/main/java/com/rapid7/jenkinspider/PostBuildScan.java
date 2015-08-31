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
import org.apache.commons.validator.UrlValidator;
import org.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
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
    private final String SUCCESSFUL_SCAN = "Completed|Stopped";
    private final String UNSUCCESSFUL_SCAN = "ReportError";
    private final String FINISHED_SCANNING = SUCCESSFUL_SCAN + "|" + UNSUCCESSFUL_SCAN;

    private String configName;  // Not set to final since it may change
                                // if user decided to create a new scan config

    private final String reportName;
    private final Boolean enableScan;
    private final Boolean generateReport;

    private String scanConfigName;
    private String scanConfigUrl;
    private String scanConfigEngineGroupName;

    @DataBoundConstructor
    public PostBuildScan(String configName, String reportName,
                         Boolean enableScan, Boolean generateReport,
                         String scanConfigName, String scanConfigUrl,
                         String scanConfigEngineGroupName ) {
        this.configName = configName;
        this.reportName = reportName;
        this.enableScan = enableScan;
        this.generateReport = generateReport;
        this.scanConfigName = scanConfigName;
        this.scanConfigUrl = scanConfigUrl;
        this.scanConfigEngineGroupName = scanConfigEngineGroupName;
    }


    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /*
    *  This will be used from the config.jelly
    * */
    public String getConfigName() {
        return configName;
    }

    public String getReportName() {
        return reportName;
    }

    public Boolean getEnableScan() {
        return enableScan;
    }

    public Boolean getReport() {
        return generateReport;
    }

    public String getScanConfigEngineGroupName() {
        return scanConfigEngineGroupName;
    }

    /**
     * @param build
     * @param launcher
     * @param listener
     * @return
     */
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        PrintStream log = listener.getLogger();
        String ntoEntUrl = getDescriptor().getNtoEntUrl();
        String ntoEntApiKey = null;
        String ntoLogin = getDescriptor().getNtoLogin();
        String ntoPassword = getDescriptor().getNtoPassword();

        log.println("Value of NTOEnterprise Server Url: " + ntoEntUrl);
        log.println("Value of NTOEnterprise Server API Key: [FILTERED]");
        log.println("Value of NTOEnterprise Login: " + ntoLogin);
        log.println("Value of NTOEnterprise Password: [FILTERED]");
        log.println("Value of Scan Configuration name: " + configName);
        log.println("Value of Scan Filename: " + reportName);

        // Don't perform a scan
        if (!enableScan) {
            log.println("Scan is not enabled. Continuing the build without scanning.");
            return false;
        }

        /*
        * Check if we need an authentication token
        * */
        if (ntoEntApiKey == null || ntoEntApiKey.isEmpty()) {
            // We need to get the authToken
            ntoEntApiKey = Authentication.authenticate(ntoEntUrl, ntoLogin, ntoPassword);
        }

        if (isANewScanConfig()) {
            log.println("Value of Scan Config Name: " + scanConfigName);
            log.println("Value of Scan Config URL: " + scanConfigUrl);
            log.println("Value of Scan Config Engine Group name: " + scanConfigEngineGroupName);

            /* Need to indicate to the user that we are going to overwrite the existing scan config */

            /* Create a new scan config */
            String engineGroupId = ScanEngineGroup.getEngineGroupIdFromName(ntoEntUrl, ntoEntApiKey, scanConfigEngineGroupName);
            ScanConfiguration.saveConfig(ntoEntUrl, ntoEntApiKey, scanConfigName, scanConfigUrl, engineGroupId);
            log.println("Successfully created the scan config " + scanConfigName);

            // Set the configName to the new created scan config
            configName = scanConfigName;
            log.println("Value of Scan Configuration name: " + configName);

            /* Reset scanConfigName and scanConfigUrl */
            scanConfigName = null;
            scanConfigUrl = null;
        }

        /*
        * (1) Execute the scan
        * (2) Obtain the response from the NTOEnterprise Server
        * */
        JSONObject scanResponse = ScanManagement.runScanByConfigName(ntoEntUrl, ntoEntApiKey, configName);

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
        String scan_status = ScanManagement.getScanStatus(ntoEntUrl,ntoEntApiKey,scanId);
        while(!scan_status.matches(FINISHED_SCANNING)) {
            log.println("Waiting for scan to finish");
            try {
                TimeUnit.SECONDS.sleep(SLEEPTIME);
                ntoEntApiKey = Authentication.authenticate(ntoEntUrl, ntoLogin, ntoPassword);
                scan_status = ScanManagement.getScanStatus(ntoEntUrl, ntoEntApiKey, scanId);
                log.println("Scan status: [" + scan_status +"]");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        /* Scan finished */
        if (!ScanManagement.hasReport(ntoEntUrl, ntoEntApiKey, scanId)) {
            log.println("No reports for this scan: " + scanId);
        }

        log.println("Finished scanning!");

        if (!(ScanManagement.getScanStatus(ntoEntUrl, ntoEntApiKey, scanId))
                .matches(SUCCESSFUL_SCAN)) {
            log.println("Scan was complete but was not successful. Status was '" +
                    ScanManagement.getScanStatus(ntoEntUrl, ntoEntApiKey, scanId) +
                    "'");
            return true;
        }

        /* Scan completed with either a 'Complete' or 'Stopped' status */
        FilePath filePath = build.getWorkspace();
        log.println("Generating xml report to:" + filePath.getBaseName());
        String xmlFile = ReportManagement.getVulnerabilitiesSummaryXml(ntoEntUrl, ntoEntApiKey, scanId);

        /* Saving the Report*/
        SaveToFile(filePath.getParent() + "/" + filePath.getBaseName() + "/" + reportName + "_" +
                new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(new Date()) + ".xml", xmlFile);
        log.println("Generating report done.");

        return true;
    }

    /**
     *
     * @param filename
     * @param data
     */
    private void SaveToFile(String filename, String data) {
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

    private boolean isANewScanConfig() {
        return (!(scanConfigName == null || scanConfigName.isEmpty()) &&
                !(scanConfigUrl  == null || scanConfigUrl.isEmpty()));
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
        private String[] scanConfigNames;
        private String[] scanConfigEngines;

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

        public String getNtoEntUrl() { return ntoEntUrl; }

        public String getNtoLogin() { return ntoLogin; }

        public String getNtoPassword() { return ntoPassword; }

        public String[] getScanConfigNames() { return scanConfigNames; }

        public String[] getScanConfigEngines() { return scanConfigEngines; }

        public String getJiraRestUrl() { return jiraRestUrl; }

        public String getJiraLogin() { return jiraLogin; }

        public String getJiraPassword() { return jiraPassword; }

        @Override
        public boolean configure(StaplerRequest req, net.sf.json.JSONObject formData) throws FormException {
            this.ntoEntUrl = formData.getString("ntoEntUrl");
            this.ntoLogin = formData.getString("ntoLogin");
            this.ntoPassword = formData.getString("ntoPassword");
            this.ntoEntApiKey = null;
            this.jiraRestUrl = formData.getString("jiraRestUrl");
            this.jiraLogin = formData.getString("jiraLogin");
            this.jiraPassword = formData.getString("jiraPassword");
            save();
            return super.configure(req, net.sf.json.JSONObject.fromObject(formData));
        }

        /**
         * Method for populating the dropdown menu with
         * all the available scan configs
         * @return
         */
        public ListBoxModel doFillConfigNameItems() {
            scanConfigNames = getConfigNames();
            ListBoxModel items = new ListBoxModel();
            items.add("[Select a scan config name]"); // Adding a default "Pick a scan configuration" entry
            for (int i = 0; i < scanConfigNames.length; i++) {
                items.add(scanConfigNames[i]);
            }
            return items;
        }

        /**
         * Method for populating the dropdown menu with
         * all the available scan engine groups
         * @return
         */
        public ListBoxModel doFillScanConfigEngineGroupNameItems() {
            scanConfigEngines = getEngineGroups();
            ListBoxModel items = new ListBoxModel();
            items.add("[Select an engine group name]"); // Adding a default "Pick a engine group name" entry
            for (int i = 0; i < scanConfigEngines.length; i++ ) {
                items.add(scanConfigEngines[i]);
            }
            return items;
        }

        /**
         * @param ntoRestUrl
         * @param ntoLogin
         * @param ntoPassword
         * @return
         */
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

        /**
         *
         * @param jiraRestUrl
         * @param jiraLogin
         * @param jiraPassword
         * @return
         */
        public FormValidation doTestJiraCredentials(@QueryParameter("jiraRestUrl") final String jiraRestUrl,
                                                    @QueryParameter("jiraLogin") final String jiraLogin,
                                                    @QueryParameter("jiraPassword") final String jiraPassword ) {
            try {
                return FormValidation.error("Not yet implemented");
            } catch (NullPointerException e) {
                return FormValidation.error("Invalid username / password combination");
            }

        }

        public FormValidation doTestUrl(@QueryParameter("scanConfigUrl") final String scanConfigUrl) {
            try {
                if (new UrlValidator().isValid(scanConfigUrl)) {
                    URL url = new URL(scanConfigUrl);
                    URLConnection conn = url.openConnection();
                    conn.connect();
                    return FormValidation.ok("'" + scanConfigUrl + "' is a valid url");
                } else {
                    return FormValidation.error("Invalid url. Check the protocol. For example: 'http://"+scanConfigUrl+"'");
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return FormValidation.error("Unable to connect to \"" + scanConfigUrl +"\". Try again in a few mins or " +
                        "try another url");
            } catch (IOException e) {
                e.printStackTrace();
                return FormValidation.error("Unable to connect to \"" + scanConfigUrl +"\". Try again in a few mins or " +
                        "try another url");
            }
        }

        /**
         * @return
         */
        private String[] getConfigNames() {
            if (ntoEntApiKey == null || ntoEntApiKey.isEmpty()) {
                this.ntoEntApiKey = Authentication.authenticate(ntoEntUrl, ntoLogin, ntoPassword);
            }
            return ScanConfiguration.getConfigNames(ntoEntUrl, ntoEntApiKey);
        }

        /**
         * @return
         */
        private String[] getEngineGroups() {
            if (ntoEntApiKey == null || ntoEntApiKey.isEmpty()) {
                this.ntoEntApiKey = Authentication.authenticate(ntoEntUrl, ntoLogin, ntoPassword);
            }
            return ScanEngineGroup.getEngineNamesGroupsForClient(ntoEntUrl,ntoEntApiKey);
        }

    }

}
