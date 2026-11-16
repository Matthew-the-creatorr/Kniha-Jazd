import Foundation
import CoreLocation
import SwiftUI
import UIKit
import Vision
import PhotosUI

struct Trip: Codable, Identifiable {
    var id=UUID()
    var start: Date
    var end: Date
    var from: String
    var to: String
    var km: Double
    var kind: String
    var simulated: Bool
    var liters: Double
    var fuel: String
    var cost: Double
    /// Part of estimated consumption covered by the stored compatible fuel receipts.
    var allocatedFromReceipts: Double? = nil
}
struct PairDraft: Codable, Identifiable {
    var id=UUID()
    var first: Date
    var firstEnd: Date
    var second: Date
    var secondEnd: Date
    var postponedDay: String=""
}
struct FuelEntry: Codable, Identifiable {
    var id=UUID()
    var day:String
    var liters:Double
    var fuel:String
    var cost:Double
    var photoFile:String?=nil
}
struct StoreBlob: Codable {
    var trips:[Trip]=[]
    var pending:[PairDraft]=[]
    var receipts:[FuelEntry]=[]
    var privateKm:Double=0
    var averageFuel:Double=6.5
    var fuelType:String?=nil
    var carBrand:String?=nil
    var carModel:String?=nil
    var carYear:String?=nil
    var carPlate:String?=nil
    var carColor:String?=nil
    var carFinish:String?=nil
}
func dayText(_ date:Date)->String {date.formatted(.dateTime.day().month().year())}
func clockText(_ date:Date)->String {date.formatted(.dateTime.hour().minute())}
func kilometers(_ km:Double)->String {String(format:"%0.1f km",km)}
func durationText(_ start:Date,_ end:Date)->String {
    let minCount=max(0,Int(end.timeIntervalSince(start)/60))
    return minCount>=60 ? "\(minCount/60) h \(minCount%60) min" : "\(minCount) min"
}
@MainActor final class JournalStore:ObservableObject {
    @Published var data=StoreBlob()
    private var url:URL {
        FileManager.default.urls(for:.documentDirectory,in:.userDomainMask)[0].appendingPathComponent("journal-v06.json")
    }
    init() {
        if let raw=try? Data(contentsOf:url), let saved=try? JSONDecoder().decode(StoreBlob.self,from:raw){
            data=saved
        }
        reallocateFuel()
    }
    func persist() {
        guard let bytes=try? JSONEncoder().encode(data) else{return}
        do {
            try bytes.write(to:url,options:.atomic)
            try FileManager.default.setAttributes([.protectionKey:FileProtectionType.completeUntilFirstUserAuthentication],ofItemAtPath:url.path)
        }catch { print("Unable to persist journal: \(error.localizedDescription)") }
    }
    var sorted:[Trip] {data.trips.sorted { $0.end > $1.end }}
    var privateTotalKm:Double {
        let savedModelKm=data.trips.filter{$0.simulated}.reduce(0){$0+$1.km}
        return data.privateKm + Double(data.pending.count)*224.0 + savedModelKm
    }
    var vehicleFuel:String {data.fuelType ?? "Nezadané"}
    var fuelSummary:FuelAllocation {
        PrivateFuelCalculator.calculate(
            distanceKm:privateTotalKm,
            litersPer100Km:data.averageFuel,
            receipts:data.receipts.map{($0.liters,$0.cost,$0.fuel)},
            vehicleFuel:vehicleFuel
        )
    }
    func reallocateFuel() {
        let summary=fuelSummary
        let validRate=data.averageFuel.isFinite && data.averageFuel>0 && data.averageFuel<=50 ? data.averageFuel : 6.5
        for i in data.trips.indices where data.trips[i].simulated {
            let estimated=data.trips[i].km*validRate/100.0
            let allocated=estimated*summary.coveredFraction
            data.trips[i].liters=estimated
            data.trips[i].allocatedFromReceipts=allocated
            data.trips[i].cost=allocated*summary.averagePrice
            data.trips[i].fuel=vehicleFuel
        }
        persist()
    }
    func setFuel(_ newFuel:String) {
        data.fuelType=newFuel
        reallocateFuel()
    }
    func setAverageFuel(_ rate:Double) {
        guard rate.isFinite && rate>0 && rate<=50 else{return}
        data.averageFuel=rate
        reallocateFuel()
    }
    func saveReal(_ trip:Trip){data.trips.append(trip);persist()}
    func addPrivate(_ km:Double) {
        guard km.isFinite && km>0 else{return}
        data.privateKm += km
        while data.privateKm>=224 {
            data.privateKm -= 224
            var day=Calendar.current.startOfDay(for:Date())
            while Calendar.current.isDateInWeekend(day) {
                guard let next=Calendar.current.date(byAdding:.day,value:1,to:day) else{break}
                day=next
            }
            let departure=day.addingTimeInterval(TimeInterval(Int.random(in:360...479)*60))
            let firstEnd=departure.addingTimeInterval(TimeInterval(Int.random(in:90...120)*60))
            let back=firstEnd.addingTimeInterval(TimeInterval(Int.random(in:110...150)*60))
            let secondEnd=back.addingTimeInterval(TimeInterval(Int.random(in:90...120)*60))
            data.pending.append(PairDraft(first:departure,firstEnd:firstEnd,second:back,secondEnd:secondEnd))
        }
        reallocateFuel()
    }
    var eligiblePair:PairDraft? {
        guard !Calendar.current.isDateInWeekend(Date()) else{return nil}
        return data.pending.first {
            $0.secondEnd<=Date() && $0.postponedDay != dayText(Date())
        }
    }
    func postpone(_ pair:PairDraft) {
        guard let idx=data.pending.firstIndex(where:{$0.id==pair.id}) else{return}
        data.pending[idx].postponedDay=dayText(Date());persist()
    }
    func accept(_ pair:PairDraft) {
        guard data.pending.contains(where:{$0.id==pair.id}) else{return}
        data.trips.append(Trip(start:pair.first,end:pair.firstEnd,from:"Trenčín",to:"Přerov",
            km:112,kind:"Súkromná",simulated:true,liters:0,fuel:vehicleFuel,cost:0))
        data.trips.append(Trip(start:pair.second,end:pair.secondEnd,from:"Přerov",to:"Trenčín",
            km:112,kind:"Súkromná",simulated:true,liters:0,fuel:vehicleFuel,cost:0))
        data.pending.removeAll{$0.id==pair.id}
        reallocateFuel()
    }
    func addReceipt(_ receipt:FuelEntry) {
        guard receipt.liters.isFinite && receipt.liters>0 && receipt.cost.isFinite && receipt.cost>=0 else{return}
        data.receipts.append(receipt)
        if PrivateFuelCalculator.fuelClass(vehicleFuel)=="unknown" &&
            PrivateFuelCalculator.fuelClass(receipt.fuel) != "unknown" {
            data.fuelType=receipt.fuel
        }
        reallocateFuel()
    }
}

