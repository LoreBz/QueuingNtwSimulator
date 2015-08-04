/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package queuingnetworksimulator;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Random;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.distribution.WeibullDistribution;

/**
 *
 * @author Lorenzo
 */
public class QNSim {

    public static EventQueue eventQueue = new EventQueue();
    public static double time = 0.0;
    public static Random random = new Random(System.currentTimeMillis());
    public static HashMap<String, NTWqueue> queues = new HashMap<>();
    public static PrintWriter ntwTraversalWrt;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException {
        long start = System.currentTimeMillis();
        setLookAndFeel();
        PrintWriter sizeWriter = new PrintWriter("queues_lenght.csv");
        ntwTraversalWrt = new PrintWriter("network_traversal_time.csv");
        sizeWriter.println("time,q1length,q2length,q3length");
        NTWqueue q1 = new NTWqueue("q1", new UniformRealDistribution(1.0, 4.0), 2, 20);
        NTWqueue q2 = new NTWqueue("q2", new ExponentialDistribution(1), 1, 50);
        NTWqueue q3 = new NTWqueue("q3", new ExponentialDistribution(2), 1, 50);
        WeibullDistribution extInput = new WeibullDistribution(0.6, 1.5);
        queues.put("q1", q1);
        queues.put("q2", q2);
        queues.put("q3", q3);

        double deadline = 1000000.0;
        long customerTicket = 0;
        long eventsCounter = 0;

        //first event
        Event firstEvent = new Event(extInput.sample(), new Packet(customerTicket, Def.externalInput, "q1"));
        eventQueue.push(firstEvent);

        while (time < deadline) {
            Event e = eventQueue.pop();
            time = e.scheduledTime;
            Packet p = e.getPacket();
            //control sorgente
            switch (p.getSource()) {
                case Def.externalInput:
                    p.setGenerationTime(time);
                    q1.enqueue(p);
                    //prossimo input dall'esterno
                    customerTicket++;
                    Event nextInputEvent = new Event(time + extInput.sample(), new Packet(customerTicket, Def.externalInput, "q1"));
                    eventQueue.push(nextInputEvent);
                    break;
                case "q1":
                    q1.dequeue(p);
                    break;
                case "q2":
                    q2.dequeue(p);
                    break;
                case "q3":
                    q3.dequeue(p);
                    break;
            }

            //write stastics on appropriate file
            sizeWriter.println(time + "," + q1.getSize() + "," + q2.getSize() + "," + q3.getSize());
            eventsCounter++;
        }

        sizeWriter.close();
        ntwTraversalWrt.close();
        PrintWriter lossesWrt = new PrintWriter("losses.csv");
        lossesWrt.println("queue,loss_rate");
        lossesWrt.println("q1," + (q1.getLostPacketCounter() * 1.0 / q1.getVisitCounter()));
        lossesWrt.println("q2," + (q2.getLostPacketCounter() * 1.0 / q2.getVisitCounter()));
        lossesWrt.println("q3," + (q3.getLostPacketCounter() * 1.0 / q3.getVisitCounter()));

        lossesWrt.close();
        long end = System.currentTimeMillis();
        JOptionPane.showMessageDialog(null, eventsCounter + " events has been simulated in " + ((end - start) / 1000) + "seconds\nLook in the log files to observe related results");
    }

    private static boolean setLookAndFeel() {
        String lookAndFeel = UIManager.getSystemLookAndFeelClassName();
        try {
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        } catch (UnsupportedLookAndFeelException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
