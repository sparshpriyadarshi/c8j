package dev.sp.c8j;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
public class C8JController {

    //@ResponseBody
    @MessageMapping("/c8j-server")
    @SendTo("/topic/c8j-messages")
    public C8JClientMessage reply(C8JClientMessage message) throws Exception {
        Thread.sleep(100); // simulated delay

        System.out.printf("Server received: %s\n", message.toString());

        C8JClientMessage resp = new C8JClientMessage(message.getClientId(), System.currentTimeMillis(), message.getType(),
                String.format("server response to %s", message.toString()));
        return resp;
    }

}
