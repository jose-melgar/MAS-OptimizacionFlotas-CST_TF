package swarmintelligence;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.util.Map;
import javax.swing.JPanel;

/**
 * AmbientePanel mejorado para "auto-fit" (escala automática) del área del mundo.
 *
 * - Calcula los límites (min/max) de las posiciones reportadas por los vehículos
 *   y ajusta la escala para que todos entren en la ventana con un margen.
 * - Si no hay vehículos, usa un tamaño de mundo por defecto.
 *
 * Reemplaza src\swarmintelligence\AmbientePanel.java por este archivo,
 * compila (javac -cp lib\jade.jar -d bin src\swarmintelligence\*.java)
 * y ejecuta run_agents.bat 5 gui (o la cantidad que uses).
 */
public class AmbientePanel extends JPanel {
    // Tamaño mundial por defecto (debe coincidir con VehicleAgent GRID_WIDTH/HEIGHT)
    public static final int DEFAULT_WORLD_WIDTH = 1500;
    public static final int DEFAULT_WORLD_HEIGHT = 1000;

    // Padding en píxeles alrededor del grupo de vehículos dentro del panel
    private static final int PADDING_PX = 40;

    public AmbientePanel() {
        super();
        setOpaque(true);
        setPreferredSize(new Dimension(900, 600)); // tamaño mínimo inicial
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Fondo
        g.setColor(new Color(230, 240, 255));
        g.fillRect(0, 0, getWidth(), getHeight());

        // Obtener estados de vehículos desde VisualiserAgent
        Map<String, VisualiserAgent.VehicleState> vehicles = VisualiserAgent.vehicles;

        // Si no hay vehículos mostramos el texto informativo
        if (vehicles == null || vehicles.isEmpty()) {
            g.setColor(Color.DARK_GRAY);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.drawString("No hay posiciones de vehículos registradas.", 10, 20);
            return;
        }

        // Calcular bounding box del conjunto de vehículos
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (VisualiserAgent.VehicleState s : vehicles.values()) {
            if (s == null) continue;
            minX = Math.min(minX, s.x);
            minY = Math.min(minY, s.y);
            maxX = Math.max(maxX, s.x);
            maxY = Math.max(maxY, s.y);
        }

        // Si por alguna razón valores inválidos, usar mundo por defecto
        if (minX == Integer.MAX_VALUE || minY == Integer.MAX_VALUE) {
            minX = 0; minY = 0; maxX = DEFAULT_WORLD_WIDTH; maxY = DEFAULT_WORLD_HEIGHT;
        }

        // Añadir un pequeño margen al bounding box en coordenadas del mundo
        int worldWidth = Math.max(1, maxX - minX);
        int worldHeight = Math.max(1, maxY - minY);

        // Convertir padding en píxeles a padding en coordenadas del mundo (no necesario,
        // trabajamos con escala pixeles <- mundo). Mejor: calculamos escala en px por unidad mundo.
        int availW = Math.max(10, getWidth() - 2 * PADDING_PX);
        int availH = Math.max(10, getHeight() - 2 * PADDING_PX);

        double scaleX = (double) availW / (double) worldWidth;
        double scaleY = (double) availH / (double) worldHeight;
        double scale = Math.min(scaleX, scaleY);

        // Si el bounding box es muy pequeño (todos los vehículos cerca) ampliamos un poco
        if (worldWidth < 50) scale = Math.min(scale, (double) availW / 100.0);
        if (worldHeight < 50) scale = Math.min(scale, (double) availH / 100.0);

        // Coordenada de origen en pixeles (esquina superior izquierda del mundo visible)
        double worldCenterX = (minX + maxX) / 2.0;
        double worldCenterY = (minY + maxY) / 2.0;

        double screenCenterX = getWidth() / 2.0;
        double screenCenterY = getHeight() / 2.0;

        // Transformación: world -> screen
        // screenX = screenCenterX + (worldX - worldCenterX) * scale
        // screenY = screenCenterY + (worldY - worldCenterY) * scale
        // (Esto centra el bounding box en el panel)
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));

        // Dibujar grid ligero relativo al mundo visible (opcional)
        g.setColor(new Color(200, 200, 200));
        int gridStepWorld = Math.max(50, (int) Math.round(100.0 / scale)); // intenta mantener grid legible
        if (gridStepWorld > 0) {
            // líneas verticales
            int startX = ((minX / gridStepWorld) - 1) * gridStepWorld;
            for (int wx = startX; wx <= maxX + gridStepWorld; wx += gridStepWorld) {
                int sx = (int) Math.round(screenCenterX + (wx - worldCenterX) * scale);
                g.drawLine(sx, 0, sx, getHeight());
            }
            // líneas horizontales
            int startY = ((minY / gridStepWorld) - 1) * gridStepWorld;
            for (int wy = startY; wy <= maxY + gridStepWorld; wy += gridStepWorld) {
                int sy = (int) Math.round(screenCenterY + (wy - worldCenterY) * scale);
                g.drawLine(0, sy, getWidth(), sy);
            }
        }

        // Dibujar cada vehículo transformando coordenadas
        for (Map.Entry<String, VisualiserAgent.VehicleState> e : vehicles.entrySet()) {
            VisualiserAgent.VehicleState s = e.getValue();
            if (s == null) continue;

            int sx = (int) Math.round(screenCenterX + (s.x - worldCenterX) * scale);
            int sy = (int) Math.round(screenCenterY + (s.y - worldCenterY) * scale);

            // Color por estado
            if ("DISPONIBLE".equalsIgnoreCase(s.status)) g.setColor(Color.GREEN.darker());
            else if ("OCUPADO".equalsIgnoreCase(s.status)) g.setColor(Color.RED.darker());
            else g.setColor(Color.ORANGE);

            int w = Math.max(6, (int) Math.round(8 * scale / 10.0)); // tamaño relativo a escala
            int h = Math.max(6, (int) Math.round(8 * scale / 10.0));
            // dibujar rectángulo del vehículo
            g.fillRect(sx - w/2, sy - h/2, w, h);

            // etiqueta con el nombre (a la derecha)
            g.setColor(Color.BLACK);
            g.drawString(e.getKey(), sx + w/2 + 4, sy + h/2);
        }

        // Leyenda / info en la esquina
        g.setColor(Color.BLACK);
        String info = String.format("Vehículos: %d  Escala: %.2f px/unidad", vehicles.size(), scale);
        g.drawString(info, 10, Math.max(20, getHeight() - 10));
    }
}