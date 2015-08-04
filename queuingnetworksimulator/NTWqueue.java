/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

    //ArrayList<Customer> waitQueue;
    Queue<Packet> buffer;
    ArrayList<Packet> servers;
    AbstractRealDistribution serviceTimeGenerator;
    private PrintWriter waitTimeWriter, traversalTimeWriter, lossWriter;
    int bufferSize;
    int numServer;
    String name;
    long visitCounter = 0;
    long lostPacketCounter = 0;

    public NTWqueue(String name, AbstractRealDistribution serviceTimeGenerator, int numServer, int bufferSize) {

        this.serviceTimeGenerator = serviceTimeGenerator;
        this.name = name;
        this.numServer = numServer;
        this.bufferSize = bufferSize;
        this.buffer = new LinkedList<>();
        this.servers = new ArrayList<>();
        try {
            waitTimeWriter = new PrintWriter(name + "_waitTimes.csv");
            traversalTimeWriter = new PrintWriter(name + "_traversalTime.csv");
            lossWriter = new PrintWriter(name + "_losses.csv");
            waitTimeWriter.println("PacketID,waitTime");
            traversalTimeWriter.println("PacketID,traversalTime");
            lossWriter.println("PacketID,lossTime");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(NTWqueue.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public boolean isFull() {
        if (buffer.size() == bufferSize && servers.size() == numServer) {
            return true;
        }
        return false;
    }

    boolean serversFull() {
        return (servers.size() == numServer);
    }

    boolean bufferFull() {
        return (buffer.size() == bufferSize);
    }

    public boolean enqueue(Packet p) {
//        //System.out.println(QNSim.time + "\t" + name + ".enqueue(" + p.getPacketID() + ")");
        visitCounter++;
        //loss of customer
        if (this.isFull()) {
            //System.out.println(QNSim.time + "\tCoda " + name + "piena! Pacchetto ID=" + p.getPacketID() + "viene perso");
            lossWriter.println(p.getPacketID() + "," + QNSim.time);
            lostPacketCounter++;
            return false;
        }

        //cliente portato direttamente in servizio
        if (!serversFull()) {
            //System.out.println(QNSim.time + "\tCoda " + name + " serve pacchetto ID="+p.getPacketID());
            servers.add(p);
            if (p.getArrivalTime() == 0.0) {
                p.setArrivalTime(QNSim.time);
            }
            p.setStartServiceTime(QNSim.time);
            //scheduling departure of packet
            double serviceTime = this.serviceTimeGenerator.sample();
            p.setSource(this.name);
            p.setDest(getDestination());

            Event nextE = new Event(QNSim.time + serviceTime, p);
            QNSim.eventQueue.push(nextE);
            return true;
        }

        //cliente messo in coda d'attesa
        if (serversFull() && !bufferFull()) {
            //System.out.println(QNSim.time + "\tCoda " + name + " mette in attesa pacchetto ID="+p.getPacketID());
            buffer.add(p);
            p.setArrivalTime(QNSim.time);
        }

        return true;
    }

    public boolean dequeue(Packet p) {
//        //System.out.println(QNSim.time + "\t" + name + ".dequeue(" + p.getPacketID() + ")");
        if (!this.servers.contains(p)) {
            //System.out.println("Errore, dequeue(c) ma c non è presente nei server!");
            return false;
        }
        //rimuoviamo il cliente dai server e carichiamo il prossimo (se c'e')
        //System.out.println(QNSim.time + "\tCoda " + name + " invia pacchetto ID="+p.getPacketID()+ " a coda "+p.getDest());
        servers.remove(p);
        if (!buffer.isEmpty()) {
            Packet toService = buffer.poll();
            this.enqueue(toService);
        }
        //mandiamo il pacchetto a destinazione
        if (p.getDest() == Def.output) {
            QNSim.ntwTraversalWrt.println(p.getPacketID() + "," + (QNSim.time - p.generationTime));
            waitTimeWriter.println(p.getPacketID() + "," + (p.getStartServiceTime() - p.getArrivalTime()));
            traversalTimeWriter.println(p.getPacketID() + "," + (QNSim.time - p.getArrivalTime()));
            //System.out.println(QNSim.time + "\tPacket ID=" + p.getPacketID() + " exit the network after " + (QNSim.time - p.generationTime) + " time units");
        } else {
            waitTimeWriter.println(p.getPacketID() + "," + (p.getStartServiceTime() - p.getArrivalTime()));
            traversalTimeWriter.println(p.getPacketID() + "," + (QNSim.time - p.getArrivalTime()));
            p.resetTime();
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
        double x = QNSim.random.nextDouble();//double between 0.0 and 1.0
        switch (this.name) {
            case "q1":
                if (x <= 0.2) {
                    dest = "q2";
                }
                if (0.2 < x && x <= 0.4) {
                    dest = "q3";
                }
                if (x > 0.4) {
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

}
