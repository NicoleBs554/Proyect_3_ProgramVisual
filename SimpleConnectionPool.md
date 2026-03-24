File
src/SimpleConnectionPool.java

Class
| Field | Value |
| --- | --- |
| Name | SimpleConnectionPool |
| Author | Tu Nombre |
| Description | Implementación de un pool de conexiones con tamaño fijo y expansión dinámica hasta un límite máximo. |
| Version | 1.0 |
| Is subclass | false |

Index
Properties
| Name |
| --- |
| adapter |
| closed |
| database |
| host |
| idle |
| maxSize |
| password |
| port |
| total |
| user |

Methods
| Name |
| --- |
| SimpleConnectionPool |
| createConnection |
| getConnection |
| releaseConnection |
| shutdown |

Properties
| Name | Declaration | Type | Description | Modifiers | Decorators | Defined in |
| --- | --- | --- | --- | --- | --- | --- |
| adapter | adapter: IAdapter | IAdapter | Adaptador para crear nuevas conexiones | private, final | @InfoAtributo | src/SimpleConnectionPool.java:0 |
| closed | closed: boolean | boolean | Indica si el pool está cerrado | private, volatile | @InfoAtributo | src/SimpleConnectionPool.java:0 |
| database | database: String | String | Nombre de la base de datos | private, final | @InfoAtributo | src/SimpleConnectionPool.java:0 |
| host | host: String | String | Host de la base de datos | private, final | @InfoAtributo | src/SimpleConnectionPool.java:0 |
| idle | idle: BlockingQueue | BlockingQueue | Cola de conexiones inactivas | private, final | @InfoAtributo | src/SimpleConnectionPool.java:0 |
| maxSize | maxSize: int | int | Tamaño máximo del pool | private, final | @InfoAtributo | src/SimpleConnectionPool.java:0 |
| password | password: String | String | Contraseña de la base de datos | private, final | @InfoAtributo | src/SimpleConnectionPool.java:0 |
| port | port: int | int | Puerto de la base de datos | private, final | @InfoAtributo | src/SimpleConnectionPool.java:0 |
| total | total: AtomicInteger | AtomicInteger | Número total de conexiones creadas | private, final | @InfoAtributo | src/SimpleConnectionPool.java:0 |
| user | user: String | String | Usuario de la base de datos | private, final | @InfoAtributo | src/SimpleConnectionPool.java:0 |

Methods
| Name | Signature | Decorators | Defined in | Returns | Description | Modifiers | Getter | Setter | Constructor | Overridden |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| SimpleConnectionPool | SimpleConnectionPool(interface IAdapter, class java.lang.String, int, class java.lang.String, class java.lang.String, class java.lang.String, int) | @InfoMetodo | src/SimpleConnectionPool.java:0 | void | Constructor que inicializa el pool con un tamaño fijo. | public | false | false | true | false |
| createConnection | createConnection() |  | src/SimpleConnectionPool.java:0 | Connection |  | private | false | false | false | false |
| getConnection | getConnection() | @InfoMetodo | src/SimpleConnectionPool.java:0 | Connection | Obtiene una conexión del pool. Espera si no hay disponibles hasta alcanzar el límite máximo. | public | false | false | false | false |
| releaseConnection | releaseConnection(interface java.sql.Connection) | @InfoMetodo | src/SimpleConnectionPool.java:0 | void | Devuelve una conexión al pool. Si la conexión está cerrada o no es válida, la descarta. | public | false | false | false | false |
| shutdown | shutdown() | @InfoMetodo | src/SimpleConnectionPool.java:0 | void | Cierra todas las conexiones y deja el pool inoperante. | public | false | false | false | false |
