package edu.umg.ahorros.datos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * DAO = Data Access Object: el MISMO patrón que usamos con JDBC y MySQL (Clase 5).
 * Aquí solo escribimos la INTERFACE; Room genera la clase que la implementa
 * (AhorroDao_Impl) al compilar.
 *
 * IMPORTANTE: todas las funciones son suspend. Room las ejecuta en OTRO hilo; si no lo
 * fueran, la app se cerraría con "Cannot access database on the main thread".
 */
@Dao
interface AhorroDao {

    // ---------- Movimientos ----------

    // Devuelve el id que SQLite generó para la fila nueva.
    @Insert
    suspend fun insertarMovimiento(movimiento: MovimientoEntity): Long

    // Orden de llegada (por id): el historial más reciente se arma en la pantalla.
    @Query("SELECT * FROM movimientos ORDER BY id")
    suspend fun listarMovimientos(): List<MovimientoEntity>

    // :id es el "?" del PreparedStatement de JDBC. Devuelve cuántas filas borró.
    @Query("DELETE FROM movimientos WHERE id = :id")
    suspend fun eliminarMovimiento(id: Long): Int

    // ---------- Metas ----------

    @Insert
    suspend fun insertarMeta(meta: MetaEntity): Long

    @Query("SELECT * FROM metas ORDER BY id")
    suspend fun listarMetas(): List<MetaEntity>
}