import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class Main extends JFrame {

    PanelPrincipal panel;

    public Main(){
        panel = new PanelPrincipal(this);
        this.add(panel);

        JMenuBar menuBar = new JMenuBar();
        JMenu menuArchivo = new JMenu("Archivo");
        JMenuItem itemImportar = new JMenuItem("Importar JSON");
        JMenuItem itemExportar = new JMenuItem("Exportar JSON");

        itemImportar.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                ArrayList<Spot> importados = PersistenciaController.cargar(fc.getSelectedFile().getAbsolutePath());
                if (importados != null && !importados.isEmpty()) {
                    panel.spots.clear();
                    panel.spots.addAll(importados);
                    panel.refrescarTabla();
                }
            }
        });
        itemExportar.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                PersistenciaController.guardar(panel.spots, fc.getSelectedFile().getAbsolutePath());
            }
        });
        menuArchivo.add(itemImportar);
        menuArchivo.add(itemExportar);
        menuBar.add(menuArchivo);
        this.setJMenuBar(menuBar);

        this.setTitle("SurfLog");
        this.setSize(PanelPrincipal.ancho_pant, PanelPrincipal.alto_pant);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        this.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e){
                PersistenciaController.guardar(panel.spots, "spots.json");
            }
        });
    }

    static void aplicarTema(){
        UIManager.put("nimbusBase",                new Color(18, 32, 58));
        UIManager.put("nimbusBlueGrey",            new Color(44, 62, 92));
        UIManager.put("control",                   new Color(26, 40, 66));
        UIManager.put("text",                      new Color(215, 205, 180));
        UIManager.put("nimbusLightBackground",     new Color(20, 34, 60));
        UIManager.put("nimbusFocus",               new Color(90, 150, 200));
        UIManager.put("nimbusSelectionBackground", new Color(70, 120, 180));
        UIManager.put("nimbusSelectedText",        new Color(255, 255, 255));
        UIManager.put("nimbusDisabledText",        new Color(110, 120, 140));
        UIManager.put("menu",                      new Color(26, 40, 66));
        UIManager.put("menuText",                  new Color(215, 205, 180));
        UIManager.put("textHighlight",             new Color(70, 120, 180));
        try {
            for (UIManager.LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()){
                if ("Nimbus".equals(laf.getName())){
                    UIManager.setLookAndFeel(laf.getClassName());
                    break;
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String args[]){
        aplicarTema();
        new Main();
    }
}
