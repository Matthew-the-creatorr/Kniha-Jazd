package sk.knihajazd.prototype

import android.os.Bundle
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
    var model by remember { mutableStateOf(p.getString("model","Panamera") ?: "Panamera") }
    var customModel by remember { mutableStateOf(p.getString("custom","") ?: "") }
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
    var customPhotoPath by remember { mutableStateOf(p.getString("custom_photo","") ?: "") }
    var photoRevision by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        loadingBrands = true
        try {
            availableBrands = (allBrands.filter { it != "Iná značka" } + CarCatalog.fetchMakes() + "Iná značka")
                .distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
            catalogStatus = "Online databáza značiek načítaná"
        } catch (e: Exception) {
            catalogStatus = "Offline katalóg: online databáza nie je dostupná."
        } finally {
            loadingBrands = false
        }
    }
    LaunchedEffect(brand) {
        loadingModels = true
        try {
            availableModels = ((carModels[brand] ?: emptyList()) + CarCatalog.fetchModels(brand))
                .distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
        } catch (e: Exception) {
            availableModels = carModels[brand] ?: emptyList()
        } finally {
            loadingModels = false
        }
    }
    val chosenModel = if(model == "Iný model") customModel.trim() else model.trim()
    val carSpec = listOf(brand, chosenModel, year, paint, finish).joinToString("|")
    val generatedPhoto = remember(aiImagePath, aiImageSpec, carSpec, aiRevision) {
        if (aiImageSpec == carSpec && aiImagePath.isNotBlank()) decodeVehicleBitmap(aiImagePath)
        else null
    }
    val generateNow: () -> Unit = {
        if (aiServer.isBlank() || aiToken.isBlank()) {
            showAiConfig = true
        } else if (chosenModel.isBlank()) {
            message = "Najprv vyber model vozidla."
        } else {
            generating = true
            scope.launch {
                try {
                    val saved = GeminiBackground.generate(
                        aiServer, aiToken, brand, chosenModel, year,
                        paint, finish, File(context.filesDir,"gemini_vehicle_wallpaper.png")
                    )
                    aiImagePath = saved
                    aiImageSpec = carSpec
                    aiRevision++
                    p.edit().putString("ai_image",saved).putString("ai_spec",carSpec)
                        .putString("ai_server",aiServer).apply()
                    customPhotoPath = ""
                    p.edit().remove("custom_photo").apply()
                    message = "AI pozadie bolo vytvorené pre $brand $chosenModel."
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
                    p.edit().putString("custom_photo", customPhotoPath).apply()
                    photoRevision++
                    message = "Tvoja fotografia je nastavená ako pozadie vozidla."
                }
            } catch (e: Exception) {
                message = "Fotografiu sa nepodarilo načítať. Skús inú."
            }
        }
    }
    val userPhoto = remember(customPhotoPath, photoRevision) {
        if(customPhotoPath.isEmpty()) null else try {
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
                if(userPhoto != null || generatedPhoto != null || (brand=="Porsche" && chosenModel=="Panamera")) {
                    Image(
                        painter=when {
                            userPhoto != null -> BitmapPainter(userPhoto)
                            generatedPhoto != null -> BitmapPainter(generatedPhoto)
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
                            SearchPick("Značka",availableBrands,brand,loadingBrands) {
                                brand=it
                                model=carModels[it]?.firstOrNull() ?: "Iný model"
                            }
                            SearchPick("Model",availableModels + "Iný model",model,loadingModels) { model=it }
                            Text(catalogStatus,fontSize=10.sp,color=Muted)
                            if(model=="Iný model") DemoTextField(customModel,{customModel=it},"Vlastný model")
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
                                    p.edit().remove("custom_photo").apply()
                                    photoRevision++
                                }) { Text("Odstrániť vlastnú fotku") }
                            }
                            Text("Bez vytvoreného AI obrázka sa cudzie auto nebude vydávať za vybraný model.",
                                fontSize=11.sp,color=Muted)
                            OutlinedButton(onClick={showAiConfig=true},modifier=Modifier.fillMaxWidth()) {
                                Icon(Icons.Rounded.AutoAwesome,null)
                                Spacer(Modifier.width(8.dp))
                                Text("Pripojiť generovanie obrázkov")
                            }
                        } }
                        item {
                            Button(
                                onClick={
                                    p.edit().putBoolean("setup_complete",true).putString("driver",driver)
                                        .putString("brand",brand).putString("model",model).putString("custom",customModel)
                                        .putString("plate",reg).putString("year",year).putString("paint",paint)
                                        .putString("finish",finish).apply()
                                    pwd="";setup=false
                                    if(aiServer.isNotBlank() && aiToken.isNotBlank()) generateNow()
                                },
                                modifier=Modifier.fillMaxWidth().height(54.dp),
                                shape=RoundedCornerShape(18.dp),
                                colors=ButtonDefaults.buttonColors(containerColor=Violet)
                            ) { Text("Vstúpiť do garáže",fontWeight=FontWeight.Bold,color=Color(0xFF10051E)) }
                            Text("DEMO: fotografia vozidla je ilustračná. Heslo sa neukladá a nevytvára sa účet.",
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
                                Text("Firemná evidencia · DEMO",fontSize=11.sp,color=Muted)
                            }
                            HeaderButton(Icons.Rounded.PlayArrow,"Štart",true){isTrip=true;isPaused=false}
                            Spacer(Modifier.width(7.dp))
                            HeaderButton(Icons.Rounded.NotificationsNone,"Upozornenia",false){showAlerts=true}
                            Spacer(Modifier.width(7.dp))
                            HeaderButton(Icons.Rounded.PersonOutline,"Profil",false){showProfile=true}
                        }
                        if(isTrip) GlassPanel(Modifier.padding(horizontal=15.dp).padding(top=8.dp)) {
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Text(if(isPaused)"Trasa • Pozastavená" else "● Trasa • Demo",color=Violet,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
                                TextButton(onClick={isPaused=!isPaused}) { Text(if(isPaused)"Pokračovať" else "Stop") }
                                TextButton(onClick={showFinish=true}){Text("Koniec")}
                            }
                        }
                        Text(
                            brand + " " + (if(model=="Iný model") customModel else model) + "  ·  " + paint,
                            modifier=Modifier.padding(horizontal=20.dp).padding(top=3.dp),
                            fontSize=11.sp,color=Bright.copy(alpha=.85f),fontWeight=FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.fillMaxWidth()) {
                            Crossfade(targetState=tab,animationSpec=tween(480),label="garage-head-turn") { page ->
                                Column(Modifier.fillMaxWidth().padding(horizontal=16.dp),
                                    verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                    when(page){
                                        0 -> GlassPanel {
                                            Row(verticalAlignment=Alignment.CenterVertically) {
                                                SquareIcon(Icons.Rounded.Route)
                                                Spacer(Modifier.width(12.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text("Posledné jazdy",fontSize=21.sp,fontWeight=FontWeight.Bold)
                                                    Text("Automatická evidencia",color=Muted,fontSize=11.sp)
                                                }
                                                IconButton(onClick={showDetails=!showDetails}) {
                                                    Icon(if(showDetails)Icons.Rounded.KeyboardArrowDown else Icons.Rounded.ChevronRight,"Podrobnosti",tint=Bright)
                                                }
                                            }
                                            HorizontalDivider(color=Violet.copy(alpha=.55f))
                                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                                                listOf("Dátum","Čas","Štart – Cieľ","Spotreba","Náklady").forEachIndexed { i,s ->
                                                    Text(s,fontSize=if(i==2) 10.sp else 9.sp,fontWeight=FontWeight.Medium,
                                                        color=Bright,modifier=Modifier.weight(if(i==2)1.5f else 1f),maxLines=1)
                                                }
                                            }
                                            Spacer(Modifier.height(14.dp))
                                            Icon(Icons.Rounded.DirectionsCar,contentDescription=null,tint=Muted,
                                                modifier=Modifier.align(Alignment.CenterHorizontally).size(35.dp))
                                            Text("Zatiaľ žiadne jazdy",fontSize=16.sp,fontWeight=FontWeight.Bold,
                                                modifier=Modifier.align(Alignment.CenterHorizontally))
                                            Text("Po aktivácii GPS sa tu objavia nové záznamy.",
                                                color=Muted,fontSize=12.sp,
                                                modifier=Modifier.align(Alignment.CenterHorizontally))
                                            if(showDetails) {
                                                HorizontalDivider(color=Color.White.copy(alpha=.14f))
                                                Text("Zobrazenie detailov: dátum, čas, trasa, spotreba, náklady a fotografie.",
                                                    fontSize=12.sp,color=Muted)
                                            }
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
                                                Statistic("Kilometre","— km",Modifier.weight(1f))
                                                Statistic("Spotreba","— l/100",Modifier.weight(1f))
                                            }
                                            Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                                                Statistic("Palivo","— €",Modifier.weight(1f))
                                                Statistic("Počet jázd","0",Modifier.weight(1f))
                                            }
                                            Text("Najazdené kilometre · mesačne",color=Muted,fontSize=12.sp)
                                            Box(Modifier.fillMaxWidth().height(100.dp)
                                                .border(1.dp,Color.White.copy(alpha=.1f),RoundedCornerShape(12.dp)),
                                                contentAlignment=Alignment.Center) {
                                                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                                                    Icon(Icons.Rounded.ShowChart,null,tint=Violet,modifier=Modifier.size(28.dp))
                                                    Text("Zatiaľ bez údajov",color=Muted,fontSize=12.sp)
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
                                            InfoRow("Značka",brand)
                                            InfoRow("Model",if(model=="Iný model")customModel.ifBlank{"Neuvedený"} else model)
                                            InfoRow("Rok výroby",year)
                                            InfoRow("ŠPZ / EČV",reg.ifBlank{"Nevyplnené"})
                                            InfoRow("Farba / povrch",paint+" · "+finish)
                                            InfoRow("Objem nádrže","Nevyplnené")
                                            InfoRow("Spotreba","Čaká na údaje")
                                            OutlinedButton(
                                                onClick=generateNow,
                                                modifier=Modifier.fillMaxWidth(),
                                                enabled=!generating
                                            ) {
                                                Icon(Icons.Rounded.AutoAwesome,null,tint=Violet)
                                                Spacer(Modifier.width(8.dp))
                                                Text(if(generating) "Gemini generuje..." else "Vygenerovať AI pozadie")
                                            }
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
                                                    p.edit().remove("custom_photo").apply()
                                                    photoRevision++
                                                }){Text("Vrátiť sa na AI showroom")}
                                            }
                                            Text(
                                                when {
                                                    userPhoto != null -> "Zobrazuje sa tvoja vlastná fotografia."
                                                    generatedPhoto != null -> "Zobrazuje sa AI pozadie pre vybraný model."
                                                    brand=="Porsche" && chosenModel=="Panamera" -> "Ukážka Porsche Panamera."
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
                if(showAiConfig) AlertDialog(
                    onDismissRequest={showAiConfig=false},
                    title={Text("AI pozadie cez Gemini")},
                    text={
                        Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                            Text("Vizuálna funkcia využíva zabezpečený firemný server, ktorý musí nastaviť správca. Gemini kľúč nepatrí do APK.",fontSize=12.sp)
                            DemoTextField(aiServer,{aiServer=it},"HTTPS adresa AI servera")
                            DemoTextField(aiToken,{aiToken=it},"Prístupový token servera",true)
                            Text("Token sa neukladá do telefónu ani na GitHub. Generovanie môže byť spoplatnené.",fontSize=11.sp,color=Muted)
                        }
                    },
                    dismissButton={TextButton(onClick={showAiConfig=false}) {Text("Zavrieť")}},
                    confirmButton={TextButton(onClick={
                        p.edit().putString("ai_server",aiServer).apply()
                        showAiConfig=false
                        generateNow()
                    }) {Text("Generovať")}}
                )
                if(showAlerts) AlertDialog(onDismissRequest={showAlerts=false},
                    confirmButton={TextButton(onClick={showAlerts=false}){Text("Zavrieť")}},
                    title={Text("Upozornenia")},text={Text("Zatiaľ nemáš žiadne upozornenia. (Demo)")} )
                if(showProfile) AlertDialog(onDismissRequest={showProfile=false},
                    confirmButton={TextButton(onClick={showProfile=false}){Text("Zavrieť")}},
                    dismissButton={TextButton(onClick={showProfile=false;setup=true}){Text("Upraviť vozidlo")}},
                    title={Text("Profil")},
                    text={Text("Vodič: "+driver.ifBlank{"Neuvedené"}+"\nVozidlo: "+brand+" "+model+"\n\nÚdaje sú uložené iba v tomto telefóne. Bez online účtu.")})
                if(showFinish) AlertDialog(onDismissRequest={showFinish=false},
                    title={Text("Ukončiť jazdu")},
                    text={
                        Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                            Text("Vizuálna ukážka: GPS sa nezaznamenáva a žiadna skutočná trasa sa neuloží.")
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                FilterChip(selected=privacy=="Služobná",onClick={privacy="Služobná"},label={Text("Služobná")})
                                Spacer(Modifier.width(8.dp))
                                FilterChip(selected=privacy=="Súkromná",onClick={privacy="Súkromná"},label={Text("Súkromná")})
                            }
                        }
                    },
                    dismissButton={TextButton(onClick={showFinish=false}){Text("Cancel")}},
                    confirmButton={TextButton(onClick={showFinish=false;showConfirm=true}){Text("Save")}})
                if(showConfirm) AlertDialog(onDismissRequest={showConfirm=false},
                    title={Text("Potvrdenie")},
                    text={Text("Uložiť demo ukončenie jazdy? Skutočné údaje sa nezapíšu.")},
                    dismissButton={TextButton(onClick={showConfirm=false;showFinish=true}){Text("Cancel")}},
                    confirmButton={TextButton(onClick={showConfirm=false;isTrip=false;message="Demo ukončené · žiadne GPS údaje neuložené"}){Text("Save")}})
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
