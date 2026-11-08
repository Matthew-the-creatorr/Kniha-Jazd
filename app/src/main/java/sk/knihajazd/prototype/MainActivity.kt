package sk.knihajazd.prototype

import android.os.Bundle
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import android.content.Context
import android.graphics.BitmapFactory
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog

private val Violet = Color(0xFFB368FF)
private val Bright = Color(0xFFF8F7FF)
private val Muted = Color(0xFFB5B5C5)
private val Charcoal = Color(0xFF090B12)
private val allBrands = listOf(
    "Alfa Romeo","Audi","BMW","Citroën","Cupra","Dacia","Fiat","Ford","Honda",
    "Hyundai","Jaguar","Jeep","Kia","Land Rover","Lexus","Mazda","Mercedes-Benz",
    "Mini","Mitsubishi","Nissan","Opel","Peugeot","Porsche","Renault","Seat",
    "Škoda","Subaru","Suzuki","Tesla","Toyota","Volkswagen","Volvo","Iná značka"
)
private val carModels = mapOf(
    "Audi" to listOf("A1","A3","A4","A5","A6","A7","A8","Q2","Q3","Q5","Q7","Q8"),
    "BMW" to listOf("Rad 1","Rad 2","Rad 3","Rad 4","Rad 5","Rad 7","X1","X3","X5","X6","X7"),
    "Mercedes-Benz" to listOf("Trieda A","Trieda B","Trieda C","Trieda E","Trieda S","CLA","GLA","GLC","GLE"),
    "Porsche" to listOf("Panamera","Cayenne","Macan","911","Taycan","718 Cayman"),
    "Škoda" to listOf("Fabia","Scala","Octavia","Superb","Kamiq","Karoq","Kodiaq","Enyaq"),
    "Volkswagen" to listOf("Polo","Golf","Passat","Tiguan","Touareg","Arteon","ID.3","ID.4"),
    "Toyota" to listOf("Yaris","Corolla","Camry","C-HR","RAV4","Land Cruiser","Prius"),
    "Ford" to listOf("Fiesta","Focus","Mondeo","Puma","Kuga","Mustang","Ranger"),
    "Hyundai" to listOf("i10","i20","i30","Tucson","Kona","Santa Fe"),
    "Kia" to listOf("Ceed","Rio","Sportage","Sorento","Stonic","EV6"),
    "Renault" to listOf("Clio","Megane","Captur","Austral","Scenic","Trafic"),
    "Peugeot" to listOf("208","308","408","508","2008","3008","5008"),
    "Opel" to listOf("Corsa","Astra","Insignia","Mokka","Grandland"),
    "Honda" to listOf("Civic","Jazz","CR-V","HR-V"),
    "Mazda" to listOf("Mazda 2","Mazda 3","Mazda 6","CX-3","CX-5","CX-60"),
    "Tesla" to listOf("Model 3","Model Y","Model S","Model X")
)
private val colors = listOf("Čierna","Biela","Strieborná","Sivá","Červená","Modrá","Zelená","Fialová","Žltá","Vlastná")
private val finishes = listOf("Lesklý","Matný","Metalíza","Perleťový","Saténový","Fólia")
private val titles = listOf("Záznam","Moja analytika","Moje vozidlo")

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(5,6,10)
        window.navigationBarColor = android.graphics.Color.rgb(5,6,10)
        window.decorView.systemUiVisibility = 0
        setContent { MainApp() }
    }
}

