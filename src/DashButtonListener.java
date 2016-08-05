/**
 * Created by Taylor on 7/15/2016.
 */

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.packet.format.FormatUtils;
import org.jnetpcap.protocol.network.Arp;

import java.io.IOException;
import java.util.*;

class DashButtonListener {

    public static void main(String[] args) {

        List<PcapIf> networkInterfaceCards = new ArrayList<>(); // Will be filled with NICs
        StringBuilder errorLog = new StringBuilder(); // For any error msgs
        Map<String, Long> timeOfLastArpFromSource = new HashMap<>();

        int result = Pcap.findAllDevs(networkInterfaceCards, errorLog);
        if (result == Pcap.NOT_OK || networkInterfaceCards.isEmpty()) {
            System.err.printf("Can't read list of devices, error is %s", errorLog.toString());
            return;
        }

        PcapIf networkInterfaceCard = networkInterfaceCards.get(0); // We know we have atleast 1 device

        Pcap pcap = Pcap.openLive(networkInterfaceCard.getName(), 64 * 1024, Pcap.MODE_PROMISCUOUS, 500, errorLog);

        if (pcap == null) {
            System.err.printf("Error while opening device for capture: %s", errorLog.toString());
            return;
        }

        Arp arp = new Arp();

        PcapPacketHandler<String> jpacketHandler = (packet, user) -> {
            if (packet.hasHeader(arp)) {
                errorLog.setLength(0);

                Arp header = packet.getHeader(arp);

                String sourceIP = FormatUtils.ip(header.spa());
                byte[] ip = header.spa();

                byte[] mac = header.sha();

                if (sourceIP.equals("0.0.0.0")) {

                    String sourceMAC = FormatUtils.mac(header.sha());

                    System.out.println("MAC: " + sourceMAC);

                    boolean discard = false;
                    Date now = new Date();
                    Long timeOfLastContact = timeOfLastArpFromSource.get(sourceMAC);
                    long timeSinceLastContact = 0L;

                    if (timeOfLastContact == null) {
                        timeOfLastContact = now.getTime();
                        timeOfLastArpFromSource.put(sourceMAC, timeOfLastContact);
                    } else {
                        timeSinceLastContact = (now.getTime() - timeOfLastContact) / 1000;
                        System.out.printf("Heard from this device %d seconds ago.\n", timeSinceLastContact);
                        if (timeSinceLastContact < 3 || (timeSinceLastContact > 40 && timeSinceLastContact < 50)) {
                            discard = true;
                        } else {
                            timeOfLastContact = now.getTime();
                            timeOfLastArpFromSource.put(sourceMAC, timeOfLastContact);
                        }
                    }

                    if (!discard) {
                        System.out.println("Sending button press to web server for processing.");

                        ButtonPressSender buttonPressSender = new ButtonPressSender(sourceMAC, now.getTime());
                        try {
                            long t1 = System.currentTimeMillis();
                            buttonPressSender.send();
                            long t2 = System.currentTimeMillis();
                            System.out.printf("send success in %d ms\n", (t2 - t1));
                        } catch (IOException ioe) {
                            System.out.println("send failed");
                        }
                    } else {
                        System.out.println("This request will be discarded.");
                    }
                    System.out.println();
                } else {

                }
            }
        };

        pcap.loop(-1, jpacketHandler, "");

        pcap.close();
    }
}
