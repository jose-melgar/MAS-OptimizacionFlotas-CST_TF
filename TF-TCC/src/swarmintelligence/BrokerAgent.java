package swarmintelligence;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

/**
 * BrokerAgent: genera solicitudes periódicas y coordina una subasta simple (CFP/PROPOSE/ACCEPT/REJECT).
 */
public class BrokerAgent extends Agent {
    private Random rnd = new Random();
    private static final int GRID_WIDTH = 1500;
    private static final int GRID_HEIGHT = 1000;

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " listo. Iniciando DemandBehaviour...");
        // TickerBehaviour que genera una nueva solicitud cada 5 segundos (5000 ms)
        addBehaviour(new TickerBehaviour(this, 5000) {
            @Override
            protected void onTick() {
                // Crear una nueva solicitud: origen y destino aleatorios
                int origX = rnd.nextInt(GRID_WIDTH);
                int origY = rnd.nextInt(GRID_HEIGHT);
                int destX = rnd.nextInt(GRID_WIDTH);
                int destY = rnd.nextInt(GRID_HEIGHT);
                String requestId = "req-" + System.currentTimeMillis();

                System.out.println("Broker genera solicitud " + requestId + " origen=(" + origX + "," + origY + ") dest=(" + destX + "," + destY + ")");

                // Buscar agentes que ofrezcan "Transporte de Pasajeros" en el DF
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("Transporte de Pasajeros");
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    if (result == null || result.length == 0) {
                        System.out.println("Broker: no se encontraron VehicleAgents registrados.");
                        return;
                    }

                    // Preparar y enviar CFP a todos los agentes encontrados
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    cfp.setConversationId(requestId);
                    cfp.setContent(origX + "," + origY + "," + destX + "," + destY);
                    for (DFAgentDescription dfd : result) {
                        cfp.addReceiver(dfd.getName());
                    }
                    send(cfp);
                    System.out.println("Broker envió CFP a " + result.length + " agentes para " + requestId);

                    // Recoger PROPOSE (ofertas) durante una ventana de tiempo (e.g., 2 segundos)
                    long waitMs = 2000;
                    long end = System.currentTimeMillis() + waitMs;
                    Map<AID, Integer> proposals = new HashMap<>();
                    while (System.currentTimeMillis() < end) {
                        long remaining = end - System.currentTimeMillis();
                        if (remaining <= 0) break;
                        MessageTemplate mt = MessageTemplate.and(
                                MessageTemplate.MatchConversationId(requestId),
                                MessageTemplate.MatchPerformative(ACLMessage.PROPOSE)
                        );
                        ACLMessage reply = blockingReceive(mt, remaining);
                        if (reply != null) {
                            try {
                                int bid = Integer.parseInt(reply.getContent().trim());
                                proposals.put(reply.getSender(), bid);
                                System.out.println("Broker recibió PROPOSE from " + reply.getSender().getLocalName() + " bid=" + bid);
                            } catch (NumberFormatException nfe) {
                                System.err.println("Broker: formato bid inválido de " + reply.getSender().getLocalName());
                            }
                        }
                    }

                    // Si no hubo propuestas válidas, terminar
                    if (proposals.isEmpty()) {
                        System.out.println("Broker: no se recibieron propuestas para " + requestId);
                        // Podríamos enviar CANCEL/NOT-UNDERSTOOD a todo el mundo, pero no es necesario aquí
                        return;
                    }

                    // Seleccionar la mejor oferta (mínima)
                    AID winner = null;
                    int best = Integer.MAX_VALUE;
                    for (Map.Entry<AID, Integer> e : proposals.entrySet()) {
                        if (e.getValue() < best) {
                            best = e.getValue();
                            winner = e.getKey();
                        }
                    }

                    // Enviar ACCEPT_PROPOSAL al ganador y REJECT_PROPOSAL a los demás
                    for (AID aid : proposals.keySet()) {
                        ACLMessage decision;
                        if (aid.equals(winner)) {
                            decision = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                            decision.addReceiver(aid);
                            decision.setConversationId(requestId);
                            // Incluir destino para que el vehículo sepa adónde debe ir
                            decision.setContent(destX + "," + destY);
                            send(decision);
                        } else {
                            decision = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                            decision.addReceiver(aid);
                            decision.setConversationId(requestId);
                            decision.setContent("otra propuesta mejor");
                            send(decision);
                        }
                    }
                    System.out.println("Broker: ganador de " + requestId + " = " + winner.getLocalName() + " con bid=" + best);

                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void takeDown() {
        System.out.println(getLocalName() + " terminando.");
    }
}