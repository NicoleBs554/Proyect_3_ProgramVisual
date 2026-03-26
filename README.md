# DBComponent - Proyecto 3 de Programación Visual

## Descripción

**DBComponent** es un componente de base de datos desacoplado en Java que permite conectarse a diferentes motores SQL (PostgreSQL, H2, MySQL) mediante un sistema de adaptadores. Ofrece un pool de conexiones interno, consultas predefinidas desde archivos `.properties` y soporte para transacciones. Además, incluye un generador de documentación basado en anotaciones y reflexión que produce archivos Markdown con la estructura de las clases.

## Características

- **Desacoplamiento**: La interfaz `IAdapter` define el contrato para cualquier base de datos. Se incluyen adaptadores para PostgreSQL, H2 y MySQL.
- **Pool de conexiones**: `SimpleConnectionPool` gestiona un conjunto de conexiones que se reutilizan, reduciendo la sobrecarga de creación.
- **Consultas predefinidas**: Las consultas SQL se almacenan en archivos `.properties` (uno por base de datos) y se invocan por nombre.
- **Transacciones**: El método `transaction()` devuelve un objeto `Transaction` que agrupa operaciones y permite `commit()` / `rollback()`.
- **Reflexión**: El constructor de `DbComponent` instancia el adaptador a partir del nombre de la clase usando `Class.forName()`.
- **Documentación automática**: El generador `Generator` extrae información de las clases mediante anotaciones (`@InfoClase`, `@InfoAtributo`, `@InfoMetodo`) y produce archivos Markdown.

## Estructura del proyecto
# DBComponent - Proyecto 3 de Programación Visual

**DBComponent** es un componente JDBC en Java totalmente desacoplado que permite la conexión a motores SQL (PostgreSQL, H2, MySQL) a través de un sistema de adaptadores. Está diseñado para ser eficiente y autodescriptivo.

## 🚀 Características Principales

- **Multimotor**: Soporte nativo mediante adaptadores (`IAdapter`) para PostgreSQL, H2 y MySQL.
- **Pool de Conexiones**: Gestión inteligente de recursos con `SimpleConnectionPool` para evitar sobrecargas.
- **Consultas Desacopladas**: El código Java no tiene SQL quemado; todo se lee desde archivos `.properties`.
- **Transacciones Seguras**: Manejo simplificado con soporte para `commit()` y `rollback()` automático.
- **Auto-Documentación**: Genera manuales en Markdown leyendo anotaciones personalizadas (`@InfoClase`, `@InfoAtributo`, `@InfoMetodo`) usando reflexión.

## 🛠️ Configuración y Arranque Rápido

### 1. Preparar las dependencias
Coloca los drivers JDBC en la carpeta `lib/`:
- `postgresql-42.7.4.jar`
- `h2-2.3.232.jar`
- `mysql-connector-java-8.0.33.jar`

### 2. Definir consultas (`.properties`)
Crea tus archivos de propiedades (ej. `queries_mysql.properties`):
```properties
findAllUsers = SELECT * FROM users
insertUser = INSERT INTO users (name, age) VALUES (?, ?)

javac -cp "lib/*" -d out (Get-ChildItem -Path src -Filter *.java | ForEach-Object FullName)

java -cp "out;lib/*" Demo