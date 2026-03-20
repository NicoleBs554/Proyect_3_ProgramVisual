import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

// Anotaciones (pueden ser usadas opcionalmente)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface InfoClase {
    String nombre();
    String autor();
    String descripcion();
    String version();
    boolean esSubclase() default false;
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface InfoAtributo {
    String tipo();
    String descripcion();
    String[] modificadores() default {};
}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@interface InfoMetodo {
    String[] parametros() default {};
    String tipoRetorno() default "";
    String descripcion() default "";
    String[] modificadores() default {};
    boolean esGetter() default false;
    boolean esSetter() default false;
    boolean esConstructor() default false;
    boolean esSobreescrito() default false;
}

public class Generator {
    public static void generarMarkdownParaClase(Class<?> clase, String nombreArchivo) {
        StringBuilder md = new StringBuilder();
        String rutaFuente = "src/" + clase.getSimpleName() + ".java";
        List<String> lineasFuente = leerLineasFuente(rutaFuente);

        InfoClase infoClase = clase.getAnnotation(InfoClase.class);
        List<PropiedadDoc> propiedades = extraerPropiedades(clase, lineasFuente);
        List<MetodoDoc> metodos = extraerMetodos(clase, lineasFuente);

        md.append("File\n");
        md.append(rutaFuente).append("\n\n");

        if (infoClase != null) {
            md.append("Class\n");
            md.append("| Field | Value |\n");
            md.append("| --- | --- |\n");
            md.append("| Name | ").append(escapeMarkdownCell(infoClase.nombre())).append(" |\n");
            md.append("| Author | ").append(escapeMarkdownCell(infoClase.autor())).append(" |\n");
            md.append("| Description | ").append(escapeMarkdownCell(infoClase.descripcion())).append(" |\n");
            md.append("| Version | ").append(escapeMarkdownCell(infoClase.version())).append(" |\n");
            md.append("| Is subclass | ").append(infoClase.esSubclase()).append(" |\n\n");
        } else {
            md.append("Class\n");
            md.append("| Field | Value |\n");
            md.append("| --- | --- |\n");
            md.append("| Name | ").append(clase.getSimpleName()).append(" |\n");
            md.append("| Package | ").append(clase.getPackageName()).append(" |\n");
            md.append("| Modifiers | ").append(Modifier.toString(clase.getModifiers())).append(" |\n\n");
        }

        md.append("Index\n");
        md.append("Properties\n");
        md.append("| Name |\n");
        md.append("| --- |\n");
        for (PropiedadDoc propiedad : propiedades) {
            md.append("| ").append(escapeMarkdownCell(propiedad.nombre)).append(" |\n");
        }
        md.append("\n");

        md.append("Methods\n");
        md.append("| Name |\n");
        md.append("| --- |\n");
        for (MetodoDoc metodo : metodos) {
            md.append("| ").append(escapeMarkdownCell(metodo.nombre)).append(" |\n");
        }
        md.append("\n");

        md.append("Properties\n");
        md.append("| Name | Declaration | Type | Description | Modifiers | Decorators | Defined in |\n");
        md.append("| --- | --- | --- | --- | --- | --- | --- |\n");
        for (PropiedadDoc propiedad : propiedades) {
            md.append("| ")
              .append(escapeMarkdownCell(propiedad.nombre))
              .append(" | ")
              .append(escapeMarkdownCell(propiedad.nombre + ": " + propiedad.tipo))
              .append(" | ")
              .append(escapeMarkdownCell(propiedad.tipo))
              .append(" | ")
              .append(escapeMarkdownCell(propiedad.descripcion))
              .append(" | ")
              .append(escapeMarkdownCell(propiedad.modificadores))
              .append(" | ")
              .append(escapeMarkdownCell(toTableMultiline(propiedad.decoradores)))
              .append(" | ")
              .append(escapeMarkdownCell(rutaFuente + ":" + propiedad.lineaDefinicion))
              .append(" |\n");
        }
        md.append("\n");

        md.append("Methods\n");
        md.append("| Name | Signature | Decorators | Defined in | Returns | Description | Modifiers | Getter | Setter | Constructor | Overridden |\n");
        md.append("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |\n");
        for (MetodoDoc metodo : metodos) {
            md.append("| ")
              .append(escapeMarkdownCell(metodo.nombre))
              .append(" | ")
              .append(escapeMarkdownCell(metodo.nombre + "(" + metodo.parametros + ")"))
              .append(" | ")
              .append(escapeMarkdownCell(toTableMultiline(metodo.decoradores)))
              .append(" | ")
              .append(escapeMarkdownCell(rutaFuente + ":" + metodo.lineaDefinicion))
              .append(" | ")
              .append(escapeMarkdownCell(metodo.retorno))
              .append(" | ")
              .append(escapeMarkdownCell(metodo.descripcion))
              .append(" | ")
              .append(escapeMarkdownCell(metodo.modificadores))
              .append(" | ")
              .append(metodo.esGetter)
              .append(" | ")
              .append(metodo.esSetter)
              .append(" | ")
              .append(metodo.esConstructor)
              .append(" | ")
              .append(metodo.esSobreescrito)
              .append(" |\n");
        }

        try (FileWriter writer = new FileWriter(nombreArchivo)) {
            writer.write(md.toString());
        } catch (IOException e) {
            System.err.println("No se pudo escribir el archivo markdown: " + e.getMessage());
        }
    }

