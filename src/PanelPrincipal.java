import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;

public class PanelPrincipal extends JPanel {

    public static final int ancho_pant = 960;
    public static final int alto_pant  = 660;
    public static final int col_foto   = 8;

    Main mainFrame;
    ArrayList<Spot> spots = new ArrayList<>();
    ArrayList<String> niveles     = new ArrayList<>(Arrays.asList("PRINCIPIANTE", "INTERMEDIO", "AVANZADO", "EXPERTO"));
    ArrayList<String> tiposFondo  = new ArrayList<>(Arrays.asList("BEACH_BREAK", "REEF_BREAK", "POINT_BREAK", "RIVER_MOUTH"));
    ArrayList<String> direcciones = new ArrayList<>(Arrays.asList("IZQUIERDA", "DERECHA", "AMBAS", "SHORE_BREAK", "CLOSEOUT"));
    ArrayList<String> tablas      = new ArrayList<>(Arrays.asList("SHORTBOARD", "FISH", "FUNBOARD", "MIDLENGTH", "LONGBOARD", "GUN"));
    ArrayList<String> alturas     = new ArrayList<>(Arrays.asList("0.5m", "1m", "1.5m", "2m", "2.5m", "3m", "4m", "5m", "6m+"));
    ArrayList<String> velocidades = new ArrayList<>(Arrays.asList("Lenta", "Media", "Rápida"));

    DefaultTableModel modelo;
    JTable tabla;
    JComboBox<String> cmbPais, cmbFiltroNivel, cmbFiltroFondo, cmbFiltroTabla;
    boolean actualizando = false;
    PanelMapa panelMapa;