@Composable
private fun MainApp() {
    val context = LocalContext.current
    val p = remember { context.getSharedPreferences("kniha_jazd_demo_v2",Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var availableBrands by remember { mutableStateOf(allBrands) }
    var availableModels by remember { mutableStateOf(carModels["Porsche"] ?: emptyList()) }
    var loadingBrands by remember { mutableStateOf(false) }
    var loadingModels by remember { mutableStateOf(false) }
    var catalogStatus by remember { mutableStateOf("") }
    var aiServer by remember { mutableStateOf(p.getString("ai_server","") ?: "") }
    var aiToken by remember { mutableStateOf("") }
    var showAiConfig by remember { mutableStateOf(false) }
    var generating by remember { mutableStateOf(false) }
    var aiImagePath by remember { mutableStateOf(p.getString("ai_image","") ?: "") }
    var aiImageSpec by remember { mutableStateOf(p.getString("ai_spec","") ?: "") }
    var aiRevision by remember { mutableIntStateOf(0) }
    var setup by remember { mutableStateOf(!p.getBoolean("setup_complete",false)) }
    var driver by remember { mutableStateOf(p.getString("driver","") ?: "") }
    var pwd by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf(p.getString("brand","Porsche") ?: "Porsche") }
    var customBrand by remember { mutableStateOf(p.getString("custom_brand","") ?: "") }
    var model by remember { mutableStateOf(p.getString("model","Panamera") ?: "Panamera") }
    var customModel by remember { mutableStateOf(p.getString("custom","") ?: "") }
    var editableModel by remember { mutableStateOf(
        if ((p.getString("model","Panamera") ?: "Panamera") == "Iný model")
            p.getString("custom","") ?: "" else p.getString("model","Panamera") ?: "Panamera"
    ) }
    var reg by remember { mutableStateOf(p.getString("plate","") ?: "") }
    var year by remember { mutableStateOf(p.getString("year","2021") ?: "2021") }
    var paint by remember { mutableStateOf(p.getString("paint","Čierna") ?: "Čierna") }
    var finish by remember { mutableStateOf(p.getString("finish","Lesklý") ?: "Lesklý") }
    var tab by remember { mutableIntStateOf(0) }
    var isTrip by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var showFinish by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var showAlerts by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var privacy by remember { mutableStateOf("Služobná") }
    var message by remember { mutableStateOf("") }
    val db = remember(context) { DriveDatabase(context.applicationContext) }
    var revision by remember { mutableIntStateOf(0) }
    val journeys = remember(revision) { db.journeys() }
    val receipts = remember(revision) { db.receipts() }
    var selectedDrive by remember { mutableStateOf<DriveRecord?>(null) }
    var firstTown by remember { mutableStateOf("Neznáme miesto") }
    var lastTown by remember { mutableStateOf("Neznáme miesto") }
    var visibleSeconds by remember { mutableLongStateOf(0L) }
    var averageFuel by remember { mutableStateOf(db.getSetting("avg_fuel","6.5")) }
    var receiptUri by remember { mutableStateOf<Uri?>(null) }
    var pendingReceiptFile by remember { mutableStateOf<File?>(null) }
    var receiptReview by remember { mutableStateOf(false) }
    var receiptDate by remember { mutableStateOf("") }
    var receiptLiters by remember { mutableStateOf("") }
    var receiptFuel by remember { mutableStateOf("") }
    var receiptTotal by remember { mutableStateOf("") }
    LaunchedEffect(LiveDrive.active) {
        while(LiveDrive.active) {
            visibleSeconds=(android.os.SystemClock.elapsedRealtime()-LiveDrive.startElapsed)/1000L
            delay(1000)
        }
    }
    LaunchedEffect(setup,revision) {
        while(!setup) {
            if(db.eligiblePair()!=null && !showAlerts) {
                showAlerts=true
                break
            }
            delay(60000)
        }
    }
    var customPhotoPath by remember { mutableStateOf(p.getString("custom_photo","") ?: "") }
    var photoRevision by remember { mutableIntStateOf(0) }
    val chosenBrand = if(brand=="Iná značka") customBrand.trim() else brand.trim()
    val chosenModel = editableModel.trim()
    val carSpec = listOf(chosenBrand, chosenModel, year, paint, finish).joinToString("|")
    val isKodiaqSilver = chosenBrand.equals("Škoda",ignoreCase=true) &&
        chosenModel.equals("Kodiaq",ignoreCase=true) &&
        (paint == "Strieborná" || paint == "Sivá")
    val generatedPhoto = remember(aiImagePath, aiImageSpec, carSpec, aiRevision) {
        if (aiImageSpec == carSpec && aiImagePath.isNotBlank()) decodeVehicleBitmap(aiImagePath)
        else null
    }
    val generateNow: () -> Unit = {
        if (aiServer.isBlank() || aiToken.isBlank()) {
            message = "Automatické AI generovanie ešte nemá pripojenú službu. Môžeš zatiaľ použiť pripravené pozadie Kodiaqu alebo vlastnú fotografiu."
        } else if (chosenBrand.isBlank() || chosenModel.isBlank()) {
            message = "Najprv vyber značku a model vozidla."
        } else {
            generating = true
            scope.launch {
                try {
                    val saved = GeminiBackground.generate(
                        aiServer, aiToken, chosenBrand, chosenModel, year,
                        paint, finish, File(context.filesDir,"gemini_vehicle_wallpaper.png")
                    )
                    aiImagePath = saved
                    aiImageSpec = carSpec
                    aiRevision++
                    p.edit().putString("ai_image",saved).putString("ai_spec",carSpec)
                        .putString("ai_server",aiServer).apply()
                    customPhotoPath = ""
                    p.edit().remove("custom_photo").remove("custom_photo_spec").apply()
                    message = "AI pozadie bolo vytvorené pre $chosenBrand $chosenModel."
                } catch (e: Exception) {
                    message = "Generovanie zlyhalo: " + (e.message ?: "Skontroluj server.")
                } finally { generating = false }
            }
        }
    }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val destination = File(context.filesDir, "my_car_photo.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destination.outputStream().use { output -> input.copyTo(output) }
                }
                if (destination.exists() && destination.length() > 0) {
                    customPhotoPath = destination.absolutePath
                    p.edit().putString("custom_photo", customPhotoPath).putString("custom_photo_spec", carSpec).apply()
                    photoRevision++
                    message = "Tvoja fotografia je nastavená ako pozadie vozidla."
                }
            } catch (e: Exception) {
                message = "Fotografiu sa nepodarilo načítať. Skús inú."
            }
        }
    }

    fun processReceipt(uri:Uri) {
        receiptUri=uri
        ReceiptReader.recognize(context,uri) { found ->
            if(found==null) message="Nepodarilo sa prečítať obrázok. Skús ostrejšiu fotku."
            else {
                receiptDate=found.date.ifBlank {dateFormat(System.currentTimeMillis())}
                receiptLiters=found.liters
                receiptFuel=found.fuel
                receiptTotal=found.amount
                receiptReview=true
            }
        }
    }
    val receiptCamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {ok->
        if(ok) pendingReceiptFile?.let { target ->
            val uri=FileProvider.getUriForFile(context,context.packageName+".fileprovider",target)
            processReceipt(uri)
        } else message="Fotografovanie bolo zrušené."
    }
    val receiptGallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri->
        if(uri!=null)processReceipt(uri)
    }
    val gpsPermissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {granted->
        if(granted[Manifest.permission.ACCESS_FINE_LOCATION]==true) {
            ContextCompat.startForegroundService(context,Intent(context,DrivingService::class.java).apply{action="START"})
        } else message="GPS vyžaduje povolenie presnej polohy. V nastaveniach povoľ presnú polohu."
    }
    val beginDrive:()->Unit={
        if(LiveDrive.active) message="Jazda už prebieha."
        else if(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
            ContextCompat.startForegroundService(context,Intent(context,DrivingService::class.java).apply{action="START"})
        } else {
            gpsPermissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.POST_NOTIFICATIONS))
        }
    }
    val userPhoto = remember(customPhotoPath, photoRevision,carSpec) {
        if(customPhotoPath.isEmpty() || p.getString("custom_photo_spec","") != carSpec) null else try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(customPhotoPath, bounds)
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            val sample = maxOf(1, longest / 1700)
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(customPhotoPath, options)?.asImageBitmap()
        } catch(e: Exception) { null }
    }
    val photoDrawable = when (paint) {
        "Biela" -> R.drawable.garage_white
        "Sivá" -> R.drawable.garage_grey
        "Strieborná" -> R.drawable.garage_silver
        "Červená" -> R.drawable.garage_red
        "Modrá" -> R.drawable.garage_blue
        "Zelená" -> R.drawable.garage_green
        "Fialová" -> R.drawable.garage_purple
        "Žltá" -> R.drawable.garage_yellow
        else -> R.drawable.garage_black
    }
    val pan by animateFloatAsState(targetValue=when(tab){0 -> 36f;1 -> 0f;else -> -36f}, animationSpec=tween(680),label="background-camera")
    MaterialTheme(colorScheme=darkColorScheme(primary=Violet,onPrimary=Color.Black,background=Charcoal,
        onBackground=Bright,surface=Color(0xFF1C2030),onSurface=Bright,onSurfaceVariant=Muted)) {
        CompositionLocalProvider(LocalContentColor provides Bright) {
            Box(Modifier.fillMaxSize().background(Charcoal)) {
                if(userPhoto != null || generatedPhoto != null || isKodiaqSilver ||
                    (chosenBrand=="Porsche" && chosenModel=="Panamera")) {
                    Image(
                        painter=when {
                            userPhoto != null -> BitmapPainter(userPhoto)
                            generatedPhoto != null -> BitmapPainter(generatedPhoto)
                            isKodiaqSilver -> painterResource(R.drawable.garage_kodiaq_silver)
                            else -> painterResource(photoDrawable)
                        },
                        contentDescription="Vybraná fotografia alebo AI obrázok vozidla",
                        contentScale=ContentScale.Crop,
                        alignment=Alignment.TopCenter,
                        modifier=Modifier.fillMaxSize().graphicsLayer {
                            translationX=pan
                            scaleX=1.04f
                            scaleY=1.04f
                        }
                    )
                } else {
                    EmptyShowroom()
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                    colorStops=arrayOf(
                        0.0f to Color(0xF404050A),
                        .16f to Color(0x9904050B),
                        .33f to Color(0x1004050B),
                        .53f to Color(0x25060911),
                        .77f to Color(0xB8080A13),
                        1f to Color(0xFB05060C)
                    )
                )))
                if(setup) {
                    LazyColumn(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        item {
                            Spacer(Modifier.height(24.dp))
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Text("◢",fontSize=38.sp,fontWeight=FontWeight.Black,color=Violet)
                                Spacer(Modifier.width(12.dp))
                                Column { Text("Kniha jázd",fontWeight=FontWeight.Bold,fontSize=28.sp,color=Bright)
                                    Text("Vstúp do svojej garáže",color=Muted,fontSize=13.sp) }
                            }
                            Spacer(Modifier.height(115.dp))
                        }
                        item { GlassPanel {
                            Text("01  Profil vodiča",color=Violet,fontWeight=FontWeight.Bold,fontSize=14.sp)
                            DemoTextField(driver,{driver=it},"Meno")
                            DemoTextField(pwd,{pwd=it},"Heslo (len ukážka)",true)
                        } }
                        item { GlassPanel {
                            Text("02  Konfigurácia vozidla",color=Violet,fontWeight=FontWeight.Bold,fontSize=14.sp)
                            Pick("Značka",allBrands,brand) {
                                brand=it
                            }
                            if(brand=="Iná značka") DemoTextField(customBrand,{customBrand=it},"Názov značky")
                            DemoTextField(editableModel,{editableModel=it},"Model auta (napríklad Kodiaq)")
                            Text("Môžeš napísať akýkoľvek model, bez hľadania v katalógu.",
                                color=Muted,fontSize=11.sp)
                            Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                                Box(Modifier.weight(1f)){DemoTextField(year,{year=it},"Rok")}
                                Box(Modifier.weight(1f)){DemoTextField(reg,{reg=it},"EČV / ŠPZ")}
                            }
                            Pick("Farba karosérie",colors,paint){paint=it}
                            Pick("Povrch",finishes,finish){finish=it}
                            OutlinedButton(onClick={photoLauncher.launch("image/*")}, modifier=Modifier.fillMaxWidth(),
                                shape=RoundedCornerShape(14.dp)) {
                                Icon(Icons.Rounded.AddPhotoAlternate,null)
                                Spacer(Modifier.width(8.dp))
                                Text("Pridať fotku svojho auta",color=Bright)
                            }
                            if (customPhotoPath.isNotEmpty()) {
                                TextButton(onClick={
                                    customPhotoPath=""
                                    p.edit().remove("custom_photo").remove("custom_photo_spec").apply()
                                    photoRevision++
                                }) { Text("Odstrániť vlastnú fotku") }
                            }
                            Text("Bez vytvoreného AI obrázka sa cudzie auto nebude vydávať za vybraný model.",
                                fontSize=11.sp,color=Muted)
                            if (isKodiaqSilver) Text("Vlastné realistické pozadie Škoda Kodiaq je pripravené.",
                                color=Violet,fontSize=12.sp)
                            else Text("Pre tento model môžeš použiť vlastnú fotografiu. Online generovanie ešte nie je aktivované.",
                                color=Muted,fontSize=12.sp)
                        } }
                        item {
                            Button(
                                onClick={
                                    p.edit().putBoolean("setup_complete",true).putString("driver",driver)
                                        .putString("brand",brand).putString("custom_brand",customBrand)
                                        .putString("model","Iný model").putString("custom",editableModel)
                                        .putString("plate",reg).putString("year",year).putString("paint",paint)
                                        .putString("finish",finish).apply()
                                    pwd="";setup=false
                                    // Pozadie sa zmení automaticky podľa zvoleného modelu a farby.
                                },
                                modifier=Modifier.fillMaxWidth().height(54.dp),
                                shape=RoundedCornerShape(18.dp),
                                colors=ButtonDefaults.buttonColors(containerColor=Violet)
                            ) { Text("Vstúpiť do garáže",fontWeight=FontWeight.Bold,color=Color(0xFF10051E)) }
                            Text("V0.6 · GPS záznam a súkromné kilometre. Heslo sa neukladá.",
                                fontSize=11.sp,color=Muted,modifier=Modifier.padding(top=8.dp,bottom=40.dp))
                        }
                    }
                } else {
                    Column(
                    Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                        .pointerInput(tab) {
                            var swipeDistance = 0f
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (swipeDistance < -75f) tab=(tab+1).coerceAtMost(2)
                                    if (swipeDistance > 75f) tab=(tab-1).coerceAtLeast(0)
                                    swipeDistance=0f
                                }
                            ) { change, delta ->
                                change.consume()
                                swipeDistance += delta
                            }
                        }
                ) {
                        Row(Modifier.fillMaxWidth().padding(start=15.dp,end=12.dp,top=13.dp,bottom=8.dp),
                            verticalAlignment=Alignment.CenterVertically) {
                            Text("◢",fontSize=31.sp,fontWeight=FontWeight.Black,color=Violet)
                            Column(Modifier.weight(1f).padding(start=9.dp)) {
                                Text("Kniha jázd",fontSize=20.sp,fontWeight=FontWeight.ExtraBold,color=Bright)
                                Text("Evidencia jázd · v0.6",fontSize=11.sp,color=Muted)
                            }
                            HeaderButton(Icons.Rounded.PlayArrow,"Štart",true){beginDrive()}
                            Spacer(Modifier.width(5.dp))
                            HeaderButton(Icons.Rounded.PhotoCamera,"Tankovací bloček",false){
                                try {
                                    val temp=File(context.cacheDir,"receipt_"+System.currentTimeMillis()+".jpg")
                                    pendingReceiptFile=temp
                                    receiptCamera.launch(FileProvider.getUriForFile(context,context.packageName+".fileprovider",temp))
                                }catch(e:Exception){message="Fotoaparát sa nepodarilo otvoriť: "+(e.message?:"")}
                            }
                            Spacer(Modifier.width(5.dp))
                            HeaderButton(Icons.Rounded.NotificationsNone,"Upozornenia",false){showAlerts=true}
                            Spacer(Modifier.width(7.dp))
                            HeaderButton(Icons.Rounded.PersonOutline,"Profil",false){showProfile=true}
                        }
                        if(LiveDrive.active) GlassPanel(Modifier.padding(horizontal=15.dp).padding(top=8.dp)) {
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("● GPS jazda prebieha",color=Violet,fontWeight=FontWeight.SemiBold)
                                    Text(durationFormat(visibleSeconds*1000L)+" · "+shownKm(LiveDrive.km),
                                        fontSize=12.sp,color=Bright)
                                    if(LiveDrive.gaps>0) Text("Výpadky GPS: "+LiveDrive.gaps,fontSize=10.sp,color=Muted)
                                }
                                TextButton(onClick={
                                    val snap=LiveDrive.finish()
                                    context.startService(Intent(context,DrivingService::class.java).apply{action="STOP"})
                                    if(snap!=null) {
                                        firstTown="Neznáme miesto";lastTown="Neznáme miesto"
                                        scope.launch{
                                            firstTown=withContext(Dispatchers.IO){cityFromLocation(context,snap.first)}
                                            lastTown=withContext(Dispatchers.IO){cityFromLocation(context,snap.last)}
                                        }
                                        showFinish=true
                                    }
                                }){Text("Koniec")}
                            }
                        }
                        Text(
                            chosenBrand + " " + chosenModel + "  ·  " + paint,
                            modifier=Modifier.padding(horizontal=20.dp).padding(top=3.dp),
                            fontSize=11.sp,color=Bright.copy(alpha=.85f),fontWeight=FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.fillMaxWidth()) {
                            Crossfade(targetState=tab,animationSpec=tween(480),label="garage-head-turn") { page ->
                                Column(Modifier.fillMaxWidth().padding(horizontal=16.dp),
                                    verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                    when(page){
                                        0 -> GlassPanel {
                                            DrivesOverview(journeys,onOpen={selectedDrive=it})
                                        }
                                        1 -> GlassPanel {
                                            Row(verticalAlignment=Alignment.CenterVertically) {
                                                SquareIcon(Icons.Rounded.BarChart)
                                                Spacer(Modifier.width(10.dp))
                                                Column {
                                                    Text("Moja analytika",fontSize=21.sp,fontWeight=FontWeight.Bold)
                                                    Text("Prehľad spotreby a nákladov",fontSize=11.sp,color=Muted)
                                                }
                                            }
                                            Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                                                Statistic("GPS kilometre",shownKm(journeys.filter{!it.simulated}.sumOf{it.km}),Modifier.weight(1f))
                                                Statistic("Súkromný zostatok",shownKm(db.privateBalance()),Modifier.weight(1f))
                                            }
                                            Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                                                Statistic("Tankovania",receipts.size.toString(),Modifier.weight(1f))
                                                Statistic("Počet jázd",journeys.size.toString(),Modifier.weight(1f))
                                            }
                                            Text("Najazdené kilometre · mesačne",color=Muted,fontSize=12.sp)
                                            Box(Modifier.fillMaxWidth().height(100.dp)
                                                .border(1.dp,Color.White.copy(alpha=.1f),RoundedCornerShape(12.dp)),
                                                contentAlignment=Alignment.Center) {
                                                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                                                    Icon(Icons.Rounded.ShowChart,null,tint=Violet,modifier=Modifier.size(28.dp))
                                                    Text("GPS a modelové jazdy sa evidujú osobitne.",color=Muted,fontSize=11.sp)
                                                }
                                            }
                                        }
                                        else -> GlassPanel {
                                            Row(verticalAlignment=Alignment.CenterVertically) {
                                                SquareIcon(Icons.Rounded.DirectionsCar)
                                                Spacer(Modifier.width(10.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text("Moje vozidlo",fontSize=21.sp,fontWeight=FontWeight.Bold)
                                                    Text("Technické údaje",fontSize=11.sp,color=Muted)
                                                }
                                                IconButton(onClick={setup=true}) {Icon(Icons.Rounded.Edit,"Upraviť vozidlo",tint=Violet)}
                                            }
                                            InfoRow("Značka",chosenBrand)
                                            InfoRow("Model",chosenModel.ifBlank{"Neuvedený"})
                                            InfoRow("Rok výroby",year)
                                            InfoRow("ŠPZ / EČV",reg.ifBlank{"Nevyplnené"})
                                            InfoRow("Farba / povrch",paint+" · "+finish)
                                            InfoRow("Objem nádrže","Nevyplnené")
                                            InfoRow("Spotreba",averageFuel+" l/100 km (odhad)")
                                            OutlinedTextField(value=averageFuel,onValueChange={
                                                averageFuel=it
                                                val parsed=it.replace(',','.').toDoubleOrNull()
                                                if(parsed!=null && parsed>0 && parsed<50) db.setSetting("avg_fuel",parsed.toString())
                                            },label={Text("Priemerná spotreba (l/100 km)")},
                                            singleLine=true,modifier=Modifier.fillMaxWidth())
                                            OutlinedButton(onClick={receiptGallery.launch("image/*")},modifier=Modifier.fillMaxWidth()) {
                                                Icon(Icons.Rounded.Receipt,null)
                                                Spacer(Modifier.width(8.dp))
                                                Text("Pridať bloček z galérie")
                                            }
                                            if(isKodiaqSilver) Text("Pozadie Škoda Kodiaq – strieborná",
                                                color=Violet,fontWeight=FontWeight.SemiBold)
                                            else Text("Pre tento model nie je pripravený AI obrázok. Použi vlastnú fotografiu.",
                                                fontSize=12.sp,color=Muted)
                                            OutlinedButton(
                                                onClick={photoLauncher.launch("image/*")},
                                                modifier=Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Rounded.AddPhotoAlternate,null,tint=Violet)
                                                Spacer(Modifier.width(8.dp))
                                                Text("Zvoliť fotku môjho auta")
                                            }
                                            if(customPhotoPath.isNotEmpty()) {
                                                TextButton(onClick={
                                                    customPhotoPath=""
                                                    p.edit().remove("custom_photo").remove("custom_photo_spec").apply()
                                                    photoRevision++
                                                }){Text("Vrátiť sa na AI showroom")}
                                            }
                                            Text(
                                                when {
                                                    userPhoto != null -> "Zobrazuje sa tvoja vlastná fotografia."
                                                    generatedPhoto != null -> "Zobrazuje sa AI pozadie pre vybraný model."
                                                    isKodiaqSilver -> "Pripravené AI pozadie Škoda Kodiaq v striebornej farbe."
                                                    chosenBrand=="Porsche" && chosenModel=="Panamera" -> "Ukážka Porsche Panamera."
                                                    else -> "AI obrázok zatiaľ chýba. Preto je zobrazená prázdna garáž, nie cudzie auto."
                                                },
                                                color=Muted,fontSize=10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(vertical=17.dp),horizontalArrangement=Arrangement.Center,
                            verticalAlignment=Alignment.CenterVertically) {
                            repeat(3){i ->
                                Box(Modifier.padding(horizontal=4.dp).size(if(tab==i)8.dp else 6.dp)
                                    .background(if(tab==i)Violet else Color(0xFF646478),CircleShape))
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(horizontal=17.dp).height(64.dp)
                            .background(Color(0xEE10121E),RoundedCornerShape(34.dp))
                            .border(1.dp,Color.White.copy(alpha=.22f),RoundedCornerShape(34.dp))
                            .padding(5.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            val icons = listOf(Icons.Rounded.Route,Icons.Rounded.BarChart,Icons.Rounded.DirectionsCar)
                            repeat(3){ i ->
                                Box(Modifier.weight(1f).fillMaxHeight()
                                    .background(if(tab==i)Brush.verticalGradient(listOf(Color(0xFF7E37B8),Color(0xFF44205D)))
                                        else Brush.verticalGradient(listOf(Color.Transparent,Color.Transparent)),RoundedCornerShape(30.dp))
                                    .clickable{tab=i},contentAlignment=Alignment.Center) {
                                    Icon(icons[i],titles[i],modifier=Modifier.size(29.dp),tint=if(tab==i)Bright else Muted)
                                }
                            }
                        }
                        Spacer(Modifier.height(13.dp))
                    }
                }
                if(showAlerts) {
                    val pending=db.eligiblePair()
                    AlertDialog(onDismissRequest={
                            if(pending!=null)db.postpone(pending.id)
                            showAlerts=false
                        },
                        title={Text("Pripravené jazdy")},
                        text={Text(if(pending!=null)
                            "Modelový okruh Trenčín – Přerov – Trenčín (2 × 112 km). Uložiť obe modelové jazdy? V podrobnostiach budú označené Simulovaná."
                            else "Zatiaľ nemáš žiadne pripravené modelové dvojice. Súkromné kilometre: "+shownKm(db.privateBalance()))},
                        confirmButton={
                            TextButton(onClick={
                                if(pending!=null){db.confirmPair(pending);revision++}
                                showAlerts=false
                            }){Text(if(pending!=null)"Uložiť obe" else "Zavrieť")}
                        },
                        dismissButton={
                            if(pending!=null) TextButton(onClick={
                                db.postpone(pending.id);showAlerts=false
                            }){Text("Odložiť")}
                        }
                    )
                }
                if(showProfile) AlertDialog(onDismissRequest={showProfile=false},
                    confirmButton={TextButton(onClick={showProfile=false}){Text("Zavrieť")}},
                    dismissButton={TextButton(onClick={showProfile=false;setup=true}){Text("Upraviť vozidlo")}},
                    title={Text("Profil")},
                    text={Text("Vodič: "+driver.ifBlank{"Neuvedené"}+"\nVozidlo: "+chosenBrand+" "+chosenModel+"\n\nÚdaje sú uložené iba v tomto telefóne. Bez online účtu.")})
                if(showFinish) AlertDialog(onDismissRequest={showFinish=false},
                    title={Text("Ukončiť GPS jazdu")},
                    text={
                        Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
                            val snap=LiveDrive.snapshot
                            Text("Nameraná vzdialenosť: "+(snap?.let{shownKm(it.km)}?:"—"))
                            Text("Trvanie: "+(snap?.let{durationFormat(it.durationMs)}?:"—"))
                            Text("GPS body: "+(snap?.fixCount?:0)+", výpadky: "+(snap?.gaps?:0),
                                color=Muted,fontSize=12.sp)
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                FilterChip(selected=privacy=="Služobná",onClick={privacy="Služobná"},label={Text("Služobná")})
                                Spacer(Modifier.width(8.dp))
                                FilterChip(selected=privacy=="Súkromná",onClick={privacy="Súkromná"},label={Text("Súkromná")})
                            }
                            if(privacy=="Súkromná")
                                Text("Uložia sa iba kilometre. GPS trasa, mestá a časy sa nezachovajú.",
                                    fontSize=12.sp,color=Muted)
                            else {
                                DemoTextField(firstTown,{firstTown=it},"Miesto odchodu")
                                DemoTextField(lastTown,{lastTown=it},"Miesto príchodu")
                            }
                        }
                    },
                    dismissButton={TextButton(onClick={showFinish=false}){Text("Späť")}},
                    confirmButton={TextButton(onClick={
                        val snap=LiveDrive.snapshot
                        if(snap==null || snap.fixCount<2 || snap.km<0.02)
                            message="GPS nezaznamenalo dostatok platných bodov. Túto jazdu nie je možné spoľahlivo uložiť."
                        else {showFinish=false;showConfirm=true}
                    }){Text("Pokračovať")}})
                if(showConfirm) AlertDialog(onDismissRequest={showConfirm=false;showFinish=true},
                    title={Text("Potvrdiť uloženie")},
                    text={Text(if(privacy=="Súkromná")
                        "Uložiť iba "+(LiveDrive.snapshot?.let{shownKm(it.km)}?:"—")+" do súkromného počítadla? Polohy a časy sa zahodia."
                        else "Uložiť skutočnú GPS jazdu "+firstTown+" – "+lastTown+"?")},
                    dismissButton={TextButton(onClick={showConfirm=false;showFinish=true}){Text("Späť")}},
                    confirmButton={TextButton(onClick={
                        LiveDrive.snapshot?.let{snap->
                            if(privacy=="Súkromná"){
                                val created=db.addPrivate(snap.km)
                                message="Súkromné kilometre pripočítané. Zostatok: "+shownKm(db.privateBalance())+
                                    if(created>0)". Pripravené modelové okruhy: "+created else ""
                            }else{
                                db.addReal(snap,firstTown.trim(),lastTown.trim(),"Služobná")
                                message="GPS jazda uložená: "+firstTown+" – "+lastTown+"."
                            }
                            LiveDrive.first=null;LiveDrive.last=null;LiveDrive.snapshot=null
                            revision++
                        }
                        showConfirm=false
                    }){Text("Uložiť")}})
                if(receiptReview) AlertDialog(onDismissRequest={receiptReview=false},
                    title={Text("Údaje z tankovacieho bločku")},
                    text={Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
                        Text("Rozpoznané údaje skontroluj a podľa potreby oprav.",fontSize=12.sp)
                        DemoTextField(receiptDate,{receiptDate=it},"Dátum")
                        DemoTextField(receiptLiters,{receiptLiters=it},"Natankované litre")
                        DemoTextField(receiptFuel,{receiptFuel=it},"Palivo (Diesel B7 / Benzín E10)")
                        DemoTextField(receiptTotal,{receiptTotal=it},"Celková cena €")
                        Text("Bločky sú samostatné tankovania, nie automaticky spotreba jázd.",
                            color=Muted,fontSize=11.sp)
                    }},
                    dismissButton={TextButton(onClick={receiptReview=false}){Text("Zrušiť")}},
                    confirmButton={TextButton(onClick={
                        val liters=receiptLiters.replace(',','.').toDoubleOrNull()
                        val total=receiptTotal.replace(',','.').toDoubleOrNull()
                        if(liters==null||liters<=0||total==null||total<0)
                            message="Skontroluj litre a celkovú cenu."
                        else {
                            val destination=File(context.filesDir,"receipt_"+System.currentTimeMillis()+".jpg")
                            try{
                                receiptUri?.let{uri->
                                    context.contentResolver.openInputStream(uri)?.use{input->
                                        destination.outputStream().use{output->input.copyTo(output)}
                                    }
                                }
                            }catch(_:Exception){}
                            db.addReceipt(receiptDate,liters,receiptFuel,total,
                                if(destination.exists())destination.absolutePath else "")
                            receiptReview=false;revision++
                            message="Tankovanie bolo uložené oddelene od jázd."
                        }
                    }){Text("Uložiť bloček")}})
                if(selectedDrive!=null) {
                    DriveDetails(selectedDrive!!,onBack={selectedDrive=null})
                }
                if(message.isNotEmpty()) AlertDialog(onDismissRequest={message=""},confirmButton={TextButton(onClick={message=""}){Text("OK")}},
                    text={Text(message)})
            }
        }
    }
}
@Composable private fun GlassPanel(modifier: Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
    Column(modifier.fillMaxWidth().shadow(16.dp,RoundedCornerShape(23.dp),ambientColor=Violet.copy(alpha=.25f))
        .background(Color(0xE6171A28),RoundedCornerShape(23.dp))
        .border(1.dp,Brush.linearGradient(listOf(Violet.copy(alpha=.9f),Color.White.copy(alpha=.12f),Violet.copy(alpha=.54f))),
            RoundedCornerShape(23.dp)).padding(15.dp),
        verticalArrangement=Arrangement.spacedBy(8.dp),content=content)
}
@Composable private fun DemoTextField(value:String,onChange:(String)->Unit,label:String,secret:Boolean=false) {
    OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},
        modifier=Modifier.fillMaxWidth(),singleLine=true,
        visualTransformation=if(secret) androidx.compose.ui.text.input.PasswordVisualTransformation()
            else androidx.compose.ui.text.input.VisualTransformation.None,
        colors=OutlinedTextFieldDefaults.colors(focusedTextColor=Bright,unfocusedTextColor=Bright,
            focusedLabelColor=Violet,unfocusedLabelColor=Muted,
            focusedBorderColor=Violet,unfocusedBorderColor=Color(0xFF646076)))
}
@Composable private fun Pick(title:String,options:List<String>,value:String,onChange:(String)->Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth(),
            shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=Bright)) {
            Text(title+":  "+value,modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
            Icon(Icons.Rounded.KeyboardArrowDown,null)
        }
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
            options.distinct().forEach { option->
                DropdownMenuItem(text={Text(option)},onClick={onChange(option);expanded=false})
            }
        }
    }
}
@Composable private fun HeaderButton(icon:androidx.compose.ui.graphics.vector.ImageVector,
    description:String,emphasis:Boolean,onClick:()->Unit) {
    Box(Modifier.size(45.dp)
        .background(if(emphasis)Color(0xFF41205D) else Color(0xB80E111B),CircleShape)
        .border(1.dp,if(emphasis)Violet else Color.White.copy(alpha=.28f),CircleShape)
        .clickable(onClick=onClick),contentAlignment=Alignment.Center) {
        Icon(icon,description,tint=if(emphasis)Violet else Bright,modifier=Modifier.size(27.dp))
    }
}
@Composable private fun SquareIcon(icon:androidx.compose.ui.graphics.vector.ImageVector) {
    Box(Modifier.size(45.dp).background(Color(0xFF3B2256),RoundedCornerShape(13.dp))
        .border(1.dp,Violet.copy(alpha=.4f),RoundedCornerShape(13.dp)),
        contentAlignment=Alignment.Center) {
        Icon(icon,null,tint=Bright,modifier=Modifier.size(27.dp))
    }
}
@Composable private fun Statistic(label:String,value:String,modifier:Modifier=Modifier) {
    Column(modifier.background(Color(0xA1232737),RoundedCornerShape(12.dp))
        .border(1.dp,Color.White.copy(alpha=.1f),RoundedCornerShape(12.dp)).padding(11.dp),
        verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Text(label,fontSize=11.sp,color=Muted)
        Text(value,fontSize=18.sp,fontWeight=FontWeight.Bold,color=Bright,maxLines=1)
    }
}
@Composable private fun InfoRow(label:String,value:String) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
        verticalAlignment=Alignment.CenterVertically) {
        Text(label,fontSize=12.sp,color=Muted,modifier=Modifier.weight(.43f))
        Text(value,fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=Bright,
            modifier=Modifier.weight(.57f),maxLines=2)
    }
    HorizontalDivider(color=Color.White.copy(alpha=.1f))
}

