package swarmintelligence;

import java.awt.*;
import java.awt.geom.*;
import java.util.Map;
import javax.swing.JPanel;

/**
 * AmbientePanel mejorado con diseño moderno y efectos visuales profesionales
 */
public class AmbientePanel extends JPanel {
    private static final int PADDING_PX = 60;
    
    // Colores modernos
    private static final Color BG_DARK = new Color(18, 18, 24);
    private static final Color BG_GRADIENT_END = new Color(28, 28, 38);
    private static final Color STREET_COLOR = new Color(60, 60, 75);
    private static final Color STREET_LINE_COLOR = new Color(80, 80, 100);
    private static final Color AVAILABLE_COLOR = new Color(46, 213, 115);
    private static final Color BUSY_COLOR = new Color(255, 71, 87);
    private static final Color CLIENT_WAITING_COLOR = new Color(255, 193, 7);
    private static final Color CLIENT_RIDING_COLOR = new Color(33, 150, 243);
    private static final Color UNKNOWN_COLOR = new Color(255, 168, 1);
    private static final Color TEXT_COLOR = new Color(240, 240, 250);
    private static final Color ACCENT_COLOR = new Color(108, 92, 231);

    public AmbientePanel() {
        super();
        setOpaque(true);
        setPreferredSize(new Dimension(900, 600));
        setBackground(BG_DARK);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Activar antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Fondo con gradiente
        GradientPaint gradient = new GradientPaint(0, 0, BG_DARK, 0, getHeight(), BG_GRADIENT_END);
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        Map<String, VisualiserAgent.VehicleState> vehicles = VisualiserAgent.vehicles;
        Map<String, VisualiserAgent.ClientState> clients = VisualiserAgent.clients;

        // Calcular escala para ajustar el mapa completo de ciudad
        int worldWidth = CityMap.WORLD_WIDTH;
        int worldHeight = CityMap.WORLD_HEIGHT;
        
        int availW = Math.max(10, getWidth() - 2 * PADDING_PX);
        int availH = Math.max(10, getHeight() - 2 * PADDING_PX);

        double scaleX = (double) availW / (double) worldWidth;
        double scaleY = (double) availH / (double) worldHeight;
        double scale = Math.min(scaleX, scaleY);

        double worldCenterX = worldWidth / 2.0;
        double worldCenterY = worldHeight / 2.0;
        double screenCenterX = getWidth() / 2.0;
        double screenCenterY = getHeight() / 2.0;

        // Dibujar mapa de calles
        drawCityStreets(g2d, worldCenterX, worldCenterY, screenCenterX, screenCenterY, scale);
        
        // Dibujar ordenes activas (lineas de pickup a delivery)
        drawActiveOrders(g2d, worldCenterX, worldCenterY, screenCenterX, screenCenterY, scale);

        // Dibujar clientes primero (debajo de vehiculos)
        if (clients != null) {
            for (Map.Entry<String, VisualiserAgent.ClientState> e : clients.entrySet()) {
                VisualiserAgent.ClientState c = e.getValue();
                if (c == null) continue;
                
                int sx = (int) Math.round(screenCenterX + (c.x - worldCenterX) * scale);
                int sy = (int) Math.round(screenCenterY + (c.y - worldCenterY) * scale);
                
                Color clientColor = "EN_VIAJE".equals(c.status) ? CLIENT_RIDING_COLOR : CLIENT_WAITING_COLOR;
                drawClient(g2d, sx, sy, e.getKey(), clientColor, scale);
            }
        }

        // Dibujar vehiculos con interpolacion
        int disponibles = 0, ocupados = 0;
        if (vehicles != null) {
            for (VisualiserAgent.VehicleState s : vehicles.values()) {
                if (s != null) {
                    s.interpolate(2.0); // velocidad de interpolacion reducida
                }
            }
            
            for (Map.Entry<String, VisualiserAgent.VehicleState> e : vehicles.entrySet()) {
                VisualiserAgent.VehicleState s = e.getValue();
                if (s == null) continue;

                int sx = (int) Math.round(screenCenterX + (s.x - worldCenterX) * scale);
                int sy = (int) Math.round(screenCenterY + (s.y - worldCenterY) * scale);

                Color vehicleColor;
                if ("DISPONIBLE".equalsIgnoreCase(s.status)) {
                    vehicleColor = AVAILABLE_COLOR;
                    disponibles++;
                } else if ("OCUPADO".equalsIgnoreCase(s.status)) {
                    vehicleColor = BUSY_COLOR;
                    ocupados++;
                } else {
                    vehicleColor = UNKNOWN_COLOR;
                }

                drawVehicle(g2d, sx, sy, e.getKey(), vehicleColor, scale);
            }
        }

        // Panel de estadisticas
        int totalClients = clients != null ? clients.size() : 0;
        int totalVehicles = vehicles != null ? vehicles.size() : 0;
        drawStatsPanel(g2d, totalVehicles, disponibles, ocupados, totalClients, scale);
    }
    
