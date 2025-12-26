package dev.sp.c8j;

public class C8JClientMessage {
    private enum Type{
        CONTROL,KEYPAD,CANARY
    }
    private String id;//TODO: reconsider this type for uuid?..
    private long timestamp;
    private Type type;
    private String content;
    
    
    public C8JClientMessage(String id, long timestamp, Type type, String content){
        this.id = id;
        this.timestamp = timestamp;
        this.type = type;
        this.content = content;
    }


    @Override
    public String toString() {
        return "C8JClientMessage [id=" + id + ", timestamp=" + timestamp + ", type=" + type + ", content=" + content
                + "]";
    }


    /* boilers for spring, jackson/jaxb mainly...*/

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
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
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
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



    
    public C8JClientMessage(){  }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
    
}
