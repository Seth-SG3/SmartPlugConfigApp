Kotlin project in android studio. Aim is to programatically configure a TASMOTA smart plug from a phone such that a wifi plug can be swapped in for a bluetooth plug that is currently used

The repo contains 2 distinct approaches, using a local only hotspot to connect to the plug, and using a mifi dongle to connect everything too.

when using the local only hotspot approach the flow is like this

connect to the plugs wifi ap
send wifi config http request (sets ssid and passowrd)
turn on hotspot and wait for plug to connect
once connected send mqtt config http request (set up mqtt so plaintext http is not required)
set up mqtt broker (the broker includes a package interceptor so no client is needed)
start a foreground service that logs the recieved telemetry package mqtt message from the plug each minute and log to csv file
service also checks hotspot is still on as it tunrs iteslf off semi regulalry
a power datapoint can be reuested via a temporary client so it can be displayed on the screen

when using the Mifi dongle approach the flow is like this...

TODO
