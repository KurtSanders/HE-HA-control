/*
Copyright 2023
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-------------------------------------------
Change history:
0.1.51 - Yves Mercier - initial version
0.1.52 - Yves Mercier - added button and health capability
0.1.59 - Yves Mercier - Change healthStatus handling
0.1.60 - Kurt Sanders - added preference logging options and duration events calculated from timeStamp event 
*/

import groovy.time.*

metadata
{
    definition(name: "Generic Component TimeStamp Sensor", namespace: "community", author: "community", importUrl: "https://raw.githubusercontent.com/ymerj/HE-HA-control/main/genericComponentTimeStampSensor.groovy")
    {
        capability "Refresh"
        capability "PushableButton"
        capability "Health Check"
    }
    preferences 
    {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        input name: "pushRequired", type: "bool", title: "Enable pushed button event at the time reported", defaultValue: true
        input ("logEnable", "bool", title: "Enable debug logging", defaultValue: true)
        input ("txtEnable", "bool", title: "Enable description text logging", defaultValue: true)
    }
    attribute "timestamp"			, "string"
    attribute "duration"			, "string"
    attribute "totalTimeLeftSecs"	, "number"
    attribute "date"				, "string"
    attribute "healthStatus"		, "enum", ["offline", "online"]
    
}

def dateDuration(argDateUTC=null) {
    if (argDateUTC==null) return 
    Date endDate
    Date startDate = new Date()
        if (logEnable) log.debug("startDate= ${startDate}")
	try {
        endDate = Date.parse("yyyy-MM-dd'T'HH:mm:ssX", argDateUTC)
        if (logEnable) log.debug("endDate=  ${endDate}")
	} catch(Exception e) {
        log.error "Error converting expetced UTC date '${argDateUTC}'from Home Assistant: '${e}'"
        return
	}
    use (TimeCategory) {
        def duration = (endDate-startDate)        
        if (logEnable) log.debug("duration= ${duration}")
        sendEvent(name: 'duration'			, value: duration)
        sendEvent(name: 'totalTimeLeftSecs'	, value: (duration.days*24*3600 + duration.hours*3600 + duration.minutes*60 + duration.seconds))        
	}
}

def logsOff(){
    log.info("debug logging disabled...")
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

void updated() {
    log.info "Updated..."
    sendEvent(name: "numberOfButtons", value: 1, displayed: false)
    log.info("debug logging is: ${logEnable == true}")
    log.info("description logging is: ${txtEnable == true}")
    unschedule()
    if (logEnable) runIn(1800,logsOff)
}

void installed() {
    log.info "Installed..."
    device.updateSetting("txtEnable",[type:"bool",value:true])
    device.updateSetting("pushRequired",[type:"bool",value:true])
    updated()
    refresh()
}

def uninstalled() {
    log.info("uninstalled...")
    unschedule()
}

void parse(String description) { log.warn "parse(String description) not implemented" }

void parse(List<Map> description) {
    description.each {
        if (it.name in ["timestamp"]) {
            if (txtEnable) log.info it.descriptionText
            sendEvent(it)
            dateDuration(it.value)
            if (pushRequired) scheduleFutureBtnPush(it.value)
        }
        if (it.name in ["duration"]) {
            if (txtEnable) log.info it.descriptionText
            sendEvent(it)
            if (pushRequired) scheduleFutureBtnPush(it.value)
        }
        if (it.name in ["healthStatus"]) {
            if (txtEnable) log.info it.descriptionText
            sendEvent(it)
        }
    }
}

def push(bn = 1) {
    sendEvent(name: "pushed", value: bn, descriptionText: "${device.label} timestamp reached", isStateChange: true)
}

def scheduleFutureBtnPush(future) {
    try {
        def activation = toDateTime(future)
        sendEvent(name: "date", value: activation)
        runOnce(activation, push, [overwrite: true])
    }
    catch(e) {
        log.error("Error: ${e}")
        sendEvent(name: "date", value: "invalid")
    }
}   
    
void refresh() {
    parent?.componentRefresh(this.device)
}

def ping() {
    refresh()
}
