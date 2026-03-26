@InfoClase(
    nombre = "DbConfig",
    autor = "Tu Nombre",
    descripcion = "Configuración de conexión para DbComponent/SimpleConnectionPool.",
    version = "1.0",
    esSubclase = false
)
public final class DbConfig {
    @InfoAtributo(tipo = "String", descripcion = "Host de la base de datos", modificadores = {"private", "final"})
    private final String host;

    @InfoAtributo(tipo = "int", descripcion = "Puerto de la base de datos", modificadores = {"private", "final"})
    private final int port;

    @InfoAtributo(tipo = "String", descripcion = "Nombre de la base de datos", modificadores = {"private", "final"})
    private final String database;

    @InfoAtributo(tipo = "String", descripcion = "Usuario", modificadores = {"private", "final"})
    private final String user;

    @InfoAtributo(tipo = "String", descripcion = "Contraseña", modificadores = {"private", "final"})
    private final String password;

    @InfoMetodo(
        parametros = {"String host", "int port", "String database", "String user", "String password"},
        tipoRetorno = "",
        descripcion = "Construye la configuración de conexión.",
        modificadores = {"public"},
        esConstructor = true
    )
    public DbConfig(String host, int port, String database, String user, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    @InfoMetodo(parametros = {}, tipoRetorno = "String", descripcion = "Devuelve el host.", modificadores = {"public"}, esGetter = true)
    public String getHost() {
        return host;
    }

    @InfoMetodo(parametros = {}, tipoRetorno = "int", descripcion = "Devuelve el puerto.", modificadores = {"public"}, esGetter = true)
    public int getPort() {
        return port;
    }

    @InfoMetodo(parametros = {}, tipoRetorno = "String", descripcion = "Devuelve la base de datos.", modificadores = {"public"}, esGetter = true)
    public String getDatabase() {
        return database;
    }

    @InfoMetodo(parametros = {}, tipoRetorno = "String", descripcion = "Devuelve el usuario.", modificadores = {"public"}, esGetter = true)
    public String getUser() {
        return user;
    }

    @InfoMetodo(parametros = {}, tipoRetorno = "String", descripcion = "Devuelve la contraseña.", modificadores = {"public"}, esGetter = true)
    public String getPassword() {
        return password;
    }
}
