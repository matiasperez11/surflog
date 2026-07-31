import javax.swing.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

public class FormSpot extends JDialog {

    JTextField txtNombre, txtPais, txtRegion, txtMejorEpoca, txtComentario, txtFecha, txtLat, txtLon;
    JComboBox<String>  cmbFondo;
    JComboBox<String>  cmbDireccion;
    JComboBox<String>  cmbNivel;
    JComboBox<String>  cmbAltura;
    JComboBox<String>  cmbVelocidad;
    JComboBox<Integer> cmbValoracion;
    JCheckBox  chkVisitado;
    JCheckBox[] chkTablas;
    JButton    btnFoto;
    JLabel     lblFotoNombre;
    String rutaFotoSeleccionada = "";
    PanelPrincipal panel;
    Spot editando;

    public FormSpot(PanelPrincipal parent){
        super(parent.mainFrame, "Nuevo Spot", true);
        this.panel = parent;
        init();
    }

    public FormSpot(PanelPrincipal parent, Spot s){
        super(parent.mainFrame, "Editar Spot", true);
        this.panel    = parent;
        this.editando = s;
        init();
        precargar(s);
    }

    void init(){
        setSize(420, 640);
        setLocationRelativeTo(panel.mainFrame);
        setLayout(new BorderLayout(4, 4));

        JPanel centro = new JPanel(new BorderLayout(0, 4));
        centro.add(buildCampos(), BorderLayout.CENTER);
        centro.add(buildTablas(), BorderLayout.SOUTH);

        add(centro,        BorderLayout.CENTER);
        add(buildBotones(), BorderLayout.SOUTH);
    }

    void precargar(Spot s){
        txtNombre.setText(s.nombre);
        txtPais.setText(safe(s.pais));
        txtRegion.setText(safe(s.region));
        txtMejorEpoca.setText(safe(s.mejorEpoca));
        txtComentario.setText(safe(s.comentario));
        cmbFondo.setSelectedItem(s.tipoFondo);
        cmbDireccion.setSelectedItem(s.direccionOla);
        cmbNivel.setSelectedItem(s.nivelDificultad);
        cmbValoracion.setSelectedItem(s.valoracion);
        cmbAltura.setSelectedItem(s.alturaOla != null && !s.alturaOla.isEmpty() ? s.alturaOla : "—");
        cmbVelocidad.setSelectedItem(s.velocidadOla != null && !s.velocidadOla.isEmpty() ? s.velocidadOla : "—");
        chkVisitado.setSelected(s.visitado);
        if (s.fechaVisita != null){
            txtFecha.setText(s.fechaVisita.toString());
            txtFecha.setForeground(txtNombre.getForeground());
        }
        if (s.rutaFoto != null && !s.rutaFoto.isEmpty()){
            rutaFotoSeleccionada = s.rutaFoto;
            String name = new File(s.rutaFoto).getName();
            lblFotoNombre.setText(name.length() > 12 ? name.substring(0, 10) + "…" : name);
            btnFoto.setToolTipText(s.rutaFoto);
        }
        if (s.latitud != 0 || s.longitud != 0){
            txtLat.setText(String.valueOf(s.latitud));
            txtLon.setText(String.valueOf(s.longitud));
        }
        ArrayList<String> opciones = panel.tablas;
        for (int i = 0; i < chkTablas.length; i++)
            chkTablas[i].setSelected(s.tablas.contains(opciones.get(i)));
    }

