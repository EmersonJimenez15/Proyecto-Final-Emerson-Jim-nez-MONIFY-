package edu.umg.ahorros.datos

import androidx.room.Entity
import androidx.room.PrimaryKey
import edu.umg.programacion2.ahorros.modelo.Movimiento

/**
 * Una FILA de la tabla "movimientos" en SQLite.
 *
 * ¿Por qué no ponemos @Entity directamente sobre Movimiento? Porque Movimiento vive en la
 * librería ahorros-core, que es Kotlin puro y NO conoce Android ni Room (por eso también
 * corre en consola). El proyecto Android tiene su propia clase "espejo" y la convierte
 * de ida y vuelta:
 *
 *   Movimiento (librería) <-- aMovimiento() / desde() --> MovimientoEntity (tabla SQLite)
 */
@Entity(tableName = "movimientos")
data class MovimientoEntity(
    // autoGenerate = true: si llega id = 0, SQLite le asigna el siguiente número.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // Guardamos el NOMBRE del tipo ("DEPOSITO" / "RETIRO"), el valor más estable.
    val tipo: String,
    val monto: Double,
    val descripcion: String,
    val fecha: String,
    // 0 = ahorro general; si no, el id de la meta a la que pertenece.
    val metaId: Long = 0
) {
    /** Fila de la tabla -> objeto de la librería (Deposito o Retiro). */
    fun aMovimiento(): Movimiento = Movimiento.crear(tipo, id, monto, descripcion, fecha, metaId)

    companion object {
        /** Objeto de la librería -> fila de la tabla. */
        fun desde(movimiento: Movimiento): MovimientoEntity = MovimientoEntity(
            id = movimiento.id,
            tipo = movimiento.tipo,
            monto = movimiento.monto,
            descripcion = movimiento.descripcion,
            fecha = movimiento.fecha,
            metaId = movimiento.metaId
        )
    }
}