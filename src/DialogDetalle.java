import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class DialogDetalle extends JDialog {

    static final Color bg_dark  = new Color(14, 22, 40);
    static final Color bg_panel = new Color(22, 35, 58);
    static final Color arena    = new Color(215, 205, 180);
    static final Color gris     = new Color(140, 145, 155);

    public DialogDetalle(Frame parent, Spot s){
        super(parent, s.nombre, true);
        setResizable(false);
        setBackground(bg_dark);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(bg_dark);
        root.add(buildFoto(s),  BorderLayout.NORTH);
        root.add(buildInfo(s),  BorderLayout.CENTER);
        root.add(buildGTA(s),   BorderLayout.SOUTH);
        add(root);

        pack();
        // nunca exceder el 92% de la pantalla en altura
        int maxH = (int)(Toolkit.getDefaultToolkit().getScreenSize().height * 0.92);
        if (getHeight() > maxH) setSize(getWidth(), maxH);
        setLocationRelativeTo(parent);
    }

    JPanel buildFoto(Spot s){
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(bg_dark);
        p.setPreferredSize(new Dimension(440, 200));

        JLabel lbl = new JLabel();
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setBackground(new Color(18, 32, 58));
        lbl.setOpaque(true);

        lbl.setIcon(cargarIcono(s.rutaFoto, 440, 200));
        p.add(lbl, BorderLayout.CENTER);
        return p;
    }

    JPanel buildInfo(Spot s){
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(bg_panel);
        p.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel lblNombre = new JLabel(s.nombre);
        lblNombre.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblNombre.setForeground(Color.WHITE);
        lblNombre.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lblNombre);
        p.add(Box.createVerticalStrut(6));

        String estrellas = "★".repeat(s.valoracion) + "☆".repeat(5 - s.valoracion);
        p.add(infoLabel("Valoración",  estrellas));
        p.add(infoLabel("País",        safe(s.pais)));
        p.add(infoLabel("Región",      safe(s.region)));
        p.add(infoLabel("Tipo fondo",  safe(s.tipoFondo)));
        p.add(infoLabel("Dirección",   safe(s.direccionOla)));
        if (s.velocidadOla != null && !s.velocidadOla.isEmpty())
            p.add(infoLabel("Velocidad",   s.velocidadOla.toUpperCase()));
        if (s.alturaOla != null && !s.alturaOla.isEmpty())
            p.add(infoLabel("Altura ola",  s.alturaOla));
        p.add(infoLabel("Nivel",       safe(s.nivelDificultad)));
        p.add(infoLabel("Tablas",      s.tablasToString().isEmpty() ? "—" : s.tablasToString()));
        p.add(infoLabel("Mejor época", safe(s.mejorEpoca)));
        p.add(infoLabel("Visitado",    s.visitado ? "Sí" : "No"));
        if (s.comentario != null && !s.comentario.isEmpty()){
            p.add(Box.createVerticalStrut(4));
            p.add(infoLabel("Comentario", s.comentario));
        }
        return p;
    }

    JLabel infoLabel(String campo, String valor){
        JLabel lbl = new JLabel("<html><div style='width:395px'><font color='#8899bb'>"
                + campo + ": </font><font color='#d7cdb4'>" + valor + "</font></div></html>");
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lbl.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    JPanel buildGTA(Spot s){
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(bg_dark);
        p.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, new Color(50, 70, 110)),
                BorderFactory.createEmptyBorder(12, 16, 14, 16)));

        JLabel lblFrase = new JLabel("<html><div style='width:395px'><i>" + getFrase(s) + "</i></div></html>");
        lblFrase.setFont(new Font("SansSerif", Font.ITALIC, 14));
        lblFrase.setForeground(Color.WHITE);
        lblFrase.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTip = new JLabel("<html><div style='width:395px'>" + getTip(s) + "</div></html>");
        lblTip.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblTip.setForeground(gris);
        lblTip.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblTip.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        JButton btnSurf = new JButton("¡Surfear!");
        btnSurf.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnSurf.addActionListener(e -> abrirJuego(s));

        p.add(lblFrase);
        p.add(lblTip);
        p.add(Box.createVerticalStrut(10));
        p.add(btnSurf);
        return p;
    }

    void abrirJuego(Spot s){
        String tablaElegida = null;
        if (s.tablas != null && s.tablas.size() > 1) {
            String[] opts = s.tablas.toArray(new String[0]);
            tablaElegida = (String) JOptionPane.showInputDialog(
                this, "¿Qué tabla quieres usar?", "Elige tu tabla",
                JOptionPane.QUESTION_MESSAGE, null, opts, opts[0]);
            if (tablaElegida == null) return; // cancelado
        } else if (s.tablas != null && !s.tablas.isEmpty()) {
            tablaElegida = s.tablas.get(0);
        }
        JDialog dlg = new JDialog((Frame) getOwner(), "¡A surfear! — " + s.nombre, true);
        PanelJuego pj = new PanelJuego(s, tablaElegida);
        dlg.add(pj);
        dlg.setSize(800, 500);
        dlg.setLocationRelativeTo(this);
        dlg.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e){
                pj.jugando = false;
            }
        });
        dlg.setVisible(true);
    }

    static String getFrase(Spot s){
        switch (s.nombre){
            case "Pipeline":      return "Where legends are made and boards are broken.";
            case "Teahupo'o":     return "Not a wave. A wall of water that wants you dead.";
            case "Mundaka":       return "The best left in Europe. Earn it.";
            case "Uluwatu":       return "Bali's temple of surf. Respect the locals.";
            case "Desert Point":  return "Six seconds of perfection. If you can handle it.";
            case "Mawi":          return "Lombok's hidden gem. Long lefts through crystal water.";
            case "Yoyo's":        return "Sumbawa's secret right. Empty barrels, zero crowds.";
            case "Castelldefels": return "Barcelona's backyard. Where surfers are born.";
            case "El Palmar":     return "Atlantic soul. Cádiz never disappoints.";
            case "Hossegor":      return "The power beach. France serves it raw and hollow.";
            case "Hookipa":       return "The windsurfer's cathedral. But surfers bow here too.";
            default:
                return (s.comentario != null && !s.comentario.isEmpty())
                        ? s.comentario : "Un spot por descubrir.";
        }
    }

    String getTip(Spot s){
        switch (s.nombre){
            case "Pipeline":      return "GUN obligatoria en días grandes (10ft+). El arrecife de coral está a menos de un metro bajo la tabla.";
            case "Teahupo'o":     return "Marea alta para los mejores tubos. El arrecife no perdona.";
            case "Mundaka":       return "Funciona con oleaje del NO y marea bajante. Paciencia.";
            case "Uluwatu":       return "Evita la zona del rip en la entrada. Respeta el turno.";
            case "Desert Point":  return "Lleva tabla de repuesto. El arrecife es despiadado.";
            case "Mawi":          return "Reef muy superficial en bajamar. Entra solo con marea media-alta. Llevar botines.";
            case "Yoyo's":        return "Spot remoto; lleva provisiones para el día. Acceso difícil fuera de temporada seca.";
            case "Castelldefels": return "Viento de Tramuntana genera las mejores condiciones. Ideal para escuelas de surf.";
            case "El Palmar":     return "Funciona mejor con swell del SO y viento de Levante o calma. Corriente lateral notable.";
            case "Hossegor":      return "Los bancos de arena cambian tras cada tormenta. Reconoce el banco del día antes de entrar.";
            case "Hookipa":       return "Comparte el agua con windsurfistas y kitesurfistas. Reglas de preferencia estrictas.";
            default:              return "Consulta las previsiones locales antes de entrar.";
        }
    }

    Icon cargarIcono(String ruta, int maxW, int maxH){
        if (ruta == null || ruta.isEmpty()) return new PlaceholderIcon(maxW, maxH);
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(ruta);
            if (is == null && new File(ruta).exists()) is = new FileInputStream(ruta);
            if (is != null){
                BufferedImage orig = ImageIO.read(is);
                double scale = Math.min((double) maxW / orig.getWidth(), (double) maxH / orig.getHeight());
                int sw = (int) (orig.getWidth()  * scale);
                int sh = (int) (orig.getHeight() * scale);
                return new ImageIcon(orig.getScaledInstance(sw, sh, Image.SCALE_SMOOTH));
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return new PlaceholderIcon(maxW, maxH);
    }

    String safe(String s){
        return (s != null && !s.isEmpty()) ? s : "—";
    }
}
