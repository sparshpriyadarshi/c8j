package dev.sp.c8j;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;


@Controller
public class C8JController {

    @MessageMapping("/c8j-server")
    @SendTo("/topic/c8j-messages")
    public C8JMessage reply(C8JMessage message) throws Exception {
        Thread.sleep(80); // simulated delay
        return new C8JMessage(System.currentTimeMillis(),"generic","server content...");
    }

}
