# Guía de sustentación — Sistema Comercial Compusoft

## 1. Estructura del proyecto

Proyecto Java 21 + JavaFX + Maven, con JDBC directo a SQL Server (sin ORM). Todo el código vive bajo `src/main/java/pe/utp/`, organizado por responsabilidad (patrón por capas):

### `Conexion/`
Todo lo relacionado a la conexión con la base de datos.
- **`ConexionDB.java`**: lee `config.properties`, abre la conexión JDBC a SQL Server y la mantiene como singleton (una sola `Connection` reutilizada, en vez de abrir una nueva en cada consulta).
- **`QueryHelper.java`**: pequeños helpers para generar SQL específico de SQL Server (`TOP`, `DATEADD`, `CAST AS DATE`) sin repetir esa sintaxis en cada DAO.

### `model/`
Las clases "POJO" que representan las tablas de la base de datos: `Venta`, `Compra`, `Producto`, `Cliente`, `Empleado`, `DetalleVenta`, `DetalleCompra`, `Lote`, etc. Solo atributos + getters/setters, sin lógica ni SQL.

### `dao/` (Data Access Object)
Un DAO por entidad principal: `VentaDAO`, `CompraDAO`, `ProductoDAO`, `ClienteDAO`, `EmpleadoDAO`, `LoteDAO`, `ReporteDAO`, etc. Cada uno solo sabe ejecutar SQL puntual contra su tabla (`SELECT`, `INSERT`, `UPDATE`) y mapear el `ResultSet` a un objeto `model`. No decide reglas de negocio ni maneja transacciones que junten varias tablas — eso ahora vive en `service/`.

### `service/`
Capa nueva (agregada en este proyecto de forma incremental) para lógica de negocio que orquesta varios DAO dentro de una misma transacción.
- **`VentaService.java`**: `anularVenta()` — verifica estado, devuelve stock a los lotes exactos de origen, marca la venta como ANULADA, todo en una transacción (`commit`/`rollback`).
- **`CompraService.java`**: `anularCompra()` — mismo patrón: valida que los lotes generados no se hayan consumido, revierte stock, anula lotes, marca la compra como ANULADA.

### `controller/`
Un controller por pantalla JavaFX (`VentaController`, `CompraController`, `ProductoController`, `ReportesController`, `EmpleadoController`, etc.). Manejan eventos de UI (botones, tablas, formularios) y llaman a `dao/` o `service/` según corresponda. Están enlazados a su `.fxml` correspondiente.

### `dialog/`
Ventanas/diálogos reutilizables (`CSDialog` para alertas de éxito/error/confirmación, `VentanaPrincipal` para la ventana raíz de la app).

### `security/`
Control de acceso por rol: `Sesion` (guarda el empleado logueado), `Rol` y `Modulo` (enums), `PermisoService`/`PermisoUtil` (deciden si el rol actual puede acceder a un módulo).

### `util/`
Utilidades genéricas sin relación a una entidad específica: `FormatoMoneda`, `RangoFechaUtil` (atajos de rango de fechas: hoy, últimos 7 días, este mes, este año), `ResultadoEliminacion`.

### `src/main/resources/`
- `config.properties`: credenciales y URL de conexión a SQL Server.
- `fxml/`: la definición visual (XML) de cada pantalla, uno por Controller.
- `styles/`: CSS de la interfaz.

**Flujo típico de una operación simple (ej. listar productos):**
`ProductoController` → `ProductoDAO.listar()` → SQL → `ResultSet` mapeado a `List<Producto>` → se muestra en la tabla JavaFX.

**Flujo típico de una operación con lógica de negocio (ej. anular venta):**
`VentaController` → `VentaService.anularVenta()` → orquesta `VentaDAO` + `LoteDAO` dentro de una transacción → `commit`/`rollback`.

---

## 2. Preguntas de ejemplo (estilo profesor) con cómo responderlas

### P1. ¿Cómo obtienes esa cadena de conexión con SQL Server, y esos valores de dónde salen?

**Respuesta:** La cadena de conexión (`url`) y las credenciales (`user`, `password`, `database`) están en `src/main/resources/config.properties`, fuera del código Java. `ConexionDB.java` (carpeta `Conexion/`) las lee con `Properties.load()` en un bloque estático al arrancar la app, y usa esos valores para llamar `DriverManager.getConnection(url, user, password)`. Guardar la conexión en un `.properties` (en vez de escribirla directo en el `.java`) permite cambiar de servidor/BD sin recompilar, y es buena práctica porque separa configuración de código.

### P2. ¿Por qué usamos DAO y no Service? ¿Qué diferencia hay?

**Respuesta:** DAO (Data Access Object) es la capa que solo sabe ejecutar SQL puntual contra una tabla y devolver objetos `model` — no toma decisiones de negocio. Service es la capa que orquesta lógica de negocio: puede llamar a varios DAO, decidir un flujo con condicionales, y manejar una transacción que involucra más de una tabla. En este proyecto originalmente todo estaba en DAO (patrón de 2 capas: Controller → DAO), pero se agregó `service/` para los casos donde SÍ hay lógica de negocio real, como anular una venta (verificar estado, devolver stock a lotes específicos, marcar anulada — todo en una transacción). Los reportes o listados simples (`SELECT` con filtro) no necesitan Service porque no hay decisiones de negocio, solo lectura.

### P3. Necesito ver mis reportes de ventas trimestrales o cuatrimestrales, y si no está, cómo sería.

**Respuesta:** No está implementado como opción directa. Existe `ReporteDAO.java` (carpeta `dao`) con métodos que reciben un rango de fechas libre, y `ReportesController.java` + su `.fxml` dejan elegir esas fechas manualmente con `DatePicker`. Además ya existe `util/RangoFechaUtil.java`, una clase de atajos de fecha (hoy, últimos 7 días, este mes, este año) que centraliza ese cálculo para no repetirlo en cada controller — pero no tiene un atajo de trimestre/cuatrimestre todavía.

