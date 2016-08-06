# Dash Button Listener

A simple Java program for Windows designed to listen for network requests made by Amazon Dash Buttons. When a request is heard, the MAC address of the device and current time is passed to an outside server to handle the request by performing the action designated for that button.

The latest release can be found [here](https://github.com/tayl/DashButtonListener/releases)


## Libraries

The jNetPcap library (http://jnetpcap.com/), a Java wrapper for the link-layer network tool WinPcap.

Apache HttpComponents (https://hc.apache.org/), for communication between the application and remote processing server.
