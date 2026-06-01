' scripts/refresh-excel.vbs
' Abre un archivo Excel con COM Automation, ejecuta RefreshAll para reconstruir
' el modelo de datos VertiPaq, y guarda el resultado.
' Requiere: Excel instalado en el servidor Windows.
'
' VBScript corre nativamente en STA (Single-Threaded Apartment),
' lo que evita los problemas de COM que tiene PowerShell desde servicios.
'
' Uso: cscript.exe //Nologo refresh-excel.vbs "C:\reportes\archivo.xlsx"
'
' Log: se escribe en <dir-del-script>\refresh-excel.log (append). Para tailing:
'      Get-Content C:\ccr_zoomin\scripts\refresh-excel.log -Wait -Tail 50

Dim filePath, fso, excel, workbook, workbooks
Dim connections, conn, oledbConn, odbcConn, model, sheets, ws
Dim LOG_PATH, RUN_ID

Set fso = CreateObject("Scripting.FileSystemObject")
LOG_PATH = fso.GetParentFolderName(WScript.ScriptFullName) & "\refresh-excel.log"
' RUN_ID permite distinguir invocaciones concurrentes en el log compartido.
Randomize
RUN_ID = Right("000000" & CStr(Int(Rnd * 1000000)), 6)

' ------------- Helpers de logging -------------
Function Tstamp()
    Dim d
    d = Now
    Tstamp = Year(d) & "-" & Right("0" & Month(d), 2) & "-" & Right("0" & Day(d), 2) & " " & _
            Right("0" & Hour(d), 2) & ":" & Right("0" & Minute(d), 2) & ":" & Right("0" & Second(d), 2)
End Function

Sub Log(msg)
    On Error Resume Next
    Dim f
    Set f = fso.OpenTextFile(LOG_PATH, 8, True)  ' 8 = ForAppending
    If Err.Number <> 0 Then
        ' Si no se puede abrir el log, escribir al stdout (capturado por Java).
        WScript.StdOut.WriteLine "[no-log] " & Tstamp() & " [" & RUN_ID & "] " & msg
        Err.Clear
        Exit Sub
    End If
    f.WriteLine Tstamp() & " [" & RUN_ID & "] " & msg
    f.Close
End Sub

Sub LogErr(context)
    If Err.Number <> 0 Then
        Log "ERROR en " & context & " | Err.Number=" & Err.Number & _
            " | Err.Source=" & Err.Source & " | Err.Description=" & Err.Description
    End If
End Sub
' ----------------------------------------------

If WScript.Arguments.Count < 1 Then
    Log "FAIL: invocación sin argumentos"
    WScript.StdErr.WriteLine "Uso: cscript.exe //Nologo refresh-excel.vbs <ruta-archivo.xlsx>"
    WScript.Quit 1
End If

filePath = WScript.Arguments(0)
Log "START | archivo=" & filePath

If Not fso.FileExists(filePath) Then
    Log "FAIL: archivo no encontrado | " & filePath
    WScript.StdErr.WriteLine "Archivo no encontrado: " & filePath
    WScript.Quit 1
End If

' Convertir a ruta absoluta
filePath = fso.GetAbsolutePathName(filePath)
Log "Ruta absoluta resuelta: " & filePath

On Error Resume Next

' Workbooks.Open puede fallar de forma transitoria en Excel COM bajo servicio
' Windows. Patrón observado en producción:
'   intento 1: Err.Number=1004 "No se puede obtener la propiedad Open..."
'   intentos 2..N: Err.Number=-2147418111 (RPC_E_SERVERFAULT), descripción vacía
' Esto significa que tras el primer fallo, Excel.Application queda internamente
' corrupto y CUALQUIER llamada subsiguiente devuelve RPC_E_SERVERFAULT. Por eso
' no alcanza con reintentar Open: hay que descartar Excel.Application entero y
' crear uno fresco entre reintentos.
'
' Bajo Excel COM en servicio, Workbooks.Open también puede devolver Nothing SIN
' levantar error (Err.Number=0 pero workbook Is Nothing). Si solo chequeamos
' Err.Number caemos en falso positivo: el script continúa con un workbook
' fantasma y todas las llamadas posteriores fallan con 424.
Dim openAttempts, openErrNum, openErrDesc, openSuccess
openAttempts = 0
openSuccess = False
openErrNum = 0
openErrDesc = ""

