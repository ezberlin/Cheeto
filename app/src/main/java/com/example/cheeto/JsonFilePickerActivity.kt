package com.example.cheeto

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cheeto.databinding.ActivityJsonFilePickerBinding
import java.io.File

class JsonFilePickerActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityJsonFilePickerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityJsonFilePickerBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val jsonFiles = listJsonFiles()
        val adapter = JsonFileAdapter(jsonFiles) { file ->
            val resultIntent = Intent().apply {
                putExtra("selectedFilePath", file.absolutePath)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        viewBinding.recyclerView.layoutManager = LinearLayoutManager(this)
        viewBinding.recyclerView.adapter = adapter
    }

    private fun listJsonFiles(): List<File> {
        val filesDir = filesDir
        return filesDir.listFiles { _, name -> name.startsWith("b") && name.endsWith(".json") }?.toList() ?: emptyList()
    }
}