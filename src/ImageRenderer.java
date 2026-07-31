import javax.swing.*;
import javax.swing.table.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class ImageRenderer extends DefaultTableCellRenderer {

    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column){

        JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, "", isSelected, hasFocus, row, column);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setIcon(null);

        if (value instanceof Spot){
            Spot s = (Spot) value;
            label.setIcon(cargarIcono(s.rutaFoto, 60, 40));
        }
        return label;
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
}

class PlaceholderIcon implements Icon {

    int w, h;
    static final Color BG     = new Color(18, 32, 58);
    static final Color BORDER = new Color(55, 80, 120);
    static final Color WAVE   = new Color(70, 110, 160);

    PlaceholderIcon(int w, int h){ this.w = w; this.h = h; }

    public void paintIcon(Component c, Graphics g, int x, int y){
        g.setColor(BG);
        g.fillRect(x, y, w, h);
        g.setColor(BORDER);
        g.drawRect(x, y, w - 1, h - 1);
        g.setColor(WAVE);
        int[] xs = {x+6, x+14, x+22, x+30, x+38, x+46, x+54};
        int cy = y + h / 2;
        int[] ys = {cy, cy-5, cy, cy-5, cy, cy-5, cy};
        g.drawPolyline(xs, ys, xs.length);
    }

    public int getIconWidth()  { return w; }
    public int getIconHeight() { return h; }
}
