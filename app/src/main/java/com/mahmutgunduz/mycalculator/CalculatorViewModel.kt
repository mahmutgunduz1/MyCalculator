package com.mahmutgunduz.mycalculator

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * Hesap makinesi iş mantığını ve kullanıcıdan gelen ifadeleri işleyen ViewModel.
 * Tüm hesaplama ve hata kontrolü burada yapılır.
 * Artık ondalık ve yüzde desteği de vardır.
 */
class CalculatorViewModel : ViewModel() {
    // Kullanıcının girdiği matematiksel ifade
    private val _input = MutableLiveData("")
    val input: LiveData<String> = _input

    // Hesaplanan sonuç veya hata mesajı
    private val _result = MutableLiveData("")
    val result: LiveData<String> = _result

    // Hata mesajı (Snackbar veya TextView ile gösterilebilir)
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    // Kullanıcıdan gelen tuş girişini işler
    fun onButtonClick(value: String) {
        when (value) {
            "C" -> clearAll()
            "DEL" -> deleteLast()
            "=" -> calculateResult()
            "." -> appendDot()
            "%" -> applyPercent()
            else -> appendInput(value)
        }
    }

    // İfadeye yeni karakter ekler
    private fun appendInput(value: String) {
        val operators = setOf('+', '-', '*', '/')
        val current = _input.value ?: ""
        if (current.isEmpty() && value in listOf("+", "-", "*", "/")) {
            _error.value = "İfade operatör ile başlayamaz."
            return
        }
        if (current.isNotEmpty() && value in listOf("+", "-", "*", "/")) {
            if (current.last() in operators) {
                _error.value = "Arka arkaya iki operatör kullanılamaz."
                return
            }
        }
        _input.value += value
        _error.value = null
    }

    // Nokta (.) ekler, bir sayıda birden fazla nokta olamaz
    private fun appendDot() {
        val current = _input.value ?: ""
        // Son sayı parçasında nokta var mı kontrol et
        val lastNumber = current.takeLastWhile { it.isDigit() || it == '.' }
        if (lastNumber.contains('.')) {
            _error.value = "Bir sayıda birden fazla nokta olamaz."
            return
        }
        // Eğer ifade boşsa başa nokta eklenemez
        if (current.isEmpty() || current.last() in setOf('+', '-', '*', '/')) {
            _input.value += "0."
        } else {
            _input.value += "."
        }
        _error.value = null
    }

    // Son karakteri siler
    private fun deleteLast() {
        val current = _input.value ?: ""
        if (current.isNotEmpty()) {
            _input.value = current.dropLast(1)
        }
        _error.value = null
    }

    // Tüm girişi temizler
    private fun clearAll() {
        _input.value = ""
        _result.value = ""
        _error.value = null
    }

    // Yüzde tuşu: Son sayıyı yüzdeye çevirir (ör: 50% -> 0.5)
    private fun applyPercent() {
        val current = _input.value ?: ""
        if (current.isEmpty()) {
            _error.value = "Önce bir sayı girin."
            return
        }
        // Son sayı parçasını bul
        val lastNumber = current.takeLastWhile { it.isDigit() || it == '.' }
        if (lastNumber.isEmpty()) {
            _error.value = "Yüzde uygulanacak sayı yok."
            return
        }
        val percentValue = try {
            lastNumber.toDouble() / 100.0
        } catch (e: Exception) {
            _error.value = "Geçersiz sayı."
            return
        }
        // Son sayıyı yüzdeye çevirip ifadeye ekle
        val newInput = current.dropLast(lastNumber.length) + percentValue.toString().trimEnd('0').trimEnd('.')
        _input.value = newInput
        _error.value = null
    }

    // Sonucu hesaplar
    private fun calculateResult() {
        val expr = _input.value ?: ""
        if (expr.isEmpty()) {
            _error.value = "Önce bir ifade girin."
            return
        }
        try {
            val result = evaluate(expr)
            _result.value = result
            _input.value = result
            _error.value = null
        } catch (e: Exception) {
            _result.value = ""
            _error.value = "Geçersiz ifade. Lütfen kontrol edin."
        }
    }

    /**
     * Matematiksel ifadeyi öncelik kurallarına göre değerlendirir.
     * Sonuç tam sayıysa tam, ondalıklıysa ondalıklı olarak döner.
     *
     * @throws Exception Geçersiz ifade durumunda fırlatılır.
     */
    private fun evaluate(expression: String): String {
        // Sadece izin verilen karakterler
        val cleanExpr = expression.replace(Regex("[^0-9+\\-*/.]"), "")
        // Basit bir parser ile işlem önceliği uygulanır
        class Parser(val expr: String) {
            var pos = -1
            var ch = 0
            fun nextChar() { ch = if (++pos < expr.length) expr[pos].code else -1 }
            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) { nextChar(); return true }
                return false
            }
            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < expr.length) throw RuntimeException("Geçersiz ifade")
                return x
            }
            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+'.code) -> x += parseTerm()
                        eat('-'.code) -> x -= parseTerm()
                        else -> return x
                    }
                }
            }
            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*'.code) -> x *= parseFactor()
                        eat('/'.code) -> {
                            val divisor = parseFactor()
                            if (divisor == 0.0) throw ArithmeticException("Sıfıra bölme hatası")
                            x /= divisor
                        }
                        else -> return x
                    }
                }
            }
            fun parseFactor(): Double {
                val startPos = pos
                while (ch in '0'.code..'9'.code || ch == '.'.code) nextChar()
                if (startPos == pos) throw RuntimeException("Sayı bekleniyor")
                return expr.substring(startPos, pos).toDouble()
            }
        }
        val result = Parser(cleanExpr).parse()
        // Sonuç tam sayıysa tam, ondalıklıysa ondalıklı göster
        return if (result % 1.0 == 0.0) result.toInt().toString() else result.toString().trimEnd('0').trimEnd('.')
    }

    // Hata mesajı gösterildikten sonra sıfırlanır
    fun clearError() {
        _error.value = null
    }
} 