package sk.knihajazd.prototype

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

data class ReceiptExtraction(val date:String,val liters:String,val fuel:String,val amount:String,val raw:String)
object ReceiptReader {
    fun recognize(context:Context,uri:Uri,onDone:(ReceiptExtraction?)->Unit){
        val image=try {InputImage.fromFilePath(context,uri)}catch(_:Exception){onDone(null);return}
        val recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener{result->onDone(parse(result.text))}
            .addOnFailureListener{onDone(null)}
            .addOnCompleteListener{recognizer.close()}
    }
    fun parse(input:String):ReceiptExtraction{
        val text=input.replace('\u00a0',' ')
        val date=Regex("""\b([0-3]?\d[./\-][01]?\d[./\-](?:20)?\d{2})\b""").find(text)?.value?:""
        val fuel=when{
            Regex("""(?i)\b(B7|DIESEL|NAFTA|MOTOROV[AÁ]\s*NAFTA)\b""").containsMatchIn(text)->"Diesel B7"
            Regex("""(?i)\b(E10|BENZ[IÍ]N\s*E10)\b""").containsMatchIn(text)->"Benzín E10"
            Regex("""(?i)\b(E5|BENZ[IÍ]N|NATURAL|SUPER\s*95)\b""").containsMatchIn(text)->"Benzín E5"
            else->""
        }
        val liters=Regex("""(?i)\b(\d{1,3}[,.]\d{1,3})\s*(?:l\b|litre|litrov|liter)\b""")
            .find(text)?.groupValues?.getOrNull(1)?.replace(',','.')?:""
        val amount=(Regex("""(?i)(?:CELKOM|SUMA|SPOLU|TOTAL|K\s*[ÚU]HRADE)\s*[:=\-]?\s*(\d{1,5}[,.]\d{2})""")
            .find(text)?.groupValues?.getOrNull(1)
            ?:Regex("""\b(\d{1,5}[,.]\d{2})\s*(?:€|EUR)\b""")
                .find(text)?.groupValues?.getOrNull(1) ?: "").replace(',','.')
        return ReceiptExtraction(date,liters,fuel,amount,text)
    }
}
