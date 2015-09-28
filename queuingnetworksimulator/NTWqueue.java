package queuingnetworksimulator;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.distribution.AbstractRealDistribution;

/**
 *
 * @author Lorenzo
 */
public class NTWqueue {

    Queue<Packet> buffer;
    ArrayList<Packet> servers;
    AbstractRealDistribution serviceTimeGenerator;
    private PrintWriter waitTimeWriter, traversalTimeWriter, lossWriter, arrivalWriter, departureWriter;
    int maxSize;
    int numServer;
    String name;
    long visitCounter = 0;
    long lostPacketCounter = 0;

    public NTWqueue(String name, AbstractRealDistribution serviceTimeGenerator, int numServer, int maxSize) {

        this.serviceTimeGenerator = serviceTimeGenerator;
        this.name = name;
        this.numServer = numServer;
        this.maxSize = maxSize;
        this.buffer = new LinkedList<>();
        this.servers = new ArrayList<>();
        try {
            waitTimeWriter = new PrintWriter(name + "_waitTimes.csv");
            traversalTimeWriter = new PrintWriter(name + "_traversalTime.csv");
            lossWriter = new PrintWriter(name + "_losses.csv");
            arrivalWriter = new PrintWriter(name + "_arrivals.csv");
            departureWriter = new PrintWriter(name + "_departures.csv");
            waitTimeWriter.println("PacketID,waitTime");
            traversalTimeWriter.println("PacketID,traversalTime");
            lossWriter.println("PacketID,lossTime");
            arrivalWriter.println("ArrivalTime,PacketID");
            departureWriter.println("DepartureTime,PacketID");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(NTWqueue.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public boolean isFull() {
        if (buffer.size() == (maxSize - numServer) && servers.size() == numServer) {
            return true;
        }
        return false;
    }

    boolean serversFull() {
        return (servers.size() == numServer);
    }

    boolean bufferFull() {
        return (buffer.size() == (maxSize - numServer));
    }

    public boolean enqueue(Packet p) {
        //System.out.println(QNSim.time + "\t" + name + ".enqueue(" + p.getPacketID() + ")");
        visitCounter++;
        arrivalWriter.println(QNSim.time + "," + p.getPacketID());
        arrivalWriter.flush();
        p.setArrivalTime(QNSim.time);
        //loss of customer
        if (this.isFull()) {
            //System.out.println(QNSim.time + "\tCoda " + name + "piena! Pacchetto ID=" + p.getPacketID() + "viene perso");
            lossWriter.println(p.getPacketID() + "," + QNSim.time);
            lostPacketCounter++;
            return false;
        }

        //packets brought to service
        if (!serversFull()) {
            bringToService(p);
            return true;
        }

        //packet put in the waiting buffer
        if (serversFull() && !bufferFull()) {
            //System.out.println(QNSim.time + "\tCoda " + name + " mette in attesa pacchetto ID="+p.getPacketID());
            buffer.add(p);
        }

        return true;
    }

    public boolean dequeue(Packet p) {
        //System.out.println(QNSim.time + "\t" + name + ".dequeue(" + p.getPacketID() + ")");
        if (!this.servers.contains(p)) {
            //System.out.println("Errore, dequeue(c) ma c non è presente nei server!");
            return false;
        }
        //rimuoviamo il cliente dai server e carichiamo il prossimo (se c'e')
        //System.out.println(QNSim.time + "\tCoda " + name + " invia pacchetto ID="+p.getPacketID()+ " a coda "+p.getDest());
        servers.remove(p);
        //log waiting & reponse time (and departure for troughput computation)
        waitTimeWriter.println(p.getPacketID() + "," + (p.getStartServiceTime() - p.getArrivalTime()));
        traversalTimeWriter.println(p.getPacketID() + "," + (QNSim.time - p.getArrivalTime()));
        departureWriter.println(QNSim.time + "," + p.getPacketID());
        if (!buffer.isEmpty()) {
            Packet toService = buffer.poll();
            bringToService(toService);
        }
        //mandiamo il pacchetto a destinazione
        if (p.getDest() == Def.output) {
            QNSim.ntwTraversalWrt.println(p.getPacketID() + "," + (QNSim.time - p.getGenerationTime()));
            //QNSim.insideNTWtime.println(p.getPacketID()+","+(QNSim.time - p.getGenerationTime()));
            QNSim.exitCounter++;
            //System.out.println(QNSim.time + "\tPacket ID=" + p.getPacketID() + " exit the network after " + (QNSim.time - p.generationTime) + " time units");
        } else {
            p.resetTime();
            //metti il pacchetto in coda a destinazione
            QNSim.queues.get(p.getDest()).enqueue(p);
        }
        waitTimeWriter.flush();
        traversalTimeWriter.flush();
        lossWriter.flush();
        QNSim.ntwTraversalWrt.flush();
        return true;
    }

    public int getSize() {
        return (servers.size() + buffer.size());
    }

    public AbstractRealDistribution getServiceTimeGenerator() {
        return serviceTimeGenerator;
    }

    private String getDestination() {
        String dest = "";
        double x = QNSim.rng.nextDouble();//double between 0.0 and 1.0
        switch (this.name) {
            case "q1":
                if (x <= 0.2) {
                    dest = "q2";
                }
                if (0.8 <= x && x <= 1.0) {
                    dest = "q3";
                }
                if (0.2 < x && x < 0.8) {
                    dest = Def.output;
                }
                break;
            case "q2":
                dest = "q1";
                break;
            case "q3":
                dest = "q1";
                break;
        }
        return dest;
    }

    public long getVisitCounter() {
        return visitCounter;
    }

    public long getLostPacketCounter() {
        return lostPacketCounter;
    }

    private void bringToService(Packet p) {
        //System.out.println(QNSim.time + "\tCoda " + name + " serve pacchetto ID="+p.getPacketID());
        servers.add(p);
        p.setStartServiceTime(QNSim.time);
        //scheduling departure of packet
        double serviceTime = this.serviceTimeGenerator.sample();
        p.setSource(this.name);
        p.setDest(getDestination());

        Event nextE = new Event(QNSim.time + serviceTime, p);
        QNSim.eventQueue.push(nextE);
    }

    public void closeOutStreams() {
        waitTimeWriter.close();
        traversalTimeWriter.close();
        lossWriter.close();
        arrivalWriter.close();
        departureWriter.close();
    }

}
