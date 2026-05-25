package dev.sp.c8j;


import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
public class C8JController {
    private static Logger logger = LoggerFactory.getLogger(LoggingController.class);
    
    private static C8JEmulator emulator;
    //private static ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
    private static ScheduledThreadPoolExecutor scheduler;
    //private static ScheduledFuture<?> future;
    
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

        switch (msgType) {
            case CANARY:
                break;
            case CONTROL:
                if (msgContent.equals("START")) {
                    logger.info("START: Initializing emulator...");
                    emulator = new C8JEmulator();
                    emulator.state = C8JEmulator.EMU_STATE.INITIALIZED;
                    scheduler = new ScheduledThreadPoolExecutor(1);
                    resp = new C8JServerMessage(message, System.currentTimeMillis(), emulator);

                } else if (msgContent.equals("STOP")) {
                    logger.info("Shutting scheduler for STOP...");

                    scheduler.shutdown();
                    try {
                        if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {// generous...
                            logger.warn("Scheduler did not terminate!");
                            scheduler.shutdownNow();

                        }

                    } catch (InterruptedException e) {
                        logger.error("Scheduler being forced to shut!!!");

                        scheduler.shutdownNow();
                        // Thread.currentThread().interrupt();
                    }

                    if (scheduler.isTerminated() && scheduler.isShutdown()) {
                        logger.info("scheduler shutdown complete");

                    }
                    emulator = new C8JEmulator();
                    emulator.state = C8JEmulator.EMU_STATE.STOPPED; 
                    scheduler = new ScheduledThreadPoolExecutor(1);
                    resp = new C8JServerMessage(message, System.currentTimeMillis(), emulator);
                    
                    logger.info("STOP: ...stopped emulator");
                } else if (msgContent.equals("STEP")) {
                    emulator.state = C8JEmulator.EMU_STATE.STEPPING;
                    emulator.step();
                    logger.info("STEP: stepped 1 instruction");
                    logger.debug(emulator.dumpString());
                    resp = new C8JServerMessage(message, System.currentTimeMillis(), emulator);

                } else if (msgContent.equals("RESUME")) {
                    logger.info("RUN/RESUME: Starting/Resuming emulation loop...");
                    
                    if (scheduler.isShutdown()) {//TODO keep this alive throughout app lifecycle ?
                        scheduler = new ScheduledThreadPoolExecutor(1);
                    }
                    
                    scheduler.scheduleAtFixedRate(emulator, 0, 1, TimeUnit.SECONDS);
                    emulator.state = C8JEmulator.EMU_STATE.RUNNING;
                    resp = new C8JServerMessage(message, System.currentTimeMillis(), emulator);


                } else if (msgContent.equals("PAUSE")) {
                    logger.info("PAUSE: Pausing emulation loop...");
                    
                    
                    //rework this to resume usage of pool, not create a new one.
                    scheduler.shutdown();
                    try {
                        if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {// generous...
                            logger.warn("Scheduler did not terminate!");
                            scheduler.shutdownNow();
                        }
                    } catch (InterruptedException e) {
                        logger.error("Scheduler being forced to shut!!!");
                        scheduler.shutdownNow();
                        // Thread.currentThread().interrupt();
                    }
                    if (scheduler.isTerminated() && scheduler.isShutdown()) {
                        logger.info("scheduler shutdown complete");
                    }
                                        emulator.state = C8JEmulator.EMU_STATE.PAUSED;
                    resp = new C8JServerMessage(message, System.currentTimeMillis(), emulator);

                }

                break;
            case KEYPAD:
                //todo thread mgmt
                emulator.consumeKeypress(msgContent);
                break;
            case FRAMEREQUEST:
                logger.debug("FRAMEREQUEST: recieved...");
                resp = new C8JServerMessage(message, System.currentTimeMillis(), emulator);
                // resp = new C8JClientMessage(
                // message.getClientId(),
                // System.currentTimeMillis(),
                // message.getType(),
                // String.format("Serving frame for %s\n<c8j>%s</c8j>", message.toString(),
                // emulator.dumpString()));

                logger.debug(resp.toString());
                break;
             case ROM: //TODO this right now is just an alias for START, fix me
                logger.debug("ROM: set binary event recieved...");
                logger.debug("messagecontent = " + message.getContent());
                
                emulator = new C8JEmulator(java.util.Base64.getDecoder().decode(message.getContent()));
                emulator.state = C8JEmulator.EMU_STATE.INITIALIZED;
                logger.debug("Restarted with ROM data ");

                scheduler = new ScheduledThreadPoolExecutor(1);
                resp = new C8JServerMessage(message, System.currentTimeMillis(), emulator);


                logger.debug(resp.toString());
                break;
            default:
                logger.error("unrecognized C8JClientMessage.Type");
                break;

        }

        return resp;
    }


    

}
