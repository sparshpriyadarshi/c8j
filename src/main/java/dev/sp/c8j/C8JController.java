package dev.sp.c8j;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
public class C8JController {
    Logger logger = LoggerFactory.getLogger(LoggingController.class);
    
    C8JEmulator emulator;

    @MessageMapping("/c8j-server") //client publishes here
    //@SendTo("/topic/c8j-messages")
    @SendTo("/queue/c8j-messages")// client subs to this / reads from
    public C8JServerMessage handleEvent(C8JClientMessage message) throws Exception {
        logger.trace(String.format("Server received: %s\n", message.toString()));
        //System.out.printf("Server received: %s\n", message.toString());
        //Thread.sleep(100); // simulated delay
        C8JClientMessage.Type msgType = message.getType();
        String msgContent = message.getContent();

        C8JServerMessage resp;
        resp = new C8JServerMessage(message, System.currentTimeMillis(), null);

        switch(msgType){
            case CANARY:
                break;
            case CONTROL:
                if(msgContent.equals("START")){
                    logger.info("START: Initializing emulator...");
                    emulator = new C8JEmulator();
                    emulator.state = C8JEmulator.EMU_STATE.INITIALIZED;
                }else if(msgContent.equals("STOP")){
                    emulator.state = C8JEmulator.EMU_STATE.STOPPED; // TODO: might be redundant
                    emulator = null;
                    logger.info("STOP: ...stopped emulator");
                }else if(msgContent.equals("STEP")){
                    emulator.state = C8JEmulator.EMU_STATE.STEPPING; 
                    emulator.step();
                    logger.info("STEP: stepped 1 instruction");
                    logger.debug(emulator.dumpString());

                }
                break;
            case KEYPAD:
                emulator.consumeKeypress(msgContent);
                break;
            case FRAMEREQUEST:
                logger.debug("FRAMEREQUEST: recieved...");
                resp = new C8JServerMessage(message, System.currentTimeMillis(), emulator);
                // resp = new C8JClientMessage(
                //         message.getClientId(),
                //         System.currentTimeMillis(),
                //         message.getType(),
                //         String.format("Serving frame for %s\n<c8j>%s</c8j>", message.toString(), emulator.dumpString()));
                
                logger.debug(resp.toString());
                break;
            default:
                logger.error("unrecognized C8JClientMessage.Type");
                break;
            
        }

        return resp;
    }


    

}
