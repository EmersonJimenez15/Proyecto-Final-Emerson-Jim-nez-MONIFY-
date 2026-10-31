package edu.umg.ahorros.datos

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * La base de datos completa: qué tablas tiene y qué DAOs ofrece.
 *
 * - entities: las clases @Entity (una tabla por cada una).
 * - version:  número del "esquema". Si cambias una Entity (ej. agregas un campo),
 *             hay que subirlo y agregar una migración.
 * - exportSchema = false: no guardamos el historial del esquema en archivos JSON.
 *
 * Es abstract: Room genera AppDatabase_Impl al compilar.
 */
@Database(entities = [MovimientoEntity::class, MetaEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun ahorroDao(): AhorroDao

    companion object {
        // @Volatile + synchronized: aunque dos partes pidan la base a la vez, se crea UNA sola.
        @Volatile
        private var instancia: AppDatabase? = null

        /**
         * Abre (o crea la primera vez) el archivo ahorros_room.db en la carpeta privada de la app.
         * Usamos applicationContext para no retener la Activity en memoria.
         */
        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ahorros_room.db"
                ).build().also { instancia = it }
            }
    }
}