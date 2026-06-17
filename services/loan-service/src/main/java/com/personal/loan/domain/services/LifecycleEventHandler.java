package com.personal.loan.domain.services;

/**
 * 
 * @param <T> id type
 * @param <V> payload type
 */
public interface LifecycleEventHandler<T, V> {
    public void handleCreationEvent(V payload);
    public void handleUpdateEvent(V payload);
    public void handleDeletionEvent(T id);
}
