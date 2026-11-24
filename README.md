# APK Ajan

Kısa video akışlarına (Instagram Reels, YouTube Shorts, TikTok ve seçtiğin diğer uygulamalar) karşı çalışan Android uygulaması.
Kotlin + Jetpack Compose. Arayüz dilleri: **AZ · TR · EN · RU**.

## Kurulum (telefon)
1. `ApkAjan.apk` dosyasını telefona at ve aç ("Bilinmeyen kaynaklar"a izin ver).
2. Uygulamada **Kurulum ve izinler** kartındaki 4 maddeyi aç:
   - Erişilebilirlik hizmeti → *APK Ajan sayacı* → Aç
     (Android 13+ anahtar gri ise: Uygulama bilgisi → ⋮ → "Kısıtlanmış ayarlara izin ver", sonra tekrar.)
   - Diğer uygulamaların üzerinde göster, Bildirimler, Pil optimizasyonu kapalı.
3. Uygulamaları ve limiti seç → **Başlat**.

## Mimari

```
app/src/main/java/com/apkajan/app/
├── ApkAjanApp.kt              Application + AppContainer (elle DI, tek süreçte ortak durum)
├── core/
│   ├── Locale.kt              AppLanguage (AZ/TR/EN/RU), LocaleHelper, DayKey (yyyy-MM-dd)
│   └── Permissions.kt         İzin kontrolleri + sistem ayar ekranı intent'leri
├── data/
│   ├── local/Database.kt      Room: AppDailyCount(gün, paket, adet) + DAO + DayTotal
│   ├── settings/SettingsRepository.kt   SharedPreferences + StateFlow (dil, limit, seçili uyg., Pomodoro...)
│   ├── remote/MessageRepository.kt      Uzak JSON → seçili dil; başarısızsa yedek
│   ├── remote/FallbackMessages.kt       4 dilde yerleşik sert cümleler + Pomodoro cümlesi
│   └── apps/InstalledAppsRepository.kt  Hızlı seçimler (Instagram/YouTube/TikTok) + yüklü uygulamalar
├── domain/
│   ├── LimitPolicy.kt         Kademeli limit matematiği (50 → 100 → 150 ...), birim testli
│   ├── CounterEngine.kt       Günlük sayaç, gece yarısı otomatik sıfırlama, "Sıfırla" ofseti, uyarı olayları
│   └── PomodoroEngine.kt      Çalışma/mola döngüsü, kalıcı bitiş zamanı, aşama bildirimleri
├── service/
│   ├── ShortsAccessibilityService.kt   Ön plan uygulama takibi, kaydırma sayımı, uyarıları tetikleme
│   ├── ShortVideoDetector.kt  Hangi kaydırma "yeni kısa video"? (görünüm kimliği + sayfa indeksi + zaman)
│   ├── OverlayController.kt   Üstte canlı sayaç balonu + tam ekran sert uyarı (düğme birkaç sn kilitli)
│   ├── MonitorService.kt      Ön plan servisi (specialUse): süreci canlı tutar, bildirimde sayaç/Pomodoro
│   ├── BootReceiver.kt        Yeniden başlatma sonrası devam
│   └── NotificationHelper.kt  Kanallar ve bildirimler (seçili dilde)
└── ui/
    ├── MainActivity.kt        Dil uygulama (attachBaseContext) + basit ekran yönlendirme
    ├── main/                  Ana ekran: sayaç, Başlat/Durdur, Sıfırla, limit, uygulamalar, izinler, JSON adresi
    ├── pomodoro/              Dairesel geri sayım, süre ayarları
    ├── stats/                 Özet kutucuklar, 7 günlük sütun grafik (limit çizgili), günlük uygulama dağılımı
    ├── apps/                  Diğer yüklü uygulamaları seçme (arama)
    ├── components/            Ortak kart/stepper/üst çubuk
    └── theme/                 Renkler, açık/koyu tema
```

### Akış
1. Kullanıcı **Başlat**'a basar → `monitoring = true`, `MonitorService` (kalıcı bildirim) başlar.
2. `ShortsAccessibilityService` seçili uygulamada `TYPE_VIEW_SCROLLED` olayı alır →
   `ShortVideoDetector` gerçekten kısa video akışı mı ve yeni videoya mı geçildi karar verir.
3. `CounterEngine.onShortVideo()` → Room'a (bugün, paket) +1 → sayaç güncellenir → üst balon yenilenir.
4. Sayaç `limit × n` değerine ulaşınca (`LimitPolicy`) bir `LimitWarning` yayılır →
   `MessageRepository` internetten seçili dilde cümle çeker (yoksa yedek) → `OverlayController` sert uyarıyı gösterir.
   Kullanıcı kapatıp devam ederse sayaç durmaz; bir sonraki uyarı bir sonraki katta gelir.
5. Pomodoro **çalışma** aşamasındayken seçili uygulama açılırsa → kısa Pomodoro uyarısı (kalınırsa dakikada bir tekrar).
6. Gün değişince sayaç 0'dan başlar; eski günler veritabanında kalır ve istatistik ekranında görünür.

### Sayaç ve sıfırlama
- **Gösterilen sayaç** = bugünkü toplam − "Sıfırla" ofseti. Sıfırla sadece canlı sayacı sıfırlar; istatistik geçmişi silinmez.
- Gece yarısı ofset ve uyarı katı kendiliğinden sıfırlanır (servis dakikada bir kontrol eder).

### Uzak mesajlar
Uygulama varsayılan olarak bu depodaki [`remote/messages.json`](remote/messages.json) dosyasını
(`https://raw.githubusercontent.com/texnikpal0-maker/apk-ajan/main/remote/messages.json`) çeker.
Cümleleri değiştirmek için GitHub'da bu dosyayı düzenlemek yeterli; APK'yı yeniden kurmaya gerek yok.
Başka bir adres, uygulamadaki **Uyarı mesajı kaynağı** alanından girilebilir. `{count}` ve `{limit}` yer tutucuları kullanılabilir.
İnternet yoksa / istek 4 sn içinde dönmezse / o dilde liste boşsa `FallbackMessages` kullanılır. Sonuç 30 dk bellekte tutulur.

## Derleme (PC)
JDK 17 + Android SDK (platform 35) gerekir. `local.properties` içindeki `sdk.dir`'i kendi SDK yoluna göre düzenle.
```
gradlew assembleDebug          # APK: app/build/outputs/apk/debug/app-debug.apk
gradlew testDebugUnitTest      # LimitPolicy testleri
```

## Bilinen sınırlar
- Sayım, uygulamaların erişilebilirlik olaylarına dayanır. Instagram/YouTube/TikTok arayüzlerini güncellerse
  görünüm kimlikleri değişebilir; o zaman `ShortVideoDetector.looksLikeShortsFeed` güncellenmelidir.
- Bazı üreticiler (Xiaomi, Huawei, Oppo...) arka plan uygulamalarını agresif kapatır. Ayarlardan "Otomatik başlatma"
  ve "Kısıtlama yok" pil modunu ayrıca açmak gerekebilir.
- Bu sürüm debug anahtarıyla imzalıdır (kişisel kullanım). Play Store için ayrı imza ve politika incelemesi gerekir.
