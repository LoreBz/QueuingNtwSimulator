
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
