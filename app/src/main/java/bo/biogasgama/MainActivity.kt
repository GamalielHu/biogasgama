package bo.biogasgama

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import bo.biogasgama.ui.AppBiogas
import org.osmdroid.config.Configuration
import java.io.File

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    private val pedirUbicacion = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.gnss.iniciar()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = File(filesDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "teselas")
        }
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
