package sk.knihajazd.prototype

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import java.util.Locale

private val Purple=Color(0xFFAD59F5)
private val TextBright=Color(0xFFF9F5FF)
private val TextLow=Color(0xFFB7A9C8)
fun shownKm(v:Double):String=String.format(Locale("sk","SK"),"%.1f",v)+" km"
fun shownLiters(v:Double):String=String.format(Locale("sk","SK"),"%.1f",v)+" l"
fun shownCost(v:Double):String=String.format(Locale("sk","SK"),"%.2f",v)+" €"

@Composable fun DrivesOverview(drives:List<DriveRecord>,onOpen:(DriveRecord)->Unit){
    Column(verticalArrangement=Arrangement.spacedBy(9.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Icon(Icons.Rounded.Route,null,tint=Purple,modifier=Modifier.size(35.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)){
                Text("Posledné jazdy",fontSize=21.sp,fontWeight=FontWeight.Bold,color=TextBright)
                Text("Automatická evidencia",fontSize=11.sp,color=TextLow)
            }
            Icon(Icons.Rounded.ChevronRight,null,tint=TextBright)
        }
        HorizontalDivider(color=Purple.copy(alpha=.7f))
        Row(horizontalArrangement=Arrangement.spacedBy(5.dp),modifier=Modifier.fillMaxWidth()){
            listOf("Dátum","Čas","Štart – Cieľ","Spotreba","Náklady").forEachIndexed{index,label->
                Text(label,color=TextBright,fontSize=if(index==2)9.sp else 8.sp,
                    modifier=Modifier.weight(if(index==2)1.55f else 1f),
                    maxLines=1,overflow=TextOverflow.Ellipsis)
            }
        }
        if(drives.isEmpty()){
            Spacer(Modifier.height(12.dp))
            Icon(Icons.Rounded.DirectionsCar,null,tint=TextLow,modifier=Modifier.align(Alignment.CenterHorizontally).size(37.dp))
            Text("Zatiaľ žiadne jazdy",fontSize=16.sp,fontWeight=FontWeight.Bold,color=TextBright,
                modifier=Modifier.align(Alignment.CenterHorizontally))
            Text("Po uložení GPS alebo modelových jázd sa tu objavia záznamy.",
                color=TextLow,fontSize=11.sp,modifier=Modifier.align(Alignment.CenterHorizontally))
        }else{
            LazyColumn(Modifier.fillMaxWidth().heightIn(max=315.dp),
                verticalArrangement=Arrangement.spacedBy(9.dp)){
                items(drives,key={it.id}){drive->
                    DriveCard(drive,onClick={onOpen(drive)})
                }
            }
        }
    }
}

@Composable private fun DriveCard(drive:DriveRecord,onClick:()->Unit){
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
        .border(1.dp,Purple.copy(alpha=.85f),RoundedCornerShape(15.dp))
        .background(Brush.verticalGradient(listOf(Color(0xE52B203C),Color(0xED0C0D1B))))
        .clickable(onClick=onClick).padding(10.dp),
        verticalArrangement=Arrangement.spacedBy(6.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            Icon(Icons.Rounded.DirectionsCar,null,tint=TextBright,modifier=Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)){
                Text(drive.origin+" – "+drive.destination,color=TextBright,
                    fontSize=13.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                Row(verticalAlignment=Alignment.CenterVertically){
                    Icon(Icons.Rounded.Schedule,null,tint=TextLow,modifier=Modifier.size(11.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(timeFormat(drive.startMs)+"    →    "+timeFormat(drive.endMs),
                        color=TextLow,fontSize=10.sp)
                }
            }
            Text(drive.kind,color=TextBright,fontSize=9.sp,
                modifier=Modifier.border(1.dp,Purple,RoundedCornerShape(20.dp))
                    .background(Color(0xFF542276),RoundedCornerShape(20.dp))
                    .padding(horizontal=7.dp,vertical=3.dp))
        }
        HorizontalDivider(color=TextLow.copy(alpha=.4f))
        Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth()){
            SmallStat(dateFormat(drive.startMs),1.2f)
            SmallStat(durationFormat(drive.endMs-drive.startMs),1.15f)
            SmallStat(drive.origin+" – "+drive.destination,1.5f)
            SmallStat(if(drive.liters>0)shownLiters(drive.liters) else "— l",1f)
            SmallStat(if(drive.cost>0)shownCost(drive.cost) else "— €",.95f)
        }
    }
}
@Composable private fun RowScope.SmallStat(value:String,weight:Float){
    Box(Modifier.weight(weight).padding(horizontal=2.dp),contentAlignment=Alignment.Center){
        Text(value,fontSize=8.sp,color=TextBright,maxLines=2,overflow=TextOverflow.Ellipsis,
            fontWeight=FontWeight.SemiBold)
    }
}

@Composable fun DriveDetails(record:DriveRecord,onBack:()->Unit){
    val rowColor=Color(0xFF15121E)
    Column(Modifier.fillMaxSize().background(Color(0xFF080710))
        .windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState())
        .padding(18.dp),verticalArrangement=Arrangement.spacedBy(15.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            IconButton(onClick=onBack){Icon(Icons.Rounded.ArrowBack,"Späť",tint=TextBright)}
            Text("Podrobnosti jazdy",fontSize=20.sp,fontWeight=FontWeight.Bold,
                color=TextBright,modifier=Modifier.weight(1f))
            if(record.simulated) Text("Simulovaná",fontSize=12.sp,color=TextBright,
                modifier=Modifier.border(1.dp,Purple,RoundedCornerShape(15.dp))
                    .padding(horizontal=9.dp,vertical=5.dp))
        }
        Column(Modifier.fillMaxWidth().background(rowColor,RoundedCornerShape(20.dp))
            .border(1.dp,Purple,RoundedCornerShape(20.dp)).padding(16.dp),
            verticalArrangement=Arrangement.spacedBy(9.dp)){
            Text(record.origin+" → "+record.destination,fontSize=21.sp,fontWeight=FontWeight.Bold,color=TextBright)
            Text(dateFormat(record.startMs)+" · "+record.kind,color=TextLow)
            if(record.simulated)Text("Modelová jazda, nie skutočný GPS záznam.",
                color=Color(0xFFF0CD8A),fontSize=12.sp)
            HorizontalDivider(color=Purple.copy(alpha=.4f))
            DetailRow("Odchod",record.origin+" · "+timeFormat(record.startMs))
            DetailRow("Príchod",record.destination+" · "+timeFormat(record.endMs))
            DetailRow("Trvanie",durationFormat(record.endMs-record.startMs))
            DetailRow("Prejdené",shownKm(record.km))
            DetailRow("Palivo",record.fuel)
            DetailRow("Spotreba",if(record.liters>0)shownLiters(record.liters)+" (odhad)" else "Nezadaná")
            DetailRow("Náklady",if(record.cost>0)shownCost(record.cost)+" (odhad)" else "Nezadané")
            if(!record.simulated)Text("Poloha a čas z GPS. Trasa sa zatiaľ neukladá na mapu.",
                color=TextLow,fontSize=11.sp)
        }
        Button(onClick=onBack,modifier=Modifier.fillMaxWidth()){Text("Späť k jazdám")}
    }
}
@Composable private fun DetailRow(name:String,value:String){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
        Text(name,color=TextLow,fontSize=13.sp,modifier=Modifier.weight(.43f))
        Text(value,color=TextBright,fontSize=13.sp,fontWeight=FontWeight.SemiBold,
            modifier=Modifier.weight(.57f))
    }
    HorizontalDivider(color=Color.White.copy(alpha=.07f))
}
