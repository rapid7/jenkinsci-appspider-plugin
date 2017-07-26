package com.rapid7.jenkinspider.PostBuildScan

import lib.FormTagLib

def f = namespace(FormTagLib)

f.section(title: "AppSpider Settings") {

    f.entry(title: _("AppSpider Credentials"),
            help: descriptor.getHelpFile()) {

        f.entry(title: _("Credentials"), field: "credentialsId") {
            f.select(onChange: "updateListBox('conName', 'descriptorByName/com.rapid7.jenkinspider.PostBuildScan/fillConfigNameItems?value='+encode(this.value)); updateListBox('engName', 'descriptorByName/com.rapid7.jenkinspider.PostBuildScan/fillScanConfigEngineGroupNameItems?value='+encode(this.value));")
        }
    }

    f.entry(title: _("Scan Configuration"), field: "configName") {
        f.select(id: "conName", default: "Loading list of scan configurations...")
    }
    f.entry(title: _("Report Name"), field: "reportName") {
        f.textbox(default: "")
    }
    f.entry(title: _("Run the scan after the build finish?"), field: "enableScan") {
        f.checkbox(checked: "true")
    }
    f.entry(title: _("Obtain the report after the scan finished?"), field: "generateReport") {
        f.checkbox(checked: "true")
    }

    f.advanced(title: _("Create new scan configuration")) {
        f.section(title: _("Create new scan configuration")) {
            f.entry(title: _("Note: This will overwrite your current scan config selection"), field: "disclaimer")

            f.entry(title: _("Name"), field: "scanConfigName") {
                f.textbox(default: "")
            }
            f.entry(title: _("Url"), field: "scanConfigUrl") {
                f.textbox(default: "")
            }
            f.entry(title: _("Scan Engine Group"), field: "scanConfigEngineGroupName") {
                f.select(id: "engName", default: "Loading list of scan engine groups...")
            }
            f.validateButton(title: _("Validate"), method: "validateNewScanConfig", with: "scanConfigName,scanConfigUrl")
        }

    }
}