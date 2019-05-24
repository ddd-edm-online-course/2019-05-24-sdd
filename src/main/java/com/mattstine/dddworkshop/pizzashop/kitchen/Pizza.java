package com.mattstine.dddworkshop.pizzashop.kitchen;

import com.mattstine.dddworkshop.pizzashop.infrastructure.events.adapters.InProcessEventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.EventLog;
import com.mattstine.dddworkshop.pizzashop.infrastructure.events.ports.Topic;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.Aggregate;
import com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports.AggregateState;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.function.BiFunction;

@Value
public final class Pizza implements Aggregate {
    PizzaRef ref;
    KitchenOrderRef kitchenOrderRef;
    Size size;
    EventLog $eventLog;
    @NonFinal
    State state;

    @Builder
    private Pizza(@NonNull PizzaRef ref,
                  @NonNull KitchenOrderRef kitchenOrderRef,
                  @NonNull Size size,
                  @NonNull EventLog eventLog) {
        this.ref = ref;
        this.kitchenOrderRef = kitchenOrderRef;
        this.size = size;
        this.$eventLog = eventLog;

        this.state = State.NEW;
    }

    /**
     * Private no-args ctor to support reflection ONLY.
     */
    @SuppressWarnings("unused")
    private Pizza() {
        this.ref = null;
        this.kitchenOrderRef = null;
        this.size = null;
        this.$eventLog = null;
    }

    public boolean isNew() {
        return state == State.NEW;
    }

    void startPrep() {
        if (!this.isNew()) {
            throw new IllegalStateException();
        }

        state = State.PREPPING;

        $eventLog.publish(new Topic("pizzas"), new PizzaPrepStartedEvent(ref));
    }

    boolean isPrepping() {
        return state == State.PREPPING;
    }

    void finishPrep() {
        if (!this.isPrepping()) {
            throw new IllegalStateException();
        }

        state = State.PREPPED;

        $eventLog.publish(new Topic("pizzas"), new PizzaPrepFinishedEvent(ref));
    }

    boolean hasFinishedPrep() {
        return state == State.PREPPED;
    }

    void startBake() {
        if (!this.hasFinishedPrep()) {
            throw new IllegalStateException();
        }

        state = State.BAKING;

        $eventLog.publish(new Topic("pizzas"), new PizzaBakeStartedEvent(ref));
    }

    boolean isBaking() {
        return state == State.BAKING;
    }

    void finishBake() {
        if (!this.isBaking()) {
            throw new IllegalStateException();
        }

        state = State.BAKED;

        $eventLog.publish(new Topic("pizzas"), new PizzaBakeFinishedEvent(ref));
    }

    boolean hasFinishedBaking() {
        return state == State.BAKED;
    }

    @Override
    public Pizza identity() {
        return Pizza.builder()
                .ref(PizzaRef.IDENTITY)
                .kitchenOrderRef(KitchenOrderRef.IDENTITY)
                .size(Size.IDENTITY)
                .eventLog(EventLog.IDENTITY)
                .build();
    }

    @Override
    public BiFunction<Pizza, PizzaEvent, Pizza> accumulatorFunction() {
        return new Accumulator();
    }

    @Override
    public PizzaRef getRef() {
        return ref;
    }

    @Override
    public PizzaState state() {
        return new PizzaState(ref, kitchenOrderRef, size);
    }

    enum Size {
        IDENTITY, SMALL, MEDIUM, LARGE
    }

    enum State {
        NEW,
        PREPPING,
        PREPPED,
        BAKING,
        BAKED
    }

    private static class Accumulator implements BiFunction<Pizza, PizzaEvent, Pizza> {

        @Override
        public Pizza apply(Pizza pizza, PizzaEvent pizzaEvent) {
            if (pizzaEvent instanceof PizzaAddedEvent) {
                PizzaAddedEvent pae = (PizzaAddedEvent) pizzaEvent;
                return Pizza.builder()
                        .ref(pae.getRef())
                        .kitchenOrderRef(pae.getState().getKitchenOrderRef())
                        .size(pae.getState().getSize())
                        .eventLog(InProcessEventLog.instance())
                        .build();
            } else if (pizzaEvent instanceof PizzaPrepStartedEvent) {
                pizza.state = State.PREPPING;
                return pizza;
            } else if (pizzaEvent instanceof PizzaPrepFinishedEvent) {
                pizza.state = State.PREPPED;
                return pizza;
            } else if (pizzaEvent instanceof PizzaBakeStartedEvent) {
                pizza.state = State.BAKING;
                return pizza;
            } else if (pizzaEvent instanceof PizzaBakeFinishedEvent) {
                pizza.state = State.BAKED;
                return pizza;
            }
            throw new IllegalArgumentException("Unknown event type!");
        }
    }

    @Value
    static class PizzaState implements AggregateState {
        PizzaRef ref;
        KitchenOrderRef kitchenOrderRef;
        Size size;
    }
}
