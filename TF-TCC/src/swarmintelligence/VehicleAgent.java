package swarmintelligence;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.DFService;
import jade.lang.acl.ACLMessage;

import java.util.Random;

/**
 * VehicleAgent (guardar como VehicleAgent.java)
 * Envía INFORMs de posición al agente "visualizer" y participa en la subasta.
 */
public class VehicleAgent extends Agent {
    public int x;
    public int y;
    public boolean disponible; // true = Disponible, false = Ocupado

    private static final int GRID_WIDTH = 1500;
    private static final int GRID_HEIGHT = 1000;
    private Random rnd = new Random();

    @Override
    protected void setup() {
        x = rnd.nextInt(GRID_WIDTH);
        y = rnd.nextInt(GRID_HEIGHT);
        disponible = true;

        // Registrar servicio en el DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Transporte de Pasajeros");
        sd.setName("TransportService");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            System.out.println(getLocalName() + " registrado en DF: Transporte de Pasajeros en (" + x + "," + y + ")");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Enviar posición inicial al visualizador (si existe)
        sendPositionUpdate();

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg == null) {
                    block(); // libera CPU hasta que llegue mensaje
                    return;
                }
                switch (msg.getPerformative()) {
                    case ACLMessage.CFP:
                        String content = msg.getContent();
                        try {
                            String[] parts = content.split(",");
                            int origX = Integer.parseInt(parts[0].trim());
                            int origY = Integer.parseInt(parts[1].trim());
                            ACLMessage reply = msg.createReply();
                            if (disponible) {
                                double dist = Math.hypot(origX - x, origY - y);
                                int bid = (int) Math.round(dist);
                                reply.setPerformative(ACLMessage.PROPOSE);
                                reply.setContent(Integer.toString(bid));
                                send(reply);
                                System.out.println(getLocalName() + " propone bid=" + bid + " para solicitud " + msg.getConversationId());
                            } else {
                                reply.setPerformative(ACLMessage.REFUSE);
                                reply.setContent("ocupado");
                                send(reply);
                                System.out.println(getLocalName() + " está ocupado, envía REFUSE para " + msg.getConversationId());
                            }
                        } catch (Exception ex) {
                            System.err.println(getLocalName() + " recibió CFP con formato incorrecto: " + content);
                        }
                        break;

                    case ACLMessage.ACCEPT_PROPOSAL:
                        String c = msg.getContent();
                        try {
                            String[] p = c.split(",");
                            int destX = Integer.parseInt(p[0].trim());
                            int destY = Integer.parseInt(p[1].trim());
                            disponible = false;
                            long travelDist = (long) Math.hypot(destX - x, destY - y);
                            long travelTimeMs = Math.max(200, travelDist * 50);
                            // Informar que quedamos ocupados (actualiza GUI)
                            sendPositionUpdate();
                            addBehaviour(new WakerBehaviour(myAgent, travelTimeMs) {
                                @Override
                                protected void onWake() {
                                    x = destX;
                                    y = destY;
                                    disponible = true;
                                    // Informar llegada y liberación al visualizador
                                    sendPositionUpdate();
                                    System.out.println(getLocalName() + " terminó viaje y está disponible en (" + x + "," + y + ")");
                                }
                            });
                        } catch (Exception ex) {
                            System.err.println(getLocalName() + " recibió ACCEPT_PROPOSAL con formato incorrecto: " + c);
                        }
                        break;

                    case ACLMessage.REJECT_PROPOSAL:
                        // Rechazado: sin acción
                        break;

                    default:
                        // otros mensajes
                        break;
                }
            }
        });
    }

    // Enviar INFORM al visualizer con formato POS:x,y,STATUS
    private void sendPositionUpdate() {
        ACLMessage m = new ACLMessage(ACLMessage.INFORM);
        m.addReceiver(new AID("visualizer", AID.ISLOCALNAME));
        String status = disponible ? "DISPONIBLE" : "OCUPADO";
        m.setContent("POS:" + x + "," + y + "," + status);
        send(m);
    }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); } catch (FIPAException e) { e.printStackTrace(); }
        System.out.println(getLocalName() + " terminando.");
    }
}