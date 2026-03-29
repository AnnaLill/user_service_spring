package org.example.kafka;

public final class NoOpUserEventNotifier implements UserEventNotifier {

    @Override
    public void notify(UserEvent event) {
    }
}
