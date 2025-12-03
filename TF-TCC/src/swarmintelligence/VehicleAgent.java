package swarmintelligence;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.DFService;
import jade.lang.acl.ACLMessage;

import java.util.List;
import java.util.Random;

/**
 * VehicleAgent - Vehiculo de paqueteria en movimiento constante
 * Se mueve por calles constantemente, ya sea en servicio o patrullando
 */
public class VehicleAgent extends Agent {
    private CityMap.Intersection currentIntersection;
    private boolean disponible = true;
    private TickerBehaviour currentMovementBehaviour = null; // Controlar movimiento actual
    private Random rnd = new Random();
    private String currentOrderId = null;
    private int deliveriesCompleted = 0;

    @Override
    protected void setup() {
        // Colocar vehiculo en una interseccion aleatoria
        currentIntersection = CityMap.getRandomIntersection(rnd);

        // Registrar servicio en el DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Servicio de Paqueteria");
        sd.setName("DeliveryService");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            System.out.println(getLocalName() + " (Vehiculo) iniciado en (" + 
                             currentIntersection.gridX + "," + currentIntersection.gridY + ")");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Enviar posicion inicial al visualizador
        sendPositionUpdate();
        
        // Iniciar patrullaje automatico
        startPatrolling();

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg == null) {
                    block();
                    return;
                }
                switch (msg.getPerformative()) {
                    case ACLMessage.CFP:
                        handleCFP(msg);
                        break;

                    case ACLMessage.ACCEPT_PROPOSAL:
                        handleAcceptProposal(msg);
                        break;

                    case ACLMessage.REJECT_PROPOSAL:
                        // Rechazado: sin accion
                        break;

                    default:
                        break;
                }
            }
        });
    }
    
    /**
     * Iniciar patrullaje automatico - el vehiculo se mueve constantemente
     */
    private void startPatrolling() {
        if (disponible && currentMovementBehaviour == null) {
            CityMap.Intersection randomDest = CityMap.getRandomIntersection(rnd);
            while (randomDest.equals(currentIntersection)) {
                randomDest = CityMap.getRandomIntersection(rnd);
            }
            patrolTo(randomDest);
        }
    }
    
    private void handleCFP(ACLMessage msg) {
        String content = msg.getContent();
        try {
            String[] parts = content.split(",");
            int clientGridX = Integer.parseInt(parts[0].trim());
            int clientGridY = Integer.parseInt(parts[1].trim());
            
            CityMap.Intersection clientLocation = new CityMap.Intersection(clientGridX, clientGridY);
            
            ACLMessage reply = msg.createReply();
            if (disponible) {
                // Calcular distancia Manhattan (cuadras)
                int distance = CityMap.getManhattanDistance(currentIntersection, clientLocation);
                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(Integer.toString(distance));
                send(reply);
                System.out.println(getLocalName() + " propone bid=" + distance + 
                                 " cuadras para " + msg.getConversationId());
            } else {
                reply.setPerformative(ACLMessage.REFUSE);
                reply.setContent("ocupado");
                send(reply);
            }
        } catch (Exception ex) {
            System.err.println(getLocalName() + " recibio CFP con formato incorrecto: " + content);
        }
    }
    
    private void handleAcceptProposal(ACLMessage msg) {
        String content = msg.getContent();
        try {
            String[] parts = content.split(",");
            int pickupGridX = Integer.parseInt(parts[0].trim());
            int pickupGridY = Integer.parseInt(parts[1].trim());
            int deliveryGridX = Integer.parseInt(parts[2].trim());
            int deliveryGridY = Integer.parseInt(parts[3].trim());
            
            CityMap.Intersection pickup = new CityMap.Intersection(pickupGridX, pickupGridY);
            CityMap.Intersection delivery = new CityMap.Intersection(deliveryGridX, deliveryGridY);
            
            currentOrderId = msg.getConversationId();
            disponible = false;
            
            // IMPORTANTE: Cancelar cualquier movimiento de patrullaje previo
            if (currentMovementBehaviour != null) {
                removeBehaviour(currentMovementBehaviour);
                currentMovementBehaviour = null;
            }
            
            System.out.println(getLocalName() + " asignado a orden " + currentOrderId);
            System.out.println("  -> Dirigiendose a recoger paquete en (" + pickupGridX + "," + pickupGridY + ")");
            
            sendPositionUpdate();
            
            // Fase 1: Ir a recoger el paquete
            List<CityMap.Intersection> pathToPickup = CityMap.calculatePath(currentIntersection, pickup);
            moveAlongPath(pathToPickup, () -> {
                System.out.println(getLocalName() + " RECOGIO paquete de orden " + currentOrderId);
                System.out.println("  -> Dirigiendose a entregar en (" + deliveryGridX + "," + deliveryGridY + ")");
                
                // Fase 2: Entregar el paquete
                List<CityMap.Intersection> pathToDelivery = CityMap.calculatePath(currentIntersection, delivery);
                moveAlongPath(pathToDelivery, () -> {
                    deliveriesCompleted++;
                    System.out.println(getLocalName() + " ENTREGO paquete de orden " + currentOrderId);
                    System.out.println("  Entregas completadas: " + deliveriesCompleted);
                    
                    // Notificar al cliente
                    ACLMessage completion = new ACLMessage(ACLMessage.INFORM);
                    completion.addReceiver(msg.getSender());
                    completion.setConversationId(currentOrderId);
                    completion.setContent("DELIVERED");
                    send(completion);
                    
                    currentOrderId = null;
                    disponible = true;
                    sendPositionUpdate();
                    
                    // Volver a patrullar
                    startPatrolling();
                });
            });
            
        } catch (Exception ex) {
            System.err.println(getLocalName() + " recibio ACCEPT_PROPOSAL con formato incorrecto: " + content);
        }
    }
    
    /**
     * Movimiento de patrullaje cuando el vehiculo esta disponible
     */
    private void patrolTo(CityMap.Intersection destination) {
        List<CityMap.Intersection> path = CityMap.calculatePath(currentIntersection, destination);
        moveAlongPath(path, () -> {
            if (disponible) {
                // Continuar patrullando
                startPatrolling();
            }
        });
    }
    
    private void moveAlongPath(List<CityMap.Intersection> path, Runnable onComplete) {
        if (path.size() <= 1) {
            // Ya estamos en el destino
            currentMovementBehaviour = null;
            if (onComplete != null) onComplete.run();
            return;
        }
        
        final int[] currentPathIndex = {1};
        final long stepDelay = 600; // 600ms por cuadra (velocidad reducida)
        
        TickerBehaviour moveBehaviour = new TickerBehaviour(this, stepDelay) {
            @Override
            protected void onTick() {
                if (currentPathIndex[0] < path.size()) {
                    currentIntersection = path.get(currentPathIndex[0]);
                    sendPositionUpdate();
                    currentPathIndex[0]++;
                } else {
                    stop();
                    currentMovementBehaviour = null;
                    if (onComplete != null) onComplete.run();
                }
            }
        };
        
        currentMovementBehaviour = moveBehaviour;
        addBehaviour(moveBehaviour);
    }

    // Enviar INFORM al visualizer con formato VEH:x,y,STATUS,DELIVERIES
    private void sendPositionUpdate() {
        ACLMessage m = new ACLMessage(ACLMessage.INFORM);
        m.addReceiver(new AID("visualizer", AID.ISLOCALNAME));
        String status = disponible ? "DISPONIBLE" : "EN_SERVICIO";
        m.setContent("VEH:" + currentIntersection.worldX + "," + currentIntersection.worldY + "," + 
                    status + "," + deliveriesCompleted);
        send(m);
    }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); } catch (FIPAException e) { e.printStackTrace(); }
        System.out.println(getLocalName() + " finalizando. Entregas completadas: " + deliveriesCompleted);
    }
}