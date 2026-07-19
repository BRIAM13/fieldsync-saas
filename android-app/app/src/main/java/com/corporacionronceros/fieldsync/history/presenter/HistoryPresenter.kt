package com.corporacionronceros.fieldsync.history.presenter

import com.corporacionronceros.fieldsync.history.contract.HistoryContract
import com.corporacionronceros.fieldsync.history.model.ServiceRecord

/**
 * Presenter MVP clásico: mantiene una referencia (nullable) a la View y la comanda
 * imperativamente. El manejo manual del ciclo de vida (attach/detach) es justo lo que
 * MVVM + lifecycle resuelven automáticamente — de ahí el valor didáctico del contraste.
 */
class HistoryPresenter : HistoryContract.Presenter {

    private var view: HistoryContract.View? = null

    override fun attach(view: HistoryContract.View) {
        this.view = view
    }

    override fun detach() {
        this.view = null // liberar para no filtrar la Activity destruida
    }

    override fun loadHistory() {
        view?.showLoading()
        try {
            // En una app real vendría de una base de datos o API; aquí datos de ejemplo.
            val records = sampleHistory()
            view?.hideLoading()
            view?.showRecords(records)
        } catch (e: Exception) {
            view?.hideLoading()
            view?.showError(e.message ?: "Error al cargar el historial")
        }
    }

    private fun sampleHistory() = listOf(
        ServiceRecord("WO-0990", "Cambio de grifería", "10/07/2026", "Sra. Rojas"),
        ServiceRecord("WO-0987", "Reparación de cortocircuito", "08/07/2026", "Bodega Central"),
        ServiceRecord("WO-0981", "Desatoro de desagüe", "05/07/2026", "Edificio Miraflores")
    )
}
