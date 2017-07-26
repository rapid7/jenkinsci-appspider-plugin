package com.rapid7.jenkinspider.PostBuildScan

import lib.FormTagLib
import lib.CredentialsTagLib

def f = namespace(FormTagLib);
def c = namespace(CredentialsTagLib)

f.section(title: "AppSpider Settings") {
    f.entry(title: _("AppSpider Enterprise Rest Url"), field: "appSpiderEntUrl") {
        f.textbox(default: "")
    }

    f.entry(title: _("AppSpider Credentials"),
            help: descriptor.getHelpFile()) {

        f.entry(title: _("Credentials"), field: "credentialsId") {
            c.select()
        }

        f.block() {
            f.validateButton(
                    title: _("Test connection"),
                    progress: _("Testing..."),
                    method: "testCredentials",
                    with: "appSpiderEntUrl,credentialsId"
            )
        }
    }
}