package com.mattstine.dddworkshop.pizzashop.infrastructure;

/**
 * @author Matt Stine
 */
public interface EventLog {
	void publish(Event event);
}
