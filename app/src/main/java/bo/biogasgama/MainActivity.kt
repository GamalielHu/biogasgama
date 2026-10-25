package bo.biogasgama

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import bo.biogasgama.ui.AppBiogas

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    private val pedirUbicacion = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.gnss.iniciar()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!vm.gnss.tienePermiso()) {
            pedirUbicacion.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        setContent { AppBiogas(vm) }
    }

    override fun onStart() {
        super.onStart()
        vm.gnss.iniciar()
    }

    override fun onStop() {
        vm.gnss.detener()
        super.onStop()
    }
}