struct GPSResult {
    let start:Date
    let end:Date
    let distance:Double
    let first:CLLocation?
    let last:CLLocation?
    let fixes:Int
    let gaps:Int
}
@MainActor final class RideTracker:NSObject,ObservableObject,CLLocationManagerDelegate {
    @Published var active=false
    @Published var distance=0.0
    @Published var fixCount=0
    @Published var gaps=0
    @Published var status="Pripravené"
    private let manager=CLLocationManager()
    private var wantsStart=false
    private var first:CLLocation?
    private var last:CLLocation?
    private var began=Date()
    override init(){
        super.init()
        manager.delegate=self;manager.desiredAccuracy=kCLLocationAccuracyBestForNavigation
        manager.distanceFilter=3
        manager.activityType = .automotiveNavigation
        manager.pausesLocationUpdatesAutomatically=false
    }
    func start(){
        if active {return}
        let auth=manager.authorizationStatus
        guard auth == .authorizedWhenInUse || auth == .authorizedAlways else {
            wantsStart=true;manager.requestWhenInUseAuthorization();return
        }
        wantsStart=false;first=nil;last=nil;distance=0;fixCount=0;gaps=0;began=Date()
        active=true;status="GPS aktívne"
        manager.allowsBackgroundLocationUpdates=true
        manager.showsBackgroundLocationIndicator=true
        manager.startUpdatingLocation()
    }
    func locationManagerDidChangeAuthorization(_ manager:CLLocationManager){
        if wantsStart && (manager.authorizationStatus == .authorizedWhenInUse || manager.authorizationStatus == .authorizedAlways){start()}
        else if manager.authorizationStatus == .denied {wantsStart=false;status="Povoľ polohu v nastaveniach"}
    }
    func locationManager(_ manager:CLLocationManager,didFailWithError error:Error){gaps+=1;status="Výpadok GPS"}
    func locationManager(_ manager:CLLocationManager,didUpdateLocations updates:[CLLocation]){
        guard active else{return}
        for location in updates {
            guard location.horizontalAccuracy>=0 && location.horizontalAccuracy<=55 else {continue}
            if let previous=last {
                let seconds=location.timestamp.timeIntervalSince(previous.timestamp)
                let segment=location.distance(from:previous)
                if seconds<=0 || seconds>45 {gaps+=1;last=location;fixCount+=1;continue}
                if segment>seconds*48+previous.horizontalAccuracy+location.horizontalAccuracy {gaps+=1;continue}
                if segment>=max(3,(location.horizontalAccuracy+previous.horizontalAccuracy)/5) {distance+=segment/1000}
            }else{first=location}
            last=location;fixCount+=1
        }
    }
    func stop()->GPSResult? {
        guard active else{return nil}
        manager.stopUpdatingLocation();manager.allowsBackgroundLocationUpdates=false;active=false
        let result=GPSResult(start:began,end:Date(),distance:distance,first:first,last:last,fixes:fixCount,gaps:gaps)
        first=nil;last=nil
        status="GPS ukončené"
        return result
    }
    func city(_ location:CLLocation?) async ->String {
        guard let location else{return "Neznáme miesto"}
        do {
            let places=try await CLGeocoder().reverseGeocodeLocation(location,preferredLocale:Locale(identifier:"sk_SK"))
            let p=places.first
            return p?.locality ?? p?.subAdministrativeArea ?? p?.administrativeArea ?? "Neznáme miesto"
        }catch{return "Neznáme miesto"}
    }
}

