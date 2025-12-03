package swarmintelligence;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Toolkit;
import javax.swing.JFrame;
import javax.swing.UIManager;

/**
 * MainFrame con diseño moderno y profesional
 */
public class MainFrame extends JFrame {
    public static AmbientePanel panel_principal;

    public MainFrame() {
        try {
            // Establecer look and feel del sistema
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            inicializar();
        } catch (Exception e) {
            System.err.println("exception " + e);
            e.printStackTrace();
        }
    }

    public void inicializar() {
        setTitle("Sistema Multiagente - Optimización de Flotas | JADE Framework");
        
        // maximizar el frame
        final GraphicsConfiguration config = getGraphicsConfiguration();
        final int left = Toolkit.getDefaultToolkit().getScreenInsets(config).left;
        final int right = Toolkit.getDefaultToolkit().getScreenInsets(config).right;
        final int top = Toolkit.getDefaultToolkit().getScreenInsets(config).top;
        final int bottom = Toolkit.getDefaultToolkit().getScreenInsets(config).bottom;
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final int width = screenSize.width - left - right;
        final int height = screenSize.height - top - bottom;
        setResizable(true);
        setSize(width, height);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(new BorderLayout());
        setBackground(new Color(18, 18, 24));

        panel_principal = new AmbientePanel();
        panel_principal.setBackground(new Color(18, 18, 24));

        add(panel_principal, BorderLayout.CENTER);
    }
}