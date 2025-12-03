package swarmintelligence;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * VisualiserAgent mejorado - maneja vehiculos y clientes por separado
 */
public class VisualiserAgent extends Agent {
    public static class VehicleState {
        public double x;
        public double y;
        public int targetX;
        public int targetY;
        public String status;
        public long lastUpdate;
        
        public VehicleState(int x, int y, String status) { 
            this.x = x;
            this.y = y;
            this.targetX = x; 
            this.targetY = y; 
            this.status = status;
            this.lastUpdate = System.currentTimeMillis();
        }
        
        public void updateTarget(int newX, int newY, String newStatus) {
            this.targetX = newX;
            this.targetY = newY;
            this.status = newStatus;
            this.lastUpdate = System.currentTimeMillis();
        }
        
        public void interpolate(double speed) {
            double dx = targetX - x;
            double dy = targetY - y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance > 0.5) {
                double moveAmount = Math.min(speed, distance);
                x += (dx / distance) * moveAmount;
                y += (dy / distance) * moveAmount;
            } else {
                x = targetX;
                y = targetY;
            }
        }
    }
    
    public static class ClientState {
        public int x;
        public int y;
        public String status; // ESPERANDO, EN_VIAJE, COMPLETADO
        public CityMap.Intersection pickup;
        public CityMap.Intersection delivery;
        
        public ClientState(int x, int y, String status) {
            this.x = x;
            this.y = y;
            this.status = status;
            this.pickup = null;
            this.delivery = null;
        }
        
        public ClientState(int x, int y, String status, CityMap.Intersection pickup, CityMap.Intersection delivery) {
            this.x = x;
            this.y = y;
            this.status = status;
            this.pickup = pickup;
            this.delivery = delivery;
        }
    }

    public static final Map<String, VehicleState> vehicles = new ConcurrentHashMap<>();
    public static final Map<String, ClientState> clients = new ConcurrentHashMap<>();

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " listo para recibir actualizaciones.");
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
                        if (content != null && content.startsWith("VEH:")) {
                            // Mensaje de vehiculo
                            String body = content.substring(4);
                            String[] parts = body.split(",");
                            int x = Integer.parseInt(parts[0].trim());
                            int y = Integer.parseInt(parts[1].trim());
                            String status = parts.length > 2 ? parts[2].trim() : "DESCONOCIDO";
                            String sender = m.getSender().getLocalName();
                            
                            VehicleState existing = vehicles.get(sender);
                            if (existing != null) {
                                existing.updateTarget(x, y, status);
                            } else {
                                vehicles.put(sender, new VehicleState(x, y, status));
                            }
                        } else if (content != null && content.startsWith("CLIENT:")) {
                            // Mensaje de cliente
                            String body = content.substring(7);
                            String[] parts = body.split(",");
                            int x = Integer.parseInt(parts[0].trim());
                            int y = Integer.parseInt(parts[1].trim());
                            String status = parts.length > 2 ? parts[2].trim() : "ESPERANDO";
                            String orderId = m.getConversationId(); // Usar conversationId como clave
                            if (orderId == null || orderId.isEmpty()) {
                                orderId = m.getSender().getLocalName();
                            }
                            
                            // Si el mensaje incluye pickup y delivery (formato extendido)
                            if (parts.length >= 7) {
                                int pickupX = Integer.parseInt(parts[3].trim());
                                int pickupY = Integer.parseInt(parts[4].trim());
                                int deliveryX = Integer.parseInt(parts[5].trim());
                                int deliveryY = Integer.parseInt(parts[6].trim());
                                CityMap.Intersection pickup = new CityMap.Intersection(pickupX, pickupY);
                                CityMap.Intersection delivery = new CityMap.Intersection(deliveryX, deliveryY);
                                clients.put(orderId, new ClientState(x, y, status, pickup, delivery));
                            } else {
                                clients.put(orderId, new ClientState(x, y, status));
                            }
                        } else if (content != null && content.startsWith("CLIENT_STATUS:")) {
                            // Actualizar estado de orden
                            String body = content.substring(14);
                            String[] parts = body.split(",");
                            String orderId = parts[0].trim();
                            String newStatus = parts[1].trim();
                            ClientState existing = clients.get(orderId);
                            if (existing != null) {
                                existing.status = newStatus;
                            }
                        } else if (content != null && content.startsWith("CLIENT_REMOVE:")) {
                            // Remover cliente
                            String clientName = content.substring(14).trim();
                            clients.remove(clientName);
                        }
                    } catch (Exception ex) {
                        System.err.println("Visualiser: formato invalido en INFORM: " + content);
                    }
                }
            }
        });
    }

    @Override
    protected void takeDown() {
        vehicles.clear();
        clients.clear();
        System.out.println(getLocalName() + " terminando.");
    }
}