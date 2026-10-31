package edu.umg.ahorros.datos

import androidx.room.Entity
import androidx.room.PrimaryKey
import edu.umg.programacion2.ahorros.modelo.MetaAhorro

/** Una FILA de la tabla "metas": cada meta de ahorro que crea el usuario. */
@Entity(tableName = "metas")
data class MetaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val objetivo: Double
) {
    fun aMeta(): MetaAhorro = MetaAhorro(id, nombre, objetivo)
}