Do
    openAttempts = openAttempts + 1

    ' Crear (o recrear) Excel.Application para este intento
    Err.Clear
    Set excel = CreateObject("Excel.Application")
    If Err.Number <> 0 Then
        openErrNum = Err.Number
        openErrDesc = "CreateObject(Excel.Application) falló: " & Err.Description
        Log "ERROR intento " & openAttempts & " | " & openErrDesc
        ' Sleep + check de salida se hacen al final del loop, comunes a ambas ramas.
    Else
        Log "Excel.Application creado intento " & openAttempts & " | versión=" & excel.Version
        excel.Visible = False
        excel.DisplayAlerts = False
        excel.AskToUpdateLinks = False
        excel.EnableEvents = False
        Set workbooks = excel.Workbooks

        Err.Clear
        Set workbook = Nothing
        Set workbook = workbooks.Open(filePath, False, False)
        If Err.Number = 0 And Not workbook Is Nothing Then
            openSuccess = True
            Exit Do
        End If

        openErrNum = Err.Number
        If Err.Number = 0 Then
            openErrDesc = "Workbooks.Open devolvió Nothing sin error COM"
        Else
            openErrDesc = Err.Description
            If openErrDesc = "" Then
                ' RPC_E_SERVERFAULT (-2147418111) y otros HRESULT no traen descripción.
                openErrDesc = "(Err.Number=" & openErrNum & " sin descripción — posible Excel COM corrupto)"
            End If
        End If
        Log "ERROR Workbooks.Open intento " & openAttempts & " | Err.Number=" & openErrNum & _
            " | Err.Description=" & openErrDesc

        ' Reciclar Excel para el próximo intento: una vez que cayó en RPC_E_SERVERFAULT
        ' todas las llamadas subsiguientes fallan igual. Quit + Set = Nothing libera el
        ' COM server y la siguiente iteración crea uno fresco.
        On Error Resume Next
        Set workbooks = Nothing
        excel.Quit
        Set excel = Nothing
        Err.Clear
    End If

    If openAttempts >= 5 Then Exit Do
    WScript.Sleep 3000
Loop

If Not openSuccess Then
    Log "FAIL: Workbooks.Open agotó " & openAttempts & " reintentos (último: " & openErrDesc & ")"
    WScript.StdErr.WriteLine "No se pudo abrir el archivo tras " & openAttempts & " intentos: " & openErrDesc
    On Error Resume Next
    Set workbooks = Nothing
    If Not excel Is Nothing Then excel.Quit
    Set excel = Nothing
    WScript.Quit 1
End If
Log "Workbook abierto OK"

Err.Clear

' Deshabilitar background queries para que RefreshAll sea sincrónico
On Error Resume Next
Set connections = workbook.Connections
Log "Conexiones encontradas: " & connections.Count
Dim connIdx
connIdx = 0
For Each conn In connections
    connIdx = connIdx + 1
    Log "  conexión #" & connIdx & " name=" & conn.Name & " type=" & conn.Type
    If conn.Type = 1 Then  ' xlConnectionTypeOLEDB
        Set oledbConn = conn.OLEDBConnection
        oledbConn.BackgroundQuery = False
        LogErr "  set BackgroundQuery=False (OLEDB) en " & conn.Name
        Set oledbConn = Nothing
    ElseIf conn.Type = 2 Then  ' xlConnectionTypeODBC
        Set odbcConn = conn.ODBCConnection
        odbcConn.BackgroundQuery = False
        LogErr "  set BackgroundQuery=False (ODBC) en " & conn.Name
        Set odbcConn = Nothing
    End If
    Err.Clear
Next
Set conn = Nothing
Set connections = Nothing

' RefreshAll: refresca conexiones, queries y pivots.
' Si falla, abortar SIN guardar: continuar al Save dejaría un xlsx con pivots
' parcialmente refrescados y el usuario vería el error DAX al abrirlo.
Log "Ejecutando RefreshAll..."
workbook.RefreshAll
If Err.Number <> 0 Then
    Dim refreshAllErrDesc
    refreshAllErrDesc = Err.Description
    LogErr "RefreshAll"
    WScript.StdErr.WriteLine "Error en RefreshAll: " & refreshAllErrDesc
    workbook.Close False
    Set workbook = Nothing
    Set workbooks = Nothing
    excel.Quit
    Set excel = Nothing
    Log "FAIL: RefreshAll falló, abortando sin guardar | " & refreshAllErrDesc
    WScript.Quit 1
