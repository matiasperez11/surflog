import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

public class ImageRenderer extends DefaultTableCellRenderer {

    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column){

        JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, "", isSelected, hasFocus, row, column);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setIcon(null);

        if (value instanceof Spot){
            Spot s = (Spot) value;
            label.setIcon(ImagenUtil.cargarIcono(s.rutaFoto, 60, 40));
        }
        return label;
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
