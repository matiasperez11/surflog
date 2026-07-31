import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Carga de imágenes compartida — antes cada clase (DialogDetalle, PanelMapa,
 * ImageRenderer, PanelJuego) tenía su propia copia casi idéntica de esta
 * lógica, con el orden de búsqueda (classpath vs fichero) ligeramente
 * distinto entre unas y otras.
 */
public class ImagenUtil {

    /** Busca primero en el classpath y luego como fichero en disco. */
    public static BufferedImage leer(String ruta) throws Exception {
        try (InputStream is = abrir(ruta)) {
            return is != null ? ImageIO.read(is) : null;
        }
    }

    /** Icono escalado para miniaturas (tabla, ficha de detalle); PlaceholderIcon si no hay foto. */
    public static Icon cargarIcono(String ruta, int maxW, int maxH) {
        if (ruta == null || ruta.isEmpty()) return new PlaceholderIcon(maxW, maxH);
        try {
            BufferedImage orig = leer(ruta);
            if (orig != null) {
                double scale = Math.min((double) maxW / orig.getWidth(), (double) maxH / orig.getHeight());
                int sw = (int) (orig.getWidth()  * scale);
                int sh = (int) (orig.getHeight() * scale);
                return new ImageIcon(orig.getScaledInstance(sw, sh, Image.SCALE_SMOOTH));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new PlaceholderIcon(maxW, maxH);
    }

    private static InputStream abrir(String ruta) throws Exception {
        InputStream is = ImagenUtil.class.getClassLoader().getResourceAsStream(ruta);
        if (is == null) {
            File f = new File(ruta);
            if (f.exists()) is = new FileInputStream(f);
        }
        return is;
    }
}