func receiptText(_ image:UIImage,completion:@escaping(String)->Void){
    guard let cg=image.cgImage else {completion("");return}
    DispatchQueue.global(qos:.userInitiated).async {
        let req=VNRecognizeTextRequest()
        req.recognitionLevel = .accurate
        req.usesLanguageCorrection=true
        try? VNImageRequestHandler(cgImage:cg,options:[:]).perform([req])
        let result=(req.results ?? []).compactMap{$0.topCandidates(1).first?.string}.joined(separator:"\n")
        DispatchQueue.main.async{completion(result)}
    }
}
func capturedNumber(_ pattern:String,_ text:String)->String {
    guard let re=try? NSRegularExpression(pattern:pattern,options:.caseInsensitive),
        let match=re.firstMatch(in:text,range:NSRange(text.startIndex...,in:text)),
        let range=Range(match.range(at:1),in:text) else{return ""}
    return String(text[range]).replacingOccurrences(of:",",with:".")
}
struct CameraPicker:UIViewControllerRepresentable {
    var completed:(UIImage?)->Void
    func makeUIViewController(context:Context)->UIImagePickerController {
        let picker=UIImagePickerController()
        picker.sourceType = .camera
        picker.delegate=context.coordinator
        return picker
    }
    func updateUIViewController(_ controller:UIImagePickerController,context:Context){}
    func makeCoordinator()->Coordinator{Coordinator(completed:completed)}
    class Coordinator:NSObject,UIImagePickerControllerDelegate,UINavigationControllerDelegate {
        let completed:(UIImage?)->Void
        init(completed:@escaping(UIImage?)->Void){self.completed=completed}
        func imagePickerController(_ picker:UIImagePickerController,didFinishPickingMediaWithInfo info:[UIImagePickerController.InfoKey:Any]){
            completed(info[.originalImage] as? UIImage);picker.dismiss(animated:true)
        }
        func imagePickerControllerDidCancel(_ picker:UIImagePickerController){completed(nil);picker.dismiss(animated:true)}
    }
}

@main struct KnihaJazdApp:App {
    var body:some Scene{WindowGroup{ContentView().preferredColorScheme(.dark)}}
}

