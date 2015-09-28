
package queuingnetworksimulator;

/**
 *
 * @author Lorenzo
 */
public class Packet {

    
    long packetID;
    double generationTime;
    double arrivalTime;
    double startServiceTime;
    String source, dest;
    //double departureTime;

    public Packet() {
    }

    public Packet(long customerID, String source, String dest) {
        this.packetID = customerID;
        this.generationTime = 0.0;
        this.arrivalTime = 0.0;
        this.startServiceTime = 0.0;
        this.source = source;
        this.dest = dest;
        // System.out.println(QNSim.time+"\tNuovo pacchetto con ID=" + packetID);
    }

    public void setGenerationTime(double generationTime) {
        this.generationTime = generationTime;
    }

    public double getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(double arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public long getPacketID() {
        return packetID;
    }

    public void setPacketID(long packetID) {
        this.packetID = packetID;
    }

    public double getStartServiceTime() {
        return startServiceTime;
    }

    public void setStartServiceTime(double startServiceTime) {
        this.startServiceTime = startServiceTime;
    }

//    public double getDepartureTime() {
//        return departureTime;
//    }
//
//    public void setDepartureTime(double departureTime) {
//        this.departureTime = departureTime;
//    }
    public double getGenerationTime() {
        return generationTime;
    }

    public String getSource() {
        return source;
    }

    public String getDest() {
        return dest;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setDest(String dest) {
        this.dest = dest;
    }

    public void resetTime() {
        this.arrivalTime = 0.0;
        this.startServiceTime = 0.0;
    }
    
    public void reset() {
        arrivalTime=0.0;
        startServiceTime=0.0;
        generationTime=0.0;
        packetID=-1;
        source="";
        dest="";
    }
    
    public void init(long customerID, String source, String dest) {
         this.packetID = customerID;
        this.generationTime = 0.0;
        this.arrivalTime = 0.0;
        this.startServiceTime = 0.0;
        this.source = source;
        this.dest = dest;
    }

}
