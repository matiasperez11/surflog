import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Random;
import java.awt.image.BufferedImage;

public class PanelJuego extends JPanel implements Runnable {

    static final double GRAVEDAD         = 0.14;
    static final double GRAVEDAD_BAJA    = 0.04;
    static final double GRAVEDAD_PARADO  = 0.024;
    static final double GRAVEDAD_CIELO   = 0.05;
    static final double AIRE_FRICCION    = 0.992;
    static final double WAVE_DRIFT       = 0.045;
    static final double IMPULSO_LINEA    = 0.055;
    static final double RESIST_SUBIDA      = 0.98;
    static final double RESIST_SUBIDA_RAPIDA = 1.01;
    static final int    TUBO_ALPHA           = 85;
    static final float  TUBO_CAPA_EXTRA      = 0.78f;
    static final int    TUBO_OFFSET_X  = 280;
    static final int    ESPUMA_OFFSET_X = 80;
    static final double AERIAL_THRESH  = 3.0;
    static final int    TUBO_PTS       = 2;
    static final int    TRAIL_MAX     = 24;
    static final int    ESPUMA_N      = 25;
    static final double TUBO_ESCALA   = 0.85;   // tamaño del tubo (1.0 = original)
    static final double ESPUMA_ESCALA = 1.0;    // tamaño de la espuma (1.0 = original)

    Spot spot;
    public String    tablaElegida = "SHORTBOARD";
    public Thread    hilo;
    public volatile boolean jugando   = false;
    public volatile boolean game_over = false;
    // Los campos de abajo se escriben desde el hilo del juego (run()) y se leen
    // desde el EDT en paintComponent(); volatile para que esas lecturas vean
    // siempre el último valor en vez de una copia potencialmente antigua.
    public volatile int       puntuacion = 0;
    int              parado_ticks = 0;

    public Surfista surfista;
    int[] estelaX = new int[TRAIL_MAX];
    int[] estelaY = new int[TRAIL_MAX];

    HashSet<Integer> teclas = new HashSet<>();

    public volatile int ola_offset = 0;
    public int ola_vel;
    public volatile int tick = 0;

    public volatile boolean aerial_active = false;
    public int     aerial_timer  = 0;
    public volatile int     aerial_pts    = 0;
    public volatile boolean en_tubo       = false;
    public volatile int     tubo_ticks    = 0;
    public volatile boolean wipeout_volando = false;
    public volatile double  wipeout_angulo  = 0;
    public volatile boolean ola_invertida   = false;
    public volatile double  peligro_x       = 0;
    int            spawn_ticks   = 0;
    static final int SPAWN_GRACE  = 50;

    int[][] espuma = new int[ESPUMA_N][4];
    Random rand = new Random();
    BufferedImage imgOla;
    BufferedImage imgDinamismo;
    BufferedImage imgTubo;
    BufferedImage imgEspuma;
    BufferedImage mascaTubo;
    BufferedImage mascaEspuma;

