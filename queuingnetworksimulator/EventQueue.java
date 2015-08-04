/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package queuingnetworksimulator;

import java.util.PriorityQueue;

/**
 *
 * @author Lorenzo
 */
public class EventQueue extends PriorityQueue<Event> {

    public EventQueue() {
        super();
    }

    public Event pop() {
        return this.poll();
    }

    public boolean push(Event e) {
        return this.add(e);
    }

}
