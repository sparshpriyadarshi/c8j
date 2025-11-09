package dev.sp.c8j;

// TODO can be a record ?
public class C8JMessage {

    public long id;
    public String type;
    public String content;
    public C8JMessage(long id, String type, String content){
        this.id = id;
        this.type = type;
        this.content = content;
    }

    
}