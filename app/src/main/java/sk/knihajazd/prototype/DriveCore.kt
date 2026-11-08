package sk.knihajazd.prototype

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import androidx.compose.runtime.*
import androidx.core.app.NotificationCompat
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.*
import kotlin.random.Random

data class DriveRecord(
    val id:Long, val kind:String, val simulated:Boolean, val startMs:Long, val endMs:Long,
    val origin:String, val destination:String, val km:Double, val liters:Double,
    val fuel:String, val cost:Double
)
data class TankReceipt(val id:Long,val dateText:String,val liters:Double,val fuel:String,val total:Double,val imagePath:String)
data class ModelPair(val id:Long,val modelDay:String,val firstMs:Long,val firstEndMs:Long,val secondMs:Long,val secondEndMs:Long)
data class DriveSnapshot(val startMs:Long,val endMs:Long,val durationMs:Long,val km:Double,
    val first:Location?,val last:Location?,val fixCount:Int,val gaps:Int)

object LiveDrive {
    var active by mutableStateOf(false)
    var startMs by mutableLongStateOf(0L)
    var startElapsed by mutableLongStateOf(0L)
    var km by mutableDoubleStateOf(0.0)
    var fixCount by mutableIntStateOf(0)
    var gaps by mutableIntStateOf(0)
    var latestAccuracy by mutableFloatStateOf(999f)
    var first:Location?=null
    var last:Location?=null
    var snapshot by mutableStateOf<DriveSnapshot?>(null)
    fun begin() {
        active=true;startMs=System.currentTimeMillis();startElapsed=SystemClock.elapsedRealtime()
        km=0.0;fixCount=0;gaps=0;latestAccuracy=999f;first=null;last=null;snapshot=null
    }
    fun record(loc:Location) {
        if(!active||!loc.hasAccuracy()||loc.accuracy>55f) return
        latestAccuracy=loc.accuracy
        val previous=last
        if(previous!=null){
            val seconds=(loc.elapsedRealtimeNanos-previous.elapsedRealtimeNanos)/1e9
            val delta=previous.distanceTo(loc).toDouble()
            if(seconds<=0.0 || seconds>45.0){gaps++;last=Location(loc);fixCount++;return}
            val plausible=delta <= (seconds*48.0+previous.accuracy+loc.accuracy)
            if(!plausible){gaps++;return}
            if(delta >= max(3.0,(previous.accuracy+loc.accuracy)/5.0)) km+=delta/1000.0
        } else first=Location(loc)
        last=Location(loc);fixCount++
    }
    fun finish():DriveSnapshot? {
        if(!active)return snapshot
        val endMs=System.currentTimeMillis()
        snapshot=DriveSnapshot(startMs,endMs,max(0L,SystemClock.elapsedRealtime()-startElapsed),
            km,first?.let(::Location),last?.let(::Location),fixCount,gaps)
        active=false
        return snapshot
    }
}

class DrivingService:Service(), LocationListener {
    private var manager:LocationManager?=null
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onCreate() {
        super.onCreate()
        val channel=NotificationChannel("recording","Prebiehajúce jazdy",NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent?.action=="STOP"){cleanup();stopSelf();return START_NOT_STICKY}
        if(!LiveDrive.active)LiveDrive.begin()
        val notification=NotificationCompat.Builder(this,"recording")
            .setContentTitle("Kniha jázd – GPS aktívne")
            .setContentText("Prebieha zaznamenávanie jazdy")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(PendingIntent.getActivity(this,0,
                Intent(this,MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .build()
        if(Build.VERSION.SDK_INT>=29) startForeground(41,notification,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        else startForeground(41,notification)
        manager=getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                manager?.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000L,0f,this,Looper.getMainLooper())
                if(manager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER)==true)
                    manager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,3000L,10f,this,Looper.getMainLooper())
            }
        }catch(_:SecurityException){LiveDrive.gaps++}
        return START_NOT_STICKY
    }
    override fun onLocationChanged(location:Location){LiveDrive.record(location)}
    override fun onProviderDisabled(provider:String){LiveDrive.gaps++}
    override fun onDestroy(){cleanup();if(LiveDrive.active)LiveDrive.finish();super.onDestroy()}
    private fun cleanup(){try{manager?.removeUpdates(this)}catch(_:Exception){}}
}

