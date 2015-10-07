/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package queuingnetworksimulator;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.distribution.WeibullDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;

/**
 *
 * @author Lorenzo
 */
public class QNSim {

    public static EventQueue eventQueue = new EventQueue();
    public static double time = 0.0;
    public static RandomGenerator rng = new Well19937c(123456);
    public static HashMap<String, NTWqueue> queues = new HashMap<>();
    public static PrintWriter ntwTraversalWrt/*
             * ,insideNTWtime
             */;
    public static long exitCounter = 0;

    public static void main(String[] args) throws FileNotFoundException {

        setLookAndFeel();
        final PrintWriter sizeWriter = new PrintWriter("queues_lenght.csv");
        ntwTraversalWrt = new PrintWriter("network_traversal_time.csv");
        sizeWriter.println("time,q1length,q2length,q3length");
        ntwTraversalWrt.println("PacketID,time");
        final NTWqueue q1 = new NTWqueue("q1", new UniformRealDistribution(rng, 1.0, 4.0), 2, 20);
        final NTWqueue q2 = new NTWqueue("q2", new ExponentialDistribution(rng, 1), 1, 50);
        final NTWqueue q3 = new NTWqueue("q3", new ExponentialDistribution(rng, 2), 1, 50);
        final WeibullDistribution extInput = new WeibullDistribution(rng,0.6, 1.5);
        queues.put("q1", q1);
        queues.put("q2", q2);
        queues.put("q3", q3);

        //first event
        Event firstEvent = new Event(extInput.sample(), new Packet(0, Def.externalInput, "q1"));
        eventQueue.push(firstEvent);
        MyFrame f = new MyFrame();
        final JProgressBar jp = f.getjProgressBar1();
        jp.setMinimum(0);
        jp.setMaximum(1000000);

        SwingWorker<Void, Double> sw = new SwingWorker<Void, Double>() {
            double deadline = 1000000.0;
            long customerTicket = 0;
            long eventsCounter = 0;
            long start;

            @Override
            protected Void doInBackground() throws Exception {
                start = System.currentTimeMillis();
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
                    if (((int) time) % 10000 == 0) {
                        publish(time);
                        System.out.println("Time: " + time);
                    }
                }
                return null;
            }

            @Override
            protected void process(List<Double> chunks) {
                Double progress = chunks.get(chunks.size() - 1);
                jp.setValue((int) time);

            }

            @Override
            protected void done() {
                long end = System.currentTimeMillis();
                JOptionPane.showMessageDialog(null, eventsCounter + " events has been simulated in " + ((end - start) / 1000) + "seconds\nLook in the log files to observe related results");
                sizeWriter.close();
                ntwTraversalWrt.close();
                PrintWriter lossesWrt = null;
                try {
                    lossesWrt = new PrintWriter("losses.csv");
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(QNSim.class.getName()).log(Level.SEVERE, null, ex);
                }
                lossesWrt.println("queue,loss_rate");
                lossesWrt.println("q1," + (q1.getLostPacketCounter() * 1.0 / q1.getVisitCounter()));
                lossesWrt.println("q2," + (q2.getLostPacketCounter() * 1.0 / q2.getVisitCounter()));
                lossesWrt.println("q3," + (q3.getLostPacketCounter() * 1.0 / q3.getVisitCounter()));

                lossesWrt.close();
                System.out.println("q1 visit: " + queues.get("q1").getVisitCounter());
                System.out.println("q2 visit: " + queues.get("q2").getVisitCounter());
                System.out.println("q3 visit: " + queues.get("q3").getVisitCounter());
                System.out.println("NTW losses=" + (1 - (exitCounter * 1.0 / customerTicket)));

                System.exit(0);
            }

        };
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setLocationRelativeTo(null);
        f.setVisible(true);

        sw.execute();

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