    JPanel buildCampos(){
        txtNombre     = new JTextField();
        txtPais       = new JTextField();
        txtRegion     = new JTextField();
        autoCapitalizar(txtNombre);
        autoCapitalizar(txtPais);
        autoCapitalizar(txtRegion);
        txtMejorEpoca = new JTextField();
        txtComentario = new JTextField();
        txtFecha = new JTextField("AAAA-MM-DD");
        Color colorTexto = txtFecha.getForeground();
        txtFecha.setForeground(Color.GRAY);
        txtFecha.addFocusListener(new FocusAdapter(){
            public void focusGained(FocusEvent e){
                if (txtFecha.getText().equals("AAAA-MM-DD")){
                    txtFecha.setText("");
                    txtFecha.setForeground(colorTexto);
                }
            }
            public void focusLost(FocusEvent e){
                if (txtFecha.getText().isEmpty()){
                    txtFecha.setText("AAAA-MM-DD");
                    txtFecha.setForeground(Color.GRAY);
                }
            }
        });
        chkVisitado = new JCheckBox("Sí");

        cmbFondo      = new JComboBox<>(panel.tiposFondo.toArray(new String[0]));
        cmbDireccion  = new JComboBox<>(panel.direcciones.toArray(new String[0]));
        cmbNivel      = new JComboBox<>(panel.niveles.toArray(new String[0]));
        cmbValoracion = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
        cmbValoracion.setSelectedItem(3);

        cmbAltura    = new JComboBox<>();
        cmbVelocidad = new JComboBox<>();
        cmbAltura.addItem("—");
        for (String v : panel.alturas)     cmbAltura.addItem(v);
        cmbVelocidad.addItem("—");
        for (String v : panel.velocidades) cmbVelocidad.addItem(v);

        btnFoto = new JButton("Elegir");
        lblFotoNombre = new JLabel("—");
        lblFotoNombre.setFont(lblFotoNombre.getFont().deriveFont(Font.ITALIC, 10f));
        btnFoto.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Imágenes (jpg, png, gif)", "jpg", "jpeg", "png", "gif"));
            fc.setAcceptAllFileFilterUsed(false);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){
                File f = fc.getSelectedFile();
                rutaFotoSeleccionada = f.getAbsolutePath();
                String name = f.getName();
                lblFotoNombre.setText(name.length() > 12 ? name.substring(0, 10) + "…" : name);
                btnFoto.setToolTipText(f.getAbsolutePath());
            }
        });
        JPanel fotoPanel = new JPanel(new BorderLayout(3, 0));
        fotoPanel.setOpaque(false);
        fotoPanel.add(btnFoto,       BorderLayout.WEST);
        fotoPanel.add(lblFotoNombre, BorderLayout.CENTER);

        JPanel p = new JPanel(new GridLayout(0, 2, 5, 4));
        p.setBorder(BorderFactory.createEmptyBorder(8, 10, 4, 10));

        p.add(new JLabel("Nombre *:")); p.add(txtNombre);
        p.add(new JLabel("País:"));     p.add(txtPais);
        p.add(new JLabel("Región:"));   p.add(txtRegion);
        p.add(new JLabel("Tipo fondo:")); p.add(cmbFondo);
        p.add(new JLabel("Dirección:")); p.add(cmbDireccion);
        p.add(new JLabel("Nivel:"));    p.add(cmbNivel);
        p.add(new JLabel("Altura ola:")); p.add(cmbAltura);
        p.add(new JLabel("Velocidad:")); p.add(cmbVelocidad);
        p.add(new JLabel("Valoración:")); p.add(cmbValoracion);
        p.add(new JLabel("Mejor época:")); p.add(txtMejorEpoca);
        p.add(new JLabel("Comentario:")); p.add(txtComentario);
        p.add(new JLabel("Visitado:"));  p.add(chkVisitado);
        p.add(new JLabel("Fecha:"));    p.add(txtFecha);
        p.add(new JLabel("Foto:"));     p.add(fotoPanel);
        txtLat = new JTextField();
        txtLon = new JTextField();
        JPanel latLon = new JPanel(new GridLayout(1, 2, 3, 0));
        latLon.add(txtLat);
        latLon.add(txtLon);
        p.add(new JLabel("Lat / Lon:")); p.add(latLon);
        return p;
    }

    JPanel buildTablas(){
        ArrayList<String> opciones = panel.tablas;
        chkTablas = new JCheckBox[opciones.size()];
        JPanel grid = new JPanel(new GridLayout(2, 3, 4, 4));
        for (int i = 0; i < opciones.size(); i++){
            chkTablas[i] = new JCheckBox(opciones.get(i));
            grid.add(chkTablas[i]);
        }
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Tablas recomendadas"));
        p.add(grid, BorderLayout.CENTER);
        return p;
    }

    JPanel buildBotones(){
        JPanel p = new JPanel();
        JButton btnCancelar = new JButton("Cancelar");
        JButton btnGuardar  = new JButton("Guardar");
        btnCancelar.addActionListener(e -> dispose());
        btnGuardar.addActionListener(e -> guardar());
        p.add(btnCancelar);
        p.add(btnGuardar);
        return p;
    }

    void guardar(){
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()){
            JOptionPane.showMessageDialog(this, "El nombre es obligatorio.");
            return;
        }

        ArrayList<String> selTablas = new ArrayList<>();
        ArrayList<String> opciones  = panel.tablas;
        for (int i = 0; i < chkTablas.length; i++){
            if (chkTablas[i].isSelected()) selTablas.add(opciones.get(i));
        }

        LocalDate fecha = null;
        String fechaTxt = txtFecha.getText().trim();
        if (!fechaTxt.isEmpty() && !fechaTxt.equals("AAAA-MM-DD")){
            try {
                fecha = LocalDate.parse(fechaTxt);
            } catch (DateTimeParseException ex){
                JOptionPane.showMessageDialog(this, "Fecha inválida. Usa el formato AAAA-MM-DD.");
                return;
            }
        }

        String alt = (String) cmbAltura.getSelectedItem();
        String vel = (String) cmbVelocidad.getSelectedItem();
        double lat = 0, lon = 0;
        try { lat = Double.parseDouble(txtLat.getText().trim()); } catch(Exception ex){}
        try { lon = Double.parseDouble(txtLon.getText().trim()); } catch(Exception ex){}

        if (editando != null){
            editando.nombre          = nombre;
            editando.pais            = txtPais.getText().trim();
            editando.region          = txtRegion.getText().trim();
            editando.tipoFondo       = (String)  cmbFondo.getSelectedItem();
            editando.direccionOla    = (String)  cmbDireccion.getSelectedItem();
            editando.nivelDificultad = (String)  cmbNivel.getSelectedItem();
            editando.tablas          = selTablas;
            editando.alturaOla       = "—".equals(alt) ? "" : alt;
            editando.velocidadOla    = "—".equals(vel) ? "" : vel;
            editando.mejorEpoca      = txtMejorEpoca.getText().trim();
            editando.valoracion      = (Integer) cmbValoracion.getSelectedItem();
            editando.comentario      = txtComentario.getText().trim();
            editando.visitado        = chkVisitado.isSelected();
            editando.rutaFoto        = rutaFotoSeleccionada;
            editando.fechaVisita     = fecha;
            editando.latitud         = lat;
            editando.longitud        = lon;
        } else {
            Spot s = new Spot(nombre, txtPais.getText().trim(), txtRegion.getText().trim(),
                    (String)  cmbFondo.getSelectedItem(),
                    (String)  cmbDireccion.getSelectedItem(),
                    (String)  cmbNivel.getSelectedItem(),
                    selTablas,
                    txtMejorEpoca.getText().trim(),
                    (Integer) cmbValoracion.getSelectedItem());
            s.alturaOla    = "—".equals(alt) ? "" : alt;
            s.velocidadOla = "—".equals(vel) ? "" : vel;
            s.comentario   = txtComentario.getText().trim();
            s.visitado     = chkVisitado.isSelected();
            s.rutaFoto     = rutaFotoSeleccionada;
            s.fechaVisita  = fecha;
            s.latitud      = lat;
            s.longitud     = lon;
            panel.spots.add(s);
        }
        panel.refrescarTabla();
        dispose();
    }

    void autoCapitalizar(JTextField txt){
        txt.addKeyListener(new KeyAdapter(){
            public void keyTyped(KeyEvent e){
                if (txt.getText().isEmpty() && Character.isLowerCase(e.getKeyChar()))
                    e.setKeyChar(Character.toUpperCase(e.getKeyChar()));
            }
        });
    }

    String safe(String v){ return v != null ? v : ""; }
}
