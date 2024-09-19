package com.example.cheeto

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.cheeto.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    private var board: List<List<Int>> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.openCameraButton.setOnClickListener {
            val intent = Intent(this, BoardSettingActivity::class.java)
            startActivity(intent)
        }

        viewBinding.chooseJsonButton.setOnClickListener {
            val intent = Intent(this, JsonFilePickerActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_PICK_JSON)
        }

        viewBinding.solveButton.setOnClickListener {
            val numberInput = viewBinding.numberInput.text.toString()
            if (numberInput.isNotEmpty()) {
                val targetNumber = numberInput.toInt()
                val result = solveBoard(board, targetNumber)
                viewBinding.resultTextView.text = result
            } else {
                viewBinding.resultTextView.text = getString(R.string.please_enter_number)
            }
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_JSON && resultCode == RESULT_OK) {
            val selectedFilePath = data?.getStringExtra("selectedFilePath")
            selectedFilePath?.let {
                val file = File(it)
                val jsonContent = file.readText()
                Log.d(TAG, "Selected JSON Content: $jsonContent")

                // Deserialize JSON content to List<List<Int>>
                val gson = Gson()
                val type = object : TypeToken<List<List<Int>>>() {}.type
                board = gson.fromJson(jsonContent, type)
                Log.d(TAG, "Deserialized Board: $board")
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PICK_JSON = 1
    }

    private fun solveBoard(board: List<List<Int>>, n: Int): String {
        val combinationChecks = listOf(
            listOf(
                { x: Int, y: Int -> board[x][y] },
                { x: Int, y: Int -> board[x - 1][y - 1] },
                { x: Int, y: Int -> board[x - 2][y - 2] }
            ),
            listOf(
                { x: Int, y: Int -> board[x][y] },
                { x: Int, y: Int -> board[x][y - 1] },
                { x: Int, y: Int -> board[x][y - 2] }
            ),
            listOf(
                { x: Int, y: Int -> board[x][y] },
                { x: Int, y: Int -> board[x + 1][y - 1] },
                { x: Int, y: Int -> board[x + 2][y - 2] }
            ),
            listOf(
                { x: Int, y: Int -> board[x][y] },
                { x: Int, y: Int -> board[x + 1][y] },
                { x: Int, y: Int -> board[x + 2][y] }
            ),
            listOf(
                { x: Int, y: Int -> board[x][y] },
                { x: Int, y: Int -> board[x + 1][y + 1] },
                { x: Int, y: Int -> board[x + 2][y + 2] }
            ),
            listOf(
                { x: Int, y: Int -> board[x][y] },
                { x: Int, y: Int -> board[x][y + 1] },
                { x: Int, y: Int -> board[x][y + 2] }
            ),
            listOf(
                { x: Int, y: Int -> board[x][y] },
                { x: Int, y: Int -> board[x - 1][y + 1] },
                { x: Int, y: Int -> board[x - 2][y + 2] }
            ),
            listOf(
                { x: Int, y: Int -> board[x][y] },
                { x: Int, y: Int -> board[x - 1][y] },
                { x: Int, y: Int -> board[x - 2][y] }
            ),
        )

        val letterList = listOf("A", "B", "C", "D", "E", "F", "G")

        for (x in 0..6) for (y in 0..6) {
            for (check in combinationChecks) try {
                val additionSum = check[0](x, y) * check[1](x, y) + check[2](x, y)
                val subtractionSum = check[0](x, y) * check[1](x, y) - check[2](x, y)
                if (additionSum == n || subtractionSum == n) return "${check[0](x, y)}, ${check[1](x, y)}, ${check[2](x, y)}, starting from ${letterList[y]}${x+1}"

            } catch (_: IndexOutOfBoundsException) {}
        }

        return getString(R.string.not_found)
    }
}