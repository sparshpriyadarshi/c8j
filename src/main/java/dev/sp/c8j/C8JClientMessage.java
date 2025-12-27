package dev.sp.c8j;

public class C8JClientMessage {
    private enum Type{
        CONTROL,KEYPAD,CANARY
    }
    private String clientId;//TODO: reconsider this type for uuid?..
    private long timestamp;
    private Type type;
    private String content;
    

    public C8JClientMessage(String clientId, long timestamp, Type type, String content){
        this.clientId = clientId;
        this.timestamp = timestamp;
        this.type = type;
        this.content = content;
    }




    /* boilers for spring, jackson/jaxb mainly...*/
    public C8JClientMessage(){
        
    }




    public String getClientId() {
        return clientId;
    }




    public void setClientId(String clientId) {
        this.clientId = clientId;
    }




    public long getTimestamp() {
        return timestamp;
    }




    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }




    public Type getType() {
        return type;
    }




    public void setType(Type type) {
        this.type = type;
    }




    public String getContent() {
        return content;
    }




    public void setContent(String content) {
        this.content = content;
    }




    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((clientId == null) ? 0 : clientId.hashCode());
        result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((content == null) ? 0 : content.hashCode());
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
        C8JClientMessage other = (C8JClientMessage) obj;
        if (clientId == null) {
            if (other.clientId != null)
                return false;
        } else if (!clientId.equals(other.clientId))
            return false;
        if (timestamp != other.timestamp)
            return false;
        if (type != other.type)
            return false;
        if (content == null) {
            if (other.content != null)
                return false;
        } else if (!content.equals(other.content))
            return false;
        return true;
    }



    @Override
    public String toString() {
        return "C8JClientMessage [clientId=" + clientId + ", timestamp=" + timestamp + ", type=" + type + ", content="
                + content + "]";
    }



    
}