    public PanelPrincipal(Main parent){
        this.mainFrame = parent;
        setLayout(new BorderLayout());

        JLabel lblCargando = new JLabel("Cargando spots...", SwingConstants.CENTER);
        lblCargando.setForeground(Color.WHITE);
        add(lblCargando, BorderLayout.CENTER);

        // cargamos en hilo para no bloquear la UI
        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ArrayList<Spot> cargados = PersistenciaController.cargar("spots.json");
            SwingUtilities.invokeLater(() -> {
                if (cargados.isEmpty()) cargarSpots();
                else spots = cargados;
                remove(lblCargando);
                construirUI();
                parent.validate();
                parent.repaint();
            });
        }).start();
    }

    void construirUI(){
        String[] columnas = {"Nombre", "País", "Tipo de fondo", "Dirección", "Nivel", "Tablas", "Velocidad", "Valoración", "Foto"};
        modelo = new DefaultTableModel(columnas, 0){
            public boolean isCellEditable(int row, int col){ return false; }
            public Class<?> getColumnClass(int col){
                return col == col_foto ? Spot.class : Object.class;
            }
        };

        tabla = new JTable(modelo);
        tabla.setRowHeight(42);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        int[] anchos = {118, 78, 108, 88, 92, 162, 70, 80, 68};
        for (int i = 0; i < anchos.length; i++)
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);

        tabla.getColumnModel().getColumn(col_foto).setCellRenderer(new ImageRenderer());

        tabla.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){
                int col = tabla.columnAtPoint(e.getPoint());
                int row = tabla.rowAtPoint(e.getPoint());
                if (col == col_foto && row >= 0){
                    new DialogDetalle(mainFrame, spotEnFila(row)).setVisible(true);
                }
            }
        });

        // --- filtros ---
        cmbPais        = new JComboBox<>();
        cmbFiltroNivel = new JComboBox<>();
        cmbFiltroFondo = new JComboBox<>();
        cmbFiltroTabla = new JComboBox<>();

        actualizarComboPaises();
        cmbFiltroNivel.addItem("Todos"); for (String v : niveles)    cmbFiltroNivel.addItem(v);
        cmbFiltroFondo.addItem("Todos"); for (String v : tiposFondo) cmbFiltroFondo.addItem(v);
        cmbFiltroTabla.addItem("Todos"); for (String v : tablas)     cmbFiltroTabla.addItem(v);

        cmbPais.setPreferredSize(new Dimension(105, 24));
        cmbFiltroNivel.setPreferredSize(new Dimension(120, 24));
        cmbFiltroFondo.setPreferredSize(new Dimension(120, 24));
        cmbFiltroTabla.setPreferredSize(new Dimension(115, 24));

        ActionListener fl = e -> { if (!actualizando) aplicarFiltros(); };
        cmbPais.addActionListener(fl);
        cmbFiltroNivel.addActionListener(fl);
        cmbFiltroFondo.addActionListener(fl);
        cmbFiltroTabla.addActionListener(fl);

        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 5));
        panelFiltros.add(new JLabel("País:"));  panelFiltros.add(cmbPais);
        panelFiltros.add(new JLabel("Nivel:")); panelFiltros.add(cmbFiltroNivel);
        panelFiltros.add(new JLabel("Fondo:")); panelFiltros.add(cmbFiltroFondo);
        panelFiltros.add(new JLabel("Tabla:")); panelFiltros.add(cmbFiltroTabla);

        // --- botones ---
        JButton btnNuevo  = new JButton("+ Nuevo");
        JButton btnEditar = new JButton("Editar");
        JButton btnBorrar = new JButton("Borrar");

        btnNuevo.addActionListener(e -> new FormSpot(this).setVisible(true));

        btnEditar.addActionListener(e -> {
            int row = tabla.getSelectedRow();
            if (row < 0){
                JOptionPane.showMessageDialog(mainFrame, "Selecciona un spot para editar.");
                return;
            }
            new FormSpot(this, spotEnFila(row)).setVisible(true);
        });

        btnBorrar.addActionListener(e -> {
            int row = tabla.getSelectedRow();
            if (row < 0){
                JOptionPane.showMessageDialog(mainFrame, "Selecciona un spot para borrar.");
                return;
            }
            Spot s = spotEnFila(row);
            int ok = JOptionPane.showConfirmDialog(mainFrame,
                    "¿Borrar \"" + s.nombre + "\"?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION){
                spots.remove(s);
                refrescarTabla();
            }
        });

        JPanel panelBotones = new JPanel();
        panelBotones.add(btnNuevo);
        panelBotones.add(btnEditar);
        panelBotones.add(btnBorrar);

        JScrollPane scroll = new JScrollPane(tabla,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel panelTabla = new JPanel(new BorderLayout());
        panelTabla.add(panelFiltros, BorderLayout.NORTH);
        panelTabla.add(scroll,       BorderLayout.CENTER);
        panelTabla.add(panelBotones, BorderLayout.SOUTH);

        panelMapa = new PanelMapa(spots);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Let's Surf",   panelMapa);
        tabs.addTab("Spot Logbook", panelTabla);
        add(tabs, BorderLayout.CENTER);

        aplicarFiltros();
    }

    void aplicarFiltros(){
        String pais  = (String) cmbPais.getSelectedItem();
        String nivel = (String) cmbFiltroNivel.getSelectedItem();
        String fondo = (String) cmbFiltroFondo.getSelectedItem();
        String tabla = (String) cmbFiltroTabla.getSelectedItem();
        modelo.setRowCount(0);
        for (Spot s : spots){
            if (!"Todos".equals(pais)  && !s.pais.equals(pais))                continue;
            if (!"Todos".equals(nivel) && !s.nivelDificultad.equals(nivel))     continue;
            if (!"Todos".equals(fondo) && !s.tipoFondo.equals(fondo))           continue;
            if (!"Todos".equals(tabla) && !s.tablas.contains(tabla))            continue;
            agregarFila(s);
        }
    }

    void actualizarComboPaises(){
        actualizando = true;
        String sel = cmbPais.getItemCount() > 0 ? (String) cmbPais.getSelectedItem() : "Todos";
        cmbPais.removeAllItems();
        cmbPais.addItem("Todos");
        ArrayList<String> vistos = new ArrayList<>();
        for (Spot s : spots){
            if (!vistos.contains(s.pais)){
                vistos.add(s.pais);
                cmbPais.addItem(s.pais);
            }
        }
        cmbPais.setSelectedItem(sel);
        actualizando = false;
    }

    Spot spotEnFila(int row){
        return (Spot) modelo.getValueAt(row, col_foto);
    }

    void cargarSpots(){
        Spot s;

        // Pipeline: izquierda (Backdoor es la derecha del mismo pico)
        s = new Spot("Pipeline",      "EEUU",      "Hawaii / North Shore",
                "REEF_BREAK", "IZQUIERDA", "EXPERTO",
                new ArrayList<>(Arrays.asList("GUN", "MIDLENGTH")),
                "Noviembre - Febrero", 5);
        s.rutaFoto      = "resources/img/pipeline.jpg";
        s.alturaOla     = "5m";
        s.velocidadOla  = "Rápida";
        s.latitud   =  21.6; s.longitud = -158.1;
        s.mapX = 65; s.mapY = 351;
        spots.add(s);

        // Teahupo'o: losa pesada, misma lógica que Pipeline
        s = new Spot("Teahupo'o",     "Polinesia", "Tahiti",
                "REEF_BREAK", "IZQUIERDA", "EXPERTO",
                new ArrayList<>(Arrays.asList("SHORTBOARD", "GUN")),
                "Abril - Octubre", 5);
        s.rutaFoto      = "resources/img/teahupoo.jpg";
        s.alturaOla     = "4m";
        s.velocidadOla  = "Rápida";
        s.latitud   = -17.8; s.longitud = -149.5;
        s.mapX = 120; s.mapY = 559;
        spots.add(s);

        // Mundaka: izquierda rápida y hueca — shortboard o fish en días pequeños
        s = new Spot("Mundaka",       "España",    "País Vasco",
                "RIVER_MOUTH", "IZQUIERDA", "AVANZADO",
                new ArrayList<>(Arrays.asList("SHORTBOARD", "FISH")),
                "Octubre - Febrero", 4);
        s.rutaFoto      = "resources/img/mundaka.jpg";
        s.alturaOla     = "2m";
        s.velocidadOla  = "Rápida";
        s.latitud   =  43.4; s.longitud =   -2.7;
        s.mapX = 777; s.mapY = 235;
        spots.add(s);

        // Uluwatu: ola larga y maniobrable; en días pequeños fish o midlength, siempre shortboard en días grandes
        s = new Spot("Uluwatu",       "Indonesia", "Bali",
                "REEF_BREAK", "DERECHA", "AVANZADO",
                new ArrayList<>(Arrays.asList("SHORTBOARD", "FISH", "MIDLENGTH")),
                "Mayo - Septiembre", 4);
        s.rutaFoto      = "resources/img/uluwatu.jpg";
        s.alturaOla     = "1.5m";
        s.velocidadOla  = "Lenta";
        s.latitud   =  -8.8; s.longitud =  115.1;
        s.mapX = 1325; s.mapY = 505;
        spots.add(s);

        // Desert Point: izquierda rapidísima y larga — arrecife de coral, shortboard y fish en días pequeños
        s = new Spot("Desert Point",  "Indonesia", "Lombok",
                "REEF_BREAK", "IZQUIERDA", "EXPERTO",
                new ArrayList<>(Arrays.asList("SHORTBOARD", "FISH", "MIDLENGTH")),
                "Mayo - Octubre", 5);
        s.rutaFoto      = "resources/img/dessertpoint.jpg";
        s.alturaOla     = "1.5m";
        s.velocidadOla  = "Rápida";
        s.latitud   =  -8.7; s.longitud =  116.4;
        s.mapX = 1335; s.mapY = 505;
        spots.add(s);

        // Mawi: arrecife mellow, ola consistente — fish, midlength y longboard, sin shortboard
        s = new Spot("Mawi",          "Indonesia", "Lombok",
                "REEF_BREAK", "IZQUIERDA", "AVANZADO",
                new ArrayList<>(Arrays.asList("FISH", "MIDLENGTH", "LONGBOARD")),
                "Mayo - Septiembre", 3);
        s.rutaFoto      = "resources/img/mawi.jpg";
        s.alturaOla     = "1m";
        s.velocidadOla  = "Lenta";
        s.latitud   =  -8.9; s.longitud =  116.3;
        s.mapX = 1336; s.mapY = 505;
        spots.add(s);

        // Yoyo's: derechas limpias en Sumbawa, ola maniobrable — shortboard, fish o midlength
        s = new Spot("Yoyo's",        "Indonesia", "Sumbawa",
                "REEF_BREAK", "DERECHA", "AVANZADO",
                new ArrayList<>(Arrays.asList("SHORTBOARD", "FISH", "MIDLENGTH")),
                "Mayo - Octubre", 3);
        s.rutaFoto      = "resources/img/yoyos.jpg";
        s.alturaOla     = "1.5m";
        s.velocidadOla  = "Media";
        s.latitud   =  -8.6; s.longitud =  117.0;
        s.mapX = 1340; s.mapY = 505;
        spots.add(s);

        // Hossegor: beach break potente, WCT stop — shortboard o fish en días pequeños
        s = new Spot("Hossegor",      "Francia",   "Landes",
                "BEACH_BREAK", "AMBAS", "AVANZADO",
                new ArrayList<>(Arrays.asList("SHORTBOARD", "FISH")),
                "Septiembre - Noviembre", 3);
        s.rutaFoto      = "resources/img/hossegor.png";
        s.alturaOla     = "3m";
        s.velocidadOla  = "Rápida";
        s.latitud   =  43.7; s.longitud =   -1.4;
        s.mapX = 789; s.mapY = 230;
        spots.add(s);

        // Hookipa: ola potente y hollow — shortboard en la mayoría de días, midlength en días pequeños
        s = new Spot("Hookipa",       "EEUU",      "Maui / North Shore",
                "REEF_BREAK", "DERECHA", "EXPERTO",
                new ArrayList<>(Arrays.asList("SHORTBOARD", "MIDLENGTH")),
                "Noviembre - Marzo", 4);
        s.rutaFoto      = "resources/img/hookipa.jpg";
        s.alturaOla     = "3m";
        s.velocidadOla  = "Rápida";
        s.latitud   =  20.9; s.longitud = -156.3;
        s.mapX = 67; s.mapY = 350;
        spots.add(s);

        // El Palmar: beach break atlántico con variedad — admite casi todo
        s = new Spot("El Palmar",     "España",    "Cádiz",
                "BEACH_BREAK", "AMBAS", "INTERMEDIO",
                new ArrayList<>(Arrays.asList("SHORTBOARD", "FISH", "FUNBOARD", "MIDLENGTH")),
                "Octubre - Marzo", 2);
        s.rutaFoto      = "resources/img/elpalmar.jpg";
        s.alturaOla     = "1m";
        s.velocidadOla  = "Rápida";
        s.latitud   =  36.3; s.longitud =   -6.1;
        s.mapX = 767; s.mapY = 273;
        spots.add(s);

        // Castelldefels: Mediterráneo tranquilo — longboard y funboard, nada más
        s = new Spot("Castelldefels", "España",    "Barcelona",
                "BEACH_BREAK", "AMBAS", "PRINCIPIANTE",
                new ArrayList<>(Arrays.asList("LONGBOARD", "FUNBOARD")),
                "Octubre - Febrero", 1);
        s.rutaFoto      = "resources/img/castelldefels.jpg";
        s.alturaOla     = "0.5m";
        s.velocidadOla  = "Lenta";
        s.latitud   =  41.3; s.longitud =    1.9;
        s.mapX = 805; s.mapY = 245;
        spots.add(s);
    }

    void refrescarTabla(){
        actualizarComboPaises();
        aplicarFiltros();
        if (panelMapa != null) panelMapa.repaint();
    }

    void agregarFila(Spot s){
        modelo.addRow(new Object[]{
            s.nombre, s.pais, s.tipoFondo, s.direccionOla, s.nivelDificultad,
            s.tablasToString(),
            (s.velocidadOla != null && !s.velocidadOla.isEmpty()) ? s.velocidadOla.toUpperCase() : "—",
            "★".repeat(s.valoracion) + "☆".repeat(5 - s.valoracion),
            s
        });
    }
}