    /**
     * Dibujar las ordenes activas con linea de pickup a delivery
     */
    private void drawActiveOrders(Graphics2D g2d, double worldCenterX, double worldCenterY,
                                   double screenCenterX, double screenCenterY, double scale) {
        Map<String, VisualiserAgent.ClientState> clients = VisualiserAgent.clients;
        if (clients == null) return;
        
        for (VisualiserAgent.ClientState client : clients.values()) {
            if (client != null && client.pickup != null && client.delivery != null) {
                // Convertir coordenadas de grid a pantalla
                double pickupWorldX = client.pickup.gridX * CityMap.BLOCK_SIZE;
                double pickupWorldY = client.pickup.gridY * CityMap.BLOCK_SIZE;
                double deliveryWorldX = client.delivery.gridX * CityMap.BLOCK_SIZE;
                double deliveryWorldY = client.delivery.gridY * CityMap.BLOCK_SIZE;
                
                int pickupX = (int) Math.round(screenCenterX + (pickupWorldX - worldCenterX) * scale);
                int pickupY = (int) Math.round(screenCenterY + (pickupWorldY - worldCenterY) * scale);
                int deliveryX = (int) Math.round(screenCenterX + (deliveryWorldX - worldCenterX) * scale);
                int deliveryY = (int) Math.round(screenCenterY + (deliveryWorldY - worldCenterY) * scale);
                
                // Linea punteada de pickup a delivery
                g2d.setColor(new Color(255, 165, 0, 150)); // Naranja semi-transparente
                Stroke oldStroke = g2d.getStroke();
                float dashSize = Math.max(5f, (float)(10 * scale));
                float[] dashPattern = {dashSize, dashSize};
                g2d.setStroke(new BasicStroke(Math.max(2f, (float)(3 * scale)), BasicStroke.CAP_ROUND, 
                                              BasicStroke.JOIN_ROUND, 1.0f, dashPattern, 0f));
                g2d.drawLine(pickupX, pickupY, deliveryX, deliveryY);
                
                // Marcador de pickup (circulo verde)
                int markerSize = Math.max(12, (int)(16 * scale));
                g2d.setStroke(new BasicStroke(Math.max(2f, (float)(2 * scale))));
                g2d.setColor(new Color(0, 255, 100));
                g2d.fillOval(pickupX - markerSize/2, pickupY - markerSize/2, markerSize, markerSize);
                g2d.setColor(new Color(0, 180, 70));
                g2d.drawOval(pickupX - markerSize/2, pickupY - markerSize/2, markerSize, markerSize);
                
                // Etiqueta "P" de pickup
                g2d.setColor(Color.WHITE);
                int fontSize = Math.max(10, (int)(12 * scale));
                g2d.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
                FontMetrics fm = g2d.getFontMetrics();
                String pickupLabel = "P";
                int labelWidth = fm.stringWidth(pickupLabel);
                int labelHeight = fm.getAscent();
                g2d.drawString(pickupLabel, pickupX - labelWidth/2, pickupY + labelHeight/3);
                
                // Marcador de delivery (circulo azul)
                g2d.setColor(new Color(100, 150, 255));
                g2d.fillOval(deliveryX - markerSize/2, deliveryY - markerSize/2, markerSize, markerSize);
                g2d.setColor(new Color(70, 120, 220));
                g2d.drawOval(deliveryX - markerSize/2, deliveryY - markerSize/2, markerSize, markerSize);
                
                // Etiqueta "D" de delivery
                g2d.setColor(Color.WHITE);
                String deliveryLabel = "D";
                labelWidth = fm.stringWidth(deliveryLabel);
                g2d.drawString(deliveryLabel, deliveryX - labelWidth/2, deliveryY + labelHeight/3);
                
                g2d.setStroke(oldStroke);
            }
        }
    }
    