class DriveDatabase(context:Context):SQLiteOpenHelper(context,"kniha_jazd_v06.db",null,1){
    override fun onCreate(db:SQLiteDatabase){
        db.execSQL("CREATE TABLE drives(id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL, simulated INTEGER NOT NULL, start_ms INTEGER NOT NULL, end_ms INTEGER NOT NULL, origin TEXT NOT NULL, destination TEXT NOT NULL, km REAL NOT NULL, liters REAL NOT NULL DEFAULT 0, fuel TEXT NOT NULL DEFAULT '', cost REAL NOT NULL DEFAULT 0)")
        db.execSQL("CREATE TABLE kv(k TEXT PRIMARY KEY, v TEXT NOT NULL)")
        db.execSQL("CREATE TABLE pairs(id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, first_ms INTEGER NOT NULL, first_end_ms INTEGER NOT NULL, second_ms INTEGER NOT NULL, second_end_ms INTEGER NOT NULL, state TEXT NOT NULL DEFAULT 'pending', prompted_day TEXT NOT NULL DEFAULT '')")
        db.execSQL("CREATE TABLE receipts(id INTEGER PRIMARY KEY AUTOINCREMENT, date_text TEXT NOT NULL, liters REAL NOT NULL, fuel TEXT NOT NULL, total REAL NOT NULL, image_path TEXT NOT NULL)")
        db.execSQL("CREATE INDEX drives_dates ON drives(start_ms DESC)")
    }
    override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int){}
    fun getSetting(k:String,default:String=""):String{
        readableDatabase.rawQuery("SELECT v FROM kv WHERE k=?",arrayOf(k)).use {c->
            return if(c.moveToFirst()) c.getString(0) else default
        }
    }
    fun setSetting(k:String,v:String){
        val cv=ContentValues().apply{put("k",k);put("v",v)}
        writableDatabase.insertWithOnConflict("kv",null,cv,SQLiteDatabase.CONFLICT_REPLACE)
    }
    fun privateBalance()=getSetting("private_km","0").toDoubleOrNull()?:0.0
    fun addPrivate(km:Double):Int{
        if(km<=0 || !km.isFinite())return 0
        val db=writableDatabase;var created=0
        db.beginTransaction()
        try{
            var balance=privateBalance()+km
            while(balance>=224.0){
                val day=nextWorkday()
                val startMinutes=360+Random.nextInt(120)
                val firstDuration=90+Random.nextInt(31)
                val pauseDuration=110+Random.nextInt(41)
                val returnDuration=90+Random.nextInt(31)
                val first=day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()+startMinutes*60000L
                val firstEnd=first+firstDuration*60000L
                val second=firstEnd+pauseDuration*60000L
                val secondEnd=second+returnDuration*60000L
                db.execSQL("INSERT INTO pairs(day,first_ms,first_end_ms,second_ms,second_end_ms) VALUES(?,?,?,?,?)",
                    arrayOf(day.toString(),first,firstEnd,second,secondEnd))
                balance-=224.0;created++
            }
            setSetting("private_km",balance.toString())
            db.setTransactionSuccessful()
        }finally{db.endTransaction()}
        return created
    }
    private fun nextWorkday():LocalDate{
        var d=LocalDate.now()
        while(d.dayOfWeek==DayOfWeek.SATURDAY||d.dayOfWeek==DayOfWeek.SUNDAY) d=d.plusDays(1)
        return d
    }
    fun listPending():List<ModelPair>{
        val output=mutableListOf<ModelPair>()
        readableDatabase.rawQuery("SELECT id,day,first_ms,first_end_ms,second_ms,second_end_ms FROM pairs WHERE state='pending' ORDER BY id",null).use{c->
            while(c.moveToNext()) output.add(ModelPair(c.getLong(0),c.getString(1),c.getLong(2),c.getLong(3),c.getLong(4),c.getLong(5)))
        }
        return output
    }
    fun eligiblePair():ModelPair?{
        val day=LocalDate.now()
        if(day.dayOfWeek==DayOfWeek.SATURDAY||day.dayOfWeek==DayOfWeek.SUNDAY)return null
        val today=day.toString()
        return listPending().firstOrNull {
            it.secondEndMs<=System.currentTimeMillis() &&
                LocalDate.parse(it.modelDay)<=day &&
                promptedDay(it.id)!=today
        }
    }
    private fun promptedDay(id:Long):String{
        readableDatabase.rawQuery("SELECT prompted_day FROM pairs WHERE id=?",arrayOf(id.toString())).use {c->
            return if(c.moveToFirst())c.getString(0) else ""
        }
    }
    fun postpone(id:Long){
        val values=ContentValues().apply{put("prompted_day",LocalDate.now().toString())}
        writableDatabase.update("pairs",values,"id=?",arrayOf(id.toString()))
    }
    fun confirmPair(pair:ModelPair){
        val db=writableDatabase
        db.beginTransaction()
        try{
            val avg=getSetting("avg_fuel","6.5").toDoubleOrNull()?:6.5
            val price=getSetting("fuel_price","0").toDoubleOrNull()?:0.0
            val fuel=getSetting("fuel_type","Nezadané")
            val liters=112.0*avg/100.0
            insertDrive(db,"Súkromná",true,pair.firstMs,pair.firstEndMs,"Trenčín","Přerov",112.0,liters,fuel,liters*price)
            insertDrive(db,"Súkromná",true,pair.secondMs,pair.secondEndMs,"Přerov","Trenčín",112.0,liters,fuel,liters*price)
            val cv=ContentValues().apply{put("state","saved")}
            db.update("pairs",cv,"id=?",arrayOf(pair.id.toString()))
            db.setTransactionSuccessful()
        }finally{db.endTransaction()}
    }
    fun addReal(snapshot:DriveSnapshot,origin:String,destination:String,kind:String){
        insertDrive(writableDatabase,kind,false,snapshot.startMs,snapshot.endMs,origin,destination,
            snapshot.km,0.0,getSetting("fuel_type","Nezadané"),0.0)
    }
    private fun insertDrive(db:SQLiteDatabase,kind:String,simulated:Boolean,start:Long,end:Long,
        origin:String,dest:String,km:Double,liters:Double,fuel:String,cost:Double){
        val cv=ContentValues().apply{
            put("kind",kind);put("simulated",if(simulated)1 else 0)
            put("start_ms",start);put("end_ms",end);put("origin",origin)
            put("destination",dest);put("km",km);put("liters",liters);put("fuel",fuel);put("cost",cost)
        }
        db.insertOrThrow("drives",null,cv)
    }
    fun journeys():List<DriveRecord>{
        val list=mutableListOf<DriveRecord>()
        readableDatabase.rawQuery("SELECT id,kind,simulated,start_ms,end_ms,origin,destination,km,liters,fuel,cost FROM drives ORDER BY end_ms DESC,id DESC",null).use{c->
            while(c.moveToNext())list.add(DriveRecord(c.getLong(0),c.getString(1),c.getInt(2)==1,c.getLong(3),
                c.getLong(4),c.getString(5),c.getString(6),c.getDouble(7),c.getDouble(8),c.getString(9),c.getDouble(10)))
        }
        return list
    }
    fun addReceipt(date:String,liters:Double,fuel:String,total:Double,imagePath:String){
        val cv=ContentValues().apply {
            put("date_text",date);put("liters",liters);put("fuel",fuel);put("total",total);put("image_path",imagePath)
        }
        writableDatabase.insertOrThrow("receipts",null,cv)
        if(fuel.isNotBlank())setSetting("fuel_type",fuel)
        if(liters>0.0 && total>0.0)setSetting("fuel_price",(total/liters).toString())
    }
    fun receipts():List<TankReceipt>{
        val list=mutableListOf<TankReceipt>()
        readableDatabase.rawQuery("SELECT id,date_text,liters,fuel,total,image_path FROM receipts ORDER BY id DESC",null).use{c->
            while(c.moveToNext())list.add(TankReceipt(c.getLong(0),c.getString(1),c.getDouble(2),c.getString(3),c.getDouble(4),c.getString(5)))
        }
        return list
    }
}
fun dateFormat(time:Long):String =
    Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
fun timeFormat(time:Long):String =
    Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
fun durationFormat(ms:Long):String{
    val minutes=max(0L,ms/60000L)
    return if(minutes>=60) (minutes/60).toString()+" h "+(minutes%60)+" min" else minutes.toString()+" min"
}
fun cityFromLocation(context:Context,point:Location?):String{
    if(point==null)return "Neznáme miesto"
    return try{
        @Suppress("DEPRECATION")
        val addresses=android.location.Geocoder(context,java.util.Locale("sk","SK"))
            .getFromLocation(point.latitude,point.longitude,1)
        val a=addresses?.firstOrNull()
        a?.locality?.takeIf{it.isNotBlank()} ?: a?.subAdminArea?.takeIf{it.isNotBlank()}
            ?: a?.adminArea ?: "Neznáme miesto"
    }catch(_:Exception){"Neznáme miesto"}
}
