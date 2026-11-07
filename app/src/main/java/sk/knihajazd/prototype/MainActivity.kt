package sk.knihajazd.prototype

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*

private val Purple = Color(0xFFB35BFF)
private val Dark = Color(0xFF080A10)
private val Glass = Color(0xDD171A29)
private val White = Color(0xFFF5F3FA)
private val brands = listOf("Audi", "BMW", "Citroën", "Dacia", "Fiat", "Ford", "Honda", "Hyundai", "Kia", "Mazda", "Mercedes-Benz", "Nissan", "Opel", "Peugeot", "Porsche", "Renault", "Seat", "Škoda", "Suzuki", "Tesla", "Toyota", "Volkswagen", "Volvo", "Iná značka")
private val models = mapOf("Audi" to listOf("A3","A4","A6","Q5","Q7"),"BMW" to listOf("Rad 1","Rad 3","Rad 5","X3","X5"),"Mercedes-Benz" to listOf("Trieda A","Trieda C","Trieda E","GLC"),"Porsche" to listOf("Panamera","Cayenne","Macan","911"),"Škoda" to listOf("Fabia","Octavia","Superb","Kodiaq","Kamiq"),"Volkswagen" to listOf("Golf","Passat","Tiguan","Polo"),"Toyota" to listOf("Corolla","Yaris","RAV4","Camry"),"Ford" to listOf("Focus","Mondeo","Kuga","Puma"))

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        setContent { App() }
    }
}