End If
Log "RefreshAll OK"

' Esperar a que TODAS las queries async terminen (correcto para modelos grandes)
Log "CalculateUntilAsyncQueriesDone (post-RefreshAll)..."
excel.CalculateUntilAsyncQueriesDone
LogErr "CalculateUntilAsyncQueriesDone (post-RefreshAll)"
Err.Clear

' Reconstruir explícitamente el modelo de datos VertiPaq (Power Pivot).
' RefreshAll por sí solo no garantiza la reconstrucción del cache binario del modelo,
' lo que deja medidas DAX (M_YTD MODELO, M_MAESTRA_ACUM, etc.) con error de dependencia
' cuando el tamaño de la tabla fuente cambia respecto al cache previo.
Dim modelRefreshed
modelRefreshed = False
Set model = workbook.Model
If Not model Is Nothing Then
    Log "Workbook.Model presente, ejecutando Model.Refresh..."
    model.Refresh
    If Err.Number <> 0 Then
        ' Model.Refresh es lo que reconstruye el cache binario VertiPaq.
        ' Si falla acá y seguimos al Save, las medidas DAX (M_YTD MODELO,
        ' M_MAESTRA_ACUM, etc.) quedan apuntando a un cache stale y el usuario
        ' ve el error de dependencia al abrir el reporte. Abortar es preferible.
        Dim modelErrDesc
        modelErrDesc = Err.Description
        LogErr "Model.Refresh"
        WScript.StdErr.WriteLine "Error en Model.Refresh: " & modelErrDesc
        Set model = Nothing
        workbook.Close False
        Set workbook = Nothing
        Set workbooks = Nothing
        excel.Quit
        Set excel = Nothing
        Log "FAIL: Model.Refresh falló, abortando sin guardar (VertiPaq stale) | " & modelErrDesc
        WScript.Quit 1
    End If
    modelRefreshed = True
    Log "Model.Refresh OK"
Else
    Log "Workbook.Model es Nothing (sin modelo de datos VertiPaq)"
End If
Set model = Nothing

' Esperar nuevamente a que el modelo termine de reconstruirse
Log "CalculateUntilAsyncQueriesDone (post-Model.Refresh)..."
excel.CalculateUntilAsyncQueriesDone
LogErr "CalculateUntilAsyncQueriesDone (post-Model.Refresh)"
Err.Clear

' Forzar cálculo completo de todas las fórmulas antes de guardar
Log "CalculateFullRebuild..."
excel.CalculateFullRebuild
LogErr "CalculateFullRebuild"
Err.Clear

' Re-ocultar hojas de datos que POI marcó como hidden
' (Excel COM puede resetear la visibilidad al hacer RefreshAll/Save)
Dim sheetNames, i
sheetNames = Array("FACT", "Calendario", "Total_Empresa", "DIM", "Hoja 1")
Set sheets = workbook.Sheets
For Each i In sheetNames
    On Error Resume Next
    Set ws = sheets(i)
    If Err.Number = 0 Then
        ws.Visible = 0  ' xlSheetHidden
    End If
    Set ws = Nothing
    Err.Clear
Next
Set sheets = Nothing
Log "Hojas de datos re-ocultadas"

' Guardar
Log "Workbook.Save..."
workbook.Save
If Err.Number <> 0 Then
    LogErr "Workbook.Save"
    WScript.StdErr.WriteLine "Error al guardar: " & Err.Description
    workbook.Close False
    Set workbook = Nothing
    Set workbooks = Nothing
    excel.Quit
    Set excel = Nothing
    WScript.Quit 1
End If
Log "Workbook.Save OK"

' Limpiar. Orden importante: liberar workbook ANTES de quit, y workbooks (colección)
' después del Close pero antes del Quit. Set = Nothing en orden inverso al uso ayuda
' a que EXCEL.EXE termine realmente en lugar de quedar como proceso zombi.
workbook.Close False
Set workbook = Nothing
Set workbooks = Nothing
excel.Quit
Set excel = Nothing
Log "Excel.Quit ejecutado"

If modelRefreshed Then
    Log "END OK (Model.Refresh ejecutado)"
    WScript.StdOut.WriteLine "OK (Model.Refresh ejecutado)"
Else
    Log "END OK (sin modelo de datos, solo RefreshAll)"
    WScript.StdOut.WriteLine "OK (sin modelo de datos, solo RefreshAll)"
End If
WScript.Quit 0
