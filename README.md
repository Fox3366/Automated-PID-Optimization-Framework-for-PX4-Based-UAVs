# LLM Log Analyzer: Automated PID Optimization Framework for PX4-Based UAVs

![Java](https://img.shields.io/badge/Java-17%2B-blue?logo=java)
![Python](https://img.shields.io/badge/Python-3.x-yellow?logo=python)
![OpenAI](https://img.shields.io/badge/OpenAI-API-green?logo=openai)

---

##  Türkçe

### 📖 Hakkında
**LLM Log Analyzer**, PX4 otopilot sistemine sahip İnsansız Hava Araçları (İHA) için PID optimizasyon sürecini modernize eden, hızlı ve akıllı bir analiz aracıdır. JavaFX tabanlı kullanıcı arayüzü ve Python veri işleme betiklerini harmanlayarak, karmaşık uçuş loglarını anlamlı ve uygulanabilir mühendislik tavsiyelerine dönüştürür.

### ✨ Temel Özellikler
* **Otomatik Veri Ayrıştırma:** `.ulg` formatındaki uçuş loglarını anında `.csv` formatına dönüştürür.
* **Akıllı Filtreleme:** Devasa log dosyaları içinden sadece analiz için kritik olan verileri (PID, Actuator, Battery vb.) ayıklar.
* **LLM Entegrasyonu:** Filtrelenmiş verileri OpenAI API (GPT modelleri) üzerinden analiz ederek iyileştirme önerileri sunar.
* **Etkileşimli Analiz:** Modelin verdiği cevaplar üzerinden takip soruları sorulmasına olanak tanıyan sohbet yapısı.
* **Raporlama:** Analiz sonuçlarını kalıcı olması adına Markdown (`.md`) formatında arşivler.

### 🛠️ Kurulum ve Gereksinimler
Uygulamanın çalışması için sisteminizde aşağıdaki bileşenlerin yüklü olması gerekmektedir:

* **Java 17+ & JavaFX:** Kullanıcı arayüzü ve ana mantık için.
* **Python 3.x:** Veri işleme operasyonları için.
* **OpenAI API Key:** Analiz motorunu çalıştırmak için gereklidir.
* **pyulog Kütüphanesi:** `.ulg` dosyalarını okumak için Python bağımlılığı.
    ```bash
    pip install pyulog
    ```

### ⚙️ Yapılandırma (Settings)
Uygulamayı kullanmaya başlamadan önce **Settings** sekmesinden aşağıdaki ayarları yapılandırmalısınız:

| Ayar | Açıklama |
| :--- | :--- |
| **API Key** | Geçerli OpenAI API anahtarınız. |
| **Model Name** | Kullanılacak dil modeli (Örn: `gpt-4-turbo` veya `gpt-4o`). |
| **Max Tokens** | Yanıt uzunluğunu ve API maliyetini kontrol etmek için token sınırı. |
| **Timeout** | Modelden yanıt beklenecek maksimum süre (saniye). |
| **Default Prompt** | Analiz için modele gönderilecek temel talimatlar. |
| **CSV Filters** | Hangi CSV dosyalarının işleneceğini belirleyen anahtar kelimeler/desenler. |

### 🚀 Kullanım Kılavuzu
1.  **Log Yükleme:** `.ulg` dosyasını sürükle-bırak yöntemiyle veya dosya seçici ile sisteme yükleyin.
2.  **Otomatik İşleme:** Uygulama logu parçalar, filtreler ve `CSVs/logAdi{tarih}` klasörüne kaydeder.
3.  **Analiz:** Arayüz üzerinden "Varsayılan Prompt ile Analiz Et" veya "Özel Prompt" seçeneklerinden birini kullanarak işlemi başlatın.
4.  **Takip Soruları:** Sağ taraftaki kutuda beliren yanıtın altına, merak ettiğiniz detayları sorarak modelle sohbete devam edin.
5.  **Kaydet:** Başarılı analizleri `reports` klasörüne rapor olarak kaydedin.

---

##  English Version

### 📖 Abstract
The **LLM Log Analyzer** is a specialized framework designed to provide an efficient and intelligent solution for PID tuning in PX4-based UAVs. By utilizing Large Language Models (LLMs), the tool interprets telemetry data and offers precise optimization recommendations.

### ⚙️ Technical Workflow
* **Data Parsing:** Ingests `.ulg` files and decomposes them into `.csv` datasets utilizing the `pyulog` library.
* **Filtering:** Isolates high-relevance telemetry topics critical for PID analysis.
* **AI Analysis:** Bundles filtered data with customizable prompts and transmits them to OpenAI's GPT models.
* **Reporting:** Generates and archives professional optimization reports in Markdown format.

### 🚀 Operational Procedures
1.  **Authentication:** Register your OpenAI API key in the application settings.
2.  **Constraints:** Configure `max_tokens` and `timeout` limits for optimized API usage.
3.  **Context Injection:** Define system prompts and CSV patterns to ensure the LLM focuses strictly on the correct flight dynamics.
4.  **Interaction:** Engage in a continuous dialogue with the model via the "Follow-up Question" module to refine tuning suggestions.

---

## 🤝 İletişim & Katkıda Bulunma / Contact & Contributing
Projeyi geliştirmek için fikirleriniz, hata bildirimleriniz veya katkılarınız varsa iletişime geçmekten çekinmeyin: / Feel free to reach out for ideas or contributions:

* **Geliştirici / Developer:** Yusuf Karagülle
* **LinkedIn:** [linkedin.com/in/yusuf-karagülle](https://linkedin.com/in/yusuf-karagülle)
