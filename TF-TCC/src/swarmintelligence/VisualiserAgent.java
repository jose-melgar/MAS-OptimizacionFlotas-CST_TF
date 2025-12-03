package swarmintelligence;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * VisualiserAgent
 * Recibe INFORMs de VehicleAgents con su posición/estado y las guarda en un mapa público.
 * Mensaje esperado (INFORM): "POS:x,y,STATUS"
 */
public class VisualiserAgent extends Agent {
    public static class VehicleState {
        public int x;
        public int y;
        public String status;
        public VehicleState(int x, int y, String status) { this.x = x; this.y = y; this.status = status; }
    }

    // Mapa estático público para que la GUI lo pueda leer
    public static final Map<String, VehicleState> vehicles = new ConcurrentHashMap<>();

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " listo para recibir actualizaciones de vehículos.");
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage m = receive();
                if (m == null) {
                    block();
                    return;
                }
                if (m.getPerformative() == ACLMessage.INFORM) {
                    String content = m.getContent();
                    try {
                        if (content != null && content.startsWith("POS:")) {
                            String body = content.substring(4);
                            String[] parts = body.split(",");
                            int x = Integer.parseInt(parts[0].trim());
                            int y = Integer.parseInt(parts[1].trim());
                            String status = parts.length > 2 ? parts[2].trim() : "DESCONOCIDO";
                            String sender = m.getSender().getLocalName();
                            vehicles.put(sender, new VehicleState(x, y, status));
                        }
                    } catch (Exception ex) {
                        System.err.println("Visualiser: formato inválido en INFORM: " + content);
                    }
                }
            }
        });
    }

    @Override
    protected void takeDown() {
        vehicles.clear();
        System.out.println(getLocalName() + " terminando.");
    }
}