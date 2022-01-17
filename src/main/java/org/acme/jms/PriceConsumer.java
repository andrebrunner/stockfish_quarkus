package org.acme.jms;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.ws.rs.PathParam;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import net.andreinc.neatchess.client.UCI;
import net.andreinc.neatchess.client.UCIResponse;
import net.andreinc.neatchess.client.model.Analysis;

/**
 * A bean consuming prices from the JMS queue.
 */
@ApplicationScoped
public class PriceConsumer implements Runnable {

    @Inject
    ConnectionFactory connectionFactory;

    private final ExecutorService scheduler = Executors.newSingleThreadExecutor();

    private volatile byte[] lastPrice;

    public String getLastPrice() {
        return new String(lastPrice, StandardCharsets.UTF_8);
    }

    void onStart(@Observes StartupEvent ev) {
        scheduler.submit(this);
    }

    void onStop(@Observes ShutdownEvent ev) {
        scheduler.shutdown();
    }

    @Override
    public void run() {
        try (JMSContext context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE)) {
            JMSConsumer consumer = context.createConsumer(context.createQueue("examples"));
            while (true) {
                Message message = consumer.receive();
                if (message == null) return;
                lastPrice = message.getBody(byte[].class);
                String s = new String(lastPrice, StandardCharsets.UTF_8);
                System.out.println("####"+s);
                System.out.println("####"+message.getJMSMessageID());
                runUCI(s);
            }
        } catch (JMSException e) {
           System.out.println(e.toString());
        }
    }

    public String runUCI( String position) {
        var uci = new UCI();
        uci.startStockfish();
        uci.setOption("MultiPV", "5");

        System.out.println(uci.uciNewGame());
        //uci.positionFen("r1bqkb1r/2pp1ppp/p1n5/1p2p3/8/1B3N2/PPPn1PPP/RNBQR1K1 w kq - 0 8");
        //uci.positionFen("r1bqkb1r%2F2pp1ppp%2Fp1n5%2F1p2p3%2F8%2F1B3N2%2FPPPn1PPP%2FRNBQR1K1%20w%20kq%20-%200%208");
        uci.positionFen(position);
        UCIResponse<Analysis> response = uci.analysis(Long.valueOf(300));
        var analysis = response.getResultOrThrow();

// Best move
        System.out.println("Best move: " + analysis.getBestMove());
        System.out.println("Is Draw: " + analysis.isDraw());
        System.out.println("Is Mate: " + analysis.isMate());

// Possible best moves
        var moves = analysis.getAllMoves();
        moves.forEach((idx, move) -> {
            System.out.println("\t" + move);
        });
        try (JMSContext context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE)) {
                 context.createProducer().send(context.createQueue("hello"), "##"+position+" ===>"+analysis.getBestMove()+"##");
        }
        uci.close();
        return "UCI";
    }
}