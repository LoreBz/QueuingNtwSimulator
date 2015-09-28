
package queuingnetworksimulator;

/**
 *
 * @author Lorenzo
 */
public class Event implements Comparable<Event> {

    double scheduledTime;
    Packet packet;

    public Event(double scheduledTime, Packet packet) {

        this.scheduledTime = scheduledTime;
        this.packet = packet;
    }

    @Override
    public int compareTo(Event other) {
        if (this == other || this.scheduledTime == other.getScheduledTime()) {
            return 0;
        }
        if (this.scheduledTime < other.scheduledTime) {
            return -1;
        } else {
            return 1;
        }
    }

    public double getScheduledTime() {
        return scheduledTime;
    }

    public Packet getPacket() {
        return packet;
    }

   
    
    

}
