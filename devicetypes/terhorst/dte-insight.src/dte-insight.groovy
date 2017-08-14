/**
 *  Copyright 2015 SmartThings
 *  Copyright 2017 Jonathan Terhorst <terhorst@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 
// Includes HTTP code from the AirScape WHF device handler.
 
preferences {
        input(name: "ip", type:"string", title:"IP", description: "IP of sensor", defaultValue: "111.222.333.444" , required: true, displayDuringSetup: true)
        input(name: "v2", type:"bool", title:"Version 2", description:"Sensor version 2?", default: true, required: true, displayDuringSetup: true)
}

metadata {
	definition (name: "DTE Insight", namespace: "terhorst", author: "terhorst@gmail.com") {
		capability "Energy Meter"
		capability "Power Meter"
		capability "Refresh"
		capability "Polling"
		capability "Sensor"

		command "reset"

		fingerprint deviceId: "0x3103", inClusters: "0x32"
		fingerprint inClusters: "0x32"
	}

    attribute "power", "number"
    
	// tile definitions
	tiles {
		valueTile("power", "device.power", width: 2, height: 2) {
			state "power", label:'${currentValue} W'
		}
		standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		main (["power"])
		details(["power","refresh"])
	}
}

def installed() {
	logger.debug "dte energy: installed"
    refresh()
}

def parse(response) {
	def msg = parseLanMessage(response)
	def parts = msg.body.split(" ")
    assert parts[1] == "kW"
    def events = []
    def W = Math.round(Float.parseFloat(parts[0]) * 1000)
    logger.debug "dte energy: watts $W"
	events.add createEvent(name: "power", value: W)
    return events
}

def path() {
	return "/" + (v2 ? "zigbee/se/" : "") + "instantaneousdemand"
}

def port() {
  return v2 ? 8888 : 80;
}

def refresh() {
  setDeviceNetworkId()
  logger.debug "dte energy: refresh"
  def request = [
    method: "GET",
    path: path(),
    headers: [
   		HOST: "$ip:${port()}"
    ]
  ]
  logger.debug request
  def r = new physicalgraph.device.HubAction(request);
  sendHubCommand(r);
}

def poll() {
	logger.debug("dte energy: polled")
	refresh()
}

private setDeviceNetworkId(){
  	def iphex = convertIPtoHex(ip)
  	def porthex = convertPortToHex(port())
  	device.deviceNetworkId = "$iphex:$porthex"
  	log.debug "Device Network Id set to ${iphex}:${porthex}"
}

private String convertIPtoHex(ipAddress) {
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport
}