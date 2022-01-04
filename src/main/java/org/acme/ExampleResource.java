package org.acme;

import net.andreinc.neatchess.client.UCI;
import net.andreinc.neatchess.client.UCIResponse;
import net.andreinc.neatchess.client.model.Analysis;


import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/hello")
public class ExampleResource {

   // @POST
    @GET
    @Path("/{pos}")
    @Produces(MediaType.TEXT_PLAIN)
    //public String hello(@FormParam("position") String position) {
    public String hello(@PathParam("pos") String position) {
        var uci = new UCI();
        uci.startStockfish();
        uci.setOption("MultiPV", "5");

        System.out.println(uci.uciNewGame());
        //uci.positionFen("r1bqkb1r/2pp1ppp/p1n5/1p2p3/8/1B3N2/PPPn1PPP/RNBQR1K1 w kq - 0 8");
        //uci.positionFen("r1bqkb1r%2F2pp1ppp%2Fp1n5%2F1p2p3%2F8%2F1B3N2%2FPPPn1PPP%2FRNBQR1K1%20w%20kq%20-%200%208");
        uci.positionFen(position);
        UCIResponse<Analysis> response = uci.analysis(Long.valueOf(30000));
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

        uci.close();
        return "Hello RESTEasy";
    }
}