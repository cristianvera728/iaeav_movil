import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * # ViewModel para la Pantalla de Carga (LoadingViewModel)
 *
 * Clase responsable de:
 *
 */

class LoadingViewModel : ViewModel() {
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    init {
        _isLoading.value = true
    }
    
    fun fetchDataFromServer() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Llamada al servidor aquí
                // apiService.getData()
                
                // Cuando reciba respuesta del servidor, cambiar isLoading a false
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }
}