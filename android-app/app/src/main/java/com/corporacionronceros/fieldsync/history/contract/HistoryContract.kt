package com.corporacionronceros.fieldsync.history.contract

import com.corporacionronceros.fieldsync.history.model.ServiceRecord

/**
 * Contrato MVP: define las fronteras entre View y Presenter mediante interfaces.
 *
 * Diferencia clave con MVVM: aquí el Presenter SOSTIENE una referencia explícita a la
 * View y la comanda con llamadas imperativas (showRecords, showError). En MVVM el
 * ViewModel expone estado observable y NUNCA conoce a la vista. Este módulo existe
 * para evidenciar que comprendo ambos patrones y la ruta de migración entre ellos.
 */
interface HistoryContract {

    /** La View (Activity/Fragment) es pasiva: solo pinta lo que el Presenter le ordena. */
    interface View {
        fun showLoading()
        fun hideLoading()
        fun showRecords(records: List<ServiceRecord>)
        fun showError(message: String)
    }

    /** El Presenter contiene la lógica de presentación y dirige a la View. */
    interface Presenter {
        fun attach(view: View)
        fun detach()          // evita fugas de memoria al soltar la referencia a la View
        fun loadHistory()
    }
}