@Composable private fun SearchPick(
    title:String, options:List<String>, current:String,
    loading:Boolean,onChange:(String)->Unit
) {
    var open by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=Bright)) {
        Text("$title: $current",modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
        if(loading) CircularProgressIndicator(modifier=Modifier.size(15.dp),strokeWidth=2.dp)
        else Icon(Icons.Rounded.KeyboardArrowDown,null)
    }
    if(open) Dialog(onDismissRequest={open=false}) {
        Column(Modifier.fillMaxWidth().background(Color(0xFF151824),RoundedCornerShape(20.dp))
            .padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Vybrať: $title",color=Bright,fontSize=20.sp,fontWeight=FontWeight.Bold)
            OutlinedTextField(value=search,onValueChange={search=it},singleLine=true,
                label={Text("Hľadať značku alebo model")},modifier=Modifier.fillMaxWidth())
            val filtered = remember(options,search) {
                options.distinctBy{it.lowercase()}
                    .filter { it.contains(search.trim(), ignoreCase=true) }
                    .take(100)
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max=350.dp)) {
                items(filtered) { option ->
                    Text(option,color=Bright,modifier=Modifier.fillMaxWidth()
                        .clickable {onChange(option);open=false;search=""}
                        .padding(12.dp),fontSize=15.sp)
                    HorizontalDivider(color=Color.White.copy(alpha=.1f))
                }
            }
            Text(if(loading)"Načítava sa online katalóg..." else "Zobrazuje sa max. 100 výsledkov. Spresni vyhľadávanie.",
                fontSize=11.sp,color=Muted)
            TextButton(onClick={open=false},modifier=Modifier.align(Alignment.End)) { Text("Zavrieť") }
        }
    }
}