@Composable private fun App() {
    var onboard by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("Porsche") }
    var model by remember { mutableStateOf("Panamera") }
    var customModel by remember { mutableStateOf("") }
    var plate by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("2021") }
    var paint by remember { mutableStateOf("Čierna") }
    var finish by remember { mutableStateOf("Lesklý") }
    var tab by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    var alerts by remember { mutableStateOf(false) }
    val colors = listOf("Čierna","Biela","Sivá","Červená","Modrá","Zelená","Žltá","Fialová","Strieborná","Vlastná")

    MaterialTheme(colorScheme = darkColorScheme(primary = Purple, background = Dark, surface = Glass, onSurface = White)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF090B12),Color(0xFF191629),Color(0xFF07070C))))) {
            GarageBackdrop(paint)
            if (onboard) {
                LazyColumn(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    item { Spacer(Modifier.height(22.dp)); Text("KNIHA JÁZD", color=Purple, fontWeight=FontWeight.Black, fontSize=15.sp); Text("Tvoja virtuálna garáž", fontSize=29.sp, fontWeight=FontWeight.Bold); Text("Vizuálny prototyp · bez GPS a AI", color=Color.LightGray) }
                    item { Panel { Text("Registrácia",fontWeight=FontWeight.Bold,fontSize=21.sp); Field(name,{name=it},"Meno"); Field(password,{password=it},"Heslo (iba demo – neukladá sa)") } }
                    item { Panel { Text("Vyber vozidlo",fontWeight=FontWeight.Bold,fontSize=21.sp); Choice("Značka",brands,brand){brand=it;model=models[it]?.firstOrNull()?:"Iný model"}; Choice("Model",(models[brand]?: listOf("Iný model"))+"Iný model",model){model=it}; if(model=="Iný model") Field(customModel,{customModel=it},"Zadaj model"); Field(year,{year=it},"Rok výroby"); Field(plate,{plate=it},"ŠPZ / EČV"); Choice("Farba",colors,paint){paint=it}; Choice("Povrch",listOf("Lesklý","Matný","Metalíza","Perleťový","Saténový"),finish){finish=it} } }
                    item { Button(onClick={onboard=false}, modifier=Modifier.fillMaxWidth(), colors=ButtonDefaults.buttonColors(containerColor=Purple)) { Text("Vstúpiť do garáže", color=Color.Black, fontWeight=FontWeight.Bold) }; Text("Katalóg je zatiaľ ukážkový. Chýbajúce modely možno zadať ručne.",fontSize=12.sp,color=Color.LightGray); Spacer(Modifier.height(20.dp)) }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal=10.dp), verticalAlignment=Alignment.CenterVertically) {
                        Text("◢",fontSize=32.sp,color=Purple,fontWeight=FontWeight.Black)
                        Column(Modifier.weight(1f).padding(start=5.dp)) { Text("Kniha jázd",fontSize=19.sp,fontWeight=FontWeight.Bold); Text("Firemná evidencia · DEMO",fontSize=10.sp,color=Color.LightGray) }
                        IconButton(onClick={running=!running}) { Icon(if(running) Icons.Default.Stop else Icons.Default.PlayArrow, "Štart", tint=Purple) }
                        IconButton(onClick={alerts=!alerts}) { Icon(Icons.Default.Notifications,"Upozornenia") }
                        IconButton(onClick={onboard=true}) { Icon(Icons.Default.Person,"Profil") }
                    }
                    if(alerts) Text("Žiadne upozornenia (demo)",Modifier.padding(18.dp),color=Purple)
                    Spacer(Modifier.weight(0.75f))
                    Box(Modifier.fillMaxWidth().weight(1.25f).pointerInput(tab) {
                        var total=0f
                        detectHorizontalDragGestures(onDragEnd={
                            if(total < -80) tab=(tab+1).coerceAtMost(2)
                            else if(total > 80) tab=(tab-1).coerceAtLeast(0)
                            total=0f
                        }) { change,amount-> change.consume(); total+=amount }
                    }) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=15.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                            if(running) Panel {
                                Row(verticalAlignment=Alignment.CenterVertically) {
                                    Text("● Trasa prebieha (demo)",color=Purple,modifier=Modifier.weight(1f))
                                    TextButton(onClick={details=!details}) { Text(if(details)"▲" else "▼") }
                                    TextButton(onClick={running=false}) { Text("Koniec") }
                                }
                                if(details) Text("Zaznamenávanie GPS zatiaľ nie je aktívne.")
                            }
                            when(tab) {
                                0 -> Panel {
                                    Text("Posledné jazdy",fontSize=23.sp,fontWeight=FontWeight.Bold)
                                    Text("Dátum  ·  Čas  ·  Štart – Cieľ  ·  Spotreba  ·  Náklady",fontSize=11.sp,color=Color.LightGray)
                                    HorizontalDivider(color=Purple)
                                    Text("Zatiaľ žiadne jazdy",fontSize=15.sp)
                                    Text("Tu sa neskôr zobrazia automatické záznamy.",fontSize=12.sp,color=Color.LightGray)
                                    OutlinedButton(onClick={details=!details}) { Text(if(details)"Skryť podrobnosti" else "Podrobnosti ›") }
                                    if(details) Text("Po ukončení jazdy sa tu zobrazí dátum, čas, trasa, spotreba a náklady.",fontSize=13.sp)
                                }
                                1 -> Panel {
                                    Text("Moja analytika",fontSize=23.sp,fontWeight=FontWeight.Bold)
                                    Text("Zatiaľ bez údajov",color=Color.LightGray)
                                    listOf("Najazdené kilometre" to "— km","Priemerná spotreba" to "— l/100 km","Náklady na palivo" to "— €","Počet jázd" to "0").forEach { (a,b)->
                                        Row(Modifier.fillMaxWidth().padding(vertical=7.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(a);Text(b,color=Purple)}
                                    }
                                    Spacer(Modifier.height(14.dp))
                                    Text("Mesačný graf",color=Color.LightGray)
                                    Row(Modifier.fillMaxWidth().height(85.dp),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.SpaceEvenly){
                                        repeat(6){Box(Modifier.width(22.dp).height(5.dp).background(Purple, RoundedCornerShape(4.dp)))}
                                    }
                                }
                                2 -> Panel {
                                    Text("Moje vozidlo",fontSize=23.sp,fontWeight=FontWeight.Bold)
                                    listOf("Značka" to brand,"Model" to (if(model=="Iný model")customModel else model),"Rok" to year,"ŠPZ / EČV" to plate.ifBlank { "Nevyplnené" },"Farba" to paint,"Povrch" to finish,"Spotreba" to "Čaká na údaje","Objem nádrže" to "Čaká na údaje").forEach{(a,b)->
                                        Row(Modifier.fillMaxWidth().padding(vertical=6.dp),horizontalArrangement=Arrangement.SpaceBetween){
                                            Text(a,color=Color.LightGray);Text(b,color=White,fontWeight=FontWeight.Medium)
                                        }
                                    }
                                    OutlinedButton(onClick={onboard=true}){Text("Upraviť vozidlo")}
                                }
                            }
                        }
                    }
                    NavigationBar(containerColor=Color(0xEE11131E), tonalElevation=0.dp) {
                        listOf(Icons.Default.Route,Icons.Default.BarChart,Icons.Default.DirectionsCar).forEachIndexed { i,ic ->
                            NavigationBarItem(
                                selected=tab==i,onClick={tab=i},
                                icon={Icon(ic,contentDescription=listOf("Záznam","Analytika","Vozidlo")[i])},
                                alwaysShowLabel=false,
                                colors=NavigationBarItemDefaults.colors(selectedIconColor=White,indicatorColor=Color(0xFF65329D),unselectedIconColor=Color.LightGray))
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun Panel(content:@Composable ColumnScope.()->Unit) {
    Column(
        Modifier.fillMaxWidth().border(1.dp,Color(0x995F3A83),RoundedCornerShape(23.dp))
            .background(Glass,RoundedCornerShape(23.dp)).padding(17.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp),content=content)
}
@Composable private fun Field(v:String, change:(String)->Unit, label:String) {
    OutlinedTextField(value=v,onValueChange=change,label={Text(label)},modifier=Modifier.fillMaxWidth(),singleLine=true)
}
@Composable private fun Choice(label:String,items:List<String>,selected:String,onChange:(String)->Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) {
            Text("$label: $selected",modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
            Icon(Icons.Default.ArrowDropDown,null)
        }
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
            items.distinct().forEach { DropdownMenuItem(text={Text(it)},onClick={onChange(it);expanded=false}) }
        }
    }
}
@Composable private fun GarageBackdrop(paint:String) {
    val bodyColor = when(paint) {
        "Biela"->Color(0xFFB8BCC7);"Červená"->Color(0xFF872534)
        "Modrá"->Color(0xFF285D9C);"Zelená"->Color(0xFF31745C)
        "Žltá"->Color(0xFFB59A34);"Fialová"->Color(0xFF5F3C8E)
        "Strieborná"->Color(0xFF858F9F);else->Color(0xFF292F3C)
    }
    Canvas(Modifier.fillMaxSize()) {
        val w=size.width; val h=size.height
        for(i in 0..5) {
            val y=h*.10f+i*34f
            drawLine(Color.White.copy(alpha=.08f),Offset(w*.12f,y),Offset(w*.90f,y-65f),3f)
        }
        drawOval(
            brush=Brush.radialGradient(listOf(Purple.copy(alpha=.25f),Color.Transparent),
            center=Offset(w*.5f,h*.34f),radius=w*.65f),
            topLeft=Offset(0f,h*.16f),
            size=androidx.compose.ui.geometry.Size(w,h*.36f)
        )
        val cy=h*.35f; val left=w*.11f;val right=w*.90f
        val outline=Path().apply {
            moveTo(left,cy);lineTo(w*.20f,cy-h*.045f);lineTo(w*.37f,cy-h*.10f)
            lineTo(w*.68f,cy-h*.10f);lineTo(w*.82f,cy-h*.055f);lineTo(right,cy);close()
        }
        drawPath(outline,brush=Brush.verticalGradient(listOf(bodyColor.copy(alpha=.92f),Color(0xFF090B10))))
        drawPath(outline,Color.White.copy(alpha=.6f),style=androidx.compose.ui.graphics.drawscope.Stroke(width=2f))
        drawLine(Color.White.copy(alpha=.65f),Offset(w*.39f,cy-h*.093f),Offset(w*.67f,cy-h*.093f),2f)
        drawCircle(Color(0xFF090A10),w*.10f,Offset(w*.30f,cy))
        drawCircle(Color(0xFF090A10),w*.10f,Offset(w*.75f,cy))
        drawCircle(Color(0xFF606979),w*.055f,Offset(w*.30f,cy))
        drawCircle(Color(0xFF606979),w*.055f,Offset(w*.75f,cy))
        drawLine(Color(0xFFBFEAFF),Offset(w*.11f,cy-h*.008f),Offset(w*.20f,cy-h*.012f),6f)
        drawLine(Color(0xFFFF344F),Offset(w*.82f,cy-h*.012f),Offset(w*.9f,cy-h*.006f),5f)
    }
}
