import java.time.LocalDate;
import java.util.ArrayList;

public class Spot {
    public String nombre;
    public String pais;
    public String region;
    public String tipoFondo;
    public String direccionOla;
    public String nivelDificultad;
    public ArrayList<String> tablas = new ArrayList<>();
    public String alturaOla;
    public String velocidadOla;
    public String mejorEpoca;
    public String rutaFoto;
    public String comentario;
    public int    valoracion;
    public boolean visitado;
    public LocalDate fechaVisita;
    public double latitud;
    public double longitud;
    public double mapX;
    public double mapY;

    public Spot(String nombre, String pais, String region,
                String tipoFondo, String direccionOla,
                String nivelDificultad, ArrayList<String> tablas,
                String mejorEpoca, int valoracion) {
        this.nombre = nombre;
        this.pais = pais;
        this.region = region;
        this.tipoFondo = tipoFondo;
        this.direccionOla = direccionOla;
        this.nivelDificultad = nivelDificultad;
        this.tablas = tablas;
        this.mejorEpoca = mejorEpoca;
        this.valoracion = valoracion;
        this.visitado = false;
    }

    public String tablasToString() {
        return String.join(", ", tablas);
    }

    public String toString() {
        return nombre + " (" + pais + ")";
    }
}
