import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashSet;

public class Surfista {

    // volatile: x/y/alto los escribe el hilo del juego y los lee paintComponent()
    // en el EDT (además, actualizarAltoSurfista() escribe "alto" desde los dos
    // hilos), así que necesitan garantía de visibilidad entre hilos.
    public volatile int x, y, ancho, alto;
    public volatile double vx = 0, vy = 0;
    public double bwMult = 1.0;

    // acumuladores sub-píxel para que velocidades pequeñas muevan al surfista
    double dpx = 0, dpy = 0;

    static final double ACEL_DERECHA = 0.50;
    static final double ACEL_IZQ     = 0.36;
    static final double FRICCION     = 0.90;
    static final double FRICCION_RAPIDO = 0.94;
    static final double ACEL_SUBIR   = 0.40;
    static final double ACEL_BAJAR   = 0.26;

    public Surfista(int x, int y, int ancho, int alto) {
        this.x     = x;
        this.y     = y;
        this.ancho = ancho;
        this.alto  = alto;
    }

    public Rectangle getTablaBounds(){
        double s  = alto / 90.0;
        int bw    = Math.max(5, (int)(22 * s * bwMult));
        int by    = y + (int)(13 * s);
        int bh    = Math.max(1, (int)(2  * s));
        return new Rectangle(x - bw, by, bw * 2, bh);
    }

    public void paint(Graphics g) {
        double s = alto / 90.0;
        int hr   = Math.max(2, (int)(5  * s));
        int hcy  = y - (int)(17 * s);
        int tt   = y - (int)(12 * s);
        int tb   = y + (int)(2  * s);
        int ay   = y - (int)(8  * s);
        int ax   = Math.max(3, (int)(12 * s));
        int lb   = y + (int)(14 * s);
        int lx   = Math.max(2, (int)(7  * s));
        int bw   = Math.max(5, (int)(22 * s * bwMult));
        int by   = y + (int)(13 * s);
        int bh   = Math.max(1, (int)(2  * s));
        g.setColor(new Color(30, 30, 30));
        g.fillOval(x - hr, hcy - hr, hr * 2, hr * 2);
        g.drawLine(x,    tt,  x,    tb);
        g.drawLine(x-ax, ay,  x+ax, ay);
        g.drawLine(x,    tb,  x-lx, lb);
        g.drawLine(x,    tb,  x+lx, lb);
        g.setColor(Color.BLACK);
        g.fillRoundRect(x - bw, by, bw * 2, bh, 3, 3);
    }

    // flechas + WASD — igual que acelerar() en Pelota
    public boolean hayInput(HashSet<Integer> teclas) {
        return teclas.contains(KeyEvent.VK_LEFT)  || teclas.contains(KeyEvent.VK_A)
            || teclas.contains(KeyEvent.VK_RIGHT) || teclas.contains(KeyEvent.VK_D)
            || teclas.contains(KeyEvent.VK_UP)    || teclas.contains(KeyEvent.VK_W)
            || teclas.contains(KeyEvent.VK_DOWN)  || teclas.contains(KeyEvent.VK_S);
    }

    public double velocidad(){
        return Math.sqrt(vx * vx + vy * vy);
    }

    public void acelerarEnOla(HashSet<Integer> teclas, double vel){
        if (teclas.contains(KeyEvent.VK_RIGHT) || teclas.contains(KeyEvent.VK_D))
            vx += ACEL_DERECHA;
        else if (teclas.contains(KeyEvent.VK_LEFT) || teclas.contains(KeyEvent.VK_A))
            vx -= ACEL_IZQ;

        if (teclas.contains(KeyEvent.VK_UP) || teclas.contains(KeyEvent.VK_W)){
            double a = ACEL_SUBIR;
            if (vel > 4.5) a += 0.10;
            vy -= a;
        } else if (teclas.contains(KeyEvent.VK_DOWN) || teclas.contains(KeyEvent.VK_S))
            vy += ACEL_BAJAR;
    }

    public void mover(double vel){
        double f = vel > 4.5 ? FRICCION_RAPIDO : FRICCION;
        vx *= f;
        vy *= f;
        dpx += vx; x += (int) dpx; dpx -= (int) dpx;
        dpy += vy; y += (int) dpy; dpy -= (int) dpy;
    }

    // estela siempre hacia el tubo (-X en draw space), spread mínimo garantizado, Y suave
    public void dibujarEstela(Graphics g, int[] estelaX, int[] estelaY) {
        double s   = alto / 90.0;
        int bw     = Math.max(5, (int)(22 * s * bwMult));
        int byOff  = (int)(15 * s);
        int n      = estelaX.length;

        // estelaX[0] = posición más reciente; extender siempre hacia -X (dirección del tubo)
        // effectiveSpread: el mayor entre el spread real en esa dirección y el mínimo visual
        int naturalSpread   = estelaX[0] - estelaX[n - 1]; // positivo = trail va hacia -X (tubo)
        int effectiveSpread = Math.max(72, naturalSpread);

        Graphics2D g2 = (Graphics2D) g;
        for (int i = 0; i < n; i++) {
            int drawX = estelaX[0] - i * effectiveSpread / (n - 1);
            int drawY = estelaY[i]; // Y real → movimiento suave arriba/abajo
            int   sz    = Math.max(1, (n - i) / 5); // ovals estrechos: max ≈ 4px radio
            float alpha = (n - i) / (float) n * 0.45f;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(Color.WHITE);
            g2.fillOval(drawX - bw - sz, drawY + byOff - sz, sz * 2, sz * 2);
        }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }
}