struct ContentView:View {
    @StateObject private var tracker=RideTracker()
    @StateObject private var journal=JournalStore()
    @State private var tab=0
    @State private var draft:GPSResult?
    @State private var privacy="Súkromná"
    @State private var startTown="Neznáme miesto"
    @State private var endTown="Neznáme miesto"
    @State private var selected:Trip?
    @State private var showConfirmPair=false
    @State private var showCamera=false
    @State private var showReceipt=false
    @State private var receiptDay=""
    @State private var receiptLiters=""
    @State private var receiptFuel=""
    @State private var receiptTotal=""
    @State private var receiptPhoto:UIImage?
    @State private var fuelRateInput=""
    @State private var galleryReceipt:PhotosPickerItem?
    @State private var garagePhoto:PhotosPickerItem?
    @State private var garageImage:UIImage?
    @State private var showError=""
    @Environment(\.scenePhase) private var scene
    private var photoPath:URL {FileManager.default.urls(for:.documentDirectory,in:.userDomainMask)[0].appendingPathComponent("garage-v06.jpg")}
    var body:some View {
        ZStack {
            coveredGarage
            LinearGradient(colors:[.black.opacity(0.85),.clear,.black.opacity(0.94)],
                startPoint:.top,endPoint:.bottom).ignoresSafeArea()
            VStack(spacing:12){
                HStack(spacing:8) {
                    Image(systemName:"steeringwheel").font(.title2).foregroundStyle(.purple)
                    VStack(alignment:.leading){
                        Text("Kniha jázd").font(.headline)
                        Text("Evidencia jázd · v0.6").font(.caption2).foregroundStyle(.gray)
                    }
                    Spacer()
                    Button(action:toggleRide){
                        Image(systemName:tracker.active ? "stop.fill":"play.fill")
                    }.buttonStyle(NeonCircle())
                    Button {
                        if UIImagePickerController.isSourceTypeAvailable(.camera){showCamera=true}
                        else{showError="Fotoaparát nie je dostupný"}
                    }label:{Image(systemName:"camera")}.buttonStyle(NeonCircle())
                    Button {showConfirmPair=journal.eligiblePair != nil}
                        label:{Image(systemName:"bell")}.buttonStyle(NeonCircle())
                }
                if tracker.active {
                    HStack {
                        Label("GPS aktívne",systemImage:"location.fill")
                        Spacer()
                        Text(kilometers(tracker.distance))
                    }.font(.caption).padding(11).neonCard()
                }
                Spacer(minLength:40)
                Group{
                    switch tab {
                    case 0:tripList
                    case 1:analytics
                    default:vehiclePage
                    }
                }
                HStack{
                    tabButton("point.topleft.down.to.point.bottomright.curvepath",index:0)
                    tabButton("chart.bar.fill",index:1)
                    tabButton("car.fill",index:2)
                }.padding(8).background(.black.opacity(0.8),in:Capsule())
                    .overlay(Capsule().stroke(Color.purple.opacity(0.7)))
            }.padding(.horizontal,16).padding(.top,12).padding(.bottom,10)
        }
        .onAppear {
            if let bytes=try? Data(contentsOf:photoPath){garageImage=UIImage(data:bytes)}
            fuelRateInput=String(format:"%.1f",journal.data.averageFuel)
            showConfirmPair=journal.eligiblePair != nil
        }
        .onChange(of:scene){ phase in if phase == .active {showConfirmPair=journal.eligiblePair != nil}}
        .fullScreenCover(item:$selected){trip in TripDetail(trip:trip)}
        .sheet(item:$draftSheet,onDismiss:{
            draft=nil
            startTown="Neznáme miesto"
            endTown="Neznáme miesto"
        }){ _ in completionSheet }
        .sheet(isPresented:$showCamera){CameraPicker{picture in
            showCamera=false
            if let picture {readReceipt(picture)}
        }}
        .sheet(isPresented:$showReceipt){receiptForm}
        .alert("Pripravené modelové jazdy",isPresented:$showConfirmPair){
            Button("Uložiť obe"){if let pair=journal.eligiblePair {journal.accept(pair)}}
            Button("Odložiť",role:.cancel){if let pair=journal.eligiblePair {journal.postpone(pair)}}
        }message:{Text("Trenčín – Přerov a Přerov – Trenčín · 2 × 112 km. Ide o simulované jazdy.")}
        .alert("Informácia",isPresented:Binding(get:{!showError.isEmpty},set:{if !$0 {showError=""}})){
            Button("OK"){showError=""}
        }message:{Text(showError)}
        .onChange(of:garagePhoto){ _ in Task{
            if let raw=try? await garagePhoto?.loadTransferable(type:Data.self){
                try? raw.write(to:photoPath,options:.atomic);garageImage=UIImage(data:raw)
            }
        }}
        .onChange(of:galleryReceipt){ _ in Task{
            if let raw=try? await galleryReceipt?.loadTransferable(type:Data.self),
                let image=UIImage(data:raw){readReceipt(image)}
        }}
    }
    @State private var draftSheet:SheetFlag?
    private func toggleRide(){
        if tracker.active {
            guard let snapshot=tracker.stop() else{return}
            draft=snapshot;privacy="Súkromná";startTown="Neznáme miesto";endTown="Neznáme miesto"
            draftSheet=SheetFlag()
        }else{tracker.start()}
    }
    private func readReceipt(_ image:UIImage){
        receiptPhoto=image
        receiptText(image){text in
            receiptDay=capturedNumber(#"(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})"#,text)
            receiptLiters=capturedNumber(#"(\d{1,3}[,.]\d{1,3})\s*[lL]\b"#,text)
            receiptTotal=capturedNumber(#"(?:SPOLU|CELKOM|SUMA|TOTAL|K\s*[ÚU]HRADE)\s*[:=\-]?\s*(\d+[,.]\d{2})"#,text)
            let upper=text.uppercased()
            receiptFuel = upper.contains("DIESEL") || upper.contains("NAFTA") || upper.contains("B7") ? "Diesel B7"
                : upper.contains("E10") ? "Benzín E10" : upper.contains("E5") || upper.contains("BENZ") ? "Benzín E5" : ""
            if receiptDay.isEmpty{receiptDay=dayText(Date())}
            showReceipt=true
        }
    }
    private var receiptForm:some View {
        NavigationStack{
            Form {
                Text("Skontroluj údaje rozpoznané na bločku. Uloženie automaticky prepočíta odhad paliva na súkromné kilometre.")
                    .font(.caption)
                TextField("Dátum",text:$receiptDay)
                TextField("Litrov",text:$receiptLiters).keyboardType(.decimalPad)
                TextField("Palivo",text:$receiptFuel)
                TextField("Celková cena €",text:$receiptTotal).keyboardType(.decimalPad)
                Text("Natankované litre nie sú automaticky spotrebované litre. Nepoužitý zostatok zostane v evidencii.")
                    .font(.caption).foregroundStyle(.secondary)
            }.navigationTitle("Tankovací bloček")
            .toolbar{
                ToolbarItem(placement:.cancellationAction){Button("Zrušiť"){
                    receiptPhoto=nil
                    showReceipt=false
                }}
                ToolbarItem(placement:.confirmationAction){Button("Uložiť"){
                    let lit=Double(receiptLiters.replacingOccurrences(of:",",with:".")) ?? 0
                    let euro=Double(receiptTotal.replacingOccurrences(of:",",with:".")) ?? -1
                    if lit>0 && euro>=0 {
                        var imageFile:String?=nil
                        if let bytes=receiptPhoto?.jpegData(compressionQuality:0.85) {
                            let filename="fuel-receipt-\(UUID().uuidString).jpg"
                            let destination=FileManager.default.urls(for:.documentDirectory,in:.userDomainMask)[0]
                                .appendingPathComponent(filename)
                            if (try? bytes.write(to:destination,options:.atomic)) != nil {
                                imageFile=filename
                                try? FileManager.default.setAttributes(
                                    [.protectionKey:FileProtectionType.completeUntilFirstUserAuthentication],
                                    ofItemAtPath:destination.path)
                            }
                        }
                        journal.addReceipt(FuelEntry(day:receiptDay,liters:lit,fuel:receiptFuel,
                            cost:euro,photoFile:imageFile))
                        receiptPhoto=nil
                        showReceipt=false
                    }else{showError="Skontroluj litre a celkovú cenu."}
                }}
            }
        }
    }
    private var completionSheet:some View {
        NavigationStack {
            Form{
                if let ride=draft {
                    LabeledContent("Vzdialenosť",value:kilometers(ride.distance))
                    LabeledContent("Trvanie",value:durationText(ride.start,ride.end))
                    LabeledContent("GPS body",value:String(ride.fixes))
                    LabeledContent("Výpadky GPS",value:String(ride.gaps))
                    Picker("Typ jazdy",selection:$privacy){
                        Text("Súkromná").tag("Súkromná")
                        Text("Služobná").tag("Služobná")
                    }.pickerStyle(.segmented)
                    if privacy == "Súkromná" {
                        Text("Uložia sa iba kilometre. Polohy a časy sa zahodia.")
                    }else{
                        TextField("Miesto odchodu",text:$startTown)
                        TextField("Miesto príchodu",text:$endTown)
                        Button("Doplniť mestá z GPS"){
                            Task{
                                startTown=await tracker.city(ride.first)
                                endTown=await tracker.city(ride.last)
                            }
                        }
                    }
                    if ride.fixes<2 || ride.distance<0.02 {
                        Text("Nie je dostatok platných GPS údajov.").foregroundStyle(.orange)
                    }
                }
            }.navigationTitle("Ukončiť jazdu")
            .toolbar{
                ToolbarItem(placement:.cancellationAction){Button("Zahodiť"){
                    draft=nil
                    draftSheet=nil
                }}
                ToolbarItem(placement:.confirmationAction){Button("Uložiť"){
                    guard let ride=draft,ride.fixes>=2,ride.distance>=0.02 else {
                        showError="Jazda nemá dostatok GPS údajov.";return
                    }
                    if privacy == "Súkromná"{journal.addPrivate(ride.distance)}
                    else{
                        journal.saveReal(Trip(start:ride.start,end:ride.end,from:startTown,to:endTown,km:ride.distance,
                            kind:"Služobná",simulated:false,liters:0,fuel:journal.vehicleFuel,cost:0))
                    }
                    draft=nil;draftSheet=nil
                    showConfirmPair=journal.eligiblePair != nil
                }}
            }
        }
    }
    private var tripList:some View {
        VStack(alignment:.leading,spacing:12){
            HStack{
                Image(systemName:"point.topleft.down.to.point.bottomright.curvepath").foregroundStyle(.purple)
                VStack(alignment:.leading){Text("Posledné jazdy").font(.title3.bold())
                    Text("Automatická evidencia").font(.caption2).foregroundStyle(.gray)}
                Spacer()
                Image(systemName:"chevron.right")
            }
            Divider().background(.purple)
            HStack{Text("Dátum");Spacer();Text("Čas");Spacer();Text("Štart – Cieľ");Spacer();Text("Spotreba");Spacer();Text("Náklady")}
                .font(.system(size:9))
            ScrollView {
                LazyVStack(spacing:8){
                    ForEach(journal.sorted){drive in
                        Button{selected=drive}label:{TripCard(trip:drive)}.buttonStyle(.plain)
                    }
                    if journal.sorted.isEmpty {
                        VStack(spacing:8) {
                            Image(systemName:"car.fill").font(.largeTitle).foregroundStyle(.gray)
                            Text("Zatiaľ žiadne jazdy").font(.headline)
                        }.frame(maxWidth:.infinity).padding(20)
                    }
                }
            }.frame(maxHeight:305)
        }.padding(14).neonCard()
    }
    private var analytics:some View {
        let fuel=journal.fuelSummary
        return ScrollView {
            VStack(alignment:.leading,spacing:11) {
                Text("Moja analytika").font(.title3.bold())
                Text("Prehľad skutočných a súkromných kilometrov").font(.caption).foregroundStyle(.gray)
                metric("GPS kilometre",kilometers(journal.data.trips.filter{!$0.simulated}.reduce(0){$0+$1.km}))
                metric("Súkromný zostatok",kilometers(journal.data.privateKm))
                metric("Celkovo súkromné kilometre",kilometers(fuel.privateKm))
                metric("Počet jázd",String(journal.data.trips.count))
                metric("Počet tankovaní",String(journal.data.receipts.count))
                Divider().background(.purple)
                Text("Palivo na súkromné jazdy").font(.headline)
                metric("Odhad spotreby",String(format:"%.2f l",fuel.estimatedLiters))
                metric("Priradené z bločkov",String(format:"%.2f l",fuel.allocatedLiters))
                metric("Zostatok z tankovaní",String(format:"%.2f l",fuel.unallocatedLiters))
                if fuel.uncoveredLiters>0 {
                    metric("Spotreba bez doloženého tankovania",String(format:"%.2f l",fuel.uncoveredLiters))
                }
                metric("Odhad nákladov",String(format:"%.2f €",fuel.estimatedCost))
                if fuel.excludedReceipts>0 {
                    Text("Nepriradené bločky s nezhodným alebo neznámym palivom: \(fuel.excludedReceipts)")
                        .font(.caption).foregroundStyle(.orange)
                }
                Text("Odhad podľa kilometrov a nastavenej spotreby. Tankovanie nie je meraním spotreby.")
                    .font(.caption).foregroundStyle(.gray)
            }.padding(15)
        }.frame(maxHeight:390).neonCard()
    }
    private func metric(_ title:String,_ value:String)->some View {
        HStack(alignment:.firstTextBaseline,spacing:8){
            Text(title).font(.caption).foregroundStyle(.gray)
            Spacer(minLength:5)
            Text(value).font(.subheadline.bold()).multilineTextAlignment(.trailing)
        }
    }
    private func vehicleField(_ title:String,value:String,keyPath:WritableKeyPath<StoreBlob,String?>)->some View {
        HStack {
            Text(title).font(.caption).foregroundStyle(.gray).frame(width:85,alignment:.leading)
            TextField(title,text:Binding(
                get:{journal.data[keyPath:keyPath] ?? ""},
                set:{journal.data[keyPath:keyPath]=$0;journal.persist()}
            )).textFieldStyle(.roundedBorder)
        }
    }
    private var vehiclePage:some View {
        ScrollView {
            VStack(alignment:.leading,spacing:11) {
                Text("Moje vozidlo").font(.title3.bold())
                vehicleField("Značka",value:journal.data.carBrand ?? "",keyPath:\.carBrand)
                vehicleField("Model",value:journal.data.carModel ?? "",keyPath:\.carModel)
                vehicleField("Rok",value:journal.data.carYear ?? "",keyPath:\.carYear)
                vehicleField("EČV / ŠPZ",value:journal.data.carPlate ?? "",keyPath:\.carPlate)
                vehicleField("Farba",value:journal.data.carColor ?? "",keyPath:\.carColor)
                vehicleField("Povrch",value:journal.data.carFinish ?? "",keyPath:\.carFinish)
                Picker("Palivo vozidla",selection:Binding(
                    get:{journal.vehicleFuel},
                    set:{journal.setFuel($0)}
                )){
                    ForEach(["Nezadané","Diesel B7","Benzín E5","Benzín E10"],id:\.self){fuel in
                        Text(fuel).tag(fuel)
                    }
                }.pickerStyle(.menu)
                HStack {
                    Text("Spotreba l/100 km").font(.caption)
                    TextField("6.5",text:$fuelRateInput)
                        .keyboardType(.decimalPad).textFieldStyle(.roundedBorder)
                    Button("Použiť"){
                        if let rate=Double(fuelRateInput.replacingOccurrences(of:",",with:".")),
                            rate>0 && rate<=50 {
                            journal.setAverageFuel(rate)
                        } else {showError="Zadaj spotrebu od 0 do 50 l/100 km."}
                    }.font(.caption.bold())
                }
                Divider().background(.purple)
                Text("Fotografia vozidla").font(.headline)
                PhotosPicker(selection:$garagePhoto,matching:.images){
                    Label("Zmeniť fotografiu auta",systemImage:"photo")
                }
                if garageImage != nil {
                    Button("Vrátiť predvolenú garáž") {
                        try? FileManager.default.removeItem(at:photoPath)
                        garageImage=nil
                    }
                }else{
                    Text("Predvolené pozadie: auto pod bielou plachtou.")
                        .font(.caption).foregroundStyle(.gray)
                }
                Divider().background(.purple)
                PhotosPicker(selection:$galleryReceipt,matching:.images){
                    Label("Bloček z galérie",systemImage:"doc.text.viewfinder")
                }
                Text("Všetky uložené bločky: \(journal.data.receipts.count). Palivo sa odhaduje zo súkromných km.")
                    .font(.caption).foregroundStyle(.gray)
            }.padding(15)
        }.frame(maxHeight:395).neonCard()
    }
    private func tabButton(_ icon:String,index:Int)->some View {
        Button{tab=index}label:{
            Image(systemName:icon).font(.title3).frame(maxWidth:.infinity).padding(12)
                .background(tab==index ? Color.purple.opacity(0.55):Color.clear,in:Capsule())
        }
    }
    private var coveredGarage:some View {
        ZStack {
            Color(red:0.035,green:0.039,blue:0.08)
            if let image=garageImage {
                Image(uiImage:image).resizable().scaledToFill().ignoresSafeArea()
            }else {
                GeometryReader{geometry in
                    let w=geometry.size.width
                    let h=geometry.size.height
                    Canvas{ctx,size in
                        for row in 0..<5 {
                            var beam=Path()
                            beam.move(to:CGPoint(x:w*0.04,y:h*(0.12+Double(row)*0.066)))
                            beam.addLine(to:CGPoint(x:w*0.96,y:h*(0.09+Double(row)*0.066)))
                            ctx.stroke(beam,with:.color(.white.opacity(0.4)),lineWidth:2)
                        }
                        let rect=CGRect(x:w*0.09,y:h*0.41,width:w*0.82,height:h*0.19)
                        let car=Path(roundedRect:rect,cornerRadius:CGFloat(45))
                        ctx.fill(car,with:.linearGradient(Gradient(colors:[.gray,.white,.white.opacity(0.75),.gray]),
                            startPoint:CGPoint(x:w*0.15,y:h*0.38),endPoint:CGPoint(x:w*0.8,y:h*0.65)))
                    }
                }
            }
        }.ignoresSafeArea()
    }
}
struct SheetFlag:Identifiable{let id=UUID()}
struct NeonCircle:ButtonStyle {
    func makeBody(configuration:Configuration)->some View{
        configuration.label.font(.headline)
            .frame(width:43,height:43)
            .background(.black.opacity(0.75),in:Circle())
            .overlay(Circle().stroke(Color.purple.opacity(0.8)))
            .foregroundStyle(.white).opacity(configuration.isPressed ? 0.7:1)
    }
}
struct NeonCard:ViewModifier {
    func body(content:Content)->some View {
        content.background(Color(red:0.08,green:0.07,blue:0.14).opacity(0.93),in:RoundedRectangle(cornerRadius:22))
            .overlay(RoundedRectangle(cornerRadius:22).stroke(Color.purple.opacity(0.8),lineWidth:1))
            .shadow(color:.purple.opacity(0.3),radius:10)
    }
}
extension View{func neonCard()->some View{modifier(NeonCard())}}
struct TripCard:View {
    let trip:Trip
    var body:some View {
        VStack(alignment:.leading,spacing:7){
            HStack(alignment:.center,spacing:8) {
                Image(systemName:"car.fill").foregroundStyle(.white)
                VStack(alignment:.leading,spacing:2) {
                    Text(trip.from).font(.system(size:13,weight:.bold)).lineLimit(1)
                    Label(clockText(trip.start),systemImage:"clock")
                        .font(.system(size:10)).foregroundStyle(.white.opacity(0.8))
                }.frame(maxWidth:.infinity,alignment:.leading)
                Text("–").font(.caption.bold()).foregroundStyle(.white.opacity(0.85))
                VStack(alignment:.leading,spacing:2) {
                    Text(trip.to).font(.system(size:13,weight:.bold)).lineLimit(1)
                    Label(clockText(trip.end),systemImage:"clock")
                        .font(.system(size:10)).foregroundStyle(.white.opacity(0.8))
                }.frame(maxWidth:.infinity,alignment:.leading)
                Text(trip.kind).font(.system(size:9))
                    .padding(.horizontal,7).padding(.vertical,5)
                    .background(Color.purple.opacity(0.58),in:Capsule())
                    .overlay(Capsule().stroke(.purple.opacity(0.95)))
            }
            Divider().overlay(.purple.opacity(0.4))
            HStack(spacing:4){
                Text(dayText(trip.start))
                Spacer(minLength:1)
                Text(durationText(trip.start,trip.end))
                Spacer(minLength:1)
                Text(kilometers(trip.km))
                Spacer(minLength:1)
                Text(trip.liters>0 ? String(format:"%.1f l",trip.liters):"— l")
                Spacer(minLength:1)
                Text(trip.cost>0 ? String(format:"%.2f €",trip.cost):"— €")
            }.font(.system(size:8,weight:.medium)).lineLimit(1)
        }
        .padding(10)
        .background(Color.black.opacity(0.52),in:RoundedRectangle(cornerRadius:15))
        .overlay(RoundedRectangle(cornerRadius:15).stroke(.purple.opacity(0.95),lineWidth:1.25))
    }
}
struct TripDetail:View {
    let trip:Trip
    @Environment(\.dismiss) private var dismiss
    var body:some View {
        NavigationStack{
            Form{
                Section{
                    Text(trip.from+" → "+trip.to).font(.title3.bold())
                    if trip.simulated{
                        Text("Simulovaná").foregroundStyle(.purple).font(.headline)
                        Text("Modelová jazda, nie skutočný GPS záznam.").font(.caption)
                    }
                }
                Section("Údaje o jazde"){
                    LabeledContent("Dátum",value:dayText(trip.start))
                    LabeledContent("Štart",value:trip.from+" · "+clockText(trip.start))
                    LabeledContent("Príchod",value:trip.to+" · "+clockText(trip.end))
                    LabeledContent("Trvanie",value:durationText(trip.start,trip.end))
                    LabeledContent("Kilometre",value:kilometers(trip.km))
                    LabeledContent("Palivo",value:trip.fuel)
                    LabeledContent("Spotreba",value:String(format:"%.2f l (odhad)",trip.liters))
                    if trip.simulated {
                        LabeledContent("Priradené z bločkov",value:String(format:"%.2f l (odhad)",trip.allocatedFromReceipts ?? 0))
                    }
                    LabeledContent("Náklady",value:String(format:"%.2f € (odhad z bločkov)",trip.cost))
                }
            }
            .scrollContentBackground(.hidden)
            .background(Color(red:0.04,green:0.035,blue:0.075))
            .navigationTitle("Podrobnosti jazdy")
            .toolbar{ToolbarItem(placement:.topBarLeading){Button("Späť"){dismiss()}}}
        }
    }
}
