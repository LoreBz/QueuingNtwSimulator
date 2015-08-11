/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package queuingnetworksimulator;

/**
 *
 * @author Lorenzo
 */
public class Event implements Comparable<Event> {

    double scheduledTime;
    Packet packet;
    int type=-1;

    public Event(double scheduledTime, Packet packet) {

        this.scheduledTime = scheduledTime;
        this.packet = packet;
        this.type=0;
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

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
    
    

}
