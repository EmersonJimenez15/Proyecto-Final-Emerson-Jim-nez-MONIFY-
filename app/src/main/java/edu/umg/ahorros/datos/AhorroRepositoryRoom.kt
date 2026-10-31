package edu.umg.ahorros.datos

import edu.umg.programacion2.ahorros.datos.AhorroRepository
import edu.umg.programacion2.ahorros.modelo.MetaAhorro
import edu.umg.programacion2.ahorros.modelo.Movimiento

/**
 * IMPLEMENTACIÓN de AhorroRepository con Room (SQLite).
 *
 * IMPORTANTE: ControlAhorros solo conoce la interface AhorroRepository, así que cambiar
 * la memoria por SQLite no obliga a tocar la lógica (la "O" de SOLID: abierto para
 * extender, cerrado para modificar).
 *
 * Esta clase es un "traductor": recibe y devuelve objetos de la librería (Movimiento,
 * MetaAhorro) y le habla al DAO con las Entity (filas de las tablas).
 */
class AhorroRepositoryRoom(private val dao: AhorroDao) : AhorroRepository {

    override suspend fun agregar(movimiento: Movimiento) {
        // id = 0 para que SQLite genere el id (autoGenerate).
        dao.insertarMovimiento(MovimientoEntity.desde(movimiento).copy(id = 0))
    }

    override suspend fun listar(): List<Movimiento> =
        dao.listarMovimientos().map { it.aMovimiento() }

    // El contrato pide Boolean: "true si existía". El DAO devuelve cuántas filas borró.
    override suspend fun eliminar(id: Long): Boolean = dao.eliminarMovimiento(id) > 0

    override suspend fun agregarMeta(nombre: String, objetivo: Double): MetaAhorro {
        val idNuevo = dao.insertarMeta(MetaEntity(nombre = nombre, objetivo = objetivo))
        return MetaAhorro(idNuevo, nombre, objetivo)
    }

    override suspend fun listarMetas(): List<MetaAhorro> =
        dao.listarMetas().map { it.aMeta() }
}