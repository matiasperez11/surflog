import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;

public class PersistenciaController {

    static void guardar(ArrayList<Spot> spots, String fichero){
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(fichero));
            pw.println("[");
            for (int i = 0; i < spots.size(); i++){
                Spot s = spots.get(i);
                pw.print("  {");
                pw.print("\"nombre\":"          + js(s.nombre)          + ",");
                pw.print("\"pais\":"            + js(s.pais)             + ",");
                pw.print("\"region\":"          + js(s.region)           + ",");
                pw.print("\"tipoFondo\":"       + js(s.tipoFondo)        + ",");
                pw.print("\"direccionOla\":"    + js(s.direccionOla)     + ",");
                pw.print("\"nivelDificultad\":" + js(s.nivelDificultad)  + ",");
                pw.print("\"tablas\":"          + js(s.tablasToString()) + ",");
                pw.print("\"mejorEpoca\":"      + js(s.mejorEpoca)       + ",");
                pw.print("\"valoracion\":"      + s.valoracion            + ",");
                pw.print("\"rutaFoto\":"        + js(s.rutaFoto)         + ",");
                pw.print("\"alturaOla\":"       + js(s.alturaOla)        + ",");
                pw.print("\"velocidadOla\":"    + js(s.velocidadOla)     + ",");
                pw.print("\"comentario\":"      + js(s.comentario)       + ",");
                pw.print("\"visitado\":"        + s.visitado              + ",");
                pw.print("\"fechaVisita\":"     + js(s.fechaVisita != null ? s.fechaVisita.toString() : "") + ",");
                pw.print("\"latitud\":"         + s.latitud               + ",");
                pw.print("\"longitud\":"        + s.longitud              + ",");
                pw.print("\"mapX\":"            + s.mapX                  + ",");
                pw.print("\"mapY\":"            + s.mapY);
                pw.print(i < spots.size()-1 ? "}," : "}");
                pw.println();
            }
            pw.println("]");
            pw.close();
        } catch (Exception e){ e.printStackTrace(); }
    }

    static ArrayList<Spot> cargar(String fichero){
        ArrayList<Spot> lista = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(fichero));
            StringBuilder sb = new StringBuilder();
            String linea;
            while ((linea = br.readLine()) != null) sb.append(linea);
            br.close();
            String json = sb.toString();
            int pos = 0;
            while (true){
                int ini = json.indexOf('{', pos);
                if (ini < 0) break;
                int fin = json.indexOf('}', ini);
                if (fin < 0) break;
                Spot s = parsear(json.substring(ini+1, fin));
                if (s != null) lista.add(s);
                pos = fin + 1;
            }
        } catch (FileNotFoundException ex){
            // primer arranque sin fichero — normal
        } catch (Exception e){ e.printStackTrace(); }
        return lista;
    }

    static Spot parsear(String obj){
        try {
            ArrayList<String> tablas = new ArrayList<>();
            String tablasStr = gs(obj, "tablas");
            if (!tablasStr.isEmpty())
                for (String t : tablasStr.split(","))
                    if (!t.trim().isEmpty()) tablas.add(t.trim());

            Spot s = new Spot(
                gs(obj, "nombre"), gs(obj, "pais"), gs(obj, "region"),
                gs(obj, "tipoFondo"), gs(obj, "direccionOla"), gs(obj, "nivelDificultad"),
                tablas, gs(obj, "mejorEpoca"), gi(obj, "valoracion"));
            s.rutaFoto     = gs(obj, "rutaFoto");
            s.alturaOla    = gs(obj, "alturaOla");
            s.velocidadOla = gs(obj, "velocidadOla");
            s.comentario   = gs(obj, "comentario");
            s.visitado     = gb(obj, "visitado");
            String fecha = gs(obj, "fechaVisita");
            if (!fecha.isEmpty()) try { s.fechaVisita = LocalDate.parse(fecha); } catch(Exception ex){}
            s.latitud  = gd(obj, "latitud");
            s.longitud = gd(obj, "longitud");
            s.mapX     = gd(obj, "mapX");
            s.mapY     = gd(obj, "mapY");
            return s;
        } catch (Exception e){ e.printStackTrace(); return null; }
    }

    static String gs(String obj, String field){
        String key = "\"" + field + "\":\"";
        int i = obj.indexOf(key);
        if (i < 0) return "";
        i += key.length();
        int j = obj.indexOf('"', i);
        return j < 0 ? "" : obj.substring(i, j);
    }

    static int gi(String obj, String field){
        String key = "\"" + field + "\":";
        int i = obj.indexOf(key);
        if (i < 0) return 0;
        i += key.length();
        int j = i;
        while (j < obj.length() && (Character.isDigit(obj.charAt(j)) || obj.charAt(j) == '-')) j++;
        try { return Integer.parseInt(obj.substring(i, j)); } catch(Exception e){ return 0; }
    }

    static boolean gb(String obj, String field){
        String key = "\"" + field + "\":";
        int i = obj.indexOf(key);
        if (i < 0) return false;
        return obj.substring(i + key.length()).trim().startsWith("true");
    }

    static double gd(String obj, String field){
        String key = "\"" + field + "\":";
        int i = obj.indexOf(key);
        if (i < 0) return 0.0;
        i += key.length();
        int j = i;
        while (j < obj.length() && (Character.isDigit(obj.charAt(j)) || obj.charAt(j) == '-' || obj.charAt(j) == '.')) j++;
        try { return Double.parseDouble(obj.substring(i, j)); } catch(Exception e){ return 0.0; }
    }

    static String js(String s){
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
