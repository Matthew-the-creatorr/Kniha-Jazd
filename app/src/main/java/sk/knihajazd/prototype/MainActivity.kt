package sk.knihajazd.prototype

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
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
    val pan by animateFloatAsState(targetValue=when(tab){0 -> 36f;1 -> 0f;else -> -36f}, animationSpec=tween(680),label="background-camera")
    MaterialTheme(colorScheme=darkColorScheme(primary=Violet,onPrimary=Color.Black,background=Charcoal,
        onBackground=Bright,surface=Color(0xFF1C2030),onSurface=Bright,onSurfaceVariant=Muted)) {
        CompositionLocalProvider(LocalContentColor provides Bright) {
            Box(Modifier.fillMaxSize().background(Charcoal)) {
                Image(
                    painter=painterResource(R.drawable.garage_showroom),
                    contentDescription="Fotografia luxusnej garáže",
                    contentScale=ContentScale.Crop,
                    alignment=Alignment.TopCenter,
                    modifier=Modifier.fillMaxSize().graphicsLayer{
                        translationX=pan
                        scaleX=1.12f
                        scaleY=1.04f
                    }
                )
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                    colorStops=arrayOf(
                        0.0f to Color(0xF404050A),
                        .16f to Color(0x9904050B),
                        .33f to Color(0x2404050B),
                        .53f to Color(0x39060911),
                        .77f to Color(0xDA080A13),
                        1f to Color(0xFB05060C)
                    )
                )))
                if(setup) {
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
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
                                brand=it; model=carModels[it]?.firstOrNull() ?: "Iný model"
                            }
                            Pick("Model",(carModels[brand] ?: emptyList()) + "Iný model",model){model=it}
                            if(model=="Iný model") DemoTextField(customModel,{customModel=it},"Vlastný model")
                            Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                                Box(Modifier.weight(1f)){DemoTextField(year,{year=it},"Rok")}
                                Box(Modifier.weight(1f)){DemoTextField(reg,{reg=it},"EČV / ŠPZ")}
                            }
                            Pick("Farba karosérie",colors,paint){paint=it}
                            Pick("Povrch",finishes,finish){finish=it}
                        } }
                        item {
                            Button(
                                onClick={
                                    p.edit().putBoolean("setup_complete",true).putString("driver",driver)
                                        .putString("brand",brand).putString("model",model).putString("custom",customModel)
                                        .putString("plate",reg).putString("year",year).putString("paint",paint)
                                        .putString("finish",finish).apply()
                                    pwd="";setup=false
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
                    Column(Modifier.fillMaxSize()) {
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
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.fillMaxWidth().pointerInput(tab){
                            var drag=0f
                            detectHorizontalDragGestures(onDragEnd={
                                if(drag < -85f) tab=(tab+1).coerceAtMost(2)
                                if(drag > 85f) tab=(tab-1).coerceAtLeast(0)
                                drag=0f
                            }){change,amount->change.consume();drag+=amount}
                        }) {
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
                                            Text("Fotografia garáže je ukážková a nemusí zobrazovať vybrané vozidlo.",
                                                color=Muted,fontSize=10.sp)
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
        verticalArrangement=Arrangement.spacedBy(10.dp),content=content)
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