    private void drawCityStreets(Graphics2D g2d, double worldCenterX, double worldCenterY,
                                  double screenCenterX, double screenCenterY, double scale) {
        g2d.setColor(STREET_COLOR);
        int streetWidth = Math.max(1, (int) (CityMap.STREET_WIDTH * scale));
        
        // Dibujar calles horizontales
        for (int gy = 0; gy <= CityMap.BLOCKS_Y; gy++) {
            int wy = gy * CityMap.BLOCK_SIZE;
            int sy = (int) Math.round(screenCenterY + (wy - worldCenterY) * scale);
            g2d.fillRect(0, sy - streetWidth/2, getWidth(), streetWidth);
        }
        
        // Dibujar calles verticales
        for (int gx = 0; gx <= CityMap.BLOCKS_X; gx++) {
            int wx = gx * CityMap.BLOCK_SIZE;
            int sx = (int) Math.round(screenCenterX + (wx - worldCenterX) * scale);
            g2d.fillRect(sx - streetWidth/2, 0, streetWidth, getHeight());
        }
        
        // Dibujar lineas de carril (opcional, solo si hay espacio)
        if (streetWidth > 6) {
            g2d.setColor(STREET_LINE_COLOR);
            g2d.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5, 5}, 0));
            
            for (int gy = 0; gy <= CityMap.BLOCKS_Y; gy++) {
                int wy = gy * CityMap.BLOCK_SIZE;
                int sy = (int) Math.round(screenCenterY + (wy - worldCenterY) * scale);
                g2d.drawLine(0, sy, getWidth(), sy);
            }
            
            for (int gx = 0; gx <= CityMap.BLOCKS_X; gx++) {
                int wx = gx * CityMap.BLOCK_SIZE;
                int sx = (int) Math.round(screenCenterX + (wx - worldCenterX) * scale);
                g2d.drawLine(sx, 0, sx, getHeight());
            }
        }
    }
    
    private void drawClient(Graphics2D g2d, int x, int y, String name, Color color, double scale) {
        int size = Math.max(10, (int) Math.round(12 * Math.min(1.5, scale / 5.0)));
        
        // Sombra
        g2d.setColor(new Color(0, 0, 0, 60));
        g2d.fillOval(x - size/2 + 2, y - size/2 + 2, size, size);
        
        // Cliente como persona (circulo con cuerpo simplificado)
        g2d.setColor(color);
        g2d.fillOval(x - size/2, y - size/2, size, size);
        
        // Borde
        g2d.setColor(color.brighter());
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawOval(x - size/2, y - size/2, size, size);
        
        // Icono de persona (simplificado)
        g2d.setColor(Color.WHITE);
        int headSize = size / 3;
        g2d.fillOval(x - headSize/2, y - headSize/2 - size/6, headSize, headSize);
        
        // Etiqueta (solo iniciales para ahorrar espacio)
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 9));
        FontMetrics fm = g2d.getFontMetrics();
        String label = name.length() > 2 ? name.substring(0, 2) : name;
        int labelWidth = fm.stringWidth(label);
        int labelX = x - labelWidth/2;
        int labelY = y + size/2 + 12;
        
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(labelX - 2, labelY - fm.getHeight() + 3, labelWidth + 4, fm.getHeight(), 3, 3);
        
        g2d.setColor(TEXT_COLOR);
        g2d.drawString(label, labelX, labelY);
    }

    private void drawVehicle(Graphics2D g2d, int x, int y, String name, Color color, double scale) {
        int size = Math.max(16, (int) Math.round(20 * Math.min(1.5, scale / 5.0)));
        
        // Sombra
        g2d.setColor(new Color(0, 0, 0, 60));
        g2d.fillOval(x - size/2 + 2, y - size/2 + 2, size, size);
        
        // Halo brillante
        int haloSize = size + 8;
        Color haloColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 40);
        g2d.setColor(haloColor);
        g2d.fillOval(x - haloSize/2, y - haloSize/2, haloSize, haloSize);
        
        // Vehículo principal
        g2d.setColor(color);
        g2d.fillOval(x - size/2, y - size/2, size, size);
        
        // Borde brillante
        g2d.setColor(color.brighter());
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawOval(x - size/2, y - size/2, size, size);
        
        // Punto central
        g2d.setColor(Color.WHITE);
        int dotSize = size / 4;
        g2d.fillOval(x - dotSize/2, y - dotSize/2, dotSize, dotSize);
        
        // Etiqueta con fondo
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 11));
        FontMetrics fm = g2d.getFontMetrics();
        int labelWidth = fm.stringWidth(name);
        int labelHeight = fm.getHeight();
        int labelX = x + size/2 + 6;
        int labelY = y + labelHeight/4;
        
        // Fondo semi-transparente
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(labelX - 3, labelY - labelHeight + 3, labelWidth + 6, labelHeight, 5, 5);
        
        // Texto
        g2d.setColor(TEXT_COLOR);
        g2d.drawString(name, labelX, labelY);
    }

    private void drawStatsPanel(Graphics2D g2d, int total, int disponibles, int ocupados, int totalClients, double scale) {
        int panelWidth = 300;
        int panelHeight = 190;
        int padding = 20;
        int x = getWidth() - panelWidth - padding;
        int y = padding;
        
        // Fondo con transparencia y borde
        g2d.setColor(new Color(28, 28, 38, 220));
        g2d.fillRoundRect(x, y, panelWidth, panelHeight, 15, 15);
        
        g2d.setColor(ACCENT_COLOR);
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRoundRect(x, y, panelWidth, panelHeight, 15, 15);
        
        // Titulo
        g2d.setColor(ACCENT_COLOR);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 16));
        g2d.drawString("EMPRESA DE PAQUETERIA", x + 15, y + 25);
        
        // Separador
        g2d.setColor(new Color(108, 92, 231, 100));
        g2d.drawLine(x + 15, y + 35, x + panelWidth - 15, y + 35);
        
        // Stats
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        int lineY = y + 55;
        int lineSpacing = 25;
        
        drawStatLine(g2d, x + 15, lineY, "Flota Total:", String.valueOf(total), TEXT_COLOR);
        drawStatLine(g2d, x + 15, lineY + lineSpacing, "Disponibles:", String.valueOf(disponibles), AVAILABLE_COLOR);
        drawStatLine(g2d, x + 15, lineY + lineSpacing * 2, "En Servicio:", String.valueOf(ocupados), BUSY_COLOR);
        drawStatLine(g2d, x + 15, lineY + lineSpacing * 3, "Ordenes Activas:", String.valueOf(totalClients), CLIENT_WAITING_COLOR);
        
        // Info adicional
        g2d.setColor(new Color(240, 240, 250, 180));
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2d.drawString("Nueva orden cada 4 segundos", x + 15, y + panelHeight - 10);
        
        // Mapa info
        g2d.setColor(new Color(240, 240, 250, 180));
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2d.drawString("Mapa: " + CityMap.BLOCKS_X + "x" + CityMap.BLOCKS_Y + " cuadras", 20, getHeight() - 20);
    }

    private void drawStatLine(Graphics2D g2d, int x, int y, String label, String value, Color valueColor) {
        g2d.setColor(TEXT_COLOR);
        g2d.drawString(label, x, y);
        
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2d.setColor(valueColor);
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(value, x + 180, y);
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    }
}