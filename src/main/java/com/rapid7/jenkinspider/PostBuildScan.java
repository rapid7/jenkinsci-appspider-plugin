package com.rapid7.jenkinspider;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import org.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import com.rapid7.appspider.*;

import static java.lang.Thread.sleep;

/**
 * Created by nbugash on 20/07/15.
 */
public class PostBuildScan extends Publisher {

    private final int SLEEPTIME = 120; //seconds
    private final String SCAN_DONE_REGEX = "ed"; // Status that ends with 'ed' is a finished scan

    private final String scanConfig;
    private final String scanFilename;
    private final Boolean enableScan;
    private final Boolean monitorScan;


    @DataBoundConstructor
    public PostBuildScan(String scanConfig, String scanFilename,
                         Boolean enableScan, Boolean monitorScan){
        this.scanConfig = scanConfig;
        this.scanFilename = scanFilename;
        this.enableScan = enableScan;
        this.monitorScan = monitorScan;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /*
    *  This will be used from the config.jelly
    * */
    public String getScanConfig()   { return scanConfig;   }
    public String getScanFilename() { return scanFilename; }
    public Boolean getEnableScan()  { return enableScan;   }
    public Boolean getMonitorScan() { return monitorScan;  }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        PrintStream log = listener.getLogger();
        String ntoEntUrl = getDescriptor().getNtoEntUrl();
        String ntoEntApiKey = getDescriptor().getNtoEntApiKey();
        String ntoLogin = getDescriptor().getNtoLogin();
        String ntoPassword = getDescriptor().getNtoPassword();

        log.println("Value of NTOEnterprise Server Url: " + ntoEntUrl);
        log.println("Value of NTOEnterprise Server API Key: " + ntoEntApiKey);
        log.println("Value of NTOEnterprise Login: " + ntoLogin);
        log.println("Value of NTOEnterprise Password: " + ntoPassword);
        log.println("Value of Scan Configuration name: " + scanConfig);
        log.println("Value of Scan Filename: " + scanFilename);

        // Don't perform a scan
        if (!enableScan) {
            log.println("Scan is not enabled. Continuing the build without scanning.");
            return false;
        }

        if (ntoEntApiKey.isEmpty()) {
            // We need to get the authToken
            ntoEntApiKey = Authentication.authenticate(ntoEntUrl,ntoLogin,ntoPassword);
        }
        JSONObject scanResponse = ScanManagement.runScanByConfigName(ntoEntUrl, ntoEntApiKey, scanConfig);

        if (scanResponse.equals(null)) {
            log.println("Error: Check the JSON response from the NTOEnterprise Server");
            return false;
        }

        if (!scanResponse.getBoolean("IsSuccess")){
            log.println("Error: Response from " + ntoEntUrl + " came back not successful");
            return false;
        }

        if (!monitorScan) {
            log.println("Continuing the build without monitoring.");
            return true;
        }

        /* In a regular interval perform a check if the scan is done */
        String scanId = scanResponse.getJSONObject("Scan").getString("Id");
        String scanStatus = ScanManagement.getScanStatus(ntoEntUrl,ntoEntApiKey,scanId).getString("Status");
        log.println("Waiting for scan to complete");
        while (!scanStatus.matches(SCAN_DONE_REGEX)){
            log.println("Still waiting for scan to complete");
            try {
                // Sleep for SLEEPTIME seconds
                TimeUnit.SECONDS.sleep(SLEEPTIME);
                ntoEntApiKey = Authentication.authenticate(ntoEntUrl,ntoLogin,ntoPassword);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        scanResponse = ScanManagement.hasReport(ntoEntUrl,ntoEntApiKey,scanId);
        if (!scanResponse.getBoolean("Result")){
            log.println("No reports for this scan: " + scanId);
            return false;
        }

        ReportManagement.getVulnerabilitiesSummaryXml(ntoEntUrl,ntoEntApiKey,scanId);
        return true;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImp getDescriptor() { return (DescriptorImp)super.getDescriptor(); }

    /**
     * Descriptor for {@link PostBuildScan}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension
    public static final class DescriptorImp extends Descriptor<Publisher>{

        private String ntoEntUrl;
        private String ntoEntApiKey;
        private String ntoLogin;
        private String ntoPassword;

        public DescriptorImp() { load(); }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user.
         */
        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a value");
            if (value.length() < 4)
                return FormValidation.warning("Isn't the value too short?");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        @Override
        public String getDisplayName() { return "Publish Scan to NTOEnterprise"; }

        public String getNtoEntUrl() { return ntoEntUrl; }

        public String getNtoEntApiKey() { return ntoEntApiKey; }

        public String getNtoLogin() {  return ntoLogin; }

        public String getNtoPassword() { return ntoPassword; }

        @Override
        public boolean configure(StaplerRequest req, net.sf.json.JSONObject formData) throws FormException {
            this.ntoEntUrl = formData.getString("ntoEntUrl");
            this.ntoEntApiKey = formData.getString("ntoEntApiKey");
            this.ntoLogin = formData.getString("ntoLogin");
            this.ntoPassword = formData.getString("ntoPassword");
            save();
            return super.configure(req, net.sf.json.JSONObject.fromObject(formData));
        }
    }

}
