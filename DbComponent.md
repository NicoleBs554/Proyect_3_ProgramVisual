File
src/DbComponent.java

Class
| Field | Value |
| --- | --- |
| Name | DbComponent |
| Author | Tu Nombre |
| Description | Componente genérico de base de datos con pool interno y consultas predefinidas. |
| Version | 1.0 |
| Is subclass | false |

Index
Properties
| Name |
| --- |
| adapter |
| pool |
| queries |

Methods
| Name |
| --- |
| DbComponent |
| lambda$loadJson$0 |
| lambda$loadToml$0 |
| lambda$loadYaml$0 |
| loadJson |
| loadProperties |
| loadQueries |
| loadToml |
| loadYaml |
| query |
| readRows |
| shutdown |
| transaction |

Properties
| Name | Declaration | Type | Description | Modifiers | Decorators | Defined in |
| --- | --- | --- | --- | --- | --- | --- |
| adapter | adapter: IAdapter | IAdapter | Adaptador concreto para la base de datos | private, final | @InfoAtributo | src/DbComponent.java:18 |
| pool | pool: ConnectionPool | ConnectionPool | Pool de conexiones interno | private, final | @InfoAtributo | src/DbComponent.java:20 |
| queries | queries: Map | Map | Mapa de consultas predefinidas | private, final | @InfoAtributo | src/DbComponent.java:248 |

Methods
| Name | Signature | Decorators | Defined in | Returns | Description | Modifiers | Getter | Setter | Constructor | Overridden |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| DbComponent | DbComponent(class java.lang.String, class java.lang.String, int, class java.lang.String, class java.lang.String, class java.lang.String, class java.lang.String, int) | @InfoMetodo | src/DbComponent.java:0 | void | Constructor que recibe todos los datos de conexión y la ruta del archivo de queries. Usa reflexión para instanciar el adaptador. | public | false | false | true | false |
| lambda$loadJson$0 | lambda$loadJson$0(class java.lang.String, class java.lang.Object) |  | src/DbComponent.java:0 | void |  | private | false | false | false | false |
| lambda$loadToml$0 | lambda$loadToml$0(class java.lang.String, class java.lang.Object) |  | src/DbComponent.java:0 | void |  | private | false | false | false | false |
| lambda$loadYaml$0 | lambda$loadYaml$0(class java.lang.String, class java.lang.Object) |  | src/DbComponent.java:0 | void |  | private | false | false | false | false |
| loadJson | loadJson(class java.lang.String) |  | src/DbComponent.java:109 | void |  | private | false | false | false | false |
| loadProperties | loadProperties(class java.lang.String) |  | src/DbComponent.java:97 | void |  | private | false | false | false | false |
| loadQueries | loadQueries(class java.lang.String) |  | src/DbComponent.java:48 | void |  | private | false | false | false | false |
| loadToml | loadToml(class java.lang.String) |  | src/DbComponent.java:147 | void |  | private | false | false | false | false |
| loadYaml | loadYaml(class java.lang.String) |  | src/DbComponent.java:125 | void |  | private | false | false | false | false |
| query | query(class java.lang.String, class Ljava.lang.Object;) | @InfoMetodo | src/DbComponent.java:171 | List | Ejecuta una consulta predefinida por nombre (sin transacción). Obtiene una conexión del pool y la libera al finalizar. | public | false | false | false | false |
| readRows | readRows(interface java.sql.ResultSet) |  | src/DbComponent.java:77 | List |  | private | false | false | false | false |
| shutdown | shutdown() | @InfoMetodo | src/DbComponent.java:231 | void | Cierra el pool y libera todos los recursos. | public | false | false | false | false |
| transaction | transaction() | @InfoMetodo | src/DbComponent.java:206 | Transaction | Inicia una transacción. Reserva una conexión del pool que se mantendrá hasta commit/rollback/close. | public | false | false | false | false |
