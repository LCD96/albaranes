# Escáner de albaranes

App Android de uso personal (solo mi teléfono, no se publica) para fotografiar albaranes, marcar sobre la foto la columna de códigos de barras y la de cantidades, extraer los pares código–cantidad y exportarlos como JSON.

## Stack

- Kotlin + Jetpack Compose (Material 3), una sola Activity.
- Gradle con Kotlin DSL y version catalog (`gradle/libs.versions.toml`). Toda dependencia nueva se añade al catálogo.
- ML Kit, todo on-device y sin conexión:
  - Document Scanner (`play-services-mlkit-document-scanner`): captura, detección de bordes y corrección de perspectiva.
  - Barcode Scanning: lectura de códigos de barras.
  - Text Recognition (alfabeto latino): OCR de cantidades y de códigos impresos como cifras.
- Coroutines para el trabajo en segundo plano.
- kotlinx.serialization para el JSON.
- Persistencia: ficheros JSON en el almacenamiento interno de la app (sin base de datos de momento).

## Flujo de la app

1. **Captura:** abrir el Document Scanner de ML Kit y obtener la imagen ya enderezada.
2. **Regiones:** mostrar la imagen y permitir dibujar, mover y redimensionar dos rectángulos: uno para **códigos** y otro para **cantidades**. Colores distintos y etiquetados.
3. **Reconocimiento:** procesar cada región por separado.
4. **Emparejado:** asociar cada código con su cantidad por posición vertical.
5. **Revisión:** tabla editable con los resultados; las líneas dudosas se resaltan.
6. **Exportación:** guardar el JSON y permitir compartirlo con la hoja de compartir de Android.

## Arquitectura

Separar estrictamente la UI de la lógica. Paquetes orientativos:

- `ui/` — pantallas Compose y ViewModels.
- `capture/` — integración con el Document Scanner.
- `recognition/` — llamadas a ML Kit. Recibe un `Bitmap` y una región; devuelve detecciones con su caja delimitadora y su texto o valor.
- `pipeline/` — **lógica pura en Kotlin, sin dependencias de Android**: parseo, validación, emparejado y construcción del resultado. Debe poder probarse en la JVM.
- `export/` — serialización a JSON, guardado y compartir.
- `model/` — data classes compartidas.

### Coordenadas

- Las regiones se guardan **normalizadas (0–1)** respecto a la imagen enderezada, nunca en píxeles de pantalla. Así sirven para cualquier resolución y para las futuras plantillas.
- La conversión pantalla ↔ imagen se hace en un único sitio y se prueba.
- Las detecciones de ML Kit sobre un recorte se trasladan a coordenadas de la imagen completa antes de salir de `recognition/`.

### Lectura de códigos

1. Intentar primero Barcode Scanning sobre el recorte de la región de códigos.
2. Si no detecta nada (o detecta muy pocos respecto a las líneas de texto), recurrir a OCR y quedarse con secuencias de 8 o 13 dígitos (EAN-8 / EAN-13), tolerando espacios intermedios.
3. Validar siempre el dígito de control EAN. Un código que no lo supere se conserva, pero marcado como dudoso.

### Lectura de cantidades

- OCR sobre la región de cantidades.
- Aceptar enteros y decimales con coma o punto (`12`, `12,5`, `1.000` en formato español = mil). Documentar en el código cómo se resuelven los casos ambiguos.
- Ignorar cabeceras y texto no numérico.

### Emparejado de filas

- Usar el centro vertical de la caja de cada detección.
- Tolerancia derivada de la altura mediana de las detecciones, no un valor fijo en píxeles.
- Emparejar cada código con la cantidad más cercana en vertical dentro de la tolerancia, sin reutilizar cantidades.
- Códigos sin cantidad y cantidades sin código se incluyen en el resultado como líneas incompletas, nunca se descartan en silencio.

## Formato del JSON

```json
{
  "version": 1,
  "id": "uuid",
  "fecha": "2026-10-24T14:41:00+02:00",
  "proveedor": null,
  "regiones": {
    "codigos":    { "left": 0.10, "top": 0.30, "right": 0.35, "bottom": 0.85 },
    "cantidades": { "left": 0.70, "top": 0.30, "right": 0.80, "bottom": 0.85 }
  },
  "lineas": [
    {
      "codigo": "8412345678905",
      "cantidad": 12,
      "origenCodigo": "barcode",
      "eanValido": true,
      "dudosa": false,
      "editadaManualmente": false
    }
  ]
}
```

`origenCodigo` puede ser `barcode`, `ocr` o `manual`. Si el formato cambia, se incrementa `version`.

## Pruebas

ML Kit necesita el runtime de Android, así que las pruebas se dividen en dos niveles:

- **Lógica pura (`pipeline/`)** → tests unitarios en la JVM (`app/src/test`). Usan como fixtures ficheros JSON con detecciones ya grabadas (cajas + texto), de modo que el emparejado y el parseo se pueden iterar sin móvil ni cámara.
- **Reconocimiento con ML Kit** → tests instrumentados (`app/src/androidTest`) con fotos reales de albaranes en `app/src/androidTest/assets/albaranes/`. Además de comprobar resultados, deben poder **volcar las detecciones a JSON** para convertirlas en fixtures de los tests de la JVM.

Las fotos de albaranes reales de `samples/` (si existen) son el banco de pruebas de referencia. No subir a git fotos con datos sensibles sin revisar.

## Vista de depuración

Mantener una pantalla o modo de depuración que pinte sobre la imagen las regiones, las cajas detectadas por ML Kit y las filas emparejadas. Es la herramienta principal para entender por qué falla un caso.

## Comandos

- Compilar: `.\gradlew assembleDebug`
- Instalar en el móvil: `.\gradlew installDebug`
- Tests de la JVM: `.\gradlew test`
- Tests instrumentados (móvil conectado): `.\gradlew connectedAndroidTest`
- Ver dispositivos: `adb devices`
- Logs de la app: `adb logcat` filtrando por el tag de la app

El entorno es Windows con PowerShell. El móvil está conectado por adb (USB o depuración inalámbrica).

## Plan por fases

Trabajar fase a fase. Cada fase termina compilando, instalada en el móvil y probada por mí antes de pasar a la siguiente.

1. **Captura:** botón que abre el Document Scanner y muestra la imagen enderezada.
2. **Editor de regiones:** dibujar, mover y redimensionar los dos rectángulos; guardar las coordenadas normalizadas.
3. **Reconocimiento:** leer ambas regiones con ML Kit y mostrar la vista de depuración con las cajas detectadas.
4. **Emparejado y revisión:** emparejado por filas, tabla editable, resaltado de líneas dudosas.
5. **Exportación e historial:** guardar el JSON, compartirlo y listar albaranes anteriores.
6. **Plantillas por proveedor:** guardar las regiones con un nombre de proveedor y reutilizarlas en capturas nuevas.

Más adelante (sin fecha): detección automática de columnas.

## Normas de trabajo

- Cambios pequeños e incrementales. Después de cada cambio, compilar con `.\gradlew assembleDebug` y corregir los errores antes de seguir.
- No cambiar versiones de Gradle, del Android Gradle Plugin, de Kotlin ni del compileSdk sin preguntarme antes.
- Al añadir una dependencia, comprobar que la versión es estable y compatible con las existentes.
- Pedir solo los permisos estrictamente necesarios.
- Nada de llamadas de red: todo el procesamiento es local.
- Código y comentarios en inglés; textos de la interfaz en español.
- Al terminar cada fase, resumir qué se ha hecho, cómo probarlo en el móvil y proponer el mensaje de commit.