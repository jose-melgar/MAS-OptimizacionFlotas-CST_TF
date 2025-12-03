package swarmintelligence;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;

/**
 * GuiAgent: agente simple que crea la ventana MainFrame al iniciarse.
 * Permite que AmbientePanel (que lee VisualiserAgent.vehicles) se muestre y actualice.
 */
public class GuiAgent extends Agent {
    private MainFrame frame;

    @Override
    protected void setup() {
        // Crear y mostrar la GUI en el hilo de Swing
        try {
            javax.swing.SwingUtilities.invokeLater(() -> {
                frame = new MainFrame();
                frame.setVisible(true);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Ticker para repintar el panel cada 16 ms (~60 FPS) para animaciones fluidas
        addBehaviour(new TickerBehaviour(this, 16) {
            @Override
            protected void onTick() {
                if (MainFrame.panel_principal != null) {
                    MainFrame.panel_principal.repaint();
                }
            }
        });

        System.out.println(getLocalName() + " inicializado y GUI mostrada.");
    }

    @Override
    protected void takeDown() {
        if (frame != null) {
            javax.swing.SwingUtilities.invokeLater(() -> frame.dispose());
        }
        System.out.println(getLocalName() + " terminando.");
    }
}