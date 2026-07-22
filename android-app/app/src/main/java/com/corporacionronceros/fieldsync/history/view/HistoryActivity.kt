package com.corporacionronceros.fieldsync.history.view

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.corporacionronceros.fieldsync.R
import com.corporacionronceros.fieldsync.history.contract.HistoryContract
import com.corporacionronceros.fieldsync.history.model.ServiceRecord
import com.corporacionronceros.fieldsync.history.presenter.HistoryPresenter

/**
 * View pasiva del patrón MVP (usa Views clásicas de Android, no Compose, a propósito:
 * representa la pantalla "heredada"). Instancia el Presenter y le delega la lógica;
 * gestiona attach/detach en el ciclo de vida para no filtrar memoria.
 */
class HistoryActivity : AppCompatActivity(), HistoryContract.View {

    private val presenter: HistoryContract.Presenter = HistoryPresenter()
    private lateinit var listView: ListView
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        listView = findViewById(R.id.list_view)
        progress = findViewById(R.id.progress)
        findViewById<TextView>(R.id.back_button).setOnClickListener { finish() }

        presenter.attach(this)
        presenter.loadHistory()
    }

    override fun onDestroy() {
        presenter.detach()
        super.onDestroy()
    }

    override fun showLoading() { progress.visibility = View.VISIBLE }
    override fun hideLoading() { progress.visibility = View.GONE }

    override fun showRecords(records: List<ServiceRecord>) {
        val labels = records.map { "${it.completedOn} · ${it.title} (${it.customerName})" }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
