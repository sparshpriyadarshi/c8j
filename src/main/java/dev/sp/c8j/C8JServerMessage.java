package dev.sp.c8j;

import java.util.Stack;

import dev.sp.c8j.C8JEmulator.EMU_STATE;

public class C8JServerMessage {

    private C8JClientMessage clientMessage;
    private long timestamp;
    private C8JEmulator emulator;

    
    @Override
    public String toString() {
        return "C8JServerMessage [clientMessage=" + clientMessage + ", timestamp=" + timestamp + ", emulator="
                + emulator + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((clientMessage == null) ? 0 : clientMessage.hashCode());
        result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
        result = prime * result + ((emulator == null) ? 0 : emulator.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        C8JServerMessage other = (C8JServerMessage) obj;
        if (clientMessage == null) {
            if (other.clientMessage != null)
                return false;
        } else if (!clientMessage.equals(other.clientMessage))
            return false;
        if (timestamp != other.timestamp)
            return false;
        if (emulator == null) {
            if (other.emulator != null)
                return false;
        } else if (!emulator.equals(other.emulator))
            return false;
        return true;
    }

    public C8JServerMessage() {
    }

    public C8JClientMessage getClientMessage() {
        return clientMessage;
    }

    public void setClientMessage(C8JClientMessage clientMessage) {
        this.clientMessage = clientMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public C8JEmulator getEmulator() {
        return emulator;
    }

    public void setEmulator(C8JEmulator emulator) {
        this.emulator = emulator;
    }

    public C8JServerMessage(C8JClientMessage clientMessage, long timestamp, C8JEmulator emulator) {
        this.clientMessage = clientMessage;
        this.timestamp = timestamp;
        this.emulator = emulator;
    }
    
  
    
}
