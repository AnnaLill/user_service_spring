package org.example.kafka;

public interface UserEventNotifier {

    void notify(UserEvent event);
}