private fun decodeVehicleBitmap(path:String): androidx.compose.ui.graphics.ImageBitmap? =
    try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds=true }
        BitmapFactory.decodeFile(path,bounds)
        val longest = maxOf(bounds.outWidth,bounds.outHeight)
        val options = BitmapFactory.Options().apply { inSampleSize=maxOf(1,longest/1700) }
        BitmapFactory.decodeFile(path,options)?.asImageBitmap()
    } catch (_:Exception){null}

@Composable private fun EmptyShowroom(){
    Canvas(Modifier.fillMaxSize()) {
        drawRect(brush=Brush.verticalGradient(listOf(Color(0xFF05060B),Color(0xFF1B202A),Color(0xFF080A0F))))
        val w=size.width; val h=size.height
        val white=Color.White.copy(alpha=.65f)
        for(i in 0..4) {
            val y=h*(.15f+i*.075f)
            drawLine(white,Offset(w*.05f,y),Offset(w*.94f,y-h*.06f),2.6f)
        }
        for(i in 0..3) {
            val x=w*(i+.5f)/4f
            drawLine(white.copy(alpha=.4f),Offset(x,h*.16f),Offset(x,h*.52f),2f)
        }
        drawLine(white.copy(alpha=.23f),Offset(0f,h*.55f),Offset(w,h*.55f),3f)
    }
}