    private static List<String> leerLineasFuente(String rutaFuente) {
        try {
            return Files.readAllLines(Path.of(rutaFuente));
        } catch (IOException e) {
            return List.of();
        }
    }

    private static List<PropiedadDoc> extraerPropiedades(Class<?> clase, List<String> lineasFuente) {
        List<PropiedadDoc> propiedades = new ArrayList<>();
        for (Field field : clase.getDeclaredFields()) {
            InfoAtributo info = field.getAnnotation(InfoAtributo.class);
            String desc = (info != null) ? info.descripcion() : "";
            String mods = (info != null) ? String.join(", ", info.modificadores()) : Modifier.toString(field.getModifiers());
            int linea = buscarLineaCampo(lineasFuente, field.getName());
            propiedades.add(new PropiedadDoc(
                field.getName(),
                field.getType().getSimpleName(),
                desc,
                mods,
                construirDecoradores(field.getAnnotations()),
                linea > 0 ? linea : 0
            ));
        }
        propiedades.sort(Comparator.comparing(p -> p.nombre));
        return propiedades;
    }

    private static List<MetodoDoc> extraerMetodos(Class<?> clase, List<String> lineasFuente) {
        List<MetodoDoc> metodos = new ArrayList<>();

        for (Method method : clase.getDeclaredMethods()) {
            InfoMetodo info = method.getAnnotation(InfoMetodo.class);
            int linea = buscarLineaMetodo(lineasFuente, method.getName());
            metodos.add(new MetodoDoc(
                method.getName(),
                Arrays.toString(method.getParameterTypes()).replace("[", "").replace("]", ""),
                method.getReturnType().getSimpleName(),
                (info != null) ? info.descripcion() : "",
                (info != null) ? String.join(", ", info.modificadores()) : Modifier.toString(method.getModifiers()),
                construirDecoradores(method.getAnnotations()),
                (info != null) && info.esGetter(),
                (info != null) && info.esSetter(),
                (info != null) && info.esConstructor(),
                (info != null) && info.esSobreescrito(),
                linea > 0 ? linea : 0
            ));
        }

        for (Constructor<?> constructor : clase.getDeclaredConstructors()) {
            InfoMetodo info = constructor.getAnnotation(InfoMetodo.class);
            int linea = buscarLineaMetodo(lineasFuente, constructor.getName());
            metodos.add(new MetodoDoc(
                constructor.getName(),
                Arrays.toString(constructor.getParameterTypes()).replace("[", "").replace("]", ""),
                "void",
                (info != null) ? info.descripcion() : "",
                (info != null) ? String.join(", ", info.modificadores()) : Modifier.toString(constructor.getModifiers()),
                construirDecoradores(constructor.getAnnotations()),
                false, false,
                (info != null) && info.esConstructor(),
                false,
                linea > 0 ? linea : 0
            ));
        }

        metodos.sort(Comparator.comparing(m -> m.nombre));
        return metodos;
    }

    private static String construirDecoradores(Annotation[] annotations) {
        StringBuilder sb = new StringBuilder();
        for (Annotation annotation : annotations) {
            sb.append("@").append(annotation.annotationType().getSimpleName());
            // Opcionalmente podrías mostrar sus valores, pero se omite para simplificar
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private static String toTableMultiline(String value) {
        return value.replace("\n", "<br>");
    }

    private static String escapeMarkdownCell(String value) {
        if (value == null) return "";
        return value.replace("|", "\\|").replace("\r", " ").replace("\n", "<br>");
    }

    private static int buscarLineaCampo(List<String> lineasFuente, String nombreCampo) {
        for (int i = 0; i < lineasFuente.size(); i++) {
            String linea = lineasFuente.get(i);
            if (linea.contains(nombreCampo) && linea.contains(";") && !linea.contains("(")) {
                return i + 1;
            }
        }
        return -1;
    }

    private static int buscarLineaMetodo(List<String> lineasFuente, String nombreMetodo) {
        for (int i = 0; i < lineasFuente.size(); i++) {
            String linea = lineasFuente.get(i);
            if (linea.contains(nombreMetodo + "(") && linea.contains("{")) {
                return i + 1;
            }
        }
        return -1;
    }

    private record PropiedadDoc(
        String nombre,
        String tipo,
        String descripcion,
        String modificadores,
        String decoradores,
        int lineaDefinicion
    ) {}

    private record MetodoDoc(
        String nombre,
        String parametros,
        String retorno,
        String descripcion,
        String modificadores,
        String decoradores,
        boolean esGetter,
        boolean esSetter,
        boolean esConstructor,
        boolean esSobreescrito,
        int lineaDefinicion
    ) {}
}