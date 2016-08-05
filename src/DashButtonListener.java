/**
 * Created by Taylor on 7/15/2016.
 */

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapBpfProgram;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.packet.format.FormatUtils;
import org.jnetpcap.protocol.network.Arp;

import java.io.IOException;
import java.util.*;

class DashButtonListener {

    public static void main(String[] args) {

        // All network devices will populate this list
        List<PcapIf> networkDevices = new ArrayList<>();

        // Errors will be logged to this sb for debugging
        StringBuilder errorLog = new StringBuilder();

        // The time the most recent valid packet was received from network devices will be stored in this map to be
        // checked against and prevent duplicate requests from being made
        Map<String, Long> timeOfLastArpFromSource = new HashMap<>();

        int result = Pcap.findAllDevs(networkDevices, errorLog);
        if (result == Pcap.NOT_OK || networkDevices.isEmpty()) {
            System.err.printf("Error finding network devices:\n%s", errorLog.toString());
            return;
        }

        PcapIf device = networkDevices.get(0);

        // The important bits of ARP packets are typically available in the first 128 bytes, so truncate to that
        Pcap pcap = Pcap.openLive(device.getName(), 128, Pcap.MODE_PROMISCUOUS, 1000, errorLog);

        if (pcap == null) {
            System.err.printf("Error while opening device for capture: %s", errorLog.toString());
            return;
        }

        // Build a filter for ARP packets
        PcapBpfProgram program = new PcapBpfProgram();
        pcap.compile(program, "arp", 0, 0xFFFFFF00);
        pcap.setFilter(program);

        Arp arp = new Arp();

        PcapPacketHandler<String> jpacketHandler = (packet, user) -> {
            if (packet.hasHeader(arp)) {

                // error log possibly causing memory issues, temp fix
                errorLog.setLength(0);

                Arp header = packet.getHeader(arp);

                String sourceProtocolAddress = FormatUtils.ip(header.spa());

                // The requests we're listening for will always have the source protocol address 0.0.0.0
                if (sourceProtocolAddress.equals("0.0.0.0")) {

                    String sourceHeaderAddress = FormatUtils.mac(header.sha());

                    System.out.println("MAC: " + sourceHeaderAddress);

                    boolean discard = false;
                    Date now = new Date();
                    Long timeOfLastContact = timeOfLastArpFromSource.get(sourceHeaderAddress);
                    long timeSinceLastContact = 0L;

                    if (timeOfLastContact == null) {
                        timeOfLastContact = now.getTime();
                        timeOfLastArpFromSource.put(sourceHeaderAddress, timeOfLastContact);
                    } else {
                        timeSinceLastContact = (now.getTime() - timeOfLastContact) / 1000;
                        System.out.printf("Heard from this device %d seconds ago.\n", timeSinceLastContact);

                        // Duplicate requests will sometimes appear.
                        // Additionally, a duplicate request will be sent approximately 44 seconds after the first.
                        // Due to limitations in place on the button, a second user generated request cannot be made
                        // within either of these time frames. We can then assume it is garbage and discard it.
                        if (timeSinceLastContact < 3 || (timeSinceLastContact > 40 && timeSinceLastContact < 50)) {
                            discard = true;
                        } else {
                            timeOfLastContact = now.getTime();
                            timeOfLastArpFromSource.put(sourceHeaderAddress, timeOfLastContact);
                        }
                    }

                    if (!discard) {
                        System.out.println("Sending button press to web server for processing.");

                        ButtonPressSender buttonPressSender = new ButtonPressSender(sourceHeaderAddress, now.getTime());

                        try {
                            buttonPressSender.send();
                        } catch (IOException ioe) {
                            System.out.printf("Failed to send button press from %s to server\n", sourceHeaderAddress);
                            ioe.printStackTrace();
                        }

                        if (buttonPressSender.getStatusCode() != 200) {
                            System.out.printf("Request returned status code %d and might have failed.\n",
                                    buttonPressSender.getStatusCode());
                        }
                    } else {
                        System.out.println("This request will be discarded.");
                    }
                    System.out.println();
                }
            }
        };

        pcap.loop(-1, jpacketHandler, "");

        pcap.close();
    }
}
