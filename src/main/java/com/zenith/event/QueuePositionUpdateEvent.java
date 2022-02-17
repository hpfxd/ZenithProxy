package com.zenith.event;

import com.collarmc.pounce.EventInfo;
import com.collarmc.pounce.Preference;

@EventInfo(preference = Preference.POOL)
public class QueuePositionUpdateEvent {
    public final int position;

    public QueuePositionUpdateEvent(int position) {
        this.position = position;
    }
}