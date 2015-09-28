
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
import org.apache.commons.math3.distribution.AbstractRealDistribution;
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
    public static RandomGenerator rng = new Well19937c(System.currentTimeMillis());
    public static HashMap<String, NTWqueue> queues = new HashMap<>();
    public static PrintWriter ntwTraversalWrt;//to log time required to cross the network
    public static long exitCounter = 0;

    public static void main(String[] args) throws FileNotFoundException {
        long start = System.currentTimeMillis();
        setLookAndFeel();
        PrintWriter sizeWriter = new PrintWriter("queues_lenght.csv");
        ntwTraversalWrt = new PrintWriter("network_traversal_time.csv");
        sizeWriter.println("time,q1length,q2length,q3length");
        ntwTraversalWrt.println("PacketID,time");
        //new NTWqueue(name,ServiceDistribution,numServer,maxSize)
        //parametrization has been done according to apache math3 references 
        //http://commons.apache.org/proper/commons-math/apidocs/org/apache/commons/math3/distribution/AbstractRealDistribution.html
        NTWqueue q1 = new NTWqueue("q1", new UniformRealDistribution(rng,1.0,4.0), 2,20);
        NTWqueue q2 = new NTWqueue("q2", new ExponentialDistribution(rng, 1), 1, 50);
        NTWqueue q3 = new NTWqueue("q3", new ExponentialDistribution(rng, (1.0/2.0)), 1, 50);
        
        AbstractRealDistribution extInput = new WeibullDistribution(rng, 0.6, 1.5);
        queues.put("q1", q1);
        queues.put("q2", q2);
        queues.put("q3", q3);

        //first event
        Event firstEvent = new Event(extInput.sample(), new Packet(0, Def.externalInput, "q1"));
        eventQueue.push(firstEvent);
        MyFrame f = new MyFrame();
        JProgressBar jp = f.getjProgressBar1();
        jp.setMinimum(0);
        jp.setMaximum(1000000);

        SwingWorker<Void, Double> sw = new SwingWorker<Void, Double>() {
            double deadline = 1000000.0;
            long customerTicket = 0;
            long eventsCounter = 0;

            @Override
            protected Void doInBackground() throws Exception {
                //basically we pop the event with minimum schedueldTime and we retrieve the related packet
                //then if the packet comes from outside we push it in q1 and we schedule the next arrival from outside
                //else if the packet comes from a queue we simply dequeue it (the dequeue operation manage the possible consequent enqueue at a different queue)
                while (time < deadline) {
                    Event e = eventQueue.pop();
                    time = e.scheduledTime;
                    Packet p = e.getPacket();
                    //control over source queue
                    switch (p.getSource()) {
                        case Def.externalInput:
                            p.setGenerationTime(time);
                            q1.enqueue(p);
                            //next input from outside
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
                queues.get("q1").closeOutStreams();
                queues.get("q2").closeOutStreams();
                queues.get("q3").closeOutStreams();
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
                lossesWrt.println("NTW," + (1 - (exitCounter * 1.0 / customerTicket)));

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
