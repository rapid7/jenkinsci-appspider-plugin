/*
 * Copyright © 2003 - 2020 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

import java.io.IOException;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * Singleton container wrapping Configuration class which should itself be used
 * as a singleton
 * 
 * <p>
 * using a wrapper to provide additional initialization 
 * </p>
 */
class FreemarkerConfiguration {
    private static FreemarkerConfiguration instance;
    private final Configuration configuration;

    /**
     * get the singelton instance, initializing it if necessary
     */
    public static FreemarkerConfiguration getInstance() {
        if (instance == null) {
            synchronized(FreemarkerConfiguration.class) {
                instance = new FreemarkerConfiguration();
            }
        }
        return instance;
    }
    /**
     * Gets a template using name {@code template} in call to inner {@code Configuration}
     * @param template name of the template to get
     * @return Template  matching {@code template}
     * @throws IOException if call to {@code Configuration.getTemplate} throws
     */
    public Template getTemplate(String template) throws IOException {
        return configuration.getTemplate(template);
    }

    private FreemarkerConfiguration() {
        configuration = new Configuration(Configuration.VERSION_2_3_30);
        configuration.setClassForTemplateLoading(EnterpriseRestClient.class, "template");
    }
}