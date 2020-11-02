package com.rapid7.jenkinspider;

import com.rapid7.appspider.*;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.impl.client.CloseableHttpClient;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Created by nbugash on 20/07/15.
 */
public class PostBuildScan extends Notifier {

    private final String configName;  // Not set to final since it may change
                                // if user decided to create a new scan config

    private final String reportName;
    private final boolean enableScan;
    private final boolean generateReport;

    private final String scanConfigName;
    private final String scanConfigUrl;
    private final String scanConfigEngineGroupName;

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
     * {@inheritDoc}
     * @return boolean representing success or failure of the action to perform
     * @throws InterruptedException if stop is requested by the user
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException {

        LoggerFacade log = new PrintStreamLoggerFacade(listener.getLogger());

        // Don't perform a scan
        if (!enableScan) {
            log.println("Scan is not enabled. Continuing the build without scanning.");
            return false;
        }

        String appSpiderEntUrl = getDescriptor().getAppSpiderEntUrl();
        log.println("Value of AppSpider Enterprise Server Url: " + appSpiderEntUrl);

        String appSpiderUsername = getDescriptor().getAppSpiderUsername();
        log.println("Value of AppSpider Username: " + appSpiderUsername);

        Secret secretAppSpiderPassword = getDescriptor().getAppSpiderPassword();
        String appSpiderPassword = !Objects.isNull(secretAppSpiderPassword)
            ? Secret.toString(secretAppSpiderPassword)
            : "";
        log.println("Value of Scan Configuration name: " + configName);

        boolean allowSelfSignedCertificate = getDescriptor().getAppSpiderAllowSelfSignedCertificate();
        log.println("Value of Allow Self-Signed certificate : " + allowSelfSignedCertificate);

        try {
            ContentHelper contentHelper = new ContentHelper(log);
            StandardEnterpriseClient client = new StandardEnterpriseClient(
                new HttpClientService(new HttpClientFactory(allowSelfSignedCertificate).getClient(), contentHelper, log),
                appSpiderEntUrl,
                new ApiSerializer(log),
                contentHelper,
                log);
            ScanSettings settings = new ScanSettings(configName, reportName, true, generateReport, scanConfigName, scanConfigUrl, scanConfigEngineGroupName);

            Scan scan = new Scan(client, settings, log);
            if (!scan.process(appSpiderUsername, appSpiderPassword))
                return false;

            FilePath filePath = build.getWorkspace();
            if (Objects.isNull(filePath)) {
                log.println("workspace not found, unable to save results");
                return false;
            }
            String scanId = scan.getId().orElse("");
            if (scanId.isEmpty()) {
                log.println("Unexepcted error, scan identifier not found, unable to save retrieve report");
                return false;
            }

            return new Report(client, settings, log)
                .saveReport(appSpiderUsername, appSpiderPassword, scanId, filePath);

        } catch (IllegalArgumentException e) {
            log.println(e.toString());
            return false;
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
     * <p>
     * Descriptor for {@link PostBuildScan}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension
    public static final class DescriptorImp extends BuildStepDescriptor<Publisher> {

        private String appSpiderEntUrl;
        private String appSpiderUsername;
        private Secret appSpiderPassword;
        private boolean appSpiderAllowSelfSignedCertificate;
        private String[] scanConfigNames;
        private String[] scanConfigEngines;

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
        public FormValidation doCheckappSpiderEntUrl(@QueryParameter String value) {
            if (value.length() == 0)
                return FormValidation.error("Please set a value");
            if (value.length() < 4)
                return FormValidation.warning("Isn't the value too short?");
            return FormValidation.ok();
        }

        /**
         * {@inheritDoc}
         */
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * @return Display Name of the plugin
         */
        @Override
        public String getDisplayName() {
            return "Scan build using AppSpider";
        }

        public String getAppSpiderEntUrl() {
            return appSpiderEntUrl;
        }
        public void setAppSpiderEntUrl(String appSpiderEntUrl) {
            this.appSpiderEntUrl = appSpiderEntUrl;
        }

        public String getAppSpiderUsername() {
            return appSpiderUsername;
        }
        public void setAppSpiderUsername(String appSpiderUsername) {
            this.appSpiderUsername = appSpiderUsername;
        }

        public Secret getAppSpiderPassword() {
            return appSpiderPassword;
        }
        public void setAppSpiderPassword(Secret appSpiderPassword) {
            this.appSpiderPassword = appSpiderPassword;
        }

        public boolean getAppSpiderAllowSelfSignedCertificate() {
            return appSpiderAllowSelfSignedCertificate;
        }
        public void setAppSpiderAllowSelfSignedCertificate(boolean appSpiderAllowSelfSignedCertificate) {
            this.appSpiderAllowSelfSignedCertificate = appSpiderAllowSelfSignedCertificate;
        }

        public String[] getScanConfigNames() {
            return scanConfigNames.clone();
        }

        public String[] getScanConfigEngines() {
            return scanConfigEngines.clone();
        }

        private StandardEnterpriseClient buildEnterpriseClient(CloseableHttpClient httpClient, String endpoint) {
            LoggerFacade logger = new LoggerFacade() {
                final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("appspider-plugin");
                @Override
                public void println(String message) {
                    logger.log(Level.INFO, message);
                }
                @Override
                public void info(String message) {
                    logger.log(Level.INFO, message);
                }
                @Override
                public void warn(String message) {
                    logger.log(Level.WARNING, message);
                }
                @Override
                public void severe(String message) {
                    logger.log(Level.SEVERE, message);
                }
            };

            ContentHelper contentHelper = new ContentHelper(logger);
            return new StandardEnterpriseClient(
                    new HttpClientService(httpClient, contentHelper, logger),
                    endpoint,
                    new ApiSerializer(logger),
                    contentHelper,
                    logger);
        }

        @Override
        public boolean configure(StaplerRequest req, net.sf.json.JSONObject formData) throws FormException {
            req.bindJSON(this, formData);

            save();
            return super.configure(req, net.sf.json.JSONObject.fromObject(formData));
        }

        /**
         * Method for populating the dropdown menu with
         * all the available scan configs
         * @return ListBoxModel containing the scan config names
         */
        public ListBoxModel doFillConfigNameItems() {
            scanConfigNames = getConfigNames();
            return buildListBoxModel("[Select a scan config name]", scanConfigNames);
        }

        /**
         * Method for populating the dropdown menu with
         * all the available scan engine groups
         * @return ListBoxModel containing engine details
         */
        public ListBoxModel doFillScanConfigEngineGroupNameItems() {
            scanConfigEngines = getEngineGroups();
            return buildListBoxModel("[Select an engine group name]", scanConfigEngines);
        }

        private static ListBoxModel buildListBoxModel(String introduction, String[] items) {
            ListBoxModel model = new ListBoxModel();
            model.add(introduction); // Adding a default "Pick a scan configuration" entry
            model.addAll(Arrays.stream(items).map(ListBoxModel.Option::new).collect(Collectors.toList()));
            return model;
        }


        /**
         * calls the login endpoint with the provided credentials reporting success/failure back to the user via form validation
         * @param allowSelfSignedCertificate If true certificate errors will be ignored, only meaningful if URL is using https
         * @param appSpiderEntUrl Full URL path including protocol to the appspider rest api endpoint
         * @param username Username used for authentication
         * @param password Password used for authentication
         * @return FormValidation result of the credentials test
         */
        public FormValidation doTestCredentials(@QueryParameter("appSpiderAllowSelfSignedCertificate") final boolean allowSelfSignedCertificate,
                                                @QueryParameter("appSpiderEntUrl") final String appSpiderEntUrl,
                                                @QueryParameter("appSpiderUsername") final String username,
                                                @QueryParameter("appSpiderPassword") final String password) {
            return executeRequest(appSpiderEntUrl, allowSelfSignedCertificate, client -> {
                try {
                    if (!client.testAuthentication(username, password)) {
                        return FormValidation.error("Invalid username / password combination");
                    } else {
                        return FormValidation.ok("Connected Successfully.");
                    }

                } catch (IllegalArgumentException e) {
                    return FormValidation.error("Invalid username / password combination");
                }
            }, FormValidation.error("Invalid username / password combination"));
        }

        public FormValidation doValidateNewScanConfig(@QueryParameter("scanConfigName") final String scanConfigName,
                                                      @QueryParameter("scanConfigUrl") final String scanConfigUrl) {
            try {
                final String ALPHANUMERIC_REGEX = "^[a-zA-Z0_\\-\\.]*$";
                if (!scanConfigName.matches(ALPHANUMERIC_REGEX) ||
                        scanConfigName.contains(" ") ||
                        scanConfigName.isEmpty()) {
                    return FormValidation.error("Invalid Scan configuration name. " +
                            "Only alpha-numeric, '.' , '_' , and '-' are allowed");
                }

                if (UrlValidator.getInstance().isValid(scanConfigUrl)) {
                    URL url = new URL(scanConfigUrl);
                    URLConnection conn = url.openConnection();
                    conn.connect();
                } else {
                    return FormValidation.error("Invalid url. Check the protocol (i.e http/https) or the port.");
                }
                return FormValidation.ok("Valid scan configuration name and url.");
            } catch (IOException /* | MalformedURLException */ e) {
                e.printStackTrace();
                return FormValidation.error("Unable to connect to \"" + scanConfigUrl +"\". Try again in a few mins or " +
                        "try another url");
            }
        }


        @FunctionalInterface
        interface AuthorizedRequest<T> {
            T executeRequest(EnterpriseClient client, String authKey);
        }

        /**
         * @return array of scan config names returned from AppSpider Enterprise
         */
        private String[] getConfigNames() {
            return executeRequestWithAuthorization((client, authKey) ->
                client.getConfigNames(authKey).orElse(new String[0]),
                new String[0]);
        }

        /**
         * @return array of Strings representing the engine group names
         */
        private String[] getEngineGroups() {
            return executeRequestWithAuthorization((client, authKey) ->
                client.getEngineGroupNamesForClient(authKey).orElse(new String[0]),
                new String[0]);
        }

        private <T> T executeRequest(String endpoint, boolean appSpiderAllowSelfSignedCertificate, Function<EnterpriseClient, T> supplier, T errorResult) {
            if (Objects.isNull(supplier))
                return errorResult;

            try (CloseableHttpClient httpClient = new HttpClientFactory(appSpiderAllowSelfSignedCertificate).getClient()) {
                EnterpriseClient client = buildEnterpriseClient(httpClient, endpoint);
                return supplier.apply(client);
            } catch (IOException e) {
                e.printStackTrace();
                return errorResult;
            }
        }

        private <T> T executeRequestWithAuthorization(AuthorizedRequest<T> request, T errorResult) {
            if (Objects.isNull(request))
                return errorResult;

            try (CloseableHttpClient httpClient = new HttpClientFactory(appSpiderAllowSelfSignedCertificate).getClient()) {
                EnterpriseClient client = buildEnterpriseClient(httpClient, appSpiderEntUrl);
                if (Objects.isNull(appSpiderPassword)) {
                    return errorResult;
                }

                Optional<String> maybeAuthKey = client.login(appSpiderUsername, Secret.toString(appSpiderPassword));
                if (!maybeAuthKey.isPresent()) {
                    FormValidation.error("Unauthorized");
                    return errorResult;
                }
                return request.executeRequest(client, maybeAuthKey.get());
            } catch (IOException e) {
                e.printStackTrace();
                return errorResult;
            }
        }
    }

}
