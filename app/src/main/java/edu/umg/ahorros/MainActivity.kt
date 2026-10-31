package edu.umg.ahorros

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.MaterialColors
import edu.umg.ahorros.databinding.ActivityMainBinding      // generados por View Binding
import edu.umg.ahorros.databinding.DialogNuevaMetaBinding
import edu.umg.ahorros.databinding.ItemMovimientoBinding
import edu.umg.ahorros.datos.AhorroRepositoryRoom
import edu.umg.ahorros.datos.AppDatabase
import edu.umg.programacion2.ahorros.modelo.Deposito         // todo esto viene de la librería
import edu.umg.programacion2.ahorros.modelo.MetaAhorro
import edu.umg.programacion2.ahorros.modelo.Movimiento
import edu.umg.programacion2.ahorros.modelo.PlanAhorro
import edu.umg.programacion2.ahorros.modelo.Retiro
import edu.umg.programacion2.ahorros.modelo.enQuetzales
import edu.umg.programacion2.ahorros.servicio.ControlAhorros
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var control: ControlAhorros

    private val planes = PlanAhorro.todos()

    /** Meta con la que estamos trabajando. null = ahorro general (sin meta). */
    private var metaActual: MetaAhorro? = null

    // Versículos: se muestra uno distinto cada día.
    private val versiculos = listOf(
        "Los pensamientos del diligente ciertamente tienden a la abundancia." to "Proverbios 21:5",
        "El que recoge con mano laboriosa las riquezas, las aumenta." to "Proverbios 13:11",
        "Porque ¿quién de vosotros, queriendo edificar una torre, no calcula primero los gastos, a ver si tiene lo que necesita para acabarla?" to "Lucas 14:28",
        "Tesoro precioso y aceite hay en la casa del sabio." to "Proverbios 21:20"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pantalla completa (edge-to-edge): dejamos espacio para la barra de estado y la de navegación.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        // La librería (ControlAhorros) no sabe que existe Room: solo conoce AhorroRepository.
        control = ControlAhorros(AhorroRepositoryRoom(AppDatabase.obtener(this).ahorroDao()))

        // Versículo de arriba y de la bienvenida
        val (texto, referencia) = versiculos[Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % versiculos.size]
        binding.tvVersiculo.text = "“$texto”"
        binding.tvVersiculoRef.text = "— $referencia"
        binding.tvBienvenidaVersiculo.text = "“$texto”\n— $referencia"

        // Bienvenida: solo se muestra al abrir la app (no al girar la pantalla)
        val yaVioBienvenida = savedInstanceState?.getBoolean(CLAVE_BIENVENIDA, false) ?: false
        binding.welcomeOverlay.visibility = if (yaVioBienvenida) View.GONE else View.VISIBLE
        binding.btnContinuar.setOnClickListener { binding.welcomeOverlay.visibility = View.GONE }

        binding.spPlan.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, planes)

        binding.btnNuevaMeta.setOnClickListener { dialogoNuevaMeta() }
        binding.btnContinuarMeta.setOnClickListener { dialogoContinuarMeta() }
        binding.btnDepositar.setOnClickListener { registrar(Deposito.TIPO) }
        binding.btnRetirar.setOnClickListener { registrar(Retiro.TIPO) }
        binding.btnPlan.setOnClickListener { calcularPlan() }

        // Recordamos la última meta usada, para "continuar" donde se quedó el usuario.
        lifecycleScope.launch {
            val idGuardado = getSharedPreferences(PREFS, MODE_PRIVATE).getLong(CLAVE_META, Movimiento.AHORRO_GENERAL)
            metaActual = control.metas().firstOrNull { it.id == idGuardado }
            refrescar()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(CLAVE_BIENVENIDA, binding.welcomeOverlay.visibility == View.GONE)
    }

    // ------------------------------------------------------------------ Metas

    private fun dialogoNuevaMeta() {
        val form = DialogNuevaMetaBinding.inflate(layoutInflater)
        val dialogo = AlertDialog.Builder(this)
            .setTitle("Nueva meta")
            .setView(form.root)
            .setPositiveButton("Guardar", null)      // lo programamos abajo para que no se cierre si hay error
            .setNegativeButton("Cancelar", null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val nombre = form.etNombreMeta.text.toString()
                val objetivo = form.etObjetivoMeta.text.toString().replace(',', '.').toDoubleOrNull() ?: 0.0
                lifecycleScope.launch {
                    val error = control.agregarMeta(nombre, objetivo)
                    if (error != null) {
                        form.tvErrorMeta.text = error
                        form.tvErrorMeta.visibility = View.VISIBLE
                    } else {
                        // La meta nueva queda como meta actual (la última creada tiene el id más alto)
                        metaActual = control.metas().maxByOrNull { it.id }
                        guardarMetaActual()
                        dialogo.dismiss()
                        mensaje("Meta creada. ¡A ahorrar!")
                        refrescar()
                    }
                }
            }
        }
        dialogo.show()
    }

    private fun dialogoContinuarMeta() {
        lifecycleScope.launch {
            val metas = control.metas()
            if (metas.isEmpty()) {
                mensaje("Aún no tienes metas. Pulsa «Nueva meta» para crear la primera.")
                return@launch
            }
            // Primera opción: ahorro general; luego una por cada meta
            val opciones = mutableListOf("Ahorro general (${control.saldoDe(Movimiento.AHORRO_GENERAL).enQuetzales()})")
            for (m in metas) {
                opciones.add("${m.nombre}: ${control.ahorradoEn(m).enQuetzales()} de ${m.objetivo.enQuetzales()}")
            }
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Continuar meta")
                .setItems(opciones.toTypedArray()) { _, posicion ->
                    metaActual = if (posicion == 0) null else metas[posicion - 1]
                    guardarMetaActual()
                    lifecycleScope.launch { refrescar() }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun guardarMetaActual() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putLong(CLAVE_META, metaActual?.id ?: Movimiento.AHORRO_GENERAL)
            .apply()
    }

    // ------------------------------------------------------------- Movimientos

    private fun registrar(tipo: String) {
        val monto = binding.etMonto.text.toString().replace(',', '.').toDoubleOrNull()
        if (monto == null) {
            mensaje("Escribe un monto válido")
            return
        }
        val descripcion = binding.etDescripcion.text.toString()
        val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val metaId = metaActual?.id ?: Movimiento.AHORRO_GENERAL

        lifecycleScope.launch {
            val error = control.registrar(tipo, monto, descripcion, fecha, metaId)
            if (error != null) {
                mensaje(error)
            } else {
                binding.etMonto.text?.clear()
                binding.etDescripcion.text?.clear()
                refrescar()
            }
        }
    }

    private fun confirmarEliminar(mov: Movimiento) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar movimiento")
            .setMessage("¿Eliminar «${mov.descripcion}» (${mov.monto.enQuetzales()})?\nEsta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    control.eliminar(mov.id)
                    mensaje("Movimiento eliminado")
                    refrescar()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // -------------------------------------------------------------------- Plan

    private fun calcularPlan() {
        val ingreso = binding.etIngreso.text.toString().replace(',', '.').toDoubleOrNull()
        if (ingreso == null) {
            mensaje("Escribe tu ingreso mensual")
            return
        }
        val plan = binding.spPlan.selectedItem as PlanAhorro
        lifecycleScope.launch {
            binding.tvPlan.text = control.sugerencia(plan, ingreso, metaActual)
        }
    }

    // ----------------------------------------------------------------- Pantalla

    /** Vuelve a dibujar la tarjeta de la meta y el historial con los datos de la base. */
    private suspend fun refrescar() {
        val meta = metaActual
        val metaId = meta?.id ?: Movimiento.AHORRO_GENERAL

        // Tarjeta de la meta actual
        if (meta == null) {
            binding.tvAccion.text = "Ahorro general · pulsa «Nueva meta» para ponerte un objetivo"
            binding.tvMetaNombre.text = "Ahorro general"
            binding.tvMetaMonto.text = control.saldoDe(metaId).enQuetzales()
            binding.progresoMeta.visibility = View.GONE
            binding.tvMetaFaltante.text = "Aquí va lo que ahorras sin una meta concreta."
        } else {
            val ahorrado = control.ahorradoEn(meta)
            val faltante = control.faltante(meta)
            binding.tvAccion.text = "Continuando meta: ${meta.nombre}"
            binding.tvMetaNombre.text = meta.nombre
            binding.tvMetaMonto.text = ahorrado.enQuetzales()
            binding.progresoMeta.visibility = View.VISIBLE
            binding.progresoMeta.setProgressCompat((control.progreso(meta) * 100).toInt(), true)
            binding.tvMetaFaltante.text =
                if (faltante <= 0.0) "¡Meta alcanzada! Objetivo: ${meta.objetivo.enQuetzales()}"
                else "Te faltan ${faltante.enQuetzales()} para llegar a ${meta.objetivo.enQuetzales()}"
        }

        // Historial: una tarjeta por movimiento de ESTA meta, el más reciente arriba
        val movimientos = control.movimientos().filter { it.metaId == metaId }.reversed()
        binding.contenedorHistorial.removeAllViews()
        if (movimientos.isEmpty()) {
            val vacio = TextView(this)
            vacio.text = "Todavía no hay movimientos aquí."
            binding.contenedorHistorial.addView(vacio)
            return
        }
        val colorDeposito = MaterialColors.getColor(binding.root, androidx.appcompat.R.attr.colorPrimary)
        val colorRetiro = MaterialColors.getColor(binding.root, androidx.appcompat.R.attr.colorError)
        for (mov in movimientos) {
            val item = ItemMovimientoBinding.inflate(layoutInflater, binding.contenedorHistorial, false)
            item.tvDescripcion.text = mov.descripcion
            // POLIMORFISMO: etiqueta() se comporta distinto en Deposito y en Retiro
            item.tvMontoItem.text = mov.etiqueta()
            item.tvMontoItem.setTextColor(if (mov is Retiro) colorRetiro else colorDeposito)
            item.tvFechaItem.text = mov.fecha
            item.btnEliminarMov.setOnClickListener { confirmarEliminar(mov) }
            binding.contenedorHistorial.addView(item.root)
        }
    }

    private fun mensaje(texto: String) {
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val PREFS = "ahorros_prefs"
        private const val CLAVE_META = "meta_actual"
        private const val CLAVE_BIENVENIDA = "bienvenida_vista"
    }
}