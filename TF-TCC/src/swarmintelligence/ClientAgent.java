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
 * ClientAgent - Generador continuo de ordenes de paqueteria
 * Simula una empresa que recibe constantemente pedidos de clientes
 */
public class ClientAgent extends Agent {
    private Random rnd = new Random();
    private int orderCounter = 0;
    private static final long ORDER_GENERATION_INTERVAL = 4000; // Nueva orden cada 4 segundos (mas ordenes)
    
    @Override
    protected void setup() {
        System.out.println(getLocalName() + " (Centro de Ordenes) iniciado - Generando pedidos continuamente...");
        
        // Comportamiento para generar ordenes continuamente
        addBehaviour(new TickerBehaviour(this, ORDER_GENERATION_INTERVAL) {
            @Override
            protected void onTick() {
                generateNewOrder();
            }
        });
        
        // Comportamiento para escuchar notificaciones de entregas completadas
        addBehaviour(new jade.core.behaviours.CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                if (msg != null && "DELIVERED".equals(msg.getContent())) {
                    String orderId = msg.getConversationId();
                    System.out.println("=== ORDEN COMPLETADA: " + orderId + " ===");
                    
                    // Notificar al visualizador para remover la orden
                    ACLMessage m = new ACLMessage(ACLMessage.INFORM);
                    m.addReceiver(new AID("visualizer", AID.ISLOCALNAME));
                    m.setContent("CLIENT_REMOVE:" + orderId);
                    send(m);
                } else {
                    block();
                }
            }
        });
    }
    
    private void generateNewOrder() {
        orderCounter++;
        
        // Generar ubicaciones aleatorias de recogida y entrega
        CityMap.Intersection pickup = CityMap.getRandomIntersection(rnd);
        CityMap.Intersection delivery = CityMap.getRandomIntersection(rnd);
        
        // Asegurarse de que no sean el mismo punto
        while (delivery.equals(pickup)) {
            delivery = CityMap.getRandomIntersection(rnd);
        }
        
        String orderId = "ORDER-" + getLocalName() + "-" + orderCounter;
        
        System.out.println("\n=== NUEVA ORDEN GENERADA ===");
        System.out.println("ID: " + orderId);
        System.out.println("Recogida: (" + pickup.gridX + "," + pickup.gridY + ")");
        System.out.println("Entrega: (" + delivery.gridX + "," + delivery.gridY + ")");
        System.out.println("Distancia estimada: " + CityMap.getManhattanDistance(pickup, delivery) + " cuadras");
        
        // Notificar al visualizador sobre la nueva orden
        notifyOrderCreated(orderId, pickup, delivery);
        
        // Buscar vehiculos disponibles
        requestDeliveryService(orderId, pickup, delivery);
    }
    
    private void requestDeliveryService(String orderId, CityMap.Intersection pickup, CityMap.Intersection delivery) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Servicio de Paqueteria");
        template.addServices(sd);
        
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result == null || result.length == 0) {
                System.out.println("ERROR: No hay vehiculos registrados para la orden " + orderId);
                return;
            }
            
            // Enviar CFP a todos los vehiculos
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            cfp.setConversationId(orderId);
            cfp.setContent(pickup.gridX + "," + pickup.gridY + "," + 
                          delivery.gridX + "," + delivery.gridY);
            
            for (DFAgentDescription dfd : result) {
                cfp.addReceiver(dfd.getName());
            }
            send(cfp);
            
            System.out.println("CFP enviado a " + result.length + " vehiculos para orden " + orderId);
            
            // Esperar propuestas
            waitForProposals(orderId, result.length, pickup, delivery);
            
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
    
    private void waitForProposals(String orderId, int expectedReplies, 
                                   CityMap.Intersection pickup, CityMap.Intersection delivery) {
        long waitMs = 3000;
        long end = System.currentTimeMillis() + waitMs;
        Map<AID, Integer> proposals = new HashMap<>();
        
        while (System.currentTimeMillis() < end) {
            long remaining = end - System.currentTimeMillis();
            if (remaining <= 0) break;
            
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchConversationId(orderId),
                    MessageTemplate.or(
                            MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                            MessageTemplate.MatchPerformative(ACLMessage.REFUSE)
                    )
            );
            ACLMessage reply = blockingReceive(mt, remaining);
            
            if (reply != null && reply.getPerformative() == ACLMessage.PROPOSE) {
                try {
                    int bid = Integer.parseInt(reply.getContent().trim());
                    proposals.put(reply.getSender(), bid);
                } catch (NumberFormatException e) {
                    // ignorar propuestas invalidas
                }
            }
        }
        
        if (proposals.isEmpty()) {
            System.out.println("ALERTA: Ningun vehiculo disponible para orden " + orderId);
            notifyOrderFailed(orderId);
            return;
        }
        
        // Seleccionar mejor oferta (menor distancia)
        AID winner = null;
        int bestBid = Integer.MAX_VALUE;
        for (Map.Entry<AID, Integer> e : proposals.entrySet()) {
            if (e.getValue() < bestBid) {
                bestBid = e.getValue();
                winner = e.getKey();
            }
        }
        
        // Enviar aceptacion al ganador y rechazo a los demas
        for (AID aid : proposals.keySet()) {
            ACLMessage decision;
            if (aid.equals(winner)) {
                decision = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                decision.addReceiver(aid);
                decision.setConversationId(orderId);
                decision.setContent(pickup.gridX + "," + pickup.gridY + "," + 
                                   delivery.gridX + "," + delivery.gridY);
                send(decision);
                
                System.out.println(">>> ORDEN " + orderId + " ASIGNADA A " + aid.getLocalName());
                System.out.println("    Distancia del vehiculo: " + bestBid + " cuadras");
                notifyOrderAssigned(orderId, aid.getLocalName());
            } else {
                decision = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                decision.addReceiver(aid);
                decision.setConversationId(orderId);
                send(decision);
            }
        }
    }
    
    private void notifyOrderCreated(String orderId, CityMap.Intersection pickup, CityMap.Intersection delivery) {
        ACLMessage m = new ACLMessage(ACLMessage.INFORM);
        m.addReceiver(new AID("visualizer", AID.ISLOCALNAME));
        // Formato: CLIENT:x,y,status,pickupX,pickupY,deliveryX,deliveryY
        m.setContent("CLIENT:" + pickup.worldX + "," + pickup.worldY + ",ESPERANDO," + 
                    pickup.gridX + "," + pickup.gridY + "," + 
                    delivery.gridX + "," + delivery.gridY);
        m.setConversationId(orderId);
        send(m);
    }
    
    private void notifyOrderAssigned(String orderId, String vehicleName) {
        ACLMessage m = new ACLMessage(ACLMessage.INFORM);
        m.addReceiver(new AID("visualizer", AID.ISLOCALNAME));
        m.setContent("CLIENT_STATUS:" + orderId + ",EN_SERVICIO");
        send(m);
    }
    
    private void notifyOrderFailed(String orderId) {
        ACLMessage m = new ACLMessage(ACLMessage.INFORM);
        m.addReceiver(new AID("visualizer", AID.ISLOCALNAME));
        m.setContent("CLIENT_REMOVE:" + orderId);
        send(m);
    }
    
    @Override
    protected void takeDown() {
        System.out.println(getLocalName() + " (Centro de Ordenes) finalizando.");
    }
}
