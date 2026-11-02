# SumoCarrito – Guía de compilación y publicación

## 0. Obtener el APK SIN instalar nada (GitHub Actions)
1. Crea una cuenta gratis en github.com → *New repository* (privado está bien).
2. Descomprime el zip y sube el CONTENIDO de la carpeta `SumoCarrito` a la raíz del repo
   (*Add file → Upload files*; incluye la carpeta oculta `.github`; si no la ves, usa Git o GitHub Desktop).
3. Pestaña **Actions** → *Compilar APK* → *Run workflow* (se ejecuta también en cada push).
4. Cuando termine (≈5–8 min) abre la ejecución → *Artifacts* → descarga **SumoCarrito-APK** → `app-debug.apk`.
5. Pásalo al teléfono (cable, Drive o WhatsApp a ti mismo) → ábrelo → permite *instalar apps de origen desconocido*.
   Es un APK de **prueba** (firmado con llave debug): los anuncios son de prueba y **las compras Pro no funcionan fuera de Play** (ver sección 5).
6. Si el workflow falla, abre el log del paso fallido: suele ser un ajuste de versión; pégamelo y lo corrijo.

## 1. Compilar en tu PC (Android Studio Koala o superior, JDK 17)
1. Abrir la carpeta `SumoCarrito` en Android Studio → esperar el *Gradle Sync*.
2. Ejecutar pruebas del motor: `./gradlew :app:testDebugUnitTest`
3. Probar en dispositivo/emulador: botón ▶ (variante `debug`).
   Si falla el sync por el wrapper: *File → Settings → Gradle* y usar Gradle 8.7+.

## 2. Antes de publicar (OBLIGATORIO)
- [ ] Reemplazar IDs de **prueba** de AdMob: `AndroidManifest.xml` (APPLICATION_ID) y `ads/AdManager.kt` (BANNER, INTERSTITIAL).
- [ ] Reemplazar el icono (`android:icon`) por un icono adaptativo propio (Image Asset Studio).
- [ ] Cambiar `applicationId` si `com.sumocarrito` ya está tomado en Play.
- [ ] Verificar IVA/IGTF vigentes con un contador y ajustar los valores por defecto en `TaxConfig`.
- [ ] Probar compras con una cuenta de *license testing* (ver punto 5).

## 3. Firmar y generar el AAB
1. Crear la llave (guárdala y respáldala; si la pierdes no podrás actualizar la app sin Play App Signing):
   `keytool -genkey -v -keystore sumocarrito.jks -keyalg RSA -keysize 2048 -validity 10000 -alias sumocarrito`
2. En `~/.gradle/gradle.properties` (nunca en el repo):
   ```
   SC_STORE_FILE=/ruta/sumocarrito.jks
   SC_STORE_PASSWORD=...
   SC_KEY_ALIAS=sumocarrito
   SC_KEY_PASSWORD=...
   ```
3. `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`
4. APK para pruebas directas: `./gradlew :app:assembleRelease`.
   Play exige **AAB** para apps nuevas; el tamaño de descarga queda muy por debajo de 15 MB con R8.

## 4. Play Console
1. Crear cuenta de desarrollador (pago único de 25 USD) y verificar identidad.
2. *Crear app* → nombre SumoCarrito, idioma Español, tipo App, gratuita.
3. Completar: *Política de privacidad* (URL pública; indicar que los datos financieros se guardan solo en el dispositivo y cifrados), *Seguridad de los datos*, *Clasificación de contenido*, *Público objetivo*, *Declaración de anuncios = Sí*, *ID de publicidad*.
4. Ficha: descripción corta/larga, icono 512×512, gráfico de funciones 1024×500, mínimo 2 capturas de teléfono.
5. *Pruebas → Prueba interna*: subir el AAB, añadir testers. Las cuentas personales nuevas deben hacer **prueba cerrada con ≥12 testers durante 14 días** antes de pedir producción.
6. Activar **Play App Signing** al subir el primer AAB.
7. *Producción → Crear versión* → revisar → enviar. La revisión suele tardar de unas horas a varios días.

## 5. Monetización
**Play Console → Monetizar:**
- Suscripción: ID `pro_monthly`, precio 0.99 USD, periodo mensual (crear un plan base y una oferta).
- Producto único: ID `pro_lifetime`, precio 4.99 USD.
- Añadir cuentas en *Pruebas de licencia* para comprar sin cobro.

**AdMob (admob.google.com):** crear app Android → crear unidad *Banner* e *Intersticial* → copiar los IDs al código.
Vincular AdMob con la ficha de Play cuando la app esté publicada.

## 6. Notas de seguridad/privacidad ya implementadas
- Base de datos cifrada con SQLCipher; la clave aleatoria vive en `EncryptedSharedPreferences` (Android Keystore).
- `allowBackup=false` y sin tráfico HTTP en claro.
- Permisos: solo `INTERNET` (AdMob/Billing) y `VIBRATE`.
- Nada financiero se envía a servidores propios.

## 7. Funciones agregadas en esta versión
- **Tasa BCV:** botón `BCV` junto a la tasa (y consulta automática al abrir si hay internet). Lee la portada de bcv.org.ve.
  Si el BCV cambia su HTML o su certificado falla, la app lo avisa y conserva la última tasa / permite escribirla a mano.
  La seguridad TLS NO se desactiva.
- **Dictado por voz:** en el botón `+` → `🎤 Dictar`. Ejemplos: «leche dos con cincuenta», «tres harina a uno con veinticinco».
  Rellena producto/precio/cantidad para que confirmes antes de agregar. Usa el modelo local si el teléfono tiene el paquete de voz en español descargado.
  Permiso de micrófono solo al primer uso → en la ficha de Play declara `RECORD_AUDIO` y que el audio lo procesa el reconocedor de Android/Google.
- **Alto contraste:** Ajustes → *Modo alto contraste* (negro/blanco/amarillo, semáforo más intenso).

## 8. Pendiente / siguientes mejoras
- Pruebas instrumentadas de UI (Compose test).