    PanelJuego(Spot spot, String tablaElegida) {
        this.spot = spot;
        this.tablaElegida = tablaElegida != null ? tablaElegida : "SHORTBOARD";

        if      ("Lenta".equals(spot.velocidadOla))   ola_vel = 1;
        else if ("Rápida".equals(spot.velocidadOla))  ola_vel = 3;
        else                                           ola_vel = 2; // Media o desconocido

        this.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e)  { synchronized(teclas){ teclas.add(e.getKeyCode()); } }
            public void keyReleased(KeyEvent e) { synchronized(teclas){ teclas.remove(e.getKeyCode()); } }
        });

        this.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (game_over) reiniciar();
            }
        });

        setFocusable(true);
        requestFocus();
        cargarCapas();
        iniciar();
    }

    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(() -> colocarInicioSeguro(getWidth(), getHeight()));
    }

    void colocarInicioSeguro(int w, int h){
        if (w <= 0) w = 800;
        if (h <= 0) h = 500;
        surfista.x = (int)(w * 0.62);
        surfista.y = labioY(h) + (int)(olaH(h) * 0.25);
        surfista.vx = 0;
        surfista.vy = 0;
        for (int i = 0; i < TRAIL_MAX; i++){
            estelaX[i] = surfista.x;
            estelaY[i] = surfista.y;
        }
    }

    void iniciar() {
        jugando       = true;
        game_over     = false;
        puntuacion    = 0;
        ola_offset    = 0;
        tick          = 0;
        spawn_ticks   = 0;
        parado_ticks  = 0;
        aerial_active = false;
        aerial_timer  = 0;
        aerial_pts    = 0;
        en_tubo       = false;
        tubo_ticks    = 0;
        wipeout_volando = false;
        wipeout_angulo  = 0;
        
        if ("IZQUIERDA".equals(spot.direccionOla)) {
            ola_invertida = false;   // izquierda: surfista va a la derecha, ola desde la izquierda
        } else if ("DERECHA".equals(spot.direccionOla)) {
            ola_invertida = true;    // derecha: surfista va a la izquierda, ola desde la derecha
        } else {
            ola_invertida = rand.nextBoolean();
        }
        
        synchronized(teclas){ teclas.clear(); }
        int w0 = getWidth()  > 0 ? getWidth()  : 800;
        int h0 = getHeight() > 0 ? getHeight() : 500;
        peligro_x       = -w0; // Comienza una pantalla entera hacia atrás
        surfista = new Surfista(w0 / 2, h0 / 2, 150, 150);
        surfista.bwMult = tablaBwMult();
        colocarInicioSeguro(w0, h0);
        // generar espuma estática con semilla fija
        Random r = new Random(42L);
        for (int i = 0; i < ESPUMA_N; i++) {
            espuma[i][0] = r.nextInt(160) - 80;
            espuma[i][1] = r.nextInt(80)  - 40;
            int len = r.nextInt(11) + 10;
            double ang = r.nextDouble() * Math.PI * 2;
            espuma[i][2] = espuma[i][0] + (int)(Math.cos(ang) * len);
            espuma[i][3] = espuma[i][1] + (int)(Math.sin(ang) * len);
        }
        hilo = new Thread(this);
        hilo.start();
    }

    void reiniciar() {
        // Se llama desde el clic del ratón, en el EDT — nunca hay que bloquearlo.
        // Antes hacía Thread.sleep(100) aquí mismo cruzando los dedos para que el
        // hilo viejo hubiera terminado; ahora se espera de verdad (join) en un
        // hilo aparte y solo se reinicia cuando el viejo ya terminó del todo.
        jugando = false;
        wipeout_volando = false; // por si se reinicia a mitad de la animación de wipeout
        Thread hiloViejo = hilo;
        new Thread(() -> {
            try {
                if (hiloViejo != null) hiloViejo.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            SwingUtilities.invokeLater(() -> {
                cargarCapas();
                iniciar();
            });
        }).start();
    }

    void cargarCapas() {
        Color agua = colorAgua();
        imgOla    = procesarImg("resources/img/ola.png", Color.WHITE);
        if (imgOla == null)
            imgOla = procesarImg("resources/img/wave.png", Color.WHITE);
            
        // Procesamos dinamismo para quitar el fondo blanco y que las rayas sean blanco semi-transparente
        imgDinamismo = procesarImg("resources/img/dinamismo2.png", new Color(255, 255, 255, 60));
        
        construirTubo("resources/img/tubo.png", colorTubo(agua), TUBO_ALPHA);
        recargarEspuma();
    }

    void recargarEspuma(){
        imgEspuma     = procesarEspuma("resources/img/espuma2.png");
        mascaEspuma   = cargarMascaraEspuma("resources/img/espuma2.png");
    }

    Color colorTubo(Color agua){
        return new Color(
            Math.min(255, agua.getRed()   + 45),
            Math.min(255, agua.getGreen() + 75),
            Math.min(255, agua.getBlue()  + 95));
    }

    boolean esLienzoBlanco(int r, int gr, int b){
        return r > 248 && gr > 248 && b > 248;
    }

    boolean esGraficoTubo(int r, int gr, int b){
        if (esLienzoBlanco(r, gr, b)) return false;
        if (r < 70 && gr < 70 && b < 70) return true;
        if (b > 90 && b >= r) return true;
        // contorno teal/cyan claro (tubo solo outline en Paint)
        return gr > 100 && b > 100 && r < 200 && b >= gr - 25;
    }

    void construirTubo(String ruta, Color tinte, int alpha){
        try {
            BufferedImage orig = leerImg(ruta);
            if (orig == null) return;
            int iw = orig.getWidth(), ih = orig.getHeight();
            int argbTubo = (alpha << 24) | (tinte.getRed() << 16) | (tinte.getGreen() << 8) | tinte.getBlue();
            int argbBlanco = (Math.min(255, alpha + 60) << 24) | 0xFFFFFF; // Blanco para el contorno (un poco más visible)
            imgTubo   = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
            mascaTubo = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
            for (int py = 0; py < ih; py++){
                for (int px = 0; px < iw; px++){
                    int rgb = orig.getRGB(px, py);
                    int r  = (rgb >> 16) & 0xFF;
                    int gr = (rgb >>  8) & 0xFF;
                    int b  =  rgb        & 0xFF;
                    if (!esGraficoTubo(r, gr, b)) continue;
                    
                    // Si es un píxel negro/oscuro del contorno original en Paint
                    if (r < 70 && gr < 70 && b < 70) {
                        imgTubo.setRGB(px, py, argbBlanco);
                    } else {
                        imgTubo.setRGB(px, py, argbTubo);
                    }
                    mascaTubo.setRGB(px, py, 0xFF000000);
                }
            }
        } catch (Exception e){ e.printStackTrace(); }
    }

    boolean esPixelEspuma(int r, int gr, int b){
        if (esLienzoBlanco(r, gr, b)) return false;
        return r > 90 || (r < 80 && gr < 80 && b < 80);
    }

    BufferedImage procesarEspuma(String ruta){
        try {
            BufferedImage orig = leerImg(ruta);
            if (orig == null) return null;
            int iw = orig.getWidth(), ih = orig.getHeight();
            BufferedImage proc = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
            for (int py = 0; py < ih; py++){
                for (int px = 0; px < iw; px++){
                    int rgb = orig.getRGB(px, py);
                    int r  = (rgb >> 16) & 0xFF;
                    int gr = (rgb >>  8) & 0xFF;
                    int b  =  rgb        & 0xFF;
                    if (esPixelEspuma(r, gr, b))
                        proc.setRGB(px, py, 0xFFFFFFFF);
                }
            }
            return proc;
        } catch (Exception e){ e.printStackTrace(); return null; }
    }

    BufferedImage cargarMascaraEspuma(String ruta){
        try {
            BufferedImage orig = leerImg(ruta);
            if (orig == null) return null;
            int iw = orig.getWidth(), ih = orig.getHeight();
            BufferedImage m = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
            for (int py = 0; py < ih; py++){
                for (int px = 0; px < iw; px++){
                    int rgb = orig.getRGB(px, py);
                    int r  = (rgb >> 16) & 0xFF;
                    int gr = (rgb >>  8) & 0xFF;
                    int b  =  rgb        & 0xFF;
                    if (esPixelEspuma(r, gr, b))
                        m.setRGB(px, py, 0xFF000000);
                }
            }
            return m;
        } catch (Exception e){ e.printStackTrace(); return null; }
    }

    int olaH(int h) {
        // La ola ocupa la fracción del panel proporcional a su altura real (ref: 1.75m = panel entero)
        double scale = Math.max(0.38, Math.min(1.0, parseAltura() / 2.2));
        return (int)(h * scale);
    }

    int olaY(int h) {
        // Anclamos la unidad (ola+tubo+espuma) desde abajo: la espuma debe tocar el suelo
        // olaY es donde empieza la imagen de ola; la espuma se dibuja espumaSube(h) por encima
        return h - olaH(h) + espumaSube(h);
    }

    // Offsets proporcionales a la altura de la ola para que escalen con ella
    int espumaSube(int h)   { return (int)(olaH(h) * 0.10);  }   // 10% de la ola
    int tuboOffsetY(int h)  { return (int)(olaH(h) * 0.024); }   // ~12px en ola de panel entero

    boolean enCielo(int h){
        int labio = labioY(h);
        return surfista.getTablaBounds().y < labio;
    }

    int labioY(int h){
        return olaY(h) + (int)(olaH(h) * 0.29);
    }

    void aplicarFisicaCielo(HashSet<Integer> t){
        surfista.vy += GRAVEDAD_CIELO;
        // Si el surfista ya alcanzó el pico y está cayendo, aumentamos la gravedad para que no flote
        if (surfista.vy > 0) {
            surfista.vy += 0.09; 
        }
        surfista.vx *= AIRE_FRICCION;
        surfista.vy *= AIRE_FRICCION;
        if (t.contains(KeyEvent.VK_LEFT) || t.contains(KeyEvent.VK_A))
            surfista.vx -= 0.28;
        if (t.contains(KeyEvent.VK_RIGHT) || t.contains(KeyEvent.VK_D))
            surfista.vx += 0.32;
    }

    void aplicarFisicaOla(boolean parado, double vel, HashSet<Integer> t){
        if (parado){
            double drift = WAVE_DRIFT * (1.0 + Math.min(parado_ticks * 0.014, 3.0));
            surfista.vx -= drift;
            surfista.vy *= 0.88;
            surfista.vy += GRAVEDAD_PARADO;
        } else {
            surfista.vy += GRAVEDAD;
            if (surfista.vy > 0)
                surfista.vy += GRAVEDAD_BAJA;
            else if (surfista.vy < 0){
                double resist = RESIST_SUBIDA;
                if (vel > 6.5)      resist = 1.03;
                else if (vel > 4.5) resist = RESIST_SUBIDA_RAPIDA;
                else if (vel > 3)   resist = 1.0;
                surfista.vy *= resist;
            }
            surfista.vx += IMPULSO_LINEA;
            surfista.acelerarEnOla(t, vel);
        }
    }

    BufferedImage leerImg(String ruta) throws Exception {
        return ImagenUtil.leer(ruta);
    }

    BufferedImage procesarImg(String ruta, Color target) {
        try {
            BufferedImage orig = leerImg(ruta);
            if (orig == null) return null;
            // Ahora respeta la transparencia (Alpha) del color que le pasemos en lugar de ser 255 siempre
            int argb = (target.getAlpha() << 24) | (target.getRed() << 16) | (target.getGreen() << 8) | target.getBlue();
            BufferedImage proc = new BufferedImage(orig.getWidth(), orig.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int py = 0; py < orig.getHeight(); py++) {
                for (int px = 0; px < orig.getWidth(); px++) {
                    int rgb = orig.getRGB(px, py);
                    int r  = (rgb >> 16) & 0xFF;
                    int gr = (rgb >>  8) & 0xFF;
                    int b  =  rgb        & 0xFF;
                    proc.setRGB(px, py, (r < 50 && gr < 50 && b < 50) ? argb : 0x00000000);
                }
            }
            return proc;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    int aerialDuration() {
        switch (tablaElegida) {
            case "SHORTBOARD": return 100;
            case "FISH":       return 80;
            case "FUNBOARD":   return 31;
            case "MIDLENGTH":  return 8;
            case "LONGBOARD":  return 14;
            case "GUN":        return 4;
            default:           return 60;
        }
    }

    double tablaBwMult() {
        switch (tablaElegida) {
            case "SHORTBOARD": return 0.65;
            case "FISH":       return 0.82;
            case "FUNBOARD":   return 1.0;
            case "MIDLENGTH":  return 1.25;
            case "LONGBOARD":  return 1.6;
            case "GUN":        return 1.35;
            default:           return 1.0;
        }
    }

    double parseAltura() {
        if (spot.alturaOla == null || spot.alturaOla.isEmpty()) return 1.75;
        try {
            return Double.parseDouble(spot.alturaOla.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) { return 1.75; }
    }

    void actualizarAltoSurfista(int h){
        // Proporcional a la raíz cuadrada de la altura: surfista más pequeño en olas grandes
        double alto = h * 0.42 / Math.sqrt(Math.max(0.5, parseAltura()));
        String sn = spot.nombre != null ? spot.nombre : "";
        if (sn.equals("Pipeline") || sn.equals("Teahupo'o")) alto *= 1.30;
        surfista.alto = (int) Math.max(30, Math.min((int)(h * 0.60), (int)alto));
    }

    boolean tablaTocaMascara(BufferedImage masca, int w, int h){
        if (masca == null) return false;
        Rectangle tab = surfista.getTablaBounds();
        int mw = masca.getWidth(), mh = masca.getHeight();
        boolean tubo   = (masca == mascaTubo);
        boolean espuma = (masca == mascaEspuma);
        int step = tubo ? Math.max(1, Math.min(tab.width, tab.height) / 8)
                        : Math.max(2, Math.min(tab.width, tab.height) / 5);
        int tubo_dx = tubo ? TUBO_OFFSET_X : (espuma ? ESPUMA_OFFSET_X : 0);
        int tubo_dy = tubo ? tuboOffsetY(h) : 0;
        int offY    = espuma ? espumaSube(h) : -tubo_dy;
        int oy = olaY(h);
        int oh = olaH(h);

        for (int py = tab.y; py <= tab.y + tab.height; py += step){
            for (int hitX = tab.x; hitX <= tab.x + tab.width; hitX += step){
                int relativeX = hitX - (int)peligro_x - tubo_dx;
                if (espuma && relativeX < 0) return true; // El hueco a la izquierda es espuma mortal
                
                int drawW = tubo ? (int)(w * TUBO_ESCALA) : (espuma ? (int)(w * ESPUMA_ESCALA) : w);
                int drawH = tubo ? (int)(oh * TUBO_ESCALA) : (espuma ? (int)(oh * ESPUMA_ESCALA) : oh);
                int mx = relativeX * mw / drawW;
                int relativeY = py - oy + offY;
                if (relativeY < 0) continue;
                int my = relativeY * mh / drawH;
                if (mx < 0 || mx >= mw || my < 0 || my >= mh) continue;
                if ((masca.getRGB(mx, my) & 0xFF000000) != 0) return true;
            }
        }
        return false;
    }

    Color colorAgua() {
        String n = spot.nombre != null ? spot.nombre : "";
        if (n.equals("Pipeline") || n.equals("Hookipa"))  return new Color(0,   185, 205);
        if (n.equals("Teahupo'o"))                        return new Color(0,   140, 165);
        if (n.equals("Mundaka"))                          return new Color(20,   70,  65);
        if (n.equals("Hossegor"))                         return new Color(45,  100, 110);
        if (n.equals("El Palmar"))                        return new Color(10,  140, 170);
        if (n.equals("Castelldefels"))                    return new Color(30,  160, 195);
        if (n.equals("Uluwatu"))                          return new Color(0,   175, 200);
        if (n.equals("Desert Point"))                     return new Color(0,   150, 160);
        if (n.equals("Mawi"))                             return new Color(15,  170, 165);
        if (n.equals("Yoyo's"))                           return new Color(0,   160, 170);
        return new Color(0, 140, 170);
    }

    Color colorCielo() {
        String n = spot.nombre != null ? spot.nombre : "";
        if (n.equals("Pipeline"))     return new Color(105, 178, 225);
        if (n.equals("Hookipa"))      return new Color(108, 175, 220);
        if (n.equals("Teahupo'o"))    return new Color(90,  178, 218);
        if (n.equals("Mundaka"))      return new Color(78,  112, 138);  // intencionalmente oscuro
        if (n.equals("Hossegor"))     return new Color(152, 175, 192);
        if (n.equals("El Palmar"))    return new Color(118, 178, 218);
        if (n.equals("Castelldefels"))return new Color(158, 205, 238);
        if (n.equals("Uluwatu"))      return new Color(82,  158, 225);
        if (n.equals("Desert Point")) return new Color(255, 155, 42);  // ámbar atardecer
        if (n.equals("Mawi"))         return new Color(168, 208, 178);
        if (n.equals("Yoyo's"))       return new Color(152, 195, 198);
        Color a = colorAgua();
        return new Color(Math.min(255, a.getRed()+80), Math.min(255, a.getGreen()+80), Math.min(255, a.getBlue()+80));
    }

    void dibujarLandscape(Graphics2D g2, int w, int h) {
        int labio = labioY(h);
        String n  = spot.nombre != null ? spot.nombre : "";

        if (n.equals("Mawi")) {
            // Tres rocas volcánicas formando una W — landmark icónico de Mawi, Lombok
            // Dos rocas laterales redondeadas + pico central más alto
            g2.setColor(new Color(28, 20, 14));
            int base = labio + 2;
            int rl   = Math.max(10, h / 10);   // altura rocas laterales
            int rm   = Math.max(16, h / 6);    // altura pico central (más alto)
            int v    = Math.max(3,  h / 28);   // valle poco profundo entre roca y pico

            // Franjas x de las 3 rocas, centradas en el 20%-80% de pantalla
            int lx1 = w * 22 / 100;   // inicio roca izquierda
            int lx2 = w * 36 / 100;   // fin roca izquierda / valle izq
            int mx  = w / 2;           // cima del pico central
            int rx1 = w * 64 / 100;   // valle derecho / inicio roca derecha
            int rx2 = w * 78 / 100;   // fin roca derecha

            // Perfil de la W: horizonte plano → roca izq (arco) → valle → pico → valle → roca der (arco) → horizonte
            int[] xs = {
                0,      lx1,    lx1 + (lx2-lx1)/2,  lx2,
                lx2 + (mx-lx2)/2,   mx,
                mx  + (rx1-mx)/2,   rx1,
                rx1 + (rx2-rx1)/2,  rx2,
                w,      w,      0
            };
            int[] ys = {
                base,   base,   base - rl,           base - v,
                base - rm*3/4,              base - rm,
                base - rm*3/4,              base - v,
                base - rl,                  base,
                base,   base+4, base+4
            };
            g2.fillPolygon(xs, ys, xs.length);

        } else if (n.equals("Pipeline") || n.equals("Hookipa")) {
            // Palmeras hawaianas — follaje abundante y verde vivo
            int base = labio;
            int[][] palmDefs = {
                {w/13,   Math.max(34, h*3/12),  9},
                {w*4/13, Math.max(27, h*9/50), -6},
                {w*9/13, Math.max(36, h*3/12), 13}
            };
            for (int[] pd : palmDefs) {
                int cx = pd[0], trH = pd[1], lean = pd[2];
                int crownX = cx + lean;
                int crownY = base - trH;
                int tw = Math.max(3, h / 75);
                int midTX = cx + lean / 3;
                int midTY = base - trH * 55 / 100;

                // Tronco curvo
                g2.setColor(new Color(58, 40, 16));
                int[] trXs = {cx-tw, cx+tw, midTX+tw*3/4, crownX+tw/2, crownX-tw/2, midTX-tw*3/4};
                int[] trYs = {base,  base,  midTY,         crownY,       crownY,       midTY};
                g2.fillPolygon(trXs, trYs, trXs.length);

                // Anillos del tronco
                g2.setColor(new Color(42, 28, 10));
                g2.setStroke(new BasicStroke(0.9f));
                for (int k = 1; k <= 8; k++) {
                    float t = k / 9.0f;
                    float kx = (1-t)*(1-t)*cx + 2*(1-t)*t*midTX + t*t*crownX;
                    float ky = (1-t)*(1-t)*base + 2*(1-t)*t*midTY + t*t*crownY;
                    float kw = tw * (1 - t * 0.45f);
                    g2.drawLine((int)(kx-kw), (int)ky, (int)(kx+kw), (int)ky);
                }

                // Cocos
                g2.setColor(new Color(64, 48, 18));
                int[] cocoOX = {-5, 0, 5, -3, 6, -7, 2};
                int[] cocoOY = { 4, 7, 3,  9, 8,  5, 11};
                for (int ci = 0; ci < 7; ci++)
                    g2.fillOval(crownX + cocoOX[ci] - 3, crownY + cocoOY[ci], 6, 6);

                // 12 frondas principales con follaje denso y verde vivo
                for (int fi = 0; fi < 12; fi++) {
                    double ang = Math.toRadians(-148 + fi * 27);
                    int fLen = (int)(trH * (0.58 + (fi % 4) * 0.05));
                    float endFX = crownX + (float)(Math.cos(ang) * fLen);
                    float endFY = crownY + (float)(Math.sin(ang) * fLen);
                    float cpFX  = crownX + (float)(Math.cos(ang) * fLen * 0.42);
                    float cpFY  = crownY + (float)(Math.sin(ang) * fLen * 0.42) + trH / 5.5f;
                    int green = Math.min(185, 95 + fi * 7);
                    Color leafC = new Color(10 + fi % 5, green, 15 + fi % 4);

                    // Frondillas — 11 puntos cubriendo toda la fronda
                    g2.setStroke(new BasicStroke(1.0f));
                    for (int s = 1; s <= 11; s++) {
                        float t = s / 12.0f;
                        float bx = (1-t)*(1-t)*crownX + 2*(1-t)*t*cpFX + t*t*endFX;
                        float by = (1-t)*(1-t)*crownY + 2*(1-t)*t*cpFY + t*t*endFY;
                        float tdx = 2*(1-t)*(cpFX-crownX) + 2*t*(endFX-cpFX);
                        float tdy = 2*(1-t)*(cpFY-crownY) + 2*t*(endFY-cpFY);
                        double tAng = Math.atan2(tdy, tdx);
                        int lLen = (int)(fLen * 0.18 * (1 - t * 0.48));
                        g2.setColor(new Color(Math.max(0, leafC.getRed()-4),
                                              Math.max(0, leafC.getGreen()-6), leafC.getBlue()));
                        g2.drawLine((int)bx, (int)by,
                            (int)(bx + Math.cos(tAng + Math.PI/2 - 0.22) * lLen),
                            (int)(by + Math.sin(tAng + Math.PI/2 - 0.22) * lLen));
                        g2.drawLine((int)bx, (int)by,
                            (int)(bx + Math.cos(tAng - Math.PI/2 + 0.22) * lLen),
                            (int)(by + Math.sin(tAng - Math.PI/2 + 0.22) * lLen));
                    }
                    // Eje de la fronda más grueso y brillante
                    g2.setColor(leafC);
                    g2.setStroke(new BasicStroke(2.2f));
                    g2.draw(new java.awt.geom.QuadCurve2D.Float(crownX, crownY, cpFX, cpFY, endFX, endFY));
                }

                // 6 frondas interiores cortas — segunda capa de follaje junto a la corona
                for (int fi2 = 0; fi2 < 6; fi2++) {
                    double ang2 = Math.toRadians(-108 + fi2 * 40);
                    int fLen2 = (int)(trH * 0.36);
                    float eX2  = crownX + (float)(Math.cos(ang2) * fLen2);
                    float eY2  = crownY + (float)(Math.sin(ang2) * fLen2);
                    float cpX2 = crownX + (float)(Math.cos(ang2) * fLen2 * 0.45);
                    float cpY2 = crownY + (float)(Math.sin(ang2) * fLen2 * 0.45) + trH / 8.0f;
                    Color leaf2 = new Color(18, 148 + fi2 * 5, 20);
                    g2.setStroke(new BasicStroke(1.0f));
                    for (int s = 1; s <= 7; s++) {
                        float t = s / 8.0f;
                        float bx = (1-t)*(1-t)*crownX + 2*(1-t)*t*cpX2 + t*t*eX2;
                        float by = (1-t)*(1-t)*crownY + 2*(1-t)*t*cpY2 + t*t*eY2;
                        float tdx2 = 2*(1-t)*(cpX2-crownX) + 2*t*(eX2-cpX2);
                        float tdy2 = 2*(1-t)*(cpY2-crownY) + 2*t*(eY2-cpY2);
                        double tAng2 = Math.atan2(tdy2, tdx2);
                        int lLen2 = (int)(fLen2 * 0.20 * (1 - t * 0.40));
                        g2.setColor(new Color(12, Math.max(0, leaf2.getGreen()-8), 14));
                        g2.drawLine((int)bx, (int)by,
                            (int)(bx + Math.cos(tAng2 + Math.PI/2 - 0.25) * lLen2),
                            (int)(by + Math.sin(tAng2 + Math.PI/2 - 0.25) * lLen2));
                        g2.drawLine((int)bx, (int)by,
                            (int)(bx + Math.cos(tAng2 - Math.PI/2 + 0.25) * lLen2),
                            (int)(by + Math.sin(tAng2 - Math.PI/2 + 0.25) * lLen2));
                    }
                    g2.setColor(leaf2);
                    g2.setStroke(new BasicStroke(1.8f));
                    g2.draw(new java.awt.geom.QuadCurve2D.Float(crownX, crownY, cpX2, cpY2, eX2, eY2));
                }
            }
            g2.setStroke(new BasicStroke(1.0f));

        } else if (n.equals("Mundaka")) {
            // Colinas verdes suaves del País Vasco al fondo
            g2.setColor(new Color(30, 62, 32));
            int base = labio + 3;
            int[] xs = { 0, w/6, w/3, w/2, w*2/3, w*5/6, w, w, 0 };
            int[] ys = { base - h/14, base - h/10, base - h/18, base - h/13, base - h/11, base - h/9, base - h/14, base+4, base+4 };
            g2.fillPolygon(xs, ys, xs.length);

        } else if (n.equals("Teahupo'o")) {
            // Picos montañosos tropicales de Tahití
            g2.setColor(new Color(22, 52, 28));
            int base = labio + 3;
            int[] xs = { 0, w/8, w/4, 3*w/8, w/2, 5*w/8, 3*w/4, 7*w/8, w, w, 0 };
            int[] ys = { base-h/10, base-h/6, base-h/14, base-h/5, base-h/8, base-h/4, base-h/12, base-h/7, base-h/10, base+4, base+4 };
            g2.fillPolygon(xs, ys, xs.length);

        } else if (n.equals("Hossegor")) {
            // Dunas de Las Landas — perfiles suaves de arena con hierba de duna
            int base = labio + 4;
            int bigH = Math.max(28, h * 24 / 100);
            int smH  = Math.max(18, h * 16 / 100);

            // Duna trasera (más alta, más alejada, más oscura)
            g2.setColor(new Color(165, 148, 108));
            int[] xs1 = {0, w/7, w*3/8, w*3/5, w*4/5, w, w, 0};
            int[] ys1 = {base-bigH/3, base-bigH*3/5, base-bigH, base-bigH*4/5, base-bigH/2, base-bigH/5, base+4, base+4};
            g2.fillPolygon(xs1, ys1, xs1.length);

            // Sombra en el flanco de barlovento
            g2.setColor(new Color(138, 122, 88, 110));
            int[] xsSh = {w/7, w*3/8, w*5/10, w*3/10};
            int[] ysSh = {base-bigH*3/5, base-bigH, base-bigH*4/5, base-bigH*3/5};
            g2.fillPolygon(xsSh, ysSh, 4);

            // Duna delantera (más clara, más cerca)
            g2.setColor(new Color(212, 194, 148));
            int[] xs2 = {0, w*2/9, w*4/9, w*6/9, w*8/9, w, w, 0};
            int[] ys2 = {base-smH/4, base-smH*3/4, base-smH, base-smH*3/4, base-smH/3, base, base+4, base+4};
            g2.fillPolygon(xs2, ys2, xs2.length);

            // Hierba de duna (ammophila) en la cresta de la duna grande
            g2.setColor(new Color(82, 105, 40));
            int cX1 = w*3/8, cY1 = base - bigH;
            g2.setStroke(new BasicStroke(1.1f));
            for (int gi = -4; gi <= 5; gi++) {
                int gx = cX1 + gi * 15;
                int gy = cY1 + (int)(Math.sin(gi * 1.1) * 4) + 2;
                for (int blade = 0; blade < 3 + Math.abs(gi)%2; blade++) {
                    double a = Math.toRadians(-112 + blade * 27 + gi * 4);
                    int len = 7 + (blade * 3) % 5;
                    g2.drawLine(gx, gy, gx+(int)(Math.cos(a)*len), gy+(int)(Math.sin(a)*len));
                }
            }
            // Hierba más pequeña en la duna delantera
            g2.setColor(new Color(70, 90, 34));
            int cX2 = w*4/9, cY2 = base - smH;
            for (int gi = -3; gi <= 3; gi++) {
                int gx = cX2 + gi * 13;
                int gy = cY2 + (int)(Math.cos(gi * 0.9) * 3) + 2;
                for (int blade = 0; blade < 3; blade++) {
                    double a = Math.toRadians(-108 + blade * 28);
                    g2.drawLine(gx, gy, gx+(int)(Math.cos(a)*7), gy+(int)(Math.sin(a)*7));
                }
            }
            g2.setStroke(new BasicStroke(1.0f));

        } else if (n.equals("Castelldefels")) {
            // Faro mediterráneo — Costa de Barcelona
            int lhX  = w * 78 / 100;
            int lhBot = labio;
            int lhH  = Math.max(50, h * 28 / 100);
            int tW   = Math.max(7, h / 32);   // semiancho base torre
            int tWt  = Math.max(3, h / 70);   // semiancho cima

            // Edificio en la base
            int bldH = lhH / 4;
            int bldW = tW * 3;
            g2.setColor(new Color(208, 200, 188));
            g2.fillRect(lhX - bldW, lhBot - bldH, bldW * 2, bldH);
            g2.setColor(new Color(165, 158, 145));
            g2.drawRect(lhX - bldW, lhBot - bldH, bldW * 2 - 1, bldH - 1);
            // Puerta
            g2.setColor(new Color(85, 62, 38));
            g2.fillRoundRect(lhX - tW/2, lhBot - bldH/2, tW, bldH/2, 3, 3);
            // Ventana
            g2.setColor(new Color(80, 115, 148));
            g2.fillRect(lhX + tW + 3, lhBot - bldH + bldH/3, 5, 5);

            // Torre (trapecio ahusado)
            int torBot = lhBot - bldH;
            int torTop = lhBot - lhH * 3 / 4;
            g2.setColor(new Color(238, 234, 224));
            int[] txs = {lhX - tW, lhX + tW, lhX + tWt, lhX - tWt};
            int[] tys = {torBot, torBot, torTop, torTop};
            g2.fillPolygon(txs, tys, 4);
            // Contorno sutil
            g2.setColor(new Color(180, 175, 162));
            g2.drawPolygon(txs, tys, 4);

            // Franja roja a media torre
            int midT = torBot - (torBot - torTop) * 4 / 10;
            int strW = tW - (tW - tWt) * 4 / 10;
            int strH = Math.max(3, lhH / 14);
            g2.setColor(new Color(188, 28, 28));
            g2.fillRect(lhX - strW, midT - strH/2, strW * 2, strH);

            // Cornisa
            int corW = tWt + 5;
            int corH = Math.max(2, h / 120);
            g2.setColor(new Color(150, 144, 132));
            g2.fillRect(lhX - corW - 1, torTop - corH, (corW + 1) * 2, corH + 2);

            // Sala de la linterna
            int lrW = tWt + 2;
            int lrH = Math.max(9, lhH / 9);
            int lrY = torTop - corH - lrH;
            g2.setColor(new Color(180, 218, 255, 160));
            g2.fillRect(lhX - lrW, lrY, lrW * 2, lrH);
            g2.setColor(new Color(28, 36, 48));
            g2.drawRect(lhX - lrW, lrY, lrW * 2 - 1, lrH - 1);
            // Panes de vidrio
            g2.setColor(new Color(25, 33, 45));
            for (int pi = 1; pi <= 2; pi++) {
                int px2 = lhX - lrW + lrW * 2 * pi / 3;
                g2.drawLine(px2, lrY + 1, px2, lrY + lrH - 2);
            }

            // Cúpula / capucho
            int capBase = lrY;
            int capH    = Math.max(7, lhH / 10);
            g2.setColor(new Color(22, 32, 48));
            int[] capXs = {lhX - lrW - 2, lhX + lrW + 2, lhX};
            int[] capYs = {capBase, capBase, capBase - capH};
            g2.fillPolygon(capXs, capYs, 3);
            // Remate punta
            g2.setColor(new Color(195, 195, 195));
            g2.fillOval(lhX - 2, capBase - capH - 3, 4, 4);

            // Rayos de luz del faro
            int lightY = capBase - capH / 2;
            int[][] rayos = {{-55, -18}, {-65, 4}, {-50, 28}, {50, 28}, {65, 4}, {55, -18}};
            g2.setStroke(new BasicStroke(1.1f));
            for (int[] r2 : rayos) {
                g2.setColor(new Color(255, 252, 200, 52));
                g2.drawLine(lhX, lightY, lhX + r2[0], lightY + r2[1]);
                g2.setColor(new Color(255, 252, 200, 22));
                g2.drawLine(lhX, lightY, lhX + r2[0] * 4/3, lightY + r2[1] * 4/3);
            }
            g2.setStroke(new BasicStroke(1.0f));

        } else if (n.equals("Yoyo's")) {
            // Acantilado volcánico Yoyo's, Sumbawa — ola DERECHA → scale(-1,1) espejado
            // drawX=0 = screenX=w (borde derecho); cliff aparece en la derecha de pantalla
            int base = labio + 2;
            int rH   = Math.max(55, h * 40 / 100);
            int rXe  = w * 25 / 100;

            // Silueta principal con doble cresta (basalto volcánico oscuro)
            int[] px = { 0,    w/28, w/15, w/10, w/7,  w/5,  rXe*8/9, rXe,  rXe, 0   };
            int[] py = {
                base - rH*5/10,
                base - rH*7/10,
                base - rH,           // cresta izquierda (máxima)
                base - rH*9/10,      // ligera silla entre crestas
                base - rH*19/20,     // segunda cresta casi igual
                base - rH*7/10,
                base - rH*5/10,
                base - rH*3/10,
                base + 5,
                base + 5
            };
            g2.setColor(new Color(32, 29, 26));
            g2.fillPolygon(px, py, px.length);

            // Cara expuesta (plano medio de basalto grisáceo cálido)
            int[] fx = { w/15, w/10, w/7,  w/5,  rXe*8/9, rXe,  rXe,  w/15 };
            int[] fy = {
                py[2] + rH/10,  py[3] + rH/12, py[4] + rH/10,
                py[5] + rH/9,   py[6] + rH/10, py[7] + rH/9,
                base + 5, base + 5
            };
            g2.setColor(new Color(70, 65, 58));
            g2.fillPolygon(fx, fy, fx.length);

            // Vetas rojizas (oxidación volcánica del basalto)
            g2.setColor(new Color(92, 64, 50));
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawLine(w/10, py[3] + rH/9, w/6, py[5] + rH/7);
            g2.setColor(new Color(80, 56, 44));
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawLine(w/13, py[2] + rH/8, w/9, py[3] + rH/6);

            // Aristas iluminadas en crestas (reflejo del sol)
            g2.setColor(new Color(112, 106, 98));
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawLine(w/15, py[2], w/10, py[3]);
            g2.drawLine(w/10, py[3], w/7, py[4]);
            g2.setColor(new Color(88, 83, 76));
            g2.setStroke(new BasicStroke(1.4f));
            g2.drawLine(0, py[0], w/28, py[1]);

            // Grietas diagonales (fractura volcánica)
            g2.setColor(new Color(20, 18, 16));
            g2.setStroke(new BasicStroke(0.7f));
            int[][] cracks = {
                { w/22, base-rH*4/10, w/15, py[2] + rH/9 },
                { w/11, base-rH*3/10, w/8,  py[3] + rH/7 },
                { w/7,  base-rH*3/10, w/5,  py[4] + rH/6 },
                { w/16, base-rH/5,    w/12, base-rH*4/10  },
                { rXe*3/5, base-rH*2/10, rXe*4/5, base-rH*4/10 },
            };
            for (int[] cr : cracks)
                g2.drawLine(cr[0], cr[1], cr[2], cr[3]);

            // Jungla tropical en las crestas (3 capas de verde)
            int[][] tops = { {w/15, py[2]}, {w/10, py[3]}, {w/7, py[4]}, {w/5, py[5]} };
            for (int[] t : tops) {
                int tx = t[0], ty = t[1];
                int tr = Math.max(9, rH * 11 / 100);
                g2.setColor(new Color(12, 48, 14));
                g2.fillOval(tx - tr, ty - tr + 2, tr * 2, (int)(tr * 1.2));
                g2.setColor(new Color(22, 76, 26));
                g2.fillOval(tx - tr*3/4, ty - tr - tr/4, (int)(tr*1.5), tr);
                g2.setColor(new Color(38, 108, 42));
                g2.fillOval(tx - tr/2, ty - tr - tr*3/5, tr, tr*3/5);
            }
            // Matas entre cimas
            g2.setColor(new Color(16, 62, 18));
            for (int i = 0; i < tops.length - 1; i++) {
                int mx = (tops[i][0] + tops[i+1][0]) / 2;
                int my = (tops[i][1] + tops[i+1][1]) / 2 - rH / 12;
                g2.fillOval(mx - rH/10, my - rH/18, rH/5, rH/9);
            }

            // Spray donde el agua golpea la roca
            Graphics2D gsp = (Graphics2D) g2.create();
            gsp.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.38f));
            gsp.setColor(new Color(230, 242, 250));
            for (int si = 0; si < 6; si++) {
                int sx = w/30 + si * (rXe / 6);
                gsp.fillOval(sx - 5, labio - 7, 11, 6);
            }
            gsp.dispose();
            g2.setStroke(new BasicStroke(1.0f));
        }
    }

    public void run() {
        while (jugando) {
            tick++;
            spawn_ticks++;
            ola_offset += ola_vel;

            int w = getWidth()  > 0 ? getWidth()  : 800;
            int h = getHeight() > 0 ? getHeight() : 500;
            if (tick == 1) colocarInicioSeguro(w, h);
            actualizarAltoSurfista(h);

            // guardar posición en estela (desplazar hacia atrás)
            for (int i = TRAIL_MAX - 1; i > 0; i--) {
                estelaX[i] = estelaX[i-1];
                estelaY[i] = estelaY[i-1];
            }
            estelaX[0] = surfista.x;
            estelaY[0] = surfista.y;

            boolean estaba_en_tubo = en_tubo;

            // copia atómica de teclas para evitar ConcurrentModificationException
            HashSet<Integer> teclasCopia;
            synchronized (teclas) { teclasCopia = new HashSet<>(teclas); }

            // Mapeo de controles: en ola derecha se invierten izquierda/derecha
            HashSet<Integer> teclasLogicas = new HashSet<>();
            for (Integer k : teclasCopia) {
                if (ola_invertida) {
                    if      (k == KeyEvent.VK_LEFT)  teclasLogicas.add(KeyEvent.VK_RIGHT);
                    else if (k == KeyEvent.VK_RIGHT) teclasLogicas.add(KeyEvent.VK_LEFT);
                    else if (k == KeyEvent.VK_A)     teclasLogicas.add(KeyEvent.VK_D);
                    else if (k == KeyEvent.VK_D)     teclasLogicas.add(KeyEvent.VK_A);
                    else                             teclasLogicas.add(k);
                } else {
                    teclasLogicas.add(k);
                }
            }

            double vel = surfista.velocidad();
            boolean parado = !surfista.hayInput(teclasLogicas);
            boolean cielo  = enCielo(h);

            if (cielo)
                aplicarFisicaCielo(teclasLogicas);
            else
                aplicarFisicaOla(parado, vel, teclasLogicas);

            if (!cielo && parado) parado_ticks++;
            else parado_ticks = 0;

            surfista.mover(surfista.velocidad());

            // limitar X al panel
            surfista.x = Math.max(10, Math.min(w - 10, surfista.x));

            // LÓGICA DE SUPERVIVENCIA: La ola avanza sola, variando su velocidad y termina cerrando (closeout)
            double baseAv = ola_vel == 1 ? 0.18 : ola_vel == 2 ? 0.26 : 0.38;
            double avance = baseAv + Math.sin(tick * 0.02) * baseAv * 0.4;
            peligro_x += avance;

            boolean cieloPost = enCielo(h);
            int labio_y = labioY(h);
            if (aerial_timer > 0) {
                aerial_timer--;
                if (aerial_timer == 0) { aerial_active = false; aerial_pts = 0; }
            } else if (cieloPost && surfista.vy < -AERIAL_THRESH && !estaba_en_tubo) {
                aerial_active = true;
                aerial_timer  = aerialDuration();
                aerial_pts    = 0;
            }

            if (spawn_ticks > SPAWN_GRACE && tablaTocaMascara(mascaEspuma, w, h)){
                jugando = false;
                game_over = true;
                wipeout_volando = true;
            }

            en_tubo = tablaTocaMascara(mascaTubo, w, h);

            if ((en_tubo || estaba_en_tubo) && cieloPost) {
                jugando = false;
                game_over = true;
                wipeout_volando = true;
            }

            if (en_tubo) {
                if (teclasLogicas.contains(KeyEvent.VK_RIGHT) || teclasLogicas.contains(KeyEvent.VK_D)) surfista.vx += 0.85;
                if (teclasLogicas.contains(KeyEvent.VK_LEFT)  || teclasLogicas.contains(KeyEvent.VK_A)) surfista.vx -= 0.85;
                if (teclasLogicas.contains(KeyEvent.VK_UP)    || teclasLogicas.contains(KeyEvent.VK_W)) surfista.vy -= 0.85;
                if (teclasLogicas.contains(KeyEvent.VK_DOWN)  || teclasLogicas.contains(KeyEvent.VK_S)) surfista.vy += 0.85;
            }

            if (jugando) {
                if (en_tubo) { puntuacion += TUBO_PTS; tubo_ticks++; }
                else if (aerial_active) { puntuacion++; aerial_pts++; }
                else if (!cieloPost) { tubo_ticks = 0; }
            }

            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            repaint();
        }
        
        // Animación de salir volando dando volteretas en CUALQUIER wipeout
        while (wipeout_volando && game_over) {
            surfista.x -= 10; // Lógicamente siempre es succionado hacia dentro del tubo/espuma
            surfista.y -= 8;
            wipeout_angulo -= 0.3;
            if (surfista.x < -100 || surfista.y < -100) wipeout_volando = false;
            try { Thread.sleep(16); } catch (InterruptedException e) { e.printStackTrace(); }
            repaint();
        }

        repaint();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();

        Graphics2D g2 = (Graphics2D) g;
        java.awt.geom.AffineTransform oldTransform = g2.getTransform();

        if (ola_invertida) {
            g2.translate(w, 0);
            g2.scale(-1, 1);
        }

        dibujarOla(g2, w, h);

        actualizarAltoSurfista(h);
        
        if (!wipeout_volando) {
            surfista.x    = Math.max(10, Math.min(w - 10, surfista.x));
            surfista.y    = Math.max(0,  Math.min(h,       surfista.y));
        }

        if (!enCielo(h) && !wipeout_volando) { // La estela desaparece al hacer el aerial o volar
            surfista.dibujarEstela(g2, estelaX, estelaY);
        }
        
        if (wipeout_volando || wipeout_angulo != 0) {
            g2.rotate(wipeout_angulo, surfista.x, surfista.y);
            surfista.paint(g2);
            g2.rotate(-wipeout_angulo, surfista.x, surfista.y);
        } else {
            surfista.paint(g2);
        }
        dibujarTuboEncima(g2, w, h);
        dibujarEspuma(g2, w, h);

        if (ola_invertida) {
            g2.setTransform(oldTransform);
        }

        // HUD
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString(spot.nombre + "   " + puntuacion + " pts", 10, 20);

        if (en_tubo) {
            g.setFont(new Font("SansSerif", Font.BOLD, 24));
            g.setColor(new Color(100, 220, 255));
            g.drawString("TUBO  +" + (tubo_ticks * TUBO_PTS) + " pts", w / 2 - 90, 55);
        }

        if (aerial_active) {
            g.setFont(new Font("SansSerif", Font.BOLD, 24));
            g.setColor(new Color(255, 230, 50));
            g.drawString("🤙 AERIAL! +" + aerial_pts, w / 2 - 95, 55);
        }

        if (game_over) {
            g.setColor(new Color(10, 20, 40));
            g.fillRoundRect(w/2 - 130, h/2 - 55, 260, 120, 10, 10);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 30));
            g.drawString("WIPEOUT!", w/2 - 76, h/2 - 10);
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            g.drawString("Puntuación: " + puntuacion, w/2 - 62, h/2 + 18);
            g.setFont(new Font("SansSerif", Font.ITALIC, 12));
            g.drawString("clic para reintentar", w/2 - 68, h/2 + 46);
        }
    }

    void dibujarOla(Graphics g, int w, int h) {
        Color agua = colorAgua();
        int labio_y = labioY(h);
        int oy = olaY(h);
        int oh = olaH(h);
        int px = (int)peligro_x;

        String sn = spot.nombre != null ? spot.nombre : "";
        if (sn.equals("Desert Point") && labio_y > 0) {
            Graphics2D g2s = (Graphics2D) g;
            java.awt.LinearGradientPaint lgp = new java.awt.LinearGradientPaint(
                0, 0, 0, labio_y,
                new float[]{0f, 0.45f, 1f},
                new Color[]{new Color(52, 22, 82), new Color(218, 78, 30), new Color(255, 160, 45)}
            );
            g2s.setPaint(lgp);
            g2s.fillRect(0, 0, w, labio_y);
            g2s.setPaint(null);
        } else {
            g.setColor(colorCielo());
            g.fillRect(0, 0, w, labio_y);
        }
        g.setColor(agua.darker());
        g.fillRect(0, labio_y, w, h - labio_y);

        // Landscape característico del spot (detrás del sol)
        Graphics2D gSol = (Graphics2D) g;
        gSol.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        dibujarLandscape(gSol, w, h);
        // Re-fill de la cara de la ola: el landscape puede sobresalir por debajo de labio_y
        g.setColor(agua.darker());
        g.fillRect(0, labio_y, w, h - labio_y);

        // Sol minimalista — siempre en la izquierda de pantalla.
        // Cuando ola_invertida=true el eje x está espejado: drawX=0.82 → screenX=0.18.
        int solX = ola_invertida ? (int)(w * 0.82) : (int)(w * 0.18);
        int solY = Math.max(18, labio_y / 3);
        int solR = 14;
        gSol.setColor(new Color(255, 230, 100, 210));
        gSol.fillOval(solX - solR, solY - solR, solR * 2, solR * 2);
        gSol.setColor(new Color(255, 210, 60, 140));
        gSol.setStroke(new BasicStroke(1.8f));
        for (int i = 0; i < 8; i++) {
            double ang = i * Math.PI / 4;
            int rx1 = (int)(Math.cos(ang) * (solR + 4));
            int ry1 = (int)(Math.sin(ang) * (solR + 4));
            int rx2 = (int)(Math.cos(ang) * (solR + 10));
            int ry2 = (int)(Math.sin(ang) * (solR + 10));
            gSol.drawLine(solX + rx1, solY + ry1, solX + rx2, solY + ry2);
        }
        gSol.setStroke(new BasicStroke(1.0f));

        // Closeout: rellenamos la zona rota de la ola con espuma blanca (sin gap)
        if (px > 0) {
            g.setColor(new Color(242, 244, 248));
            g.fillRect(0, oy - espumaSube(h), px + 12, h);
        }

        if (imgDinamismo != null) {
            // Si la ola aún está fuera de pantalla, empezamos desde el borde izquierdo
            int dinX = Math.max(0, px);
            g.drawImage(imgDinamismo, dinX, oy + 4, w, oh, this);
        }
        
        // Efecto de rayitas horizontales de velocidad (siempre activo para dar la sensación de correr)
        g.setColor(new Color(255, 255, 255, 60));
        int numLineas = 40;
        for (int i = 0; i < numLineas; i++) {
            int anchoLinea = 30 + (i * 17 % 100);
            int yLinea = labio_y + 15 + (i * 37 % (h - labio_y - 20));
            // Ajustamos la velocidad para dar una buena sensación de movimiento rápido
            int velocidadEfecto = ola_vel * 2 + (yLinea / 120);
            int xL = ((w + (i * 97) - ((ola_offset * velocidadEfecto) / 4) % w) % w);
            if (xL < 0) xL += w;
            if (xL >= px) { // Solo dibujarlas sobre la pared de agua limpia, no sobre la espuma
                g.fillRoundRect(xL, yLinea, anchoLinea, 2, 2, 2);
            }
        }

        // Gradiente que funde el dinamismo hacia el tubo en toda la altura de la cara de la ola
        Graphics2D g2 = (Graphics2D) g;
        Paint prevPaint = g2.getPaint();
        int grad_w = (int)(w * 0.42);
        Color cFondo  = agua.darker();
        Color cCielo  = colorCielo();
        Color cFondoT = new Color(cFondo.getRed(),  cFondo.getGreen(),  cFondo.getBlue(),  0);
        Color cCieloT = new Color(cCielo.getRed(),  cCielo.getGreen(),  cCielo.getBlue(),  0);
        // franja del cielo (oy → labio_y)
        if (labio_y > oy) {
            g2.setPaint(new GradientPaint(px, oy, cCielo, px + grad_w, oy, cCieloT));
            g2.fillRect(px, oy, grad_w, labio_y - oy);
        }
        // franja del agua (labio_y → h)
        g2.setPaint(new GradientPaint(px, labio_y, cFondo, px + grad_w, labio_y, cFondoT));
        g2.fillRect(px, labio_y, grad_w, h - labio_y);
        g2.setPaint(prevPaint);

        if (imgOla != null) {
            g.drawImage(imgOla, px, oy, w, oh, this);
            if (px < 0) {
                int sw = imgOla.getWidth();
                g.drawImage(imgOla, px + w, oy, w, oy + oh, sw - 2, 0, sw - 1, imgOla.getHeight(), this);
            }
        }
    }

    void dibujarEspuma(Graphics g, int w, int h){
        if (imgEspuma == null) return;
        int ew = (int)(w * ESPUMA_ESCALA);
        int eh = (int)(olaH(h) * ESPUMA_ESCALA);
        g.drawImage(imgEspuma, (int)peligro_x + ESPUMA_OFFSET_X, olaY(h) - espumaSube(h), ew, eh, this);
    }

    void dibujarTuboEncima(Graphics g, int w, int h){
        if (imgTubo == null) return;
        Graphics2D g2 = (Graphics2D) g;
        Composite prev = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, TUBO_CAPA_EXTRA));
        int tw = (int)(w * TUBO_ESCALA);
        int th = (int)(olaH(h) * TUBO_ESCALA);
        g2.drawImage(imgTubo, (int)peligro_x + TUBO_OFFSET_X, olaY(h) + tuboOffsetY(h), tw, th, this);
        g2.setComposite(prev);
    }
}
