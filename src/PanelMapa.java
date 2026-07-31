import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class PanelMapa extends JPanel {

    static final Color pin_color = new Color(240, 80, 50);
    static final Color sombra    = new Color(14, 22, 40);
    static final int   line_dx   = 22;
    static final int   line_dy   = 26;
    static final int   orig_w    = 1688;
    static final int   orig_h    = 769;

    ArrayList<Spot> spots;
    BufferedImage imgMapa;

    PanelMapa(ArrayList<Spot> spots){
        this.spots = spots;
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("resources/img/map.png");
            if (is == null){
                File f = new File("resources/img/map.png");
                if (f.exists()) is = new FileInputStream(f);
            }
            if (is != null) imgMapa = ImageIO.read(is);
        } catch (Exception e){ e.printStackTrace(); }

        addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){
                FontMetrics fm = getFontMetrics(new Font("SansSerif", Font.BOLD, 10));
                for (Spot s : spots){
                    if (s.mapX == 0 && s.mapY == 0) continue;
                    int px = pinX(s);
                    int py = pinY(s);
                    int[] d = pinDelta(s.nombre);
                    int lx  = px + d[0];
                    int ly  = py + d[1];
                    int tw  = fm.stringWidth(s.nombre);
                    int tx  = d[0] >= 0 ? lx + 3 : lx - tw - 3;
                    int ty  = ly - fm.getAscent() / 2;
                    if (e.getX() >= tx && e.getX() <= tx + tw + 6 &&
                        e.getY() >= ty && e.getY() <= ty + 15){
                        Frame owner = (Frame) SwingUtilities.getWindowAncestor(PanelMapa.this);
                        new DialogDetalle(owner, s).setVisible(true);
                        break;
                    }
                }
            }
        });
    }

    int pinX(Spot s){ return (int)(s.mapX / orig_w * getWidth());  }
    int pinY(Spot s){ return (int)(s.mapY / orig_h * getHeight()); }

    int[] pinDelta(String nombre){
        switch (nombre){
            // Pacífico — borde izquierdo, línea hacia la derecha
            case "Pipeline":      return new int[]{  26, -20 };
            case "Hookipa":       return new int[]{  34,  22 };
            case "Teahupo'o":     return new int[]{  28, -20 };
            // Europa
            case "Mundaka":       return new int[]{ -38,  34 };
            case "Hossegor":      return new int[]{  -4, -30 };
            case "Castelldefels": return new int[]{  28, -22 };
            case "El Palmar":     return new int[]{  26,  22 };
            // Indonesia
            case "Uluwatu":       return new int[]{  52,  44 };
            case "Desert Point":  return new int[]{  -6, -32 };
            case "Mawi":          return new int[]{  18, -22 };
            case "Yoyo's":        return new int[]{  44,  -8 };
            default:              return new int[]{ -line_dx, -line_dy };
        }
    }

    public void paintComponent(Graphics g){
        super.paintComponent(g);
        if (imgMapa != null)
            g.drawImage(imgMapa, 0, 0, getWidth(), getHeight(), this);
        else {
            g.setColor(new Color(10, 30, 70));
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        FontMetrics fm = g.getFontMetrics();
        for (Spot s : spots){
            if (s.mapX == 0 && s.mapY == 0) continue;
            int px = pinX(s);
            int py = pinY(s);
            int[] d = pinDelta(s.nombre);
            int lx  = px + d[0];
            int ly  = py + d[1];
            int tw  = fm.stringWidth(s.nombre);
            int tx  = d[0] >= 0 ? lx + 3 : lx - tw - 3;
            int ty  = ly + fm.getAscent() / 2;
            g.setColor(sombra);
            g.drawLine(px+1, py+1, lx+1, ly+1);
            g.drawString(s.nombre, tx+1, ty+1);
            g.setColor(pin_color);
            g.drawLine(px, py, lx, ly);
            g.drawString(s.nombre, tx, ty);
        }
    }
}