Para implementarlo: agregar un método nuevo en `RangoFechaUtil.java` (ej. `trimestreActual()` o `trimestre(int numero, int anio)`) que calcule el rango de fechas del trimestre, siguiendo el mismo patrón que los métodos existentes. Luego en `ReportesController.java` agregar un combo/botones ("Q1, Q2, Q3, Q4") que llamen a ese nuevo método y pasen el rango resultante a los métodos ya existentes de `ReporteDAO`. No se toca la base de datos ni el DAO — el DAO ya acepta cualquier rango de fechas.

**Carpetas a tocar:** `util/` (nuevo método de rango), `controller/` (UI + lógica de selección), `resources/fxml/` (nuevo control visual).

### P4. Necesito todos los productos devueltos o ventas anuladas del primer trimestre del año pasado. Si no se tiene, ¿qué carpetas tocar y qué implementar?

**Respuesta:** Ventas anuladas sí existe como concepto: la tabla `venta` tiene columna `estado`, y se marca `'ANULADA'` vía `VentaService.anularVenta()`. `ReporteDAO` ya excluye las anuladas de sus reportes normales (`AND (estado IS NULL OR estado <> 'ANULADA')`), así que el dato existe, solo falta un reporte que muestre justo lo contrario.

Lo que NO existe es "producto devuelto" como concepto independiente (una devolución parcial de un producto dentro de una venta que sigue activa) — hoy solo se puede anular la venta completa.

Para lo que pide tal cual (ventas anuladas del Q1 del año pasado): no se toca la base de datos. Se agrega un método nuevo en `ReporteDAO.java` (carpeta `dao`), ej. `listarVentasAnuladas(fechaInicio, fechaFin)` con `WHERE estado = 'ANULADA' AND fecha BETWEEN ? AND ?`, combinado con el nuevo cálculo de trimestre de `RangoFechaUtil` (P3). Se muestra en `ReportesController.java` + su `.fxml`.

Si además se quisiera registrar devoluciones de productos específicos (no toda la venta), eso sí es una funcionalidad nueva: tabla nueva en SQL Server (ej. `devolucion`), su `DAO` (`DevolucionDAO.java`), su `model` (`Devolucion.java`), y su `Controller`/`.fxml`.

---

## 3. Preguntas adicionales de práctica (mismo estilo)

### P5. ¿Por qué usan un patrón singleton para la conexión en vez de abrir una nueva cada vez?

**Respuesta:** Abrir una `Connection` nueva en cada consulta y no cerrarla nunca genera una fuga de conexiones: cada navegación entre pantallas sumaría una conexión más contra SQL Server hasta agotar el límite del motor. `ConexionDB.getConexion()` reutiliza una única conexión estática y solo abre una nueva si la anterior nunca existió o quedó cerrada/caída (`if (conexion == null || conexion.isClosed())`). Es más simple que un pool de conexiones real (como HikariCP), pero resuelve el problema para una app de escritorio con un solo usuario por instancia.

### P6. ¿Qué pasa si dos personas anulan la misma venta al mismo tiempo? ¿Está controlado?

**Respuesta:** `VentaService.anularVenta()` primero hace `SELECT estado` y luego, dentro de la misma transacción, decide y hace los `UPDATE`. Si dos anulaciones llegaran exactamente al mismo tiempo desde conexiones distintas, en teoría podría haber una condición de carrera dependiendo del nivel de aislamiento de SQL Server por defecto (`READ COMMITTED`), que no bloquea la fila solo con un `SELECT`. Para garantizarlo al 100% habría que usar un `SELECT ... WITH (UPDLOCK)` o verificar filas afectadas en el `UPDATE` final. Es un punto real de mejora, útil para responder con honestidad si lo preguntan a fondo.

### P7. ¿Por qué el costeo de productos usa "lotes" (FIFO) en vez de un precio promedio simple?

**Respuesta:** `LoteDAO` documenta que reemplaza un esquema anterior de Costo Promedio Ponderado (CPP). Con lotes, cada compra genera un lote con su costo real, y cada venta consume stock siguiendo FIFO (`consumirFIFO()`): descuenta primero del lote más antiguo. Esto permite conocer el costo REAL de cada unidad vendida (para calcular utilidad bruta exacta) y, al anular una venta, devolver la cantidad al lote EXACTO de donde salió, sin mezclar costos entre lotes distintos.

### P8. ¿Cómo controlan qué empleado puede acceder a qué módulo del sistema?

**Respuesta:** La carpeta `security/` maneja esto: `Sesion` guarda el `Empleado` logueado tras el login, `Rol` y `Modulo` son enums que representan los roles del sistema y las pantallas/funcionalidades, y `PermisoService.puedeAcceder(modulo)` revisa el rol de la sesión activa contra un mapeo de permisos por rol para decidir si se muestra o bloquea esa opción en la UI.

### P9. Si mañana quieren agregar un reporte de "productos más rentables" combinando ventas y costos, ¿dónde iría esa lógica?

**Respuesta:** Sería un buen candidato para `service/` (ej. `ReporteService.java`), no solo `ReporteDAO`. Si el cálculo es un simple `SELECT` con `JOIN` y `GROUP BY`, se queda en el DAO. Pero si hay que combinar el precio de venta (de `VentaDAO`/`ReporteDAO`) con el costo real por lote (de `LoteDAO`) y calcular una utilidad por producto con alguna regla adicional (por ejemplo, excluir productos con menos de X ventas), eso ya es orquestar varias fuentes + una regla de negocio — el mismo criterio que se usó para crear `VentaService` y `CompraService`.